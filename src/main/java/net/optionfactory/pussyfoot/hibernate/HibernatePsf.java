package net.optionfactory.pussyfoot.hibernate;

import net.optionfactory.pussyfoot.SortRequest.Direction;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;
import net.optionfactory.pussyfoot.SliceRequest;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;

/**
 * Hibernate implementation of the pagination, sorting, filtering API.
 */
public class HibernatePsf implements Psf<Criteria> {

    private final SessionFactory hibernate;
    private final ConcurrentMap<String, Function<String, Criterion>> availableFilters;
    private final ConcurrentMap<String, Function<Direction, Order>> availableSorters;
    private final ConcurrentMap<String, String> availableAliases;

    public HibernatePsf(SessionFactory hibernate, ConcurrentMap<String, Function<String, Criterion>> availableFilters,
            ConcurrentMap<String, Function<Direction, Order>> availableSorters,
            ConcurrentMap<String, String> availableAliases) {
        this.hibernate = hibernate;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
        this.availableAliases = availableAliases;
    }

    public Criteria countCriteria(Class klass, PageRequest request) {
        final Session session = hibernate.getCurrentSession();
        final Criteria criteriaForCount = session.createCriteria(klass, "main");
        availableAliases.forEach((associationPath, alias) -> {
            criteriaForCount.createAlias(associationPath, alias);
        });
        criteriaForCount.setProjection(Projections.rowCount());
        Stream.of(request.filters)
                .filter((net.optionfactory.pussyfoot.FilterRequest f) -> availableFilters.containsKey(f.name))
                .forEach((net.optionfactory.pussyfoot.FilterRequest f) -> {
                    criteriaForCount.add(availableFilters.get(f.name).apply(f.value));
                });
        return criteriaForCount;
    }

    @Override
    public <T> PageResponse<T> queryForPage(Class<T> klass, PageRequest request, BiConsumer<Criteria, Criteria> cb) {
        final Session session = hibernate.getCurrentSession();
        final Criteria criteriaForSlice = session.createCriteria(klass, "main");
        final Criteria criteriaForCount = session.createCriteria(klass, "main");
        availableAliases.forEach((associationPath, alias) -> {
            criteriaForSlice.createAlias(associationPath, alias);
            criteriaForCount.createAlias(associationPath, alias);
        });
        criteriaForSlice.setFirstResult(request.slice.start);
        if (request.slice.limit != SliceRequest.UNLIMITED) {
            criteriaForSlice.setMaxResults(request.slice.limit);
        }
        criteriaForCount.setProjection(Projections.rowCount());
        Stream.of(request.filters)
                .filter((net.optionfactory.pussyfoot.FilterRequest f) -> availableFilters.containsKey(f.name))
                .forEach((net.optionfactory.pussyfoot.FilterRequest f) -> {
                    criteriaForSlice.add(availableFilters.get(f.name).apply(f.value));
                    criteriaForCount.add(availableFilters.get(f.name).apply(f.value));
                });

        Stream.of(request.sorters)
                .filter((net.optionfactory.pussyfoot.SortRequest s) -> availableSorters.containsKey(s.name))
                .forEach((net.optionfactory.pussyfoot.SortRequest s) -> {
                    criteriaForSlice.addOrder(availableSorters.get(s.name).apply(s.direction));
                    // no need to sort when counting.
                });

        cb.accept(criteriaForSlice, criteriaForCount);
        final long total = ((Number) criteriaForCount.uniqueResult()).longValue();
        final List<T> slice = (List<T>) criteriaForSlice.list();
        return PageResponse.of(total, slice);
    }

    /**
     * A builder for HibernatePsf.
     */
    public static class Builder {

        private final ConcurrentMap<String, Function<String, Criterion>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Function<Direction, Order>> sorters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, String> aliases = new ConcurrentHashMap<>();

        public Builder createAlias(String associationPath, String alias) {
            aliases.put(associationPath, alias);
            return this;
        }

        public Builder canFilter(String name, Function<String, Criterion> filter) {
            filters.put(name, filter);
            return this;
        }

        public Builder canSort(String name, Function<Direction, Order> sorter) {
            sorters.put(name, sorter);
            return this;
        }

        public Builder canSort(String name, String field) {
            sorters.put(name, d -> d == Direction.ASC ? Order.asc(field) : Order.desc(field));
            return this;
        }

        public HibernatePsf build(SessionFactory hibernate) {
            return new HibernatePsf(hibernate, filters, sorters, aliases);
        }
    }

    public static class Orders {

        public static Order fromDirection(String propertyName, Direction dir) {
            return dir == Direction.ASC ? Order.asc(propertyName) : Order.desc(propertyName);
        }

    }
}
