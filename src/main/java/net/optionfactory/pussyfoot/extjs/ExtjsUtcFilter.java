package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

public class ExtjsUtcFilter<T extends Instant> implements HibernatePsf.JpaFilter {

    private final BiFunction<CriteriaBuilder, Root, Expression<T>> path;
    private final ObjectMapper objectMapper;

    public ExtjsUtcFilter(BiFunction<CriteriaBuilder, Root, Expression<T>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root root, Object value) {
        try {
            final DateFilter dateFilter = objectMapper.readValue((String) value, DateFilter.class);
            switch (dateFilter.operator) {
                case lt:
                    return cb.lessThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.timestamp));
                case gt:
                    return cb.greaterThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.timestamp));
                case eq:
                    return cb.and(cb.greaterThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.timestamp)),
                            cb.lessThan(path.apply(cb, root), Instant.ofEpochMilli(dateFilter.timestamp).plus(1, ChronoUnit.DAYS)));
                default:
                    throw new AssertionError(dateFilter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
