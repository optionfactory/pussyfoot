package net.optionfactory.pussyfoot.hibernate;

import java.math.BigDecimal;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

public class Predicates {

    public static Predicate like(CriteriaBuilder cb, Expression<String> path, String v) {
        return cb.like(cb.lower(path), ('%' + v + '%').toLowerCase());
    }

    public static <T extends Number> Predicate matchesNumber(CriteriaBuilder cb, Expression<T> path, Function<String, T> mapper, String v) {
        try {
            final T i = mapper.apply(v);
            return cb.equal(path, i);
        } catch (NumberFormatException ex) {
            return cb.or();
        }
    }

    public static Predicate matchesBigDecimal(CriteriaBuilder cb, Expression<BigDecimal> path, String v) {
        return matchesNumber(cb, path, val -> new BigDecimal(val), v);
    }

    public static Predicate matchesInt(CriteriaBuilder cb, Expression<Integer> path, String v) {
        return matchesNumber(cb, path, val -> Integer.parseInt(val), v);
    }

    public static Predicate matchesLong(CriteriaBuilder cb, Expression<Long> path, String v) {
        return matchesNumber(cb, path, val -> Long.parseLong(val), v);
    }
}
