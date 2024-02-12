package net.optionfactory.pussyfoot.hibernate.executors;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimpleExecutor;

public class LikeExecutor implements SimpleExecutor<String, String> {

    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<String> resolvedColValue, String resolvedFilterValue) {
        return StringPredicates.like(criteriaBuilder, resolvedColValue, resolvedFilterValue);
    }

}
