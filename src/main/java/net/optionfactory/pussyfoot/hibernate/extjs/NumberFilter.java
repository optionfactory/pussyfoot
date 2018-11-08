package net.optionfactory.pussyfoot.hibernate.extjs;

import net.optionfactory.pussyfoot.extjs.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

/**
 * @deprecated replaced by
 * {@link Builder#withFilterComparator(java.lang.String, java.util.function.Function)}
 * in conjunction with {@link ExtJs#comparator }
 */
@Deprecated
public class NumberFilter<TRoot, T extends Number> implements JpaFilter<TRoot, String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path;
    private final ObjectMapper objectMapper;

    public NumberFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> root, String value) {
        try {
            final NumericFilter filter = objectMapper.readValue((String) value, NumericFilter.class);
            switch (filter.operator) {
                case lt:
                    return cb.lt(path.apply(cb, root), filter.value);
                case lte:
                    return cb.le(path.apply(cb, root), filter.value);
                case gt:
                    return cb.gt(path.apply(cb, root), filter.value);
                case gte:
                    return cb.ge(path.apply(cb, root), filter.value);
                case eq:
                    return cb.equal(path.apply(cb, root), filter.value);
                default:
                    throw new AssertionError(filter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
