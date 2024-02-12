package net.optionfactory.pussyfoot.hibernate.executors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimpleExecutor;

public class EqualExecutor<TCol, TFilterValue extends TCol> implements SimpleExecutor<TCol, TFilterValue> {

    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<TCol> resolvedColValue, TFilterValue resolvedFilterValue) {
        return criteriaBuilder.equal(resolvedColValue, resolvedFilterValue);
    }

}
