package net.optionfactory.pussyfoot.hibernate.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

/**
 * @deprecated replaced by
 * {@link Builder#withFilterIn(java.lang.String, java.util.function.Function)}
 * in conjunction with {@link ExtJs#valuesList }
 */
@Deprecated
public class ListFilter<TRoot, T> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path;
    private final ObjectMapper objectMapper;

    public ListFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> root, String value) {
        try {
            final List<?> listUnmarshalled = objectMapper.readValue(value, List.class);

            if (!listUnmarshalled.contains(null)) {
                return path.apply(cb, root).in(listUnmarshalled);
            }
            
            final List<?> withoutNulls = listUnmarshalled.stream().filter(i -> i != null).collect(Collectors.toList());
            if (withoutNulls.isEmpty()) {
                return path.apply(cb, root).isNull();
            }
            return cb.or(
                    path.apply(cb, root).in(withoutNulls),
                    path.apply(cb, root).isNull()
            );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
