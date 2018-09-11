package net.optionfactory.pussyfoot.hibernate.extjs;

import net.optionfactory.pussyfoot.extjs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

/**
 * @deprecated replaced by {@link UtcInstantInDayRange}
 */
public class ExtjsUtcFilter<TRoot, T extends Instant> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path;
    private final ObjectMapper objectMapper;

    public ExtjsUtcFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> root, String value) {
        try {
            final NumericFilter dateFilter = objectMapper.readValue(value, NumericFilter.class);
            switch (dateFilter.operator) {
                case lt:
                    return cb.lessThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.value));
                case lte:
                    return cb.lessThanOrEqualTo(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.value));
                case gt:
                    return cb.greaterThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.value));
                case gte:
                    return cb.greaterThanOrEqualTo(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.value));
                case eq:
                    return cb.and(cb.greaterThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.value)),
                            cb.lessThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.value).plus(1, ChronoUnit.DAYS)));
                default:
                    throw new AssertionError(dateFilter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
