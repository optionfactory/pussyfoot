package net.optionfactory.pussyfoot.hibernate.builders;

import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;
import net.optionfactory.pussyfoot.hibernate.StringPredicates;

/**
 * Defines an partial, case-insensitive match filter on string columns.
 *
 * <p>
 * PSQL equivalent when applied:<br/>
 * ... where col ilike %:value:%
 * </p>
 *
 *
 * @param <TRoot> The type of the root object the query starts from
 * @param <TRawValue> The type of the value of the {@link PageRequest}'s
 * {@link FilterRequest}, before adaptation (e.g.: received from an API)
 */
public class LikeFilterBuilder<TRoot, TRawValue> extends BaseFilterBuilder<TRoot, TRawValue, String, String> {

    /**
     * Instantiate a new instance of {@link LikeFilterBuilder}, which extends
     * {@link BaseFilterBuilder}
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TRawValue> The type of the value of the {@link PageRequest}'s
     * {@link FilterRequest}, before adaptation (e.g.: received from an API)
     */
    public LikeFilterBuilder(HibernatePsf.Builder<TRoot> builder, String filterName, Function<TRawValue, String> filterValueAdapter) {
        super(builder, filterName, filterValueAdapter);
    }

    @Override
    protected Predicate createPredicate(CriteriaBuilder cb, Expression<String> path, String filterValue) {
        return StringPredicates.like(cb, path, filterValue);
    }

}
