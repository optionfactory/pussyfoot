package net.optionfactory.pussyfoot.hibernate.typed;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@FunctionalInterface
public interface JpaFilter<TRoot, T> {

    Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> r, T value);

}
