package net.optionfactory.pussyfoot.hibernate;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import net.optionfactory.pussyfoot.extjs.DateFilter;
import net.optionfactory.pussyfoot.extjs.NumberFilter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

/**
 * Hibernate implementation of the pagination, sorting, filtering API.
 */
public class HibernatePsf implements Psf {

    private final SessionFactory hibernate;
    private final ConcurrentMap<String, JpaFilter<?>> availableFilters;
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, SorterContext>> availableSorters;

    public HibernatePsf(
            SessionFactory hibernate,
            ConcurrentMap<String, JpaFilter<?>> availableFilters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, SorterContext>> availableSorters) {
        this.hibernate = hibernate;
        this.availableFilters = availableFilters;
        this.availableSorters = availableSorters;
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

        final CriteriaQuery<Long> ccq = cb.createQuery(Long.class);
        final Root<T> countRoot = ccq.from(klass);
        ccq.select(cb.count(countRoot));
        final List<Predicate> predicates = Stream.of(request.filters)
                .filter(filterRequest -> availableFilters.containsKey(filterRequest.name))
                .map(filterRequest -> {
                    return predicateForNameAndValue(filterRequest.name, filterRequest.value, cb, countRoot);
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
                    r.groupers.ifPresent(g -> scq.groupBy(g));
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

        private final ConcurrentMap<String, JpaFilter<?>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root, SorterContext>> sorters = new ConcurrentHashMap<>();

        public <T> Builder customFilter(String name, JpaFilter<T> filter) {
            filters.put(name, filter);
            return this;
        }

        public <T> Builder equals(String name, Function<Root, Path<T>> path) {
            return customFilter(name, (cb, root, value) -> cb.equal(path.apply(root), value));
        }

        public <T extends Number> Builder numericFilter(String name, Function<Root, Path<T>> path, ObjectMapper objectMapper) {
            return customFilter(name, (cb, root, value) -> {
                try {
                    final NumberFilter numericFilter = objectMapper.readValue((String) value, NumberFilter.class);
                    switch (numericFilter.operator) {
                        case lt:
                            return cb.le(path.apply(root), numericFilter.number);
                        case gt:
                            return cb.gt(path.apply(root), numericFilter.number);
                        case eq:
                            return cb.equal(path.apply(root), numericFilter.number);
                        default:
                            throw new AssertionError(numericFilter.operator.name());
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        public <T extends Instant> Builder dayFilter(String name, Function<Root, Path<T>> path, ObjectMapper objectMapper) {
            return customFilter(name, (cb, root, value) -> {
                try {
                    final DateFilter dateFilter = objectMapper.readValue((String) value, DateFilter.class);
                    switch (dateFilter.operator) {
                        case lt:
                            return cb.lessThan(path.apply(root), Instant.ofEpochMilli(dateFilter.timestamp));
                        case gt:
                            return cb.greaterThan(path.apply(root), Instant.ofEpochMilli(dateFilter.timestamp));
                        case eq:
                            return cb.and(cb.greaterThan(path.apply(root), Instant.ofEpochMilli(dateFilter.timestamp)),
                                    cb.lessThan(path.apply(root), Instant.ofEpochMilli(dateFilter.timestamp).plus(1, ChronoUnit.DAYS)));
                        default:
                            throw new AssertionError(dateFilter.operator.name());
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        public <T, X> Builder equals(String name, Function<Root, Path<T>> path, Function<X, T> valueAdapter) {
            return customFilter(name, (CriteriaBuilder cb, Root root, X value) -> cb.equal(path.apply(root), valueAdapter.apply(value)));
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
