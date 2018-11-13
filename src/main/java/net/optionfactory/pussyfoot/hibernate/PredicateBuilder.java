package net.optionfactory.pussyfoot.hibernate;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * Defines a generic filter to be applied within a {@link HibernatePsf}
 * definition
 *
 * @param <TRoot> the type of the query's {@link Root} object
 * @param <TVal> the type of the filter's value
 */
@FunctionalInterface
public interface PredicateBuilder<TRoot, TVal> {

    /**
     * Given context (query's {@link CriteriaBuilder} and {@link Root}) and the
     * filter value, return the JPA's {@link Predicate} to apply
     *
     * @param criteriaBuilder query's {@link CriteriaBuilder} 
     * @param root query's {@link Root}
     * @param filterValue filter raw value
     * @return the JPA's {@link Predicate} to apply
     */
    Predicate predicateFor(CriteriaBuilder criteriaBuilder, Root<TRoot> root, TVal filterValue);

}
