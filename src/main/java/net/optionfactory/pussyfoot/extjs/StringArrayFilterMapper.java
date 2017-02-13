package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class StringArrayFilterMapper implements Function<String, Criterion> {

    private final ObjectMapper jackson;
    private final String fieldName;

    public StringArrayFilterMapper(String fieldName, ObjectMapper jackson) {
        this.jackson = jackson;
        this.fieldName = fieldName;
    }

    @Override
    public Criterion apply(String value) {
        try {
            final Criterion[] criterions = ((List<String>) jackson.readValue(value, List.class))
                    .stream()
                    .map((java.lang.String item) -> Restrictions.eq(fieldName, item))
                    .toArray(Criterion[]::new);
            return Restrictions.or(criterions);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
