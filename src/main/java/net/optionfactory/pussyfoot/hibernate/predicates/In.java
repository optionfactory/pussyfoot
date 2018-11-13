package net.optionfactory.pussyfoot.hibernate.predicates;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimplePredicateBuilder;

public class In<T> implements SimplePredicateBuilder<T, List<T>> {

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Expression<T> path, List<T> filterValue) {
        /* Normally, a filterValue with no elements should actually never reach 
         * here, as there is no point in issuing a FilterRequest with an empty 
         * array, hoever older versions of Ext did, in some cases, issue empty lists
         */
        if (filterValue.isEmpty()) {
            return cb.conjunction();
        }
        /*
         * JPA's <path>.in fails if you pass either an empty list or a list with null values
         */
        if (!filterValue.contains(null)) {
            return path.in(filterValue);
        }

        final List<?> withoutNulls = filterValue.stream().filter(i -> i != null).collect(Collectors.toList());
        if (withoutNulls.isEmpty()) {
            return path.isNull();
        }
        return cb.or(
                path.in(withoutNulls),
                path.isNull()
        );

    }

}
