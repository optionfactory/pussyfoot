package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Function;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class LocalDateMapper implements Function<String, Criterion> {

    private final String fieldName;
    private final ObjectMapper jackson;

    public LocalDateMapper(String fieldName, ObjectMapper jackson) {
        this.fieldName = fieldName;
        this.jackson = jackson;
    }

    @Override
    public Criterion apply(String value) {
        try {
            final LocalDateFilter dateFilter = jackson.readValue(value, LocalDateFilter.class);
            switch (dateFilter.operator) {
                case eq:
                    return Restrictions.eq(fieldName, dateFilter.date);
                case lt:
                    return Restrictions.lt(fieldName, dateFilter.date);
                case gt:
                    return Restrictions.gt(fieldName, dateFilter.date);
                default:
                    throw new RuntimeException(String.format("Invalid operator '%s'", dateFilter.operator));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
