package net.optionfactory.pussyfoot.hibernate.builders;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

/**
 * Defines an filter that matches a (calculated) column's value against a list
 * of possible values
 *
 * <p>
 * SQL equivalent when applied:<br/>
 *
 * ... where col IN (:val1:,:val2:, ... )
 * </p>
 *
 * @param <TRoot> The type of the root object the query starts from
 * @param <TRawValue> The type of the value of the {@link PageRequest}'s
 * {@link FilterRequest}, before adaptation (e.g.: received from an API)
 * @param <TCol> The type of the column this filter will apply to
 */
public class InFilterBuilder<TRoot, TRawValue, TCol> extends BaseFilterBuilder<TRoot, TRawValue, List<TCol>, TCol> {

    /**
     * Instantiate a new instance of {@link InFilterBuilder}, which extends
     * {@link BaseFilterBuilder}
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TRawValue> The type of the value of the {@link PageRequest}'s
     * {@link FilterRequest}, before adaptation (e.g.: received from an API)
     * @param <TCol> The type of the column this filter will apply to
     */
    public InFilterBuilder(HibernatePsf.Builder<TRoot> builder, String name, Function<TRawValue, List<TCol>> filterValueAdapter) {
        super(builder, name, filterValueAdapter);
    }

    @Override
    protected Predicate createPredicate(CriteriaBuilder cb, Expression<TCol> path, List<TCol> filterValue) {
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
