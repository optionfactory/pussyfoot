package net.optionfactory.pussyfoot.hibernate;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@FunctionalInterface
public interface JpaFilter<TRoot, TRawVal> {

    Predicate predicateFor(CriteriaBuilder criteriaBuilder, Root<TRoot> root, TRawVal rawValue);

}
