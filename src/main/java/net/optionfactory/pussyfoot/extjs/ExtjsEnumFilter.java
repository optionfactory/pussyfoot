package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import java.io.IOException;
import java.util.EnumSet;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

public class ExtjsEnumFilter<T> implements HibernatePsf.JpaFilter {

    private final BiFunction<CriteriaBuilder, Root, Expression<T>> path;
    private final ObjectMapper objectMapper;
    private final CollectionLikeType enumSetType;

    public ExtjsEnumFilter(BiFunction<CriteriaBuilder, Root, Expression<T>> path, Class<? extends Enum> enumClass, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
        this.enumSetType = objectMapper.getTypeFactory().constructCollectionLikeType(EnumSet.class, enumClass);
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root root, Object value) {
        try {
            final EnumSet<?> items = objectMapper.readValue((String) value, enumSetType);
            return path.apply(cb, root).in(items);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
