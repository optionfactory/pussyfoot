package net.optionfactory.pussyfoot.hibernate;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Defines a generic filter to be applied within a {@link HibernatePsf}
 * definition
 *
 * @param <TRoot> the type of the query's {@link Root} object
 * @param <TFilterValue> the type of the filter's value
 */
@FunctionalInterface
public interface FilterExecutor<TRoot, TFilterValue> {

    /**
     * Given context (query's {@link CriteriaBuilder} and {@link Root}) and the
     * filter value, return the JPA's {@link Predicate} to apply
     *
     * @param query the query this filter is being applied to
     * @param criteriaBuilder query's {@link CriteriaBuilder} 
     * @param root query's {@link Root}
     * @param filterValue filter raw value
     * @return the JPA's {@link Predicate} to apply
     */
    Predicate predicateFor(final CriteriaQuery<Tuple> query, CriteriaBuilder criteriaBuilder, Root<TRoot> root, TFilterValue filterValue);

}
