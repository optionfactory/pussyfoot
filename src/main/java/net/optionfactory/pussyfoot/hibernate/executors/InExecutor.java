package net.optionfactory.pussyfoot.hibernate.executors;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.SimpleExecutor;

public class InExecutor<T,Coll extends Collection<T>> implements SimpleExecutor<T, Coll> {

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Expression<T> path, Coll filterValue) {
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
