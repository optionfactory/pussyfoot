package net.optionfactory.pussyfoot.hibernate.extjs;

import java.time.LocalDate;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.extjs.GenericComparableFilter;
import static net.optionfactory.pussyfoot.extjs.Operator.eq;
import static net.optionfactory.pussyfoot.extjs.Operator.gt;
import static net.optionfactory.pussyfoot.extjs.Operator.gte;
import static net.optionfactory.pussyfoot.extjs.Operator.lt;
import static net.optionfactory.pussyfoot.extjs.Operator.lte;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

public class ComparableFilter<TRoot, T extends Comparable<? super T>> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path;
    private final Function<String, GenericComparableFilter<T>> transformer;

    public ComparableFilter(
            BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path,
            Function<String, GenericComparableFilter<T>> transformer
    ) {
        this.path = path;
        this.transformer = transformer;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> root, String value) {
        final GenericComparableFilter<T> filter = transformer.apply(value);
        switch (filter.operator) {
            case lt:
                return cb.lessThan(path.apply(cb, root), filter.value);
            case lte:
                return cb.lessThanOrEqualTo(path.apply(cb, root), filter.value);
            case gt:
                return cb.greaterThan(path.apply(cb, root), filter.value);
            case gte:
                return cb.greaterThanOrEqualTo(path.apply(cb, root), filter.value);
            case eq:
                return cb.equal(path.apply(cb, root), filter.value);
            default:
                throw new AssertionError(filter.operator.name());
        }
    }

}
