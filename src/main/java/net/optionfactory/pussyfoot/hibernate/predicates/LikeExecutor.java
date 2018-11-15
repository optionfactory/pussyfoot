package net.optionfactory.pussyfoot.hibernate.executors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimpleExecutor;

public class LikeExecutor implements SimpleExecutor<String, String> {

    @Override
    public Predicate predicateFor(CriteriaBuilder criteriaBuilder, Expression<String> resolvedColValue, String resolvedFilterValue) {
        return StringPredicates.like(criteriaBuilder, resolvedColValue, resolvedFilterValue);
    }

}
