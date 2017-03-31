package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class DateOverInstantMapper implements Function<String, Criterion> {

    private final String fieldName;
    private final ObjectMapper jackson;

    public DateOverInstantMapper(String fieldName, ObjectMapper jackson) {
        this.fieldName = fieldName;
        this.jackson = jackson;
    }

    @Override
    public Criterion apply(String value) {
        try {
            final DateFilter dateFilter = jackson.readValue(value, DateFilter.class);
            final Instant filterValue = Instant.ofEpochMilli(dateFilter.timestamp).truncatedTo(ChronoUnit.DAYS);
            switch (dateFilter.operator) {
                case eq:
                    return Restrictions.conjunction(
                            Restrictions.gt(fieldName, filterValue),
                            Restrictions.lt(fieldName, filterValue.atZone(ZoneId.of("UTC")).plus(Period.ofDays(1)).toInstant()));
                case lt:
                    return Restrictions.lt(fieldName, filterValue);
                case gt:
                    return Restrictions.gt(fieldName, filterValue);
                default:
                    throw new RuntimeException(String.format("Invalid operator '%s'", dateFilter.operator));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
