package net.optionfactory.pussyfoot.hibernate;

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
    private final Optional<Consumer<Root<TRoot>>> rootEnhancer;
    private final ConcurrentMap<String, JpaFilter<TRoot, ?>> availableFilters;
    private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> availableSorters;
    private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers;

    public HibernatePsf(
            SessionFactory hibernate,
            Class<TRoot> klass,
            Optional<Consumer<Root<TRoot>>> rootEnhancer,
            ConcurrentMap<String, JpaFilter<TRoot, ?>> availableFilters,
            ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> availableSorters,
            ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers
    ) {
        this.hibernate = hibernate;
        this.klass = klass;
        this.rootEnhancer = rootEnhancer;
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

        private final ConcurrentMap<String, JpaFilter<TRoot, ?>> filters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, List<BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext>>> sorters = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>>> reducers = new ConcurrentHashMap<>();
        private Optional<Consumer<Root<TRoot>>> rootEnhancer = Optional.empty();

        public <T> Builder<TRoot> addFilter(String name, JpaFilter<TRoot, T> filter) {
            filters.put(name, filter);
            return this;
        }

        public <T> Builder<TRoot> addFilterEquals(String name, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path) {
            return addFilter(name, (cb, root, value) -> cb.equal(path.apply(cb, root), value));
        }

        public <T, X> Builder<TRoot> addFilterEquals(String name, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, Function<X, T> valueAdapter) {
            return addFilter(name, (CriteriaBuilder cb, Root<TRoot> root, X value) -> cb.equal(path.apply(cb, root), valueAdapter.apply(value)));
        }

        public <T> Builder<TRoot> addFilterEquals(String name) {
            return addFilter(name, (cb, root, value) -> cb.equal(root.get(name), value));
        }

        public Builder<TRoot> addFilterLike(String name, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<String>> path) {
            return addFilter(name, (CriteriaBuilder cb, Root<TRoot> root, String value) -> {
                return Predicates.like(cb, path.apply(cb, root), value);
            });
        }

        public Builder<TRoot> addSorter(String name, BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext> sorter) {
            if (!sorters.containsKey(name)) {
                sorters.put(name, new ArrayList<>());
            }
            sorters.get(name).add(sorter);
            return this;
        }

        public Builder<TRoot> addSorter(String name, Function<Root<TRoot>, Path<?>> sorter) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = sorter.apply(root);
                return orderingContext;
            });
        }

        public Builder<TRoot> addSorter(String name, String field) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = root.get(field);
                return orderingContext;
            });
        }

        public Builder<TRoot> addRootEnhancer(Consumer<Root<TRoot>> rootEnhancer) {
            this.rootEnhancer = Optional.of(rootEnhancer);
            return this;
        }

        public Builder<TRoot> addSorter(String name) {
            return addSorter(name, (cb, root) -> {
                final SorterContext orderingContext = new SorterContext();
                orderingContext.sortExpression = root.get(name);
                return orderingContext;
            });
        }

        public Builder<TRoot> addReducer(String name, BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>> reduction) {
            reducers.put(name, reduction);
            return this;
        }

        public HibernatePsf build(Class<TRoot> clazz, SessionFactory hibernate) {
            return new HibernatePsf(hibernate, clazz, rootEnhancer, filters, sorters, reducers);
        }
    }
}
