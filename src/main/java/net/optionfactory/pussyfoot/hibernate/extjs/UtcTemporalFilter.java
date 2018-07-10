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
import javax.persistence.criteria.Root;

public class UtcTemporalFilter<TRoot, T extends Temporal & Comparable<? super T>> extends ComparableFilter<TRoot, T> {

    public UtcTemporalFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, Function<String, GenericComparableFilter<T>> transformer) {
        super(path, transformer);
    }

    public static GenericComparableFilter<Instant> toInstant(ObjectMapper objectMapper, String value) {
        try {
            final NumericFilter numericFilter = objectMapper.readValue(value, NumericFilter.class);
            return new GenericComparableFilter(Instant.ofEpochMilli(numericFilter.value), numericFilter.operator);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static GenericComparableFilter<LocalDate> toLocalDate(ObjectMapper objectMapper, String value) {
        try {
            final NumericFilter numericFilter = objectMapper.readValue(value, NumericFilter.class);
            final Instant referenceInstant = Instant.ofEpochMilli(numericFilter.value);
            return new GenericComparableFilter(LocalDateTime.ofInstant(referenceInstant, ZoneOffset.UTC).toLocalDate(), numericFilter.operator);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static GenericComparableFilter<LocalDateTime> toLocalDateTime(ObjectMapper objectMapper, String value) {
        try {
            final NumericFilter numericFilter = objectMapper.readValue(value, NumericFilter.class);
            final Instant referenceInstant = Instant.ofEpochMilli(numericFilter.value);
            return new GenericComparableFilter(LocalDateTime.ofInstant(referenceInstant, ZoneOffset.UTC), numericFilter.operator);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
