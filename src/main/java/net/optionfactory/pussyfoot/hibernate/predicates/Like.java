package net.optionfactory.pussyfoot.hibernate.predicates;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimplePredicateBuilder;

public class Like implements SimplePredicateBuilder<String, String> {

    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<String> resolvedColValue, String resolvedFilterValue) {
        return StringPredicates.like(criteriaBuilder, resolvedColValue, resolvedFilterValue);
    }

}
