package net.optionfactory.pussyfoot.hibernate.builders;

import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

/**
 * Defines an equality filter.
 *
 * <p>
 * SQL equivalent when applied:<br/>
 * ... where col = :value
 * </p>
 *
 * @param <TRoot> The type of the root object the query starts from
 * @param <TRawValue> The type of the value of the {@link PageRequest}'s
 * {@link FilterRequest}, before adaptation (e.g.: received from an API)
 * @param <TCol> The type of the column this filter will apply to
 */
public class EqualFilterBuilder<TRoot, TRawValue, TCol> extends BaseFilterBuilder<TRoot, TRawValue, TCol, TCol> {

    /**
     * Instantiate a new instance of {@link EqualFilterBuilder}, which extends
     * {@link BaseFilterBuilder}
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TRawValue> The type of the value of the {@link PageRequest}'s
     * {@link FilterRequest}, before adaptation (e.g.: received from an API)
     * @param <TCol> The type of the column this filter will apply to
     */
    public EqualFilterBuilder(HibernatePsf.Builder<TRoot> builder, String filterName, Function<TRawValue, TCol> filterValueAdapter) {
        super(builder, filterName, filterValueAdapter);
    }

    @Override
    public Predicate createPredicate(CriteriaBuilder cb, Expression<TCol> path, TCol filterValue) {
        return cb.equal(path, filterValue);
    }

}
