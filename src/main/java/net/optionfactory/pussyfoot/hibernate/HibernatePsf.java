package net.optionfactory.pussyfoot.hibernate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import net.optionfactory.pussyfoot.Psf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import net.emaze.dysfunctional.contracts.dbc;
import net.emaze.dysfunctional.tuples.Pair;
import net.emaze.dysfunctional.tuples.Triple;
import net.optionfactory.pussyfoot.AbsolutePageRequest;
import net.optionfactory.pussyfoot.AbsolutePageResponse;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;
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

        final CriteriaQuery<Tuple> ccq = cb.createTupleQuery();
        final Root<TRoot> countRoot = ccq.from(klass);
        final List<Selection<?>> countSelectors = new ArrayList<>();
        countSelectors.add((this.useCountDistinct ? cb.countDistinct(countRoot) : cb.count(countRoot)).alias("psfcount"));
        reducers.entrySet().stream()
                .map(e -> e.getValue().apply(cb, countRoot).alias(e.getKey()))
                .collect(Collectors.toCollection(() -> countSelectors));
        ccq.select(cb.tuple(countSelectors.toArray(new Selection<?>[0])));
        List<Predicate> predicates = requestToPredicates(request.filters, ccq, cb, countRoot);
        ccq.where(cb.and(predicates.toArray(new Predicate[0])));
        final Query<Tuple> countQuery = session.createQuery(ccq);
        final Tuple countResult = countQuery.getSingleResult();
        final Long total = countResult.get("psfcount", Long.class);
        final Map<String, Object> reductions = countResult.getElements().stream()
                .filter(e -> reducers.containsKey(e.getAlias()))
                .collect(Collectors.toMap(t -> t.getAlias(), t -> countResult.get(t.getAlias())));

        final List<TRoot> slice = executeSlice(cb, request, session);

        return PageResponse.of(total, slice, reductions);
    }

    @Override
    public PageResponse<TRoot> queryForPageInfiniteScrolling(PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();

        final List<TRoot> slice = executeSlice(cb, request, session);
        final boolean moreRecordsLikelyPresent = slice.size() < request.slice.limit;
        long totalRecords = moreRecordsLikelyPresent ? request.slice.start + slice.size() : request.slice.start + slice.size() + 1;
        return PageResponse.of(totalRecords, slice, Collections.EMPTY_MAP);
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
        final List<Predicate> predicates = requestToPredicates(request.filters, scq, cb, sliceRoot);
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

    private List<Predicate> requestToPredicates(FilterRequest<?>[] filterRequests, final CriteriaQuery<Tuple> ccq, final CriteriaBuilder cb, final Root<TRoot> root) {
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

    public static SortRequest.Direction determineEffectiveDirection(SortRequest.Direction requestDirection, Optional<PagingDirection> pagingDirection) {
        final boolean isPreviousPageToken = pagingDirection.map(dir -> dir == PagingDirection.PreviousPage).orElse(false);
        final boolean isNextPageToken = pagingDirection.map(dir -> dir == PagingDirection.NextPage).orElse(true);
        return (requestDirection == SortRequest.Direction.ASC && isNextPageToken)
                || (requestDirection == SortRequest.Direction.DESC && isPreviousPageToken)
                        ? SortRequest.Direction.ASC
                        : SortRequest.Direction.DESC;
    }

    @Override
    public AbsolutePageResponse<TRoot> queryForRelativePage(AbsolutePageRequest request, ObjectMapper mapper) throws JsonProcessingException {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();

        final CriteriaQuery<Tuple> scq = cb.createTupleQuery();
        final Root<TRoot> sliceRoot = scq.from(klass);
        rootEnhancer.ifPresent(re -> re.accept(sliceRoot));
        final List<Selection<?>> selectors = new ArrayList<>();
        selectors.add(sliceRoot);
        final List<Predicate> predicates = new ArrayList<>();
        final Optional<PageToken> pageToken;
        if (request.slice.reference.isPresent()) {
            try {
                final PageToken token = mapper.readValue(Base64.getDecoder().decode(request.slice.reference.get()), PageToken.class);
                pageToken = Optional.of(token);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            pageToken = Optional.empty();
        }
        final boolean isPreviousPageToken = pageToken.map(token -> token.direction == PagingDirection.PreviousPage).orElse(false);
        final boolean isNextPageToken = pageToken.map(token -> token.direction == PagingDirection.NextPage).orElse(false);

        pageToken.ifPresent(token -> {
            AtomicInteger tokenIndex = new AtomicInteger(0);
            final List<Triple<Expression, SortRequest.Direction, Object>> columnDirectionAndValue = Stream.of(request.sorters)
                    .filter(s -> availableSorters.containsKey(s.name))
                    .flatMap(s -> {
                        return availableSorters.get(s.name).stream().map(sortExpressionResolver -> {
                            dbc.state(tokenIndex.get() < token.columnValues.size() - 1, "Invalid token index");
                            final Expression expr = sortExpressionResolver.resolve(scq, cb, sliceRoot);
                            Object val = token.columnValues.get(tokenIndex.getAndIncrement());
                            return Triple.of(expr, s.direction, val);
                        });
                    }).collect(Collectors.toList());
            columnDirectionAndValue.add(Triple.of(this.uniqueKeyFinder.resolve(scq, cb, sliceRoot), SortRequest.Direction.ASC, token.columnValues.get(token.columnValues.size() - 1)));

            predicates.add(convertToPredicate(token.direction, cb, columnDirectionAndValue.iterator()));
        });
        predicates.addAll(requestToPredicates(request.filters, scq, cb, sliceRoot));

        final List<Order> orderers = new ArrayList<>();
        orderers.addAll(Stream.of(request.sorters)
                .filter(s -> availableSorters.containsKey(s.name))
                .flatMap(s -> {
                    return availableSorters.get(s.name).stream().map(sortExpressionResolver -> {
                        final Expression<?> sortExpression = sortExpressionResolver.resolve(scq, cb, sliceRoot);
                        selectors.add(sortExpression);

                        return determineEffectiveDirection(s.direction, pageToken.map(pt -> pt.direction)) == SortRequest.Direction.ASC
                                ? cb.asc(sortExpression)
                                : cb.desc(sortExpression);
                    });
                }).collect(Collectors.toList())
        );
        final Expression<?> resolvedUnique = this.uniqueKeyFinder.resolve(scq, cb, sliceRoot);
        orderers.add(
                determineEffectiveDirection(SortRequest.Direction.ASC, pageToken.map(pt -> pt.direction)) == SortRequest.Direction.ASC
                ? cb.asc(resolvedUnique)
                : cb.desc(resolvedUnique)
        );
        selectors.add(resolvedUnique);
        scq.select(cb.tuple(selectors.toArray(new Selection<?>[0])));

        scq.where(cb.and(predicates.toArray(new Predicate[0])));
        scq.orderBy(orderers);
        final Query<Tuple> sliceQuery = session.createQuery(scq);
        if (request.slice.limit != SliceRequest.UNLIMITED) {
            sliceQuery.setMaxResults(request.slice.limit + 1);//we get an extra record to check whether there is a next page
        }

        final List<Tuple> queryResults = sliceQuery.getResultList();
        final boolean thereIsAnotherPage = queryResults.size() == request.slice.limit + 1;
        final List<Tuple> tuplesInPage = queryResults.subList(0, thereIsAnotherPage ? queryResults.size() - 1 : queryResults.size());
        if (isPreviousPageToken) {
            Collections.reverse(tuplesInPage);
        }
        final List<TRoot> slice = tuplesInPage
                .stream()
                .map(tuple -> tuple.get(0, klass))
                .collect(Collectors.toList());
        final PageToken prevPageToken = new PageToken(PagingDirection.PreviousPage, new ArrayList<>());
        final PageToken nextPageToken = new PageToken(PagingDirection.NextPage, new ArrayList<>());
        if (tuplesInPage.size() > 0) {
            final Tuple firstTupleInPage = tuplesInPage.get(0);
            final Tuple lastTupleInPage = tuplesInPage.get(tuplesInPage.size() - 1);
            for (int i = 1; i != firstTupleInPage.getElements().size(); ++i) {
                prevPageToken.columnValues.add(firstTupleInPage.get(i));
                nextPageToken.columnValues.add(lastTupleInPage.get(i));
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
        final boolean thereIsAPreviousPage = (isPreviousPageToken && thereIsAnotherPage) || isNextPageToken;
        /**
         * Similarly to the previous page case above. You know that you have
         * another page if:
         *
         * 1) You have no token, or a token for the NextPage, and there are
         * enough records
         *
         * 2) You have a "previous page" token - which you could only obtain by
         * a "next page"
         */
        final boolean thereIsANextPage = ((!pageToken.isPresent() || isNextPageToken) && thereIsAnotherPage) || isPreviousPageToken;

        return new AbsolutePageResponse<>(slice,
                thereIsAPreviousPage ? Optional.ofNullable(encodeToken(mapper, prevPageToken)) : Optional.empty(),
                thereIsANextPage ? Optional.ofNullable(encodeToken(mapper, nextPageToken)) : Optional.empty()
        );
    }

    private static String encodeToken(ObjectMapper mapper, final PageToken prevPageToken) throws JsonProcessingException {
        return Base64.getEncoder().encodeToString(mapper.writeValueAsString(prevPageToken).getBytes());
    }

    private static <Y extends Comparable<? super Y>> Predicate gt(CriteriaBuilder cb, Expression expression, Object value) {
        return cb.greaterThan(expression, (Y) value);
    }

    private static <Y extends Comparable<? super Y>> Predicate lt(CriteriaBuilder cb, Expression expression, Object value) {
        return cb.lessThan(expression, (Y) value);
    }

    private Predicate convertToPredicate(PagingDirection direction, CriteriaBuilder cb, Iterator<Triple<Expression, SortRequest.Direction, Object>> iterator) {
        if (!iterator.hasNext()) {
            return cb.disjunction();
        }
        final Triple<Expression, SortRequest.Direction, Object> current = iterator.next();
        return cb.or(
                determineEffectiveDirection(current.second(), Optional.of(direction)) == SortRequest.Direction.ASC
                /**/ ? gt(cb, current.first(), current.third())
                /**/ : lt(cb, current.first(), current.third()),
                cb.and(
                        cb.equal(current.first(), current.third()),
                        convertToPredicate(direction, cb, iterator)
                )
        );
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
         * @param sorterContextBuilder The {@link SorterContext} containing all
         * the information necessary to apply the desired sorting to the query
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
         * @param rootEnhancer
         * @return
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
         * @param hibernate Hibernate's {@link SessionFactory}
         * @return
         */
        public HibernatePsf build(Class<TRoot> clazz, ExpressionResolver<TRoot, ?> uniqueKeyFinder, SessionFactory hibernate) {
            return new HibernatePsf(hibernate, clazz, uniqueKeyFinder, useCountDistinct, rootEnhancer, filters, sorters, reducers);
        }
    }
}
