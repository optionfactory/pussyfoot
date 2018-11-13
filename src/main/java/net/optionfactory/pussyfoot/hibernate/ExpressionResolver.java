package net.optionfactory.pussyfoot.hibernate;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

@FunctionalInterface
public interface ExpressionResolver<TRoot, T> {

    Expression<T> resolve(final CriteriaQuery<Tuple> query, CriteriaBuilder criteriaBuilder, Root<TRoot> root);

}
