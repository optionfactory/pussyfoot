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
import net.optionfactory.pussyfoot.extjs.DateFilterWithTimeZone;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

public class ZonedDateTimeInDayRange<TRoot> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<ZonedDateTime>> path;
    private final ObjectMapper objectMapper;

    public ZonedDateTimeInDayRange(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<ZonedDateTime>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> r, String value) {
        try {
            final DateFilterWithTimeZone filter = objectMapper.readValue(value, DateFilterWithTimeZone.class);
            final ZonedDateTime beginningOfDay = Instant.ofEpochMilli(filter.value).atZone(ZoneId.of(filter.timeZone)).truncatedTo(ChronoUnit.DAYS);
            final ZonedDateTime beginningOfNextDay = beginningOfDay.plus(1, ChronoUnit.DAYS);
            switch (filter.operator) {
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
                    throw new AssertionError(filter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
