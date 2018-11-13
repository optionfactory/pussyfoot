package net.optionfactory.pussyfoot.hibernate;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import net.emaze.dysfunctional.tuples.Pair;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;

public class FilterRequestProcessorBuilder<TRoot, TFilterRawValue, TFilterValue, TCol> {

    private final Builder<TRoot> builder;
    private final Pair<String, Class<?>> filterKey;
    private final Function<TFilterRawValue, TFilterValue> filterValueAdapter;
    private final SinglePathPredicateBuilder<TFilterValue, TCol> function;

    public FilterRequestProcessorBuilder(Builder<TRoot> builder, Pair<String, Class<?>> filterKey, Function<TFilterRawValue, TFilterValue> filterValueAdapter, SinglePathPredicateBuilder<TFilterValue, TCol> function) {
        this.builder = builder;
        this.filterKey = filterKey;
        this.filterValueAdapter = filterValueAdapter;
        this.function = function;
    }

    public Builder<TRoot> onPath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver) {
        this.builder.withFilterRequestProcessor(new FilterRequestProcessor<>(
                filterKey,
                filterValueAdapter,
                new PredicateBuilder<TRoot, TFilterValue>() {
            @Override
            public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Root<TRoot> root, TFilterValue filterValue) {
                return function.predicateFor(criteriaBuilder, filterValue, pathResolver.apply(criteriaBuilder, root));
            }
        }));
        return builder;
    }

    public Builder<TRoot> onNullablePath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return onPath((CriteriaBuilder cb, Root<TRoot> r) -> {
            final Expression<TCol> path = pathResolver.apply(cb, r);
            return cb.coalesce(path, defaultIfNull);
        });
    }

    public Builder<TRoot> onEitherPath(BiFunction<CriteriaBuilder, Root<TRoot>, List<Expression<TCol>>> pathsResolver) {
        this.builder.withFilterRequestProcessor(new FilterRequestProcessor<>(
                filterKey,
                filterValueAdapter,
                new PredicateBuilder<TRoot, TFilterValue>() {
            @Override
            public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Root<TRoot> root, TFilterValue filterValue) {
                final List<Expression<TCol>> paths = pathsResolver.apply(criteriaBuilder, root);
                return paths
                        .stream()
                        .map(path -> function.predicateFor(criteriaBuilder, filterValue, path))
                        .collect(Collectors.reducing(criteriaBuilder.disjunction(), (p1, p2) -> criteriaBuilder.or(p1, p2)));
            }
        }));
        return builder;

    }

    public Builder<TRoot> onPath(Function<Root<TRoot>, Expression<TCol>> pathResolver) {
        return onPath((cb, r) -> pathResolver.apply(r));
    }

    public Builder<TRoot> onNullablePath(Function<Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return onNullablePath((cb, r) -> pathResolver.apply(r), defaultIfNull);
    }

    public Builder<TRoot> onEitherPath(Function<Root<TRoot>, List<Expression<TCol>>> pathsResolver) {
        return onEitherPath((cb, r) -> pathsResolver.apply(r));
    }

    public Builder<TRoot> onColumn(SingularAttribute<TRoot, TCol> column) {
        return onPath((cb, r) -> r.get(column));
    }

    public Builder<TRoot> onNullableColumn(SingularAttribute<TRoot, TCol> column, TCol defaultIfNull) {
        return onNullablePath((cb, r) -> r.get(column), defaultIfNull);
    }

    public Builder<TRoot> onEitherColumnName(List<SingularAttribute<TRoot, TCol>> columns) {
        return onEitherPath((CriteriaBuilder cb, Root<TRoot> r) -> columns.stream().map((p) -> r.get(p)).collect(Collectors.toList()));
    }

    public Builder<TRoot> onColumn(String columnName) {
        return onPath((cb, r) -> r.get(columnName));
    }

    public Builder<TRoot> onNameMatchingColumn() {
        return onPath((cb, r) -> r.get(this.filterKey.first()));
    }

    public Builder<TRoot> onNullableColumn(String columnName, TCol defaultIfNull) {
        return onNullablePath((cb, r) -> r.get(columnName), defaultIfNull);
    }

    public Builder<TRoot> onNameMatchingNullableColumn(TCol defaultIfNull) {
        return onNullablePath((cb, r) -> r.get(this.filterKey.first()), defaultIfNull);
    }

    public <TCHILD> Builder<TRoot> onColumnsChain(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, TCol> leafColumn) {
        return onPath((cb, r) -> r.get(firstLevelColumn).get(leafColumn));
    }

    public <TCHILD, TGRANDCHILD> Builder<TRoot> onColumnsChain(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, TGRANDCHILD> secondLevelColumn, SingularAttribute<TGRANDCHILD, TCol> leafColumn) {
        return onPath((cb, r) -> r.get(firstLevelColumn).get(secondLevelColumn).get(leafColumn));
    }

    public Builder<TRoot> onColumnsChain(String firstColumnName, String... columnNames) {
        return onPath((CriteriaBuilder cb, Root<TRoot> r)
                -> Stream.of(columnNames)
                        .reduce(r.get(firstColumnName), Path::get, (p1, p2) -> {
                            throw new IllegalStateException("Can't parallelize this!");
                        }));
    }

    public Builder<TRoot> onEitherByColumn(List<String> columnNames) {
        return onEitherPath((cb, r) -> columnNames
                .stream()
                .map((cn) -> r.<TCol>get(cn))
                .collect(Collectors.toList()));
    }
}
