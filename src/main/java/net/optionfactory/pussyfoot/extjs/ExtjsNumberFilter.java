package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

public class ExtjsNumberFilter<T extends Number> implements HibernatePsf.JpaFilter {

    private final BiFunction<CriteriaBuilder, Root, Expression<T>> path;
    private final ObjectMapper objectMapper;

    public ExtjsNumberFilter(BiFunction<CriteriaBuilder, Root, Expression<T>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root root, Object value) {
        try {
            final NumberFilter numericFilter = objectMapper.readValue((String) value, NumberFilter.class);
            switch (numericFilter.operator) {
                case lt:
                    return cb.le(path.apply(cb, root), numericFilter.number);
                case gt:
                    return cb.gt(path.apply(cb, root), numericFilter.number);
                case eq:
                    return cb.equal(path.apply(cb, root), numericFilter.number);
                default:
                    throw new AssertionError(numericFilter.operator.name());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
