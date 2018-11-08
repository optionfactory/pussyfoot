package net.optionfactory.pussyfoot.hibernate.builders;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import net.optionfactory.pussyfoot.extjs.Comparator;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

public class DateInFilterBuilder<TRoot, TRawValue, TCol extends Temporal & Comparable<TCol>> extends BaseFilterBuilder<TRoot, TRawValue, Comparator<ZonedDateTime>, TCol> {
    
    private final Function<ZonedDateTime, TCol> filterToColumnAdapter;

    public DateInFilterBuilder(HibernatePsf.Builder<TRoot> builder, String filterName, Function<TRawValue, Comparator<ZonedDateTime>> filterValueAdapter, Function<ZonedDateTime, TCol> filterToColumnAdapter) {
        super(builder, filterName, filterValueAdapter);
        this.filterToColumnAdapter = filterToColumnAdapter;
    }

    @Override
    protected Predicate createPredicate(CriteriaBuilder cb, Expression<TCol> path, Comparator<ZonedDateTime> filterValue) {
        final ZonedDateTime beginningOfDay = filterValue.value.truncatedTo(ChronoUnit.DAYS);
        final ZonedDateTime beginningOfNextDay = beginningOfDay.plus(1, ChronoUnit.DAYS);
        switch (filterValue.operator) {
            case lt:
                return cb.lessThan(path, filterToColumnAdapter.apply(beginningOfDay));
            case lte:
                return cb.lessThan(path, filterToColumnAdapter.apply(beginningOfNextDay));
            case eq:
                return cb.and(cb.greaterThanOrEqualTo(path, filterToColumnAdapter.apply(beginningOfDay)), cb.lessThan(path, filterToColumnAdapter.apply(beginningOfNextDay)));
            case gt:
                return cb.greaterThanOrEqualTo(path, filterToColumnAdapter.apply(beginningOfNextDay));
            case gte:
                return cb.greaterThanOrEqualTo(path, filterToColumnAdapter.apply(beginningOfDay));
            default:
                throw new AssertionError(filterValue.operator.name());
        }
    }
    
}
