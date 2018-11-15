package net.optionfactory.pussyfoot.hibernate;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

/**
 * Defines a function capable of retrieving a jpa Expression out of its query
 * components. Used to retrieve a specific column, optionally adapted
 *
 * @param <TRoot> the type of the query's {@link Root} object
 * @param <TCol> the type of the expression returned (most of the times, the
 * type of the column referenced)
 */
@FunctionalInterface
public interface ExpressionResolver<TRoot, TCol> {

    /**
     * Returns an expression (usually a column value) out of the current query context
     * @param query The query being built
     * @param criteriaBuilder The related {@link CriteriaBuilder}
     * @param root The query's {@link Root} object
     * @param <TRoot> the type of the query's {@link Root} object
     * @param <TCol> the type of the expression returned (most of the times, the
     * @return the resolved expression (ranging from a straight column resolution to complex expressions)
     */
    Expression<TCol> resolve(final CriteriaQuery<Tuple> query, CriteriaBuilder criteriaBuilder, Root<TRoot> root);

}
