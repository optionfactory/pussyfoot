package net.optionfactory.pussyfoot.hibernate.builders;

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

    public HibernatePsf.Builder<TRoot> on(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<?>> pathResolver) {
        return as((CriteriaBuilder cb, Root<TRoot> root) -> {
            final SorterContext orderingContext = new SorterContext();
            orderingContext.sortExpression = pathResolver.apply(cb, root);
            return orderingContext;
        });
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullable(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return as((CriteriaBuilder cb, Root<TRoot> root) -> {
            final SorterContext orderingContext = new SorterContext();
            final Expression<TCol> path = pathResolver.apply(cb, root);
            orderingContext.sortExpression = cb.coalesce(path, defaultIfNull);
            return orderingContext;
        });
    }

    public HibernatePsf.Builder<TRoot> on(Function<Root<TRoot>, Expression<?>> pathResolver) {
        return on((cb, r) -> pathResolver.apply(r));
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullable(Function<Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return onNullable((cb, r) -> pathResolver.apply(r), defaultIfNull);
    }

    public HibernatePsf.Builder<TRoot> on(SingularAttribute<TRoot, ?> column) {
        return on((cb, r) -> r.get(column));
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullable(SingularAttribute<TRoot, TCol> column, TCol defaultIfNull) {
        return onNullable((cb, r) -> r.get(column), defaultIfNull);
    }

    public HibernatePsf.Builder<TRoot> on(String columnName) {
        return on((cb, r) -> r.get(columnName));
    }

    public HibernatePsf.Builder<TRoot> onColumnWithSameName() {
        return on((cb, r) -> r.get(name));
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullable(String columnName, TCol defaultIfNull) {
        return onNullable((cb, r) -> r.<TCol>get(columnName), defaultIfNull);
    }

    public <TCol> HibernatePsf.Builder<TRoot> onNullableColumnWithSameName(TCol defaultIfNull) {
        return onNullable((cb, r) -> r.<TCol>get(name), defaultIfNull);
    }

}
