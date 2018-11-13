package net.optionfactory.pussyfoot.hibernate;


import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

@FunctionalInterface
public interface SinglePathPredicateBuilder<TFilterValue, TCol> {

    Predicate predicateFor(CriteriaBuilder criteriaBuilder, TFilterValue resolvedFilterValue, Expression<TCol> resolvedColValue);

}
