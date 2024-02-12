package net.optionfactory.pussyfoot.hibernate;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

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
     * <div>
     * Returns an expression (usually a column value) out of the current query context
     * </div>
     * <p>
     * Type Parameters:
     * </p>
     * <ul>
     * <li>TRoot - the type of the query's {@link Root} object</li>
     * <li>TCol - the type of the expression returned (most of the times, the
 * type of the column referenced)
     * </ul>
     * @param query The query being built
     * @param criteriaBuilder The related {@link CriteriaBuilder}
     * @param root The query's {@link Root} object
     * @return the resolved expression (ranging from a straight column resolution to complex expressions)
     */
    Expression<TCol> resolve(final CriteriaQuery<Tuple> query, CriteriaBuilder criteriaBuilder, Root<TRoot> root);

}
