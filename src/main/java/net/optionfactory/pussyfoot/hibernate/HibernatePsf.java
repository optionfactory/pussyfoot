package net.optionfactory.pussyfoot.hibernate;

import net.optionfactory.pussyfoot.hibernate.builders.SorterBuilder;
import net.optionfactory.pussyfoot.hibernate.builders.ComparatorFilterBuilder;
import net.optionfactory.pussyfoot.hibernate.builders.EqualFilterBuilder;
import net.optionfactory.pussyfoot.hibernate.builders.LikeFilterBuilder;
import net.optionfactory.pussyfoot.hibernate.builders.DateInFilterBuilder;
import net.optionfactory.pussyfoot.hibernate.builders.InFilterBuilder;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import net.optionfactory.pussyfoot.Psf;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.SingularAttribute;
import net.emaze.dysfunctional.tuples.Pair;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;
import net.optionfactory.pussyfoot.SliceRequest;
import net.optionfactory.pussyfoot.SortRequest;
import net.optionfactory.pussyfoot.extjs.Operator;
import net.optionfactory.pussyfoot.extjs.Comparator;
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
    private final boolean useCountDistinct;
    private final Optional<Consumer<Root<TRoot>>> rootEnhancer;
    private final ConcurrentMap<Pair<String, Class<? extends Object>>, FilterRequestProcessor<TRoot, ?, ?>> availableFilters;
    private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> availableSorters;
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers;

    public HibernatePsf(
            SessionFactory hibernate,
            Class<TRoot> klass,
            boolean useCountDistinct,
            Optional<Consumer<Root<TRoot>>> rootEnhancer,
            ConcurrentMap<Pair<String, Class<? extends Object>>, FilterRequestProcessor<TRoot, ?, ?>> availableFilters,
            ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> availableSorters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers
    ) {
        this.hibernate = hibernate;
        this.klass = klass;
        this.useCountDistinct = useCountDistinct;
        this.rootEnhancer = rootEnhancer;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
        this.reducers = reducers;
    }

    private <T> Predicate predicateForNameAndValue(String name, T value, CriteriaBuilder cb, Root<TRoot> r) {
        final PredicateBuilder<TRoot, T> filter = (PredicateBuilder<TRoot, T>) availableFilters.get(name);
        return filter.predicateFor(cb, r, value);
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
            final List<Predicate> predicates = Stream.of(request.filters)
                    .filter(filterRequest -> {
                        final Pair<String, Class<? extends Object>> key = Pair.of(filterRequest.name, filterRequest.value.getClass());
                        return availableFilters.containsKey(key);
                    }).map(filterRequest -> {
                return predicateForNameAndValue(filterRequest.name, filterRequest.value, cb, countRoot);
            }).collect(Collectors.toList());
        ccq.where(cb.and(predicates.toArray(new Predicate[0])));
        final Query<Tuple> countQuery = session.createQuery(ccq);
        final Tuple countResult = countQuery.getSingleResult();
        final Long total = countResult.get("psfcount", Long.class);
        final Map<String, Object> reductions = countResult.getElements().stream()
                .filter(e -> reducers.containsKey(e.getAlias()))
                .collect(Collectors.toMap(t -> t.getAlias(), t -> countResult.get(t.getAlias())));

        final CriteriaQuery<Tuple> scq = cb.createTupleQuery();
        final Root<TRoot> sliceRoot = scq.from(klass);
        rootEnhancer.ifPresent(re -> re.accept(sliceRoot));
        final List<Selection<?>> selectors = new ArrayList<>();
        selectors.add(sliceRoot);
        final List<Order> orderers = new ArrayList<>();
        orderers.addAll(Stream.of(request.sorters)
                .filter(s -> availableSorters.containsKey(s.name))
                .flatMap(s -> {
                    return availableSorters.get(s.name).stream().map(cnsmr -> {
                        final SorterContext r = cnsmr.apply(cb, sliceRoot);
                        r.additionalSelection.ifPresent(sel -> selectors.add(sel));
                        r.groupers.ifPresent(g -> scq.groupBy(g));
                        return s.direction == SortRequest.Direction.ASC ? cb.asc(r.sortExpression) : cb.desc(r.sortExpression);
                    });
                }).collect(Collectors.toList())
        );

        scq.select(cb.tuple(selectors.toArray(new Selection<?>[0])));
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
        return PageResponse.of(total, slice, reductions);
    }

    /**
     * A builder for HibernatePsf.
     */
    public static class Builder<TRoot> {

        private boolean useCountDistinct = false;
    private final ConcurrentMap<Pair<String, Class<? extends Object>>, FilterRequestProcessor<TRoot, ?, ?>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> sorters = new ConcurrentHashMap<>();
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
        public SorterBuilder<TRoot> withSorter(String sorterName) {
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
        public Builder<TRoot> withSorter(String sorterName, BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext> sorterContextBuilder) {
            this.sorters.putIfAbsent(sorterName, new ArrayList<>());
            this.sorters.get(sorterName).add(sorterContextBuilder);
            return this;
        }

        public Builder<TRoot> withFilterRequestProcessor(FilterRequestProcessor<TRoot, ?, ?> processor) {
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
         * Allows to apply side effects on the query's {@link Root}. Main use
         * case is to specify eager joins
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
        public HibernatePsf build(Class<TRoot> clazz, SessionFactory hibernate) {
            return new HibernatePsf(hibernate, clazz, useCountDistinct, rootEnhancer, filters, sorters, reducers);
        }
    }
}
