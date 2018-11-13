package net.optionfactory.pussyfoot.hibernate.predicates;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimplePredicateBuilder;

public class Equal<TCol, TFilterValue extends TCol> implements SimplePredicateBuilder<TCol, TFilterValue> {

    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<TCol> resolvedColValue, TFilterValue resolvedFilterValue) {
        return criteriaBuilder.equal(resolvedColValue, resolvedFilterValue);
    }

}
