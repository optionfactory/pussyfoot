package net.optionfactory.pussyfoot.hibernate.builders;

import java.time.Instant;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.extjs.Operator;
import net.optionfactory.pussyfoot.extjs.Comparator;
import net.optionfactory.pussyfoot.extjs.UTCDate;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

/**
 * Defines an {@link Operator}-based filter against a comparable value
 *
 * <p>
 * SQL equivalent when applied, depending on the
 * {@link Comparator#operator}:<br/>
 * ... where col {@link Operator} :value:
 * </p>
 * sky
 *
 *
 * @param <TRoot> The type of the root object the query starts from
 * @param <TRawValue> The type of the value of the {@link PageRequest}'s
 * {@link FilterRequest}, before adaptation (e.g.: received from an API)
 * @param <TCol> The type of the column this filter will apply to
 */
public class ComparatorFilterBuilder<TRoot, TRawValue, TCol extends Comparable<TCol>> extends BaseFilterBuilder<TRoot, TRawValue, Comparator<TCol>, TCol> {


    /**
     * Instantiate a new instance of {@link OperatorFilterBuilder}, which
     * extends {@link BaseFilterBuilder}
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TRawValue> The type of the value of the {@link PageRequest}'s
     * {@link FilterRequest}, before adaptation (e.g.: received from an API)
     * @param <TCol> The type of the column this filter will apply to
     */
    public ComparatorFilterBuilder(HibernatePsf.Builder<TRoot> builder, String filterName, Function<TRawValue, Comparator<TCol>> filterValueAdapter) {
        super(builder, filterName, filterValueAdapter);
    }

    @Override
    protected Predicate createPredicate(CriteriaBuilder cb, Expression<TCol> path, Comparator<TCol> filterValue) {
        switch (filterValue.operator) {
            case lt:
                return cb.lessThan(path, filterValue.value);
            case lte:
                return cb.lessThanOrEqualTo(path, filterValue.value);
            case eq:
                return cb.equal(path, filterValue.value);
            case gt:
                return cb.greaterThan(path, filterValue.value);
            case gte:
                return cb.greaterThanOrEqualTo(path, filterValue.value);
            default:
                throw new AssertionError(filterValue.operator.name());
        }
    }

}
