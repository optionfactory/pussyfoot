package net.optionfactory.pussyfoot.hibernate.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.extjs.ExtJs;
import net.optionfactory.pussyfoot.extjs.NumericFilter;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

/**
 * @deprecated replaced by
 * {@link Builder#withFilterDateIn(java.lang.String, java.util.function.Function, java.util.function.Function)}
 * in conjunction with {@link ExtJs#utcDate } and a converter function from
 * {@link ZonedDateTime} to your column's type
 */
@Deprecated
public class UtcInstantInDayRange<TRoot> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<Instant>> path;
    private final ObjectMapper objectMapper;

    public UtcInstantInDayRange(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<Instant>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> r, String value) {
        try {
            final NumericFilter numericFilter = objectMapper.readValue(value, NumericFilter.class);
            final ZonedDateTime truncatedToDay = Instant.ofEpochMilli(numericFilter.value).atZone(ZoneId.of("UTC")).truncatedTo(ChronoUnit.DAYS);
            final Instant beginningOfDay = truncatedToDay.toInstant();
            final Instant beginningOfNextDay = truncatedToDay.plus(1, ChronoUnit.DAYS).toInstant();
            switch (numericFilter.operator) {
                case lt:
                    return cb.lessThan(path.apply(cb, r), beginningOfDay);
                case lte:
                    return cb.lessThan(path.apply(cb, r), beginningOfNextDay);
                case gt:
                    return cb.greaterThanOrEqualTo(path.apply(cb, r), beginningOfNextDay);
                case gte:
                    return cb.greaterThanOrEqualTo(path.apply(cb, r), beginningOfDay);
                case eq:
                    return cb.and(
                            cb.lessThan(path.apply(cb, r), beginningOfNextDay),
                            cb.greaterThanOrEqualTo(path.apply(cb, r), beginningOfDay)
                    );
                default:
                    throw new AssertionError(numericFilter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
