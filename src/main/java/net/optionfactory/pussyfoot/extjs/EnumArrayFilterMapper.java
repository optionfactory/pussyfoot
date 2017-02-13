package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class EnumArrayFilterMapper<T extends Enum<T>> implements Function<String, Criterion> {

    private final ObjectMapper jackson;
    private final Class<T> enumClazz;
    private final String fieldName;

    public EnumArrayFilterMapper(String fieldName, Class enumClazz, ObjectMapper jackson) {
        this.fieldName = fieldName;
        this.enumClazz = enumClazz;
        this.jackson = jackson;
    }

    @Override
    public Criterion apply(String value) {
        try {
            final Criterion[] criterions = ((List<String>) jackson.readValue(value, List.class))
                    .stream()
                    .map(item -> Enum.valueOf(enumClazz, item))
                    .map(enumItem -> Restrictions.eq(fieldName, enumItem))
                    .toArray(Criterion[]::new);
            return Restrictions.or(criterions);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
