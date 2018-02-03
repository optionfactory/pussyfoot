package net.optionfactory.pussyfoot.hibernate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @param v the third function argument
         * @return the function result
         */
        R apply(T t, U u, V v);
    }

    private final SessionFactory hibernate;
    private final ConcurrentMap<String, TriFunction<CriteriaBuilder, Root, String, Predicate>> availableFilters;
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, SorterContext>> availableSorters;

    public HibernatePsf(
            SessionFactory hibernate,
            ConcurrentMap<String, TriFunction<CriteriaBuilder, Root, String, Predicate>> availableFilters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, SorterContext>> availableSorters) {
        this.hibernate = hibernate;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
    }

    public static class SorterContext {

        public SorterContext() {
            additionalSelection = Optional.empty();
            grouper = Optional.empty();
        }

        public Optional<Selection<?>> additionalSelection;
        public Optional<Expression<?>> grouper;
        public Expression<?> sortExpression;
    }

    @Override
    public <T> PageResponse<T> queryForPage(Class<T> klass, PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder cb = session.getCriteriaBuilder();

        final CriteriaQuery<Long> ccq = cb.createQuery(Long.class);
        final Root<T> countRoot = ccq.from(klass);
        ccq.select(cb.count(countRoot));
        final List<Predicate> predicates = Stream.of(request.filters)
                .filter(f -> availableFilters.containsKey(f.name))
                .map(f -> {
                    return availableFilters.get(f.name).apply(cb, countRoot, f.value);
                }).collect(Collectors.toList());
        ccq.where(cb.and(predicates.toArray(new Predicate[0])));
        final Query<Long> countQuery = session.createQuery(ccq);
        final Long total = countQuery.getSingleResult();

        final CriteriaQuery<Tuple> scq = cb.createTupleQuery();
        final Root<T> sliceRoot = scq.from(klass);
        final List<Selection<?>> selectors = new ArrayList<>();
        selectors.add(sliceRoot);
        final List<Order> orderers = new ArrayList<>();
        Stream.of(request.sorters)
                .filter(s -> availableSorters.containsKey(s.name))
                //                .map(s -> availableSorters.get(s.name))
                .forEach(s -> {
                    final BiFunction<CriteriaBuilder, Root, SorterContext> cnsmr = availableSorters.get(s.name);
                    final SorterContext r = cnsmr.apply(cb, sliceRoot);
                    r.additionalSelection.ifPresent(sel -> selectors.add(sel));
                    r.grouper.ifPresent(g -> scq.groupBy(g));
                    orderers.add(s.direction == SortRequest.Direction.ASC ? cb.asc(r.sortExpression) : cb.desc(r.sortExpression));
                });

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
        return PageResponse.of(total, slice);
    }

    /**
     * A builder for HibernatePsf.
     */
    public static class Builder {

        private final ConcurrentMap<String, TriFunction<CriteriaBuilder, Root, String, Predicate>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, SorterContext>> sorters = new ConcurrentHashMap<>();

        public Builder canFilter(String name, TriFunction<CriteriaBuilder, Root, String, Predicate> filter) {
            filters.put(name, filter);
            return this;
        }

        public <X> Builder canFilter(String name, Function<Root, Path<X>> path, Function<String, X> valueAdapter, TriFunction<CriteriaBuilder, Path<X>, X, Predicate> predicateBuilder) {
            return canFilter(name, (cb, root, value) -> predicateBuilder.apply(cb, path.apply(root), valueAdapter.apply(value)));
        }

        public Builder canFilter(String name, Function<Root, Path<String>> path, TriFunction<CriteriaBuilder, Path<String>, String, Predicate> predicateBuilder) {
            return canFilter(name, (cb, root, value) -> predicateBuilder.apply(cb, path.apply(root), value));
        }

        public <X> Builder canFilter(String name, Function<Root, Path<X>> path, Function<String, X> valueAdapter) {
            return canFilter(name, (cb, root, value) -> cb.equal(path.apply(root), valueAdapter.apply(value)));
        }

        public Builder canFilter(String name, Function<Root, Path<String>> path) {
            return canFilter(name, (cb, root, value) -> cb.equal(path.apply(root), value));
        }

        public Builder canFilter(String name, String field) {
            return canFilter(name, (cb, root, value) -> cb.equal(root.get(field), value));
        }

        public Builder canSort(String name, BiFunction<CriteriaBuilder, Root, SorterContext> sorter) {
            sorters.put(name, sorter);
            return this;
        }

        public Builder canSort(String name, Function<Root, Path<?>> sorter) {
            return canSort(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = sorter.apply(root);
                return orderingContext;
            });
        }

        public Builder canSort(String name, String field) {
            return canSort(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = root.get(field);
                return orderingContext;
            });
        }

        public HibernatePsf build(SessionFactory hibernate) {
            return new HibernatePsf(hibernate, filters, sorters);
        }
    }
}
