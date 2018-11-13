package net.optionfactory.pussyfoot.hibernate.predicates;

import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.emaze.dysfunctional.tuples.Pair;
import static net.optionfactory.pussyfoot.extjs.Operator.eq;
import static net.optionfactory.pussyfoot.extjs.Operator.gt;
import static net.optionfactory.pussyfoot.extjs.Operator.gte;
import static net.optionfactory.pussyfoot.extjs.Operator.lt;
import static net.optionfactory.pussyfoot.extjs.Operator.lte;
import net.optionfactory.pussyfoot.hibernate.SimplePredicateBuilder;

public class DualThresholdComparator<TCol extends Comparable<? super TCol>, TFilterValue extends Comparable<? super TFilterValue>> implements SimplePredicateBuilder<TCol, net.optionfactory.pussyfoot.extjs.Comparison<TFilterValue>> {

    private final Function<TFilterValue, Pair<TCol, TCol>> valueToThresholds;

    public DualThresholdComparator(Function<TFilterValue, Pair<TCol, TCol>> valueToThresholds) {
        this.valueToThresholds = valueToThresholds;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Expression<TCol> path, net.optionfactory.pussyfoot.extjs.Comparison<TFilterValue> filterValue) {
        final Pair<TCol, TCol> thresholds = valueToThresholds.apply(filterValue.value);
        final TCol lowerThreshold = thresholds.first();
        final TCol upperThreshold = thresholds.second();
        switch (filterValue.operator) {
            case lt:
                return cb.lessThan(path, lowerThreshold);
            case lte:
                return cb.lessThan(path, upperThreshold);
            case eq:
                return cb.and(cb.greaterThanOrEqualTo(path, lowerThreshold), cb.lessThan(path, upperThreshold));
            case gt:
                return cb.greaterThanOrEqualTo(path, upperThreshold);
            case gte:
                return cb.greaterThanOrEqualTo(path, lowerThreshold);
            default:
                throw new AssertionError(filterValue.operator.name());
        }
    }

}