package net.optionfactory.pussyfoot.hibernate.extjs;

import net.optionfactory.pussyfoot.extjs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

public class ExtjsUtcTemporalFilter<TRoot, T extends Temporal & Comparable<? super T>> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path;
    private final ObjectMapper objectMapper;
    private final Function<DateFilter, T> transformer;

    public ExtjsUtcTemporalFilter(
            BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path,
            ObjectMapper objectMapper,
            Function<DateFilter, T> transformer
    ) {
        this.path = path;
        this.objectMapper = objectMapper;
        this.transformer = transformer;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> root, String value) {
        try {
            final DateFilter dateFilter = objectMapper.readValue(value, DateFilter.class);
            final T ref = transformer.apply(dateFilter);
            switch (dateFilter.operator) {
                case lt:
                    return cb.lessThan(path.apply(cb, root), ref);
                case lte:
                    return cb.lessThanOrEqualTo(path.apply(cb, root), ref);
                case gt:
                    return cb.greaterThan(path.apply(cb, root), ref);
                case gte:
                    return cb.greaterThanOrEqualTo(path.apply(cb, root), ref);
                case eq:
                    return cb.equal(path.apply(cb, root), ref);
                default:
                    throw new AssertionError(dateFilter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static LocalDate toLocalDate(DateFilter df) {
        final Instant referenceInstant = Instant.ofEpochMilli(df.timestamp);
        return LocalDateTime.ofInstant(referenceInstant, ZoneOffset.UTC).toLocalDate();
    }

    public static LocalDateTime toLocalDateTime(DateFilter df) {
        final Instant referenceInstant = Instant.ofEpochMilli(df.timestamp);
        return LocalDateTime.ofInstant(referenceInstant, ZoneOffset.UTC);
    }

}
