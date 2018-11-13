package net.optionfactory.pussyfoot.hibernate;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

@FunctionalInterface
public interface SimplePredicateBuilder<TCol, TFilterValue> {

    Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<TCol> resolvedColValue, TFilterValue resolvedFilterValue);

}
