package net.optionfactory.pussyfoot.hibernate.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import java.io.IOException;
import java.util.EnumSet;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

@Deprecated
/**
 * @deprecated replaced by {@link EnumFilter}
 */
public class ExtjsEnumFilter<TRoot, T> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path;
    private final ObjectMapper objectMapper;
    private final CollectionLikeType enumSetType;

    public ExtjsEnumFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, Class<? extends Enum> enumClass, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
        this.enumSetType = objectMapper.getTypeFactory().constructCollectionLikeType(EnumSet.class, enumClass);
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> root, String value) {
        try {
            final EnumSet<?> items = objectMapper.readValue(value, enumSetType);
            if (items.isEmpty()) {
                return cb.conjunction();
            }
            return path.apply(cb, root).in(items);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
