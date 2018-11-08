package net.optionfactory.pussyfoot.hibernate;

import java.math.BigDecimal;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

/**
 * @deprecated replaced by {@link StringPredicates}
 */
@Deprecated
public class Predicates {

    /**
     * @deprecated replaced by {@link StringPredicates#like}
     */
    @Deprecated
    public static Predicate like(CriteriaBuilder cb, Expression<String> path, String filterValue) {

        return filterValue == null
                ? cb.disjunction()
                : cb.like(cb.lower(path), String.format("%%%s%%", filterValue.toLowerCase()));
    }

    /**
     * @deprecated replaced by {@link StringPredicates#matchesNumber}
     */
    @Deprecated
    public static <T extends Number> Predicate matchesNumber(CriteriaBuilder cb, Expression<T> path, Function<String, T> mapper, String filterValue) {
        try {
            final T i = mapper.apply(filterValue);
            return cb.equal(path, i);
        } catch (NumberFormatException ex) {
            return cb.or();
        }
    }

    /**
     * @deprecated replaced by {@link StringPredicates#matchesBigDecimal}
     */
    @Deprecated
    public static Predicate matchesBigDecimal(CriteriaBuilder cb, Expression<BigDecimal> path, String filterValue) {
        return matchesNumber(cb, path, val -> new BigDecimal(val), filterValue);
    }

    /**
     * @deprecated replaced by {@link StringPredicates#matchesInt}
     */
    @Deprecated
    public static Predicate matchesInt(CriteriaBuilder cb, Expression<Integer> path, String v) {
        return matchesNumber(cb, path, val -> Integer.parseInt(val), v);
    }

    /**
     * @deprecated replaced by {@link StringPredicates#matchesLong}
     */
    @Deprecated
    public static Predicate matchesLong(CriteriaBuilder cb, Expression<Long> path, String v) {
        return matchesNumber(cb, path, val -> Long.parseLong(val), v);
    }
}
