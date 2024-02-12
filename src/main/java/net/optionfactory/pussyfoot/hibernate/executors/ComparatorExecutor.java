package net.optionfactory.pussyfoot.hibernate.executors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.extjs.Comparison;
import static net.optionfactory.pussyfoot.extjs.Operator.eq;
import static net.optionfactory.pussyfoot.extjs.Operator.gt;
import static net.optionfactory.pussyfoot.extjs.Operator.gte;
import static net.optionfactory.pussyfoot.extjs.Operator.lt;
import static net.optionfactory.pussyfoot.extjs.Operator.lte;
import net.optionfactory.pussyfoot.hibernate.SimpleExecutor;

public class ComparatorExecutor<TCol extends Comparable<TCol>> implements SimpleExecutor<TCol, Comparison<TCol>> {

    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<TCol> resolvedColValue, Comparison<TCol> resolvedFilterValue) {
        switch (resolvedFilterValue.operator) {
            case lt:
                return criteriaBuilder.lessThan(resolvedColValue, resolvedFilterValue.value);
            case lte:
                return criteriaBuilder.lessThanOrEqualTo(resolvedColValue, resolvedFilterValue.value);
            case eq:
                return criteriaBuilder.equal(resolvedColValue, resolvedFilterValue.value);
            case gt:
                return criteriaBuilder.greaterThan(resolvedColValue, resolvedFilterValue.value);
            case gte:
                return criteriaBuilder.greaterThanOrEqualTo(resolvedColValue, resolvedFilterValue.value);
            default:
                throw new AssertionError(resolvedFilterValue.operator.name());
        }
    }

}
