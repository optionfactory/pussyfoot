package net.optionfactory.pussyfoot.hibernate.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.extjs.GenericComparableFilter;
import net.optionfactory.pussyfoot.extjs.NumericFilter;

public class UtcInstantFilter<TRoot> extends ComparableFilter<TRoot, Instant> {

    public UtcInstantFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<Instant>> path, ObjectMapper objectMapper) {
        super(path, (java.lang.String value) -> {
            try {
                final NumericFilter numericFilter = objectMapper.readValue(value, NumericFilter.class);
                return new GenericComparableFilter<>(Instant.ofEpochMilli(numericFilter.value), numericFilter.operator);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

}
