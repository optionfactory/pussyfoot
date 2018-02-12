package net.optionfactory.pussyfoot.extjs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

public class ExtjsListFilter<T> implements HibernatePsf.JpaFilter {

    private final BiFunction<CriteriaBuilder, Root, Expression<T>> path;
    private final ObjectMapper objectMapper;

    public ExtjsListFilter(BiFunction<CriteriaBuilder, Root, Expression<T>> path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
    }

    @Override
    public Predicate predicateFor(CriteriaBuilder cb, Root root, Object value) {
        try {
            final List listUnmarshalled = objectMapper.readValue((String) value, List.class);
            return path.apply(cb, root).in(listUnmarshalled);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
