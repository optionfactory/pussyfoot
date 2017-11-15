package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import java.io.IOException;
import java.util.EnumSet;
import java.util.function.Function;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class EnumArrayFilterMapper implements Function<String, Criterion> {

    private final ObjectMapper jackson;
    private final String fieldName;
    private final CollectionLikeType enumSetType;

    public EnumArrayFilterMapper(String fieldName, Class<? extends Enum> klass, ObjectMapper jackson) {
        this.jackson = jackson;
        this.fieldName = fieldName;
        this.enumSetType = jackson.getTypeFactory().constructCollectionLikeType(EnumSet.class, klass);
    }

    @Override
    public Criterion apply(String value) {
        try {
            final Criterion[] criterions = ((EnumSet<?>) jackson.readValue(value, enumSetType))
                    .stream()
                    .map((Enum<?> item) -> Restrictions.eq(fieldName, item))
                    .toArray(Criterion[]::new);
            return Restrictions.or(criterions);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
