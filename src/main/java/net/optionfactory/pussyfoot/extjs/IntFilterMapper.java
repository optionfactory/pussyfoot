package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.Function;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class IntFilterMapper implements Function<String, Criterion> {
    
    private final String fieldName;
    private final ObjectMapper jackson;

    public IntFilterMapper(String fieldName, ObjectMapper jackson) {
        this.fieldName = fieldName;
        this.jackson = jackson;
    }

    @Override
    public Criterion apply(String value) {
        try {
            final NumberFilter filter = jackson.readValue(value, NumberFilter.class);
            switch (filter.operator) {
                case eq:
                    return Restrictions.eq(fieldName, (int) filter.number);
                case lt:
                    return Restrictions.lt(fieldName, (int) filter.number);
                case gt:
                    return Restrictions.gt(fieldName, (int) filter.number);
                default:
                    throw new RuntimeException(String.format("Invalid operator '%s'", filter.operator));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
