package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class DateOverTimestampMapper implements Function<String, Criterion> {

    private final String fieldName;
    private final ObjectMapper jackson;

    public DateOverTimestampMapper(String fieldName, ObjectMapper jackson) {
        this.fieldName = fieldName;
        this.jackson = jackson;
    }

    @Override
    public Criterion apply(String value) {
        try {
            final DateFilter dateFilter = jackson.readValue(value, DateFilter.class);
            switch (dateFilter.operator) {
                case eq:
                    return Restrictions.conjunction(
                            Restrictions.gt(fieldName, dateFilter.timestamp),
                            Restrictions.lt(fieldName, LocalDateTime.ofInstant(Instant.ofEpochMilli(dateFilter.timestamp), ZoneOffset.UTC)
                                    .plus(1, ChronoUnit.DAYS).toInstant(ZoneOffset.UTC).toEpochMilli()));
                case lt:
                    return Restrictions.lt(fieldName, dateFilter.timestamp);
                case gt:
                    return Restrictions.gt(fieldName, dateFilter.timestamp);
                default:
                    throw new RuntimeException(String.format("Invalid operator '%s'", dateFilter.operator));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
