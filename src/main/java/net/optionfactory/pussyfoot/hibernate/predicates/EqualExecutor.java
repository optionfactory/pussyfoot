package net.optionfactory.pussyfoot.hibernate.executors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimpleExecutor;

public class EqualExecutor<TCol, TFilterValue extends TCol> implements SimpleExecutor<TCol, TFilterValue> {

    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<TCol> resolvedColValue, TFilterValue resolvedFilterValue) {
        return criteriaBuilder.equal(resolvedColValue, resolvedFilterValue);
    }

}
