package net.optionfactory.pussyfoot.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
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
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;
import net.optionfactory.pussyfoot.SliceRequest;
import net.optionfactory.pussyfoot.SortRequest;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/**
 * Hibernate implementation of the pagination, sorting, filtering API.
 */
public class HibernatePsf implements Psf {

    private final SessionFactory hibernate;
    private final ConcurrentMap<String, JpaFilter<?>> availableFilters;
    private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root, SorterContext>>> availableSorters;
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, Expression<?>>> reducers;

    public static Predicate like(CriteriaBuilder cb, Expression<String> path, String v) {
        return cb.like(cb.lower(path), ('%' + v + '%').toLowerCase());
    }

    public static Predicate matchesInt(CriteriaBuilder cb, Expression<String> path, String v) {
        try {
            final Integer i = Integer.parseInt(v);
            return cb.equal(path, i);
        } catch (NumberFormatException ex) {
            return cb.or();
        }
    }

    public HibernatePsf(
            SessionFactory hibernate,
            ConcurrentMap<String, JpaFilter<?>> availableFilters,
            ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root, SorterContext>>> availableSorters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, Expression<?>>> reducers
    ) {
        this.hibernate = hibernate;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
        this.reducers = reducers;
    }

    public static class SorterContext {

        public SorterContext() {
            additionalSelection = Optional.empty();
            groupers = Optional.empty();
        }

        public Optional<Selection<?>> additionalSelection;
        public Optional<List<Expression<?>>> groupers;
        public Expression<?> sortExpression;
    }

    @FunctionalInterface
    public interface JpaFilter<T> {

        Predicate predicateFor(CriteriaBuilder cb, Root r, T value);
    }

    private <T> Predicate predicateForNameAndValue(String name, T value, CriteriaBuilder cb, Root r) {
        final JpaFilter<T> filter = (JpaFilter<T>) availableFilters.get(name);
        return filter.predicateFor(cb, r, value);
    }

    @Override
    public <T> PageResponse<T> queryForPage(Class<T> klass, PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();

        final CriteriaQuery<Tuple> ccq = cb.createTupleQuery();
        final Root<T> countRoot = ccq.from(klass);
        final List<Selection<?>> countSelectors = new ArrayList<>();
        countSelectors.add(cb.countDistinct(countRoot).alias("psfcount"));
        reducers.entrySet().stream()
                .map(e -> e.getValue().apply(cb, countRoot).alias(e.getKey()))
                .collect(Collectors.toCollection(() -> countSelectors));
        ccq.select(cb.tuple(countSelectors.toArray(new Selection<?>[0])));
        final List<Predicate> predicates = Stream.of(request.filters)
                .filter(filterRequest -> availableFilters.containsKey(filterRequest.name))
                .map(filterRequest -> {
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
        final Root<T> sliceRoot = scq.from(klass);
        final List<Selection<?>> selectors = new ArrayList<>();
        selectors.add(sliceRoot);
        final List<Order> orderers = new ArrayList<>();
        orderers.addAll(
                Stream.of(request.sorters)
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

        final List<T> slice = sliceQuery.getResultList()
                .stream()
                .map(tuple -> tuple.get(0, klass))
                .collect(Collectors.toList());
        return PageResponse.of(total, slice, reductions);
    }

    /**
     * A builder for HibernatePsf.
     */
    public static class Builder {

        private final ConcurrentMap<String, JpaFilter<?>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root, SorterContext>>> sorters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, Expression<?>>> reducers = new ConcurrentHashMap<>();

        public <T> Builder addFilter(String name, JpaFilter<T> filter) {
            filters.put(name, filter);
            return this;
        }

        @Deprecated()
        /**
         *  * @deprecated long method name </br>
         * use {@link #addFilter()} instead
         */
        public <T> Builder addCustomFilter(String name, JpaFilter<T> filter) {
            filters.put(name, filter);
            return this;
        }

        public <T> Builder addFilterEquals(String name, BiFunction<CriteriaBuilder, Root, Expression<T>> path) {
            return addFilter(name, (cb, root, value) -> cb.equal(path.apply(cb, root), value));
        }

        public <T, X> Builder addFilterEquals(String name, BiFunction<CriteriaBuilder, Root, Expression<T>> path, Function<X, T> valueAdapter) {
            return addFilter(name, (CriteriaBuilder cb, Root root, X value) -> cb.equal(path.apply(cb, root), valueAdapter.apply(value)));
        }

        public <T> Builder addFilterEquals(String name) {
            return addFilter(name, (cb, root, value) -> cb.equal(root.get(name), value));
        }

        public Builder addFilterLike(String name, BiFunction<CriteriaBuilder, Root, Expression<String>> path) {
            return addFilter(name, (CriteriaBuilder cb, Root root, String value) -> {
                return HibernatePsf.like(cb, path.apply(cb, root), value);
            });
        }

        public Builder addSorter(String name, BiFunction<CriteriaBuilder, Root, SorterContext> sorter) {
            if (!sorters.containsKey(name)) {
                sorters.put(name, new ArrayList<>());
            }
            sorters.get(name).add(sorter);
            return this;
        }

        public Builder addSorter(String name, Function<Root, Path<?>> sorter) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = sorter.apply(root);
                return orderingContext;
            });
        }

        public Builder addSorter(String name, String field) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = root.get(field);
                return orderingContext;
            });
        }

        public Builder addSorter(String name) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = root.get(name);
                return orderingContext;
            });
        }

        public Builder addReducer(String name, BiFunction<CriteriaBuilder, Root, Expression<?>> reduction) {
            reducers.put(name, reduction);
            return this;
        }

        public HibernatePsf build(SessionFactory hibernate) {
            return new HibernatePsf(hibernate, filters, sorters, reducers);
        }
    }
}
