package net.optionfactory.pussyfoot.hibernate.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.extjs.GenericComparableFilter;
import net.optionfactory.pussyfoot.extjs.NumericFilter;

public class UtcLocalDateFilter<TRoot> extends ComparableFilter<TRoot, LocalDate> {

    public UtcLocalDateFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<LocalDate>> path, ObjectMapper objectMapper) {
        super(path, (java.lang.String value) -> {
            try {
                final NumericFilter numericFilter = objectMapper.readValue(value, NumericFilter.class);
                return new GenericComparableFilter<>(LocalDateTime.ofInstant(Instant.ofEpochMilli(numericFilter.value), ZoneOffset.UTC).toLocalDate(), numericFilter.operator);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

}
