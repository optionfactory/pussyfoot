package net.optionfactory.pussyfoot.hibernate;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;
import net.optionfactory.pussyfoot.hibernate.SorterContext;

public class SorterBuilder<TRoot> {

    private final HibernatePsf.Builder<TRoot> builder;
    private final String name;

    public SorterBuilder(HibernatePsf.Builder<TRoot> builder, String name) {
        this.builder = builder;
        this.name = name;
    }

    public HibernatePsf.Builder<TRoot> as(BiFunction<CriteriaBuilder, Root<TRoot>, SorterContext> sorterContextBuilder) {
        return builder.withSorter(this.name, sorterContextBuilder);
    }

    public HibernatePsf.Builder<TRoot> usePath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<? extends Comparable<?>>> pathResolver) {
        return as((CriteriaBuilder cb, Root<TRoot> root) -> {
            final SorterContext orderingContext = new SorterContext();
            orderingContext.sortExpression = pathResolver.apply(cb, root);
            return orderingContext;
        });
    }

    public <TCol extends Comparable<TCol>> HibernatePsf.Builder<TRoot> useNullablePath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return as((CriteriaBuilder cb, Root<TRoot> root) -> {
            final SorterContext orderingContext = new SorterContext();
            final Expression<TCol> path = pathResolver.apply(cb, root);
            orderingContext.sortExpression = cb.coalesce(path, defaultIfNull);
            return orderingContext;
        });
    }

    public HibernatePsf.Builder<TRoot> usePath(Function<Root<TRoot>, Expression<? extends Comparable<?>>> pathResolver) {
        return usePath((cb, r) -> pathResolver.apply(r));
    }

    public <TCol extends Comparable<TCol>> HibernatePsf.Builder<TRoot> useNullablePath(Function<Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return useNullablePath((cb, r) -> pathResolver.apply(r), defaultIfNull);
    }

    public HibernatePsf.Builder<TRoot> useColumn(SingularAttribute<TRoot, ? extends Comparable<?>> column) {
        return usePath((cb, r) -> r.get(column));
    }

    public <TCol extends Comparable<TCol>> HibernatePsf.Builder<TRoot> useNullableColumn(SingularAttribute<TRoot, TCol> column, TCol defaultIfNull) {
        return useNullablePath((cb, r) -> r.get(column), defaultIfNull);
    }

    public HibernatePsf.Builder<TRoot> useColumn(String columnName) {
        return usePath((cb, r) -> r.get(columnName));
    }

    public HibernatePsf.Builder<TRoot> useColumnWithSameName() {
        return usePath((cb, r) -> r.get(name));
    }

    public <TCol extends Comparable<TCol>> HibernatePsf.Builder<TRoot> useNullableColumn(String columnName, TCol defaultIfNull) {
        return useNullablePath((cb, r) -> r.<TCol>get(columnName), defaultIfNull);
    }

    public <TCol extends Comparable<TCol>> HibernatePsf.Builder<TRoot> useNullableColumnWithSameName(TCol defaultIfNull) {
        return useNullablePath((cb, r) -> r.<TCol>get(name), defaultIfNull);
    }

    public <TCHILD> HibernatePsf.Builder<TRoot> useColumnsChain(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, ? extends Comparable<?>> leafColumn) {
        return usePath((cb, r) -> r.get(firstLevelColumn).get(leafColumn));
    }

    public <TCHILD, TGRANDCHILD> HibernatePsf.Builder<TRoot> useColumnsChain(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, TGRANDCHILD> secondLevelColumn, SingularAttribute<TGRANDCHILD, ? extends Comparable<?>> leafColumn) {
        return usePath((cb, r) -> r.get(firstLevelColumn).get(secondLevelColumn).get(leafColumn));
    }

    public HibernatePsf.Builder<TRoot> useColumnsChain(String firstColumnName, String... columnNames) {
        return usePath((CriteriaBuilder cb, Root<TRoot> r)
                -> Stream.of(columnNames)
                        .reduce(r.get(firstColumnName), Path::get, (p1, p2) -> {
                            throw new IllegalStateException("Can't parallelize this!");
                        }));
    }
}
