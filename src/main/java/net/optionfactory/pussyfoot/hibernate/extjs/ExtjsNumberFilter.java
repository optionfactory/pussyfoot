package net.optionfactory.pussyfoot.hibernate.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.extjs.ExtJs;
import net.optionfactory.pussyfoot.extjs.NumericFilter;
import static net.optionfactory.pussyfoot.extjs.Operator.eq;
import static net.optionfactory.pussyfoot.extjs.Operator.gt;
import static net.optionfactory.pussyfoot.extjs.Operator.gte;
import static net.optionfactory.pussyfoot.extjs.Operator.lte;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;
import net.optionfactory.pussyfoot.hibernate.JpaFilter;

/**
 * @deprecated replaced by
 * {@link Builder#withFilterComparator(java.lang.String, java.util.function.Function)}
 * in conjunction with {@link ExtJs#comparator }
 */
@Deprecated
public class ExtjsNumberFilter<TRoot,T extends Number> implements JpaFilter<TRoot,String> {

    private final BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path;
    private final ObjectMapper objectMapper;

    public ExtjsNumberFilter(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<T>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root<TRoot> root, String value) {
        try {
            final NumericFilter numericFilter = objectMapper.readValue((String) value, NumericFilter.class);
            switch (numericFilter.operator) {
                case lt:
                    return cb.lt(path.apply(cb, root), numericFilter.value);
                case lte:
                    return cb.le(path.apply(cb, root), numericFilter.value);
                case gt:
                    return cb.gt(path.apply(cb, root), numericFilter.value);
                case gte:
                    return cb.ge(path.apply(cb, root), numericFilter.value);
                case eq:
                    return cb.equal(path.apply(cb, root), numericFilter.value);
                default:
                    throw new AssertionError(numericFilter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
