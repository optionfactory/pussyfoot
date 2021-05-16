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
import java.util.function.BiConsumer;
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
    private final Optional<BiConsumer<CriteriaBuilder, Root<TRoot>>> rootEnhancer;
    private final Optional<BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> uniqueKeyResolver;
    private final ConcurrentMap<String, JpaFilter<TRoot, ?>> availableFilters;
    private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> availableSorters;
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers;

    public HibernatePsf(
            SessionFactory hibernate,
            Class<TRoot> klass,
            boolean useCountDistinct,
            Optional<BiConsumer<CriteriaBuilder, Root<TRoot>>> rootEnhancer,
            Optional<BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> uniqueKeyResolver,
            ConcurrentMap<String, JpaFilter<TRoot, ?>> availableFilters,
            ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> availableSorters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers
    ) {
        this.hibernate = hibernate;
        this.klass = klass;
        this.useCountDistinct = useCountDistinct;
        this.rootEnhancer = rootEnhancer;
        this.uniqueKeyResolver = uniqueKeyResolver;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
        this.reducers = reducers;
    }

    private <T> Predicate predicateForNameAndValue(String name, T value, CriteriaBuilder cb, Root<TRoot> r) {
        final JpaFilter<TRoot, T> filter = (JpaFilter<TRoot, T>) availableFilters.get(name);
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
        final List<Predicate> countPredicates = Stream.of(request.filters)
                .filter(filterRequest -> availableFilters.containsKey(filterRequest.name))
                .map(filterRequest -> {
                    return predicateForNameAndValue(filterRequest.name, filterRequest.value, cb, countRoot);
                }).collect(Collectors.toList());
        ccq.where(cb.and(countPredicates.toArray(new Predicate[0])));
        final Query<Tuple> countQuery = session.createQuery(ccq);
        final Tuple countResult = countQuery.getSingleResult();
        final Long total = countResult.get("psfcount", Long.class);
        final Map<String, Object> reductions = countResult.getElements().stream()
                .filter(e -> reducers.containsKey(e.getAlias()))
                .collect(Collectors.toMap(t -> t.getAlias(), t -> countResult.get(t.getAlias())));

        final CriteriaQuery<Tuple> scq = cb.createTupleQuery();
        final Root<TRoot> sliceRoot = scq.from(klass);
        rootEnhancer.ifPresent(re -> re.accept(cb, sliceRoot));
        final List<Selection<?>> selectors = new ArrayList<>();
        selectors.add(sliceRoot);
        if (this.useCountDistinct) {
            selectors.add(cb.countDistinct(countRoot));
            scq.groupBy(countRoot);
        }
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
        this.uniqueKeyResolver.ifPresent(keyResolver -> {
            orderers.add(cb.asc(keyResolver.apply(cb, sliceRoot)));
        });

        scq.select(cb.tuple(selectors.toArray(new Selection<?>[0])));
        final List<Predicate> slicePredicates = Stream.of(request.filters)
                .filter(filterRequest -> availableFilters.containsKey(filterRequest.name))
                .map(filterRequest -> {
                    return predicateForNameAndValue(filterRequest.name, filterRequest.value, cb, sliceRoot);
                }).collect(Collectors.toList());
        scq.where(cb.and(slicePredicates.toArray(new Predicate[0])));

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
        private final ConcurrentMap<String, JpaFilter<TRoot, ?>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> sorters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers = new ConcurrentHashMap<>();
        private Optional<BiConsumer<CriteriaBuilder, Root<TRoot>>> rootEnhancer = Optional.empty();
        private Optional<BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> uniqueKeyResolver = Optional.empty();

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

        public Builder<TRoot> withUniqueKey(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>> uniqueKeyResolver) {
            this.uniqueKeyResolver = Optional.ofNullable(uniqueKeyResolver);
            return this;
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

        /**
         * Defines a potential filter function. The filtering will be applied
         * only when a {@link FilterRequest} with a {@link FilterRequest#name}
         * matching this filter's name is found within a {@link PageRequest}
         *
         * @param filterName The name of this filter
         * @param filter The {@link JpaFilter} containing all the information
         * necessary to apply the desired filter to the query
         * @return builder itself, in order to chain further filters/sorterers
         */
        public <T> Builder<TRoot> withFilter(String filterName, JpaFilter<TRoot, T> filter) {
            filters.put(filterName, filter);
            return this;
        }

        /**
         * Defines an equality filter.
         *
         * <p>
         * SQL equivalent when applied:<br/>
         * ... where col = :value
         * </p>
         * <p>
         * This filter is applied when a {@link PageRequest} containing a
         * {@link FilterRequest} with the field {@link FilterRequest#name}
         * matching the name of this filter is applied.
         * </p>
         *
         * @param <TCol> The type of the column this filter applies to, and of
         * the value contained in the {@link PageRequest}'s
         * {@link FilterRequest}
         * @param filterName the name of the filter to be referenced in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @return a filter builder, to be used to specify the column(s) to
         * filter on
         * @see EqualFilterBuilder
         */
        public <TCol> EqualFilterBuilder<TRoot, TCol, TCol> withFilterEqual(String filterName) {
            return new EqualFilterBuilder<>(this, filterName, Function.identity());
        }

        /**
         * Defines an equality filter.
         *
         * <p>
         * SQL equivalent when applied:<br/>
         * ... where col = :value:
         * </p>
         * <p>
         * This filter is applied when a {@link PageRequest} containing a
         * {@link FilterRequest} with the field {@link FilterRequest#name}
         * matching the name of this filter is applied.
         * </p>
         *
         * @param <TRawVal> The type of the value contained in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param <TCol> The type of the column this filter applies to the value
         * contained in the {@link PageRequest}'s {@link FilterRequest}
         * @param filterName the name of the filter to be referenced in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param filterValueAdapter a function to adapt the filter value to the
         * column value
         * @return a filter builder, to be used to specify the column(s) to
         * filter on
         */
        public <TRawVal, TCol> EqualFilterBuilder<TRoot, TRawVal, TCol> withFilterEqual(String filterName, Function<TRawVal, TCol> filterValueAdapter) {
            return new EqualFilterBuilder<>(this, filterName, filterValueAdapter);
        }

        /**
         * Defines an partial, case-insensitive match filter on string columns.
         *
         * <p>
         * PSQL equivalent when applied:<br/>
         * ... where col ilike %:value:%
         * </p>
         * <p>
         * This filter is applied when a {@link PageRequest} containing a
         * {@link FilterRequest} with the field {@link FilterRequest#name}
         * matching the name of this filter is applied.
         * </p>
         *
         * @param filterName the name of the filter to be referenced in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @return a filter builder, to be used to specify the column(s) to
         * filter on
         */
        public LikeFilterBuilder<TRoot, String> withFilterLike(String filterName) {
            return new LikeFilterBuilder<>(this, filterName, Function.identity());
        }

        /**
         * Defines an partial, case-insensitive match filter on string columns.
         *
         * <p>
         * PSQL equivalent when applied:<br/>
         * ... where col ilike %:value:%
         * </p>
         * <p>
         * This filter is applied when a {@link PageRequest} containing a
         * {@link FilterRequest} with the field {@link FilterRequest#name}
         * matching the name of this filter is applied.
         * </p>
         *
         * @param <TRawVal> The type of the value contained in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param filterName the name of the filter to be referenced in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param filterValueAdapter a function to adapt the filter value to the
         * column value
         * @return a filter builder, to be used to specify the column(s) to
         * filter on
         */
        public <TRawVal> LikeFilterBuilder<TRoot, TRawVal> withFilterLike(String filterName, Function<TRawVal, String> filterValueAdapter) {
            return new LikeFilterBuilder<>(this, filterName, filterValueAdapter);
        }

        /**
         * Defines an {@link Operator}-based filter against a comparable value
         *
         * <p>
         * SQL equivalent when applied, depending on the
         * {@link Comparator#operator}:<br/>
         * ... where col OPERATOR :value:
         * </p>
         * <p>
         * This filter is applied when a {@link PageRequest} containing a
         * {@link FilterRequest} with the field {@link FilterRequest#name}
         * matching the name of this filter is applied.
         * </p>
         *
         * @param <TRawVal> The type of the value contained in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param <TCol> The type of the column this filter applies to the value
         * contained in the {@link PageRequest}'s {@link FilterRequest}
         * @param filterName the name of the filter to be referenced in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @return a filter builder, to be used to specify the column(s) to
         * filter on
         */
        public <TCol extends Comparable<TCol>> ComparatorFilterBuilder<TRoot, Comparator<TCol>, TCol> withFilterComparator(String filterName) {
            return new ComparatorFilterBuilder<>(this, filterName, Function.identity());
        }

        /**
         * A generalization of a
         * {@link #withFilterComparator(java.lang.String)}, which performs an
         * adaptation of the {@link FilterRequest#value} before applying the
         * comparation function
         *
         * @param filterValueAdapter a function to adapt the raw filter value to
         * final {@link Comparator}
         */
        public <TRawVal, TCol extends Comparable<TCol>> ComparatorFilterBuilder<TRoot, TRawVal, TCol> withFilterComparator(String filterName, Function<TRawVal, Comparator<TCol>> filterValueAdapter) {
            return new ComparatorFilterBuilder<>(this, filterName, filterValueAdapter);
        }

        /**
         * A generalization of a
         * {@link #withFilterComparator(java.lang.String)}, which performs a
         * two-step adaptation of the {@link FilterRequest#value} before
         * applying the comparation function. The main use case is to have a
         * first step of deserialization of an incoming
         * {@link FilterRequest#value} of type {@link String}, and then adapting
         * the deserialized {@link Comparator#value} to the column's value
         *
         * @param filterValueAdapter the first function to apply to adapt the
         * raw filter value to final {@link Comparator}
         * @param filterToColumnConverter the second function to apply (against
         * the result of the first function above) to adapt the raw filter value
         * to final {@link Comparator}
         */
        public <TRawVal, TComparatorValue, TCol extends Comparable<TCol>> ComparatorFilterBuilder<TRoot, TRawVal, TCol> withFilterComparator(String filterName, Function<TRawVal, Comparator<TComparatorValue>> filterValueAdapter, Function<TComparatorValue, TCol> filterToColumnConverter) {
            return new ComparatorFilterBuilder<>(this, filterName, v -> filterValueAdapter.apply(v).map(filterToColumnConverter));
        }

        /**
         * Defines a filter, similar to {@link #withFilterComparator}, that
         * given a {@link ZonedDateTime}'s value extracts beginning and and of
         * such day, and then applies a comparation function as follows:
         *
         * <p>
         * Given START_OF_DAY and END_OF_DAY, SQL equivalent when applied:
         * <ul>
         * <li>{@link Operator#gt}: ... where col &gt; END_OF_DAY</li>
         * <li>{@link Operator#gte}: ... where col &ge; START_OF_DAY</li>
         * <li>{@link Operator#lt}: ... where col &lt; START_OF_DAY</li>
         * <li>{@link Operator#lte}: ... where col &le; END_OF_DAY</li>
         * <li>{@link Operator#eq}: ... where col &ge; START_OF_DAY and col &le;
         * END_OF_DAY</li>
         * </ul>
         * </p>
         * <p>
         * This filter is applied when a {@link PageRequest} containing a
         * {@link FilterRequest} with the field {@link FilterRequest#name}
         * matching the name of this filter is applied.
         * </p>
         *
         * @param <TRawVal> The type of the value contained in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param <TCol> The type of the column this filter applies to the value
         * contained in the {@link PageRequest}'s {@link FilterRequest}
         * @param filterName the name of the filter to be referenced in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param filterValueAdapter a function to adapt the raw filter value to
         * final {@link Comparator}
         * @param filterToColumnConverter the second function to apply (against
         * the result of the first function above) to adapt the raw filter value
         * to final {@link Comparator}
         * @return a filter builder, to be used to specify the column(s) to
         * filter on
         */
        public <TRawVal, TCol extends Temporal & Comparable<TCol>> DateInFilterBuilder<TRoot, TRawVal, TCol> withFilterDateIn(String filterName, Function<TRawVal, Comparator<ZonedDateTime>> filterValueAdapter, Function<ZonedDateTime, TCol> filterToColumnConverter) {
            return new DateInFilterBuilder<>(this, filterName, filterValueAdapter, filterToColumnConverter);
        }

        /**
         * Same as
         * {@link #withFilterDateIn(java.lang.String, java.util.function.Function, java.util.function.Function)},
         * but assuming the filter value is already of type
         * {@link ZonedDateTime}
         *
         *
         * @param <TCol> The type of the column this filter applies to the value
         * contained in the {@link PageRequest}'s {@link FilterRequest}
         * @param filterName the name of the filter to be referenced in the
         * {@link PageRequest}'s {@link FilterRequest}
         * @param filterToColumnConverter the second function to apply (against
         * the result of the first function above) to adapt the
         * {@link ZonedDateTime} filter value to the column's value
         * @return a filter builder, to be used to specify the column(s) to
         * filter on
         */
        public <TRawVal, TCol extends Temporal & Comparable<TCol>> DateInFilterBuilder<TRoot, Comparator<ZonedDateTime>, TCol> withFilterDateIn(String filterName, Function<ZonedDateTime, TCol> filterToColumnConverter) {
            return new DateInFilterBuilder<>(this, filterName, Function.identity(), filterToColumnConverter);
        }

        /**
         * Defines an filter that matches a (calculated) column's value against
         * a list of possible values
         *
         * <p>
         * SQL equivalent when applied:<br/>
         *
         * ... where col IN (:val1:,:val2:, ... )
         * </p>
         *
         * @param <TCol> The type of the column this filter will apply to
         */
        public <TCol extends Comparable<TCol>> InFilterBuilder<TRoot, List<TCol>, TCol> withFilterIn(String filterName) {
            return new InFilterBuilder<>(this, filterName, Function.identity());
        }

        public <TRawVal, TCol extends Comparable<TCol>> InFilterBuilder<TRoot, TRawVal, TCol> withFilterIn(String filterName, Function<TRawVal, List<TCol>> filterValueAdapter) {
            return new InFilterBuilder<>(this, filterName, filterValueAdapter);
        }

        /**
         * @deprecated replaced by {@link #withFilter(java.lang.String, net.optionfactory.pussyfoot.hibernate.JpaFilter)
         * }
         */
        public <T> Builder<TRoot> addFilter(String filterName, JpaFilter<TRoot, T> filter) {
            filters.put(filterName, filter);
            return this;
        }

        /**
         * @deprecated replaced by {@link #withFilterEqual}
         */
        public <T> Builder<TRoot> addFilterEquals(String filterName, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path) {
            return withFilter(filterName, (cb, root, value) -> cb.equal(path.apply(cb, root), value));
        }

        /**
         * @deprecated replaced by {@link #withFilterEqual}
         */
        public <T, X> Builder<TRoot> addFilterEquals(String filterName, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, Function<X, T> valueAdapter) {
            return withFilter(filterName, (CriteriaBuilder cb, Root<TRoot> root, X value) -> cb.equal(path.apply(cb, root), valueAdapter.apply(value)));
        }

        /**
         * @deprecated replaced by {@link #withFilterEqual}
         */
        public <T> Builder<TRoot> addFilterEquals(String filterName) {
            return withFilter(filterName, (cb, root, value) -> cb.equal(root.get(filterName), value));
        }

        /**
         * @deprecated replaced by {@link #withFilterLike(java.lang.String) }
         */
        public Builder<TRoot> addFilterLike(String filterName, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<String>> path) {
            return withFilter(filterName, (CriteriaBuilder cb, Root<TRoot> root, String value) -> {
                return StringPredicates.like(cb, path.apply(cb, root), value);
            });
        }

        /**
         * @deprecated replaced by {@link #withSorter }
         */
        public Builder<TRoot> addSorter(String name, BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext> sorter) {
            if (!sorters.containsKey(name)) {
                sorters.put(name, new ArrayList<>());
            }
            sorters.get(name).add(sorter);
            return this;
        }

        /**
         * @deprecated replaced by {@link #withSorter }
         */
        public Builder<TRoot> addSorter(String name, Function<Root<TRoot>, Path<?>> path) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = path.apply(root);
                return orderingContext;
            });
        }

        /**
         * @deprecated replaced by {@link #withSorter }
         */
        public <T> Builder<TRoot> addSorter(String name, SingularAttribute<TRoot, T> column) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = root.get(column);
                return orderingContext;
            });
        }

        /**
         * @deprecated replaced by {@link #withSorter }
         */
        public Builder<TRoot> addSorter(String name) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = root.get(name);
                return orderingContext;
            });
        }

        /**
         * @deprecated replaced by {@link #withReducer }
         */
        public Builder<TRoot> addReducer(String name, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>> reduction) {
            reducers.put(name, reduction);
            return this;
        }
        
        public Builder<TRoot> withReducer(String name, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>> reduction) {
            reducers.put(name, reduction);
            return this;
        }

        /**
         * @deprecated replaced by {@link #withRootEnhancer}
         */
        @Deprecated
        public Builder<TRoot> addRootEnhancer(Consumer<Root<TRoot>> rootEnhancer) {
            this.rootEnhancer = Optional.of((cb, r) -> rootEnhancer.accept(r));
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
            this.rootEnhancer = Optional.of((cb, r) -> rootEnhancer.accept(r));
            return this;
        }

        /**
         * Allows to apply side effects on the query's {@link Root}. Main use
         * case is to specify eager joins
         *
         * @param rootEnhancer
         * @return
         */
        public Builder<TRoot> withRootEnhancer(BiConsumer<CriteriaBuilder, Root<TRoot>> rootEnhancer) {
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
            return new HibernatePsf(hibernate, clazz, useCountDistinct, rootEnhancer, uniqueKeyResolver, filters, sorters, reducers);
        }
    }
}
