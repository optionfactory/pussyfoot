package net.optionfactory.pussyfoot.hibernate;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
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
        return builder.withSorter(this.name,sorterContextBuilder);
    }

    public HibernatePsf.Builder<TRoot> onPath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>> pathResolver) {
        return as((CriteriaBuilder cb, Root<TRoot> root) -> {
            final SorterContext orderingContext = new SorterContext();
            orderingContext.sortExpression = pathResolver.apply(cb, root);
            return orderingContext;
        });
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullablePath(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return as((CriteriaBuilder cb, Root<TRoot> root) -> {
            final SorterContext orderingContext = new SorterContext();
            final Expression<TCol> path = pathResolver.apply(cb, root);
            orderingContext.sortExpression = cb.coalesce(path, defaultIfNull);
            return orderingContext;
        });
    }

    public HibernatePsf.Builder<TRoot> onPath(Function<Root<TRoot>, Expression<?>> pathResolver) {
        return SorterBuilder.this.onPath((cb, r) -> pathResolver.apply(r));
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullablePath(Function<Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return SorterBuilder.this.onNullablePath((cb, r) -> pathResolver.apply(r), defaultIfNull);
    }

    public HibernatePsf.Builder<TRoot> onColumn(SingularAttribute<TRoot, ?> column) {
        return SorterBuilder.this.onPath((cb, r) -> r.get(column));
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullableColumn(SingularAttribute<TRoot, TCol> column, TCol defaultIfNull) {
        return SorterBuilder.this.onNullablePath((cb, r) -> r.get(column), defaultIfNull);
    }

    public HibernatePsf.Builder<TRoot> onColumn(String columnName) {
        return SorterBuilder.this.onPath((cb, r) -> r.get(columnName));
    }

    public HibernatePsf.Builder<TRoot> onColumnWithSameName() {
        return SorterBuilder.this.onPath((cb, r) -> r.get(name));
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullableColumn(String columnName, TCol defaultIfNull) {
        return SorterBuilder.this.onNullablePath((cb, r) -> r.<TCol>get(columnName), defaultIfNull);
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullableColumnWithSameName(TCol defaultIfNull) {
        return SorterBuilder.this.onNullablePath((cb, r) -> r.<TCol>get(name), defaultIfNull);
    }

}
