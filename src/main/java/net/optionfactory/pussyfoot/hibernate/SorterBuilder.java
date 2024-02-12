package net.optionfactory.pussyfoot.hibernate;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class SorterBuilder<TRoot> {

    private final HibernatePsf.Builder<TRoot> builder;
    private final String name;

    public SorterBuilder(HibernatePsf.Builder<TRoot> builder, String name) {
        this.builder = builder;
        this.name = name;
    }

    public <TCol> HibernatePsf.Builder<TRoot> as(ExpressionResolver<TRoot, TCol> sorterContextBuilder) {
        return builder.withSorter(this.name, sorterContextBuilder);
    }

    public <TCol> HibernatePsf.Builder<TRoot> usePath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver) {
        return as((CriteriaQuery<Tuple> query, CriteriaBuilder cb, Root<TRoot> root) -> {
            return pathResolver.apply(cb, root);
        });
    }

    public <TCol> HibernatePsf.Builder<TRoot> useNullablePath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return as((CriteriaQuery<Tuple> query, CriteriaBuilder cb, Root<TRoot> root) -> {
            return cb.coalesce(pathResolver.apply(cb, root), defaultIfNull);
        });
    }

    public <TCol> HibernatePsf.Builder<TRoot> usePath(Function<Root<TRoot>, Expression<TCol>> pathResolver) {
        return usePath((cb, r) -> pathResolver.apply(r));
    }

    public <TCol> HibernatePsf.Builder<TRoot> useNullablePath(Function<Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return useNullablePath((cb, r) -> pathResolver.apply(r), defaultIfNull);
    }

    public <TCol> HibernatePsf.Builder<TRoot> useColumn(SingularAttribute<TRoot, TCol> column) {
        return usePath((cb, r) -> r.get(column));
    }

    public <TCol> HibernatePsf.Builder<TRoot> useNullableColumn(SingularAttribute<TRoot, TCol> column, TCol defaultIfNull) {
        return useNullablePath((cb, r) -> r.get(column), defaultIfNull);
    }

    public <TCol> HibernatePsf.Builder<TRoot> useColumn(String columnName) {
        return usePath((cb, r) -> r.<TCol>get(columnName));
    }

    public <TCol> HibernatePsf.Builder<TRoot> useColumnWithSameName() {
        return usePath((cb, r) -> r.<TCol>get(name));
    }

    public <TCol> HibernatePsf.Builder<TRoot> useNullableColumn(String columnName, TCol defaultIfNull) {
        return useNullablePath((cb, r) -> r.<TCol>get(columnName), defaultIfNull);
    }

    public <TCol> HibernatePsf.Builder<TRoot> useNullableColumnWithSameName(TCol defaultIfNull) {
        return useNullablePath((cb, r) -> r.<TCol>get(name), defaultIfNull);
    }

    public <TCHILD, TCol> HibernatePsf.Builder<TRoot> useColumnsChain(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, TCol> leafColumn) {
        return usePath((cb, r) -> r.get(firstLevelColumn).<TCol>get(leafColumn));
    }

    public <TCHILD, TGRANDCHILD, TCol> HibernatePsf.Builder<TRoot> useColumnsChain(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, TGRANDCHILD> secondLevelColumn, SingularAttribute<TGRANDCHILD, TCol> leafColumn) {
        return usePath((cb, r) -> r.get(firstLevelColumn).get(secondLevelColumn).<TCol>get(leafColumn));
    }

    public <TCol> HibernatePsf.Builder<TRoot> useColumnsChain(Class<TCol> finalColumnClass, String firstColumnName, String... columnNames) {
        return usePath((CriteriaBuilder cb, Root<TRoot> r)
                -> {
            return Stream.of(columnNames)
                    .reduce(r.get(firstColumnName), Path::get, (p1, p2) -> {
                        throw new IllegalStateException("Can't parallelize this!");
                    }).as(finalColumnClass);
        });
    }
}
