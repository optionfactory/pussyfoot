package net.optionfactory.pussyfoot.hibernate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import net.optionfactory.pussyfoot.Psf;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import net.optionfactory.pussyfoot.AbsolutePageRequest;
import net.optionfactory.pussyfoot.RelativePageResponse;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;
import net.optionfactory.pussyfoot.Pair;
import net.optionfactory.pussyfoot.SliceRequest;
import net.optionfactory.pussyfoot.SortRequest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/**
 * Hibernate implementation of the pagination, sorting, filtering API.
 *
 * @param <TRoot> The type of the root class to query from
 */
public class HibernatePsf<TRoot> implements Psf<TRoot> {

    private final SessionFactory hibernate;
    private final Class<TRoot> klass;
    private final ExpressionResolver<TRoot, ?> uniqueKeyFinder;
    private final boolean useCountDistinct;
    private final Optional<Consumer<Root<TRoot>>> rootEnhancer;
    private final ConcurrentMap<Pair<String, Class<? extends Object>>, FilterRequestProcessor<TRoot, ?, ?>> availableFilters;
    private final ConcurrentMap<String, List<ExpressionResolver<TRoot, ?>>> availableSorters;
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers;

    public HibernatePsf(
            SessionFactory hibernate,
            Class<TRoot> klass,
            ExpressionResolver<TRoot, ?> uniqueKeyFinder,
            boolean useCountDistinct,
            Optional<Consumer<Root<TRoot>>> rootEnhancer,
            ConcurrentMap<Pair<String, Class<? extends Object>>, FilterRequestProcessor<TRoot, ?, ?>> availableFilters,
            ConcurrentMap<String, List<ExpressionResolver<TRoot, ?>>> availableSorters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers
    ) {
        this.hibernate = hibernate;
        this.klass = klass;
        this.uniqueKeyFinder = uniqueKeyFinder;
        this.useCountDistinct = useCountDistinct;
        this.rootEnhancer = rootEnhancer;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
        this.reducers = reducers;
    }

    @Override
    public PageResponse<TRoot> queryForPage(PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();

        final Pair<Long, Map<String, Object>> countAndReductions = executeCount(cb, request, session);

        final List<TRoot> slice = executeSlice(cb, request, session);

        return PageResponse.of(countAndReductions.first(), slice, countAndReductions.second());
    }

    @Override
    public Pair<Long, Map<String, Object>> countAndReductions(PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();

        return executeCount(cb, request, session);
    }

    @Override
    public PageResponse<TRoot> queryForPageInfiniteScrolling(PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();
        final SliceRequest requestOneRecordMoreExternalRequest = request.slice.limit == SliceRequest.UNLIMITED
                ? request.slice
                : SliceRequest.of(request.slice.start, request.slice.limit + 1);
        final List<TRoot> slice = executeSlice(
                cb,
                PageRequest.builder(request)
                        .withSlice(requestOneRecordMoreExternalRequest)
                        .build(),
                session);
        final boolean moreRecordsLikelyPresent = (request.slice.limit != SliceRequest.UNLIMITED) && (slice.size() > request.slice.limit);
        long totalRecords = request.slice.start + slice.size();
        return PageResponse.of(totalRecords, slice.subList(0, moreRecordsLikelyPresent ? slice.size() - 1 : slice.size()), Collections.EMPTY_MAP);
    }

    private Pair<Long, Map<String, Object>> executeCount(final CriteriaBuilder cb, PageRequest request, final Session session) {
        final CriteriaQuery<Tuple> ccq = cb.createTupleQuery();
        final Root<TRoot> countRoot = ccq.from(klass);
        rootEnhancer.ifPresent(re -> re.accept(countRoot));
        final List<Selection<?>> countSelectors = new ArrayList<>();
        countSelectors.add((this.useCountDistinct ? cb.countDistinct(countRoot) : cb.count(countRoot)).alias("psfcount"));
        reducers.entrySet().stream()
                .map(e -> e.getValue().apply(cb, countRoot).alias(e.getKey()))
                .collect(Collectors.toCollection(() -> countSelectors));
        ccq.select(cb.tuple(countSelectors.toArray(new Selection<?>[0])));
        List<Predicate> predicates = filterRequestsToPredicates(request.filters, ccq, cb, countRoot);
        ccq.where(cb.and(predicates.toArray(new Predicate[0])));
        final Query<Tuple> countQuery = session.createQuery(ccq);
        final Tuple countResult = countQuery.getSingleResult();
        final Long total = countResult.get("psfcount", Long.class);
        final Map<String, Object> reductions = countResult.getElements().stream()
                .filter(e -> reducers.containsKey(e.getAlias()))
                .collect(Collectors.toMap(t -> t.getAlias(), t -> countResult.get(t.getAlias())));
        return Pair.of(total, reductions);
    }

    private List<TRoot> executeSlice(final CriteriaBuilder cb, PageRequest request, final Session session) {
        final CriteriaQuery<Tuple> scq = cb.createTupleQuery();
        final Root<TRoot> sliceRoot = scq.from(klass);
        rootEnhancer.ifPresent(re -> re.accept(sliceRoot));
        final List<Selection<?>> selectors = new ArrayList<>();
        selectors.add(sliceRoot);
        final List<Order> orderers = new ArrayList<>();
        orderers.addAll(Stream.of(request.sorters)
                .filter(s -> availableSorters.containsKey(s.name))
                .flatMap(s -> {
                    return availableSorters.get(s.name).stream().map(sorterExpressionResolver -> {
                        final Expression<?> sortExpression = sorterExpressionResolver.resolve(scq, cb, sliceRoot);
                        return s.direction == SortRequest.Direction.ASC ? cb.asc(sortExpression) : cb.desc(sortExpression);
                    });
                }).collect(Collectors.toList())
        );
        scq.select(cb.tuple(selectors.toArray(new Selection<?>[0])));
        final List<Predicate> predicates = filterRequestsToPredicates(request.filters, scq, cb, sliceRoot);
        scq.where(cb.and(predicates.toArray(new Predicate[0])));
        scq.orderBy(orderers);
        final Query<Tuple> sliceQuery = session.createQuery(scq);
        sliceQuery.setFirstResult(request.slice.start);
        if (request.slice.limit != SliceRequest.UNLIMITED) {
            sliceQuery.setMaxResults(request.slice.limit);
        }
        final List<TRoot> slice = sliceQuery.getResultList()
                .stream()
                .map(tuple -> tuple.get(0, klass))
                .collect(Collectors.toList());
        return slice;
    }

    private List<Predicate> filterRequestsToPredicates(FilterRequest<?>[] filterRequests, final CriteriaQuery<Tuple> ccq, final CriteriaBuilder cb, final Root<TRoot> root) {
        final List<Predicate> predicates = Stream.of(filterRequests)
                .filter(filterRequest -> {
                    final Pair<String, Class<? extends Object>> key = Pair.of(filterRequest.name, filterRequest.value.getClass());
                    return availableFilters.containsKey(key);
                }).map(filterRequest -> {
            return predicateForNameAndValue(filterRequest.name, filterRequest.value, ccq, cb, root);
        }).collect(Collectors.toList());
        return predicates;
    }

    private <T> Predicate predicateForNameAndValue(String name, T value, final CriteriaQuery<Tuple> query, CriteriaBuilder cb, Root<TRoot> r) {
        final Pair<String, Class<? extends Object>> filterKey = Pair.of(name, value.getClass());
        final FilterRequestProcessor<TRoot, T, Object> filter = (FilterRequestProcessor<TRoot, T, Object>) availableFilters.get(filterKey);
        final Object filterFinalValue = filter.filterValueAdapter.apply(value);
        return filter.filterExecutor.predicateFor(query, cb, r, filterFinalValue);
    }

    @Override
    public RelativePageResponse<TRoot> queryForRelativePage(AbsolutePageRequest request, ObjectMapper mapper) throws JsonProcessingException {
        final Optional<PageToken> pageToken = request.slice.reference.map(t -> decodeToken(mapper, t));

        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();

        final CriteriaQuery<Tuple> scq = cb.createTupleQuery();
        final Root<TRoot> sliceRoot = scq.from(klass);
        rootEnhancer.ifPresent(re -> re.accept(sliceRoot));
        final List<Selection<?>> selectors = new ArrayList<>();
        selectors.add(sliceRoot);
        final List<Predicate> predicates = new ArrayList<>();

        List<Pair<Expression<?>, SortRequest.Direction>> sortersAndDirection = extractEffectiveSorters(request, scq, cb, sliceRoot, pageToken);
        selectors.addAll(sortersAndDirection
                .stream()
                .map(Pair::first)
                .collect(Collectors.toList()));

        final boolean isPreviousPageToken = pageToken.map(token -> token.direction == PagingDirection.PreviousPage).orElse(false);
        final boolean isNextPageToken = pageToken.map(token -> token.direction == PagingDirection.NextPage).orElse(false);

        final var sortColumnValues = pageToken.map(token -> token.columnValues).orElse(Collections.emptyList());

        final List<ExpressionDirectionAndValue> sortersAndDirectionAndValue = IntStream.range(0, sortersAndDirection.size())
                .mapToObj(i -> {
                    final var sad = sortersAndDirection.get(i);
                    if (i >= sortColumnValues.size()) {
                        return null;
                    }
                    return new ExpressionDirectionAndValue(sad.first(), sad.second(), sortColumnValues.get(i));

                })
                .filter(v -> v != null)
                .collect(Collectors.toList());

        predicates.add(convertToPredicate(cb, sortersAndDirectionAndValue.iterator()));
        predicates.addAll(filterRequestsToPredicates(request.filters, scq, cb, sliceRoot));

        final List<Order> orderers = sortersAndDirection
                .stream()
                .map(sorterAndDirection -> {
                    return sorterAndDirection.second() == SortRequest.Direction.ASC
                            ? cb.asc(sorterAndDirection.first())
                            : cb.desc(sorterAndDirection.first());
                }).collect(Collectors.toList());

        scq.select(cb.tuple(selectors.toArray(new Selection<?>[0])));
        scq.where(cb.and(predicates.toArray(new Predicate[0])));
        scq.orderBy(orderers);
        final Query<Tuple> sliceQuery = session.createQuery(scq);
        if (request.slice.limit != SliceRequest.UNLIMITED) {
            sliceQuery.setMaxResults(request.slice.limit + 1);//we get an extra record to check whether there is a next page
        }

        final List<Tuple> queryResults = sliceQuery.getResultList();
        final boolean isThereAnotherPage = queryResults.size() == request.slice.limit + 1;
        final List<Tuple> sliceTuples = queryResults.subList(0, isThereAnotherPage ? queryResults.size() - 1 : queryResults.size());
        if (isPreviousPageToken) {
            Collections.reverse(sliceTuples);
        }
        final List<TRoot> slice = sliceTuples
                .stream()
                .map(tuple -> tuple.get(0, klass))
                .collect(Collectors.toList());
        final PageToken prevPageToken = new PageToken(PagingDirection.PreviousPage, new ArrayList<>());
        final PageToken nextPageToken = new PageToken(PagingDirection.NextPage, new ArrayList<>());
        if (sliceTuples.size() > 0) {
            final Tuple firstSliceTuple = sliceTuples.get(0);
            final Tuple lastSliceTuple = sliceTuples.get(sliceTuples.size() - 1);
            for (int i = 1; i != firstSliceTuple.getElements().size(); ++i) {
                prevPageToken.columnValues.add(firstSliceTuple.get(i));
                nextPageToken.columnValues.add(lastSliceTuple.get(i));
            }
        }
        /**
         * There is a previous page if the current request was for a previous
         * page and there are still records, or if the request has a pageToken
         * The reasoning is that you are either:
         *
         * 1) no pageToken: you are on the first page possible - in this case
         * there is no previous page
         *
         * 2) with pageToken: you could only have this if you are asking for a
         * next page. In this case:
         *
         * 2a) The token was already for a previous page - in this case we check
         * if there are enought records to provide yet another page back
         *
         * 2b) The token was for a "next page" - in this case you are pretty
         * much guaranteed you have a previous page - the one you got this
         * reference token from!
         */
        final boolean thereIsAPreviousPage = (isPreviousPageToken && isThereAnotherPage) || isNextPageToken;
        /**
         * Similarly to case above. You know that you have another page if:
         *
         * 1) You have no token or a token for the NextPage, and there are
         * enough records for another page
         *
         * 2) You have a "previous page" token - which you could only obtain by
         * a "next page"
         */
        final boolean thereIsANextPage = ((!pageToken.isPresent() || isNextPageToken) && isThereAnotherPage) || isPreviousPageToken;

        return new RelativePageResponse<>(slice,
                thereIsAPreviousPage ? Optional.ofNullable(encodeToken(mapper, prevPageToken)) : Optional.empty(),
                thereIsANextPage ? Optional.ofNullable(encodeToken(mapper, nextPageToken)) : Optional.empty()
        );
    }

    private List<Pair<Expression<?>, SortRequest.Direction>> extractEffectiveSorters(AbsolutePageRequest request, final CriteriaQuery<Tuple> scq, final CriteriaBuilder cb, final Root<TRoot> sliceRoot, final Optional<PageToken> pageToken) {
        final List<Pair<Expression<?>, SortRequest.Direction>> sortersAndDirection = Stream.of(request.sorters)
                .filter(sortRequest -> availableSorters.containsKey(sortRequest.name))
                .flatMap(sortRequest -> {
                    return availableSorters.get(sortRequest.name)
                            .stream()
                            .map(sortExpressionResolver -> {
                                final Expression<?> sortExpression = sortExpressionResolver.resolve(scq, cb, sliceRoot);
                                return Pair.<Expression<?>, SortRequest.Direction>of(sortExpression, sortRequest.direction);
                            });
                }).collect(Collectors.toList());
        sortersAndDirection.add(Pair.<Expression<?>, SortRequest.Direction>of(
                this.uniqueKeyFinder.resolve(scq, cb, sliceRoot),
                SortRequest.Direction.ASC));
        return sortersAndDirection
                .stream()
                .map(pair -> Pair.<Expression<?>, SortRequest.Direction>of(pair.first(), determineEffectiveDirection(pair.second(), pageToken.map(token -> token.direction))))
                .collect(Collectors.toList());
    }

    public static SortRequest.Direction determineEffectiveDirection(SortRequest.Direction requestDirection, Optional<PagingDirection> pagingDirection) {
        final boolean isPreviousPageToken = pagingDirection.map(dir -> dir == PagingDirection.PreviousPage).orElse(false);
        final boolean isNextPageToken = pagingDirection.map(dir -> dir == PagingDirection.NextPage).orElse(true);
        return (requestDirection == SortRequest.Direction.ASC && isNextPageToken)
                || (requestDirection == SortRequest.Direction.DESC && isPreviousPageToken)
                        ? SortRequest.Direction.ASC
                        : SortRequest.Direction.DESC;
    }

    private static PageToken decodeToken(ObjectMapper mapper, final String reference) {
        try {
            return mapper.readValue(Base64.getDecoder().decode(reference), PageToken.class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String encodeToken(ObjectMapper mapper, final PageToken token) {
        try {
            return Base64.getEncoder().encodeToString(mapper.writeValueAsString(token).getBytes());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <Y extends Comparable<? super Y>> Predicate gt(CriteriaBuilder cb, Expression expression, Object value) {
        return cb.greaterThan(expression, (Y) value);
    }

    private static <Y extends Comparable<? super Y>> Predicate lt(CriteriaBuilder cb, Expression expression, Object value) {
        return cb.lessThan(expression, (Y) value);
    }

    private Predicate convertToPredicate(CriteriaBuilder cb, Iterator<ExpressionDirectionAndValue> iterator) {
        if (!iterator.hasNext()) {
            return cb.conjunction();
        }
        final ExpressionDirectionAndValue current = iterator.next();
        return cb.or(
                current.direction == SortRequest.Direction.ASC
                        /**/ ? gt(cb, current.expression, current.value)
                        /**/ : lt(cb, current.expression, current.value),
                cb.and(
                        cb.equal(current.expression, current.value),
                        convertToPredicate(cb, iterator)
                )
        );
    }

    private static class ExpressionDirectionAndValue {

        public final Expression expression;
        public final SortRequest.Direction direction;
        public final Object value;

        public ExpressionDirectionAndValue(Expression expression, SortRequest.Direction direction, Object value) {
            this.expression = expression;
            this.direction = direction;
            this.value = value;
        }

    }

    public static enum PagingDirection {
        NextPage, PreviousPage
    }

    public static class PageToken {

        public PagingDirection direction;
        public List<Object> columnValues;

        public PageToken() {
        }

        public PageToken(PagingDirection direction, List<Object> columnValues) {
            this.direction = direction;
            this.columnValues = columnValues;
        }

    }

    /**
     * A builder for HibernatePsf.
     *
     * @param <TRoot> The type of the root object to paginate, filter and sort
     * against
     */
    public static class Builder<TRoot> {

        private boolean useCountDistinct = false;
        private final ConcurrentMap<Pair<String, Class<?>>, FilterRequestProcessor<TRoot, ?, ?>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, List<ExpressionResolver<TRoot, ?>>> sorters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers = new ConcurrentHashMap<>();
        private Optional<Consumer<Root<TRoot>>> rootEnhancer = Optional.empty();

        /**
         * Enables the use of "count (distinct [...])" instead of "count([...])"
         * when querying for total rows count
         *
         * @return builder itself, in order to chain further filters/sorterers
         */
        public Builder<TRoot> useCountDistinct() {
            this.useCountDistinct = true;
            return this;
        }

        /**
         * Defines a potential sorter. The sorter will be applied only when a
         * {@link SortRequest} with a {@link SortRequest#name} matching this
         * sorter's name is found within a {@link PageRequest}
         *
         * @param sorterName The name of this sorter
         * @return a {@link SorterBuilder}, to specify against the columns to
         * use to sort
         */
        public SorterBuilder<TRoot> onSortRequest(String sorterName) {
            return new SorterBuilder<>(this, sorterName);
        }

        /**
         * Defines a potential sorter function. The sorter will be applied only
         * when a {@link SortRequest} with a {@link SortRequest#name} matching
         * this sorter's name is found within a {@link PageRequest}
         *
         * @param sorterName The name of this sorter
         * @param sorterExpressionResolver the expression to use to retrieve the
         * value to filter against
         * @return builder itself, in order to chain further filters/sorterers
         */
        public Builder<TRoot> withSorter(String sorterName, ExpressionResolver<TRoot, ?> sorterExpressionResolver) {
            this.sorters.putIfAbsent(sorterName, new ArrayList<>());
            this.sorters.get(sorterName).add(sorterExpressionResolver);
            return this;
        }

        public Builder<TRoot> withFilterRequestProcessor(FilterRequestProcessor processor) {
            this.filters.put(processor.filterRequestKey, processor);
            return this;
        }

        public <TFilterRawValue> FilterRequestProcessorPreBuilder<TRoot, TFilterRawValue, TFilterRawValue> onFilterRequest(String filterName, Class<TFilterRawValue> filterValueClass) {
            return new FilterRequestProcessorPreBuilder<>(this, Pair.of(filterName, filterValueClass));
        }

        public Builder<TRoot> withReducer(String name, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>> reduction) {
            reducers.put(name, reduction);
            return this;
        }

        /**
         * Allows to apply side effects usePath the query's {@link Root}. Main
         * use case is to specify eager joins
         *
         * @param rootEnhancer the root-enhancing function
         * @return builder itself, in order to chain further filters/sorterers
         */
        public Builder<TRoot> withRootEnhancer(Consumer<Root<TRoot>> rootEnhancer) {
            this.rootEnhancer = Optional.of(rootEnhancer);
            return this;
        }

        /**
         * Actually builds the {@link HibernatePsf} instance
         *
         * @param clazz the main class the query is built from, matches the type
         * of {@link Root}
         * @param uniqueKeyFinder The expression to use to retrieve a unique-key
         * value for the table
         * @param hibernate Hibernate's {@link SessionFactory}
         * @return The fully built {@link HibernatePsf} instance to be using for
         * querying
         */
        public HibernatePsf<TRoot> build(Class<TRoot> clazz, ExpressionResolver<TRoot, ?> uniqueKeyFinder, SessionFactory hibernate) {
            return new HibernatePsf<TRoot>(hibernate, clazz, uniqueKeyFinder, useCountDistinct, rootEnhancer, filters, sorters, reducers);
        }
    }
}
