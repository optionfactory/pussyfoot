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
import net.optionfactory.pussyfoot.extjs.NumericFilter;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

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
            final Instant plusOneDay = truncatedToDay.plus(1, ChronoUnit.DAYS).toInstant();
            switch (numericFilter.operator) {
                case lt:
                    return cb.lessThan(path.apply(cb, r), truncatedToDay.toInstant());
                case lte:
                    return cb.lessThanOrEqualTo(path.apply(cb, r), plusOneDay);
                case gt:
                    return cb.greaterThan(path.apply(cb, r), plusOneDay);
                case gte:
                    return cb.greaterThanOrEqualTo(path.apply(cb, r), truncatedToDay.toInstant());
                case eq:
                    return cb.and(
                            cb.lessThanOrEqualTo(path.apply(cb, r), plusOneDay),
                            cb.greaterThanOrEqualTo(path.apply(cb, r), truncatedToDay.toInstant())
                    );
                default:
                    throw new AssertionError(numericFilter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
