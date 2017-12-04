package net.optionfactory.pussyfoot.hibernate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;
import net.optionfactory.pussyfoot.SliceRequest;
import net.optionfactory.pussyfoot.SortRequest;
import org.hibernate.Criteria;
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
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, Path<?>>> availableSorters;

    public HibernatePsf(
            SessionFactory hibernate,
            ConcurrentMap<String, TriFunction<CriteriaBuilder, Root, String, Predicate>> availableFilters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, Path<?>>> availableSorters) {
        this.hibernate = hibernate;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
    }

    @Override
    public <T> PageResponse<T> queryForPage(Class<T> klass, PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

        final CriteriaQuery<Long> countQueryObject = criteriaBuilder.createQuery(Long.class);
        final Root<T> countRoot = countQueryObject.from(klass);
        countQueryObject.select(criteriaBuilder.count(countRoot));

        final CriteriaQuery<T> sliceQueryObject = criteriaBuilder.createQuery(klass);
        final Root<T> sliceRoot = sliceQueryObject.from(klass);
        sliceQueryObject.select(sliceRoot);

        final List<Predicate> predicates = Stream.of(request.filters)
                .filter(f -> availableFilters.containsKey(f.name))
                .map(f -> {
                    return availableFilters.get(f.name).apply(criteriaBuilder, countRoot, f.value);
                }).collect(Collectors.toList());

        countQueryObject.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
        sliceQueryObject.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));

        final Query<Long> countQuery = session.createQuery(countQueryObject);

        final List<Order> orderers = Stream.of(request.sorters)
                .filter(s -> availableSorters.containsKey(s.name))
                .map(s -> {
                    final Path<?> path = availableSorters.get(s.name).apply(criteriaBuilder, countRoot);
                    return s.direction == SortRequest.Direction.ASC ? criteriaBuilder.asc(path) : criteriaBuilder.desc(path);
                }).collect(Collectors.toList());

        sliceQueryObject.orderBy(orderers);
        final Query<T> sliceQuery = session.createQuery(sliceQueryObject);
        sliceQuery.setFirstResult(request.slice.start);
        if (request.slice.limit != SliceRequest.UNLIMITED) {
            sliceQuery.setMaxResults(request.slice.limit);
        }

        final Long total = countQuery.getSingleResult();
        final List<T> slice = sliceQuery.getResultList();
        return PageResponse.of(total, slice);
    }

    /**
     * A builder for HibernatePsf.
     */
    public static class Builder {

        private final ConcurrentMap<String, TriFunction<CriteriaBuilder, Root, String, Predicate>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, Path<?>>> sorters = new ConcurrentHashMap<>();

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

        public Builder canSort(String name, BiFunction<CriteriaBuilder, Root, Path<?>> sorter) {
            sorters.put(name, sorter);
            return this;
        }

        public Builder canSort(String name, Function<Root, Path<?>> sorter) {
            return canSort(name, (cb, root) -> sorter.apply(root));
        }

        public Builder canSort(String name, String field) {
            return canSort(name, (cb, root) -> root.get(field));
        }

        public HibernatePsf build(SessionFactory hibernate) {
            return new HibernatePsf(hibernate, filters, sorters);
        }
    }
}
