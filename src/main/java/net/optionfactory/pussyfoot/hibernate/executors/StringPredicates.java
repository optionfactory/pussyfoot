package net.optionfactory.pussyfoot.hibernate.executors;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

/**
 * Helper class that groups useful String-related predicates builders
 */
public class StringPredicates {

    /**
     * Defines an partial, case-insensitive match Predicate on string columns.
     *
     * <p>
     * PSQL equivalent when applied:<br>
     * ... where col ilike %:value:%
     * </p>
     *
     * @param cb query's {@link CriteriaBuilder}
     * @param path resolved column value
     * @param filterValue filter's value to query against
     * @return a JPA's {@link Predicate}
     */
    public static Predicate like(CriteriaBuilder cb, Expression<String> path, String filterValue) {

        return filterValue == null
                ? cb.disjunction()
                : cb.like(cb.lower(path), String.format("%%%s%%", filterValue.toLowerCase()));
    }

    /**
     * Tries to parse the filter's {@link String} value to the numeric type by
     * way of the provided mapper. If successful, matches the parsed value
     * against the resolved column's value. If the conversion is not successful,
     * returns a disjunction
     *
     * @param <T> The type of the column (must be a subclass of {@link Number})
     * @param cb query's {@link CriteriaBuilder}
     * @param path resolved column value
     * @param mapper {@link String}-to-column's type resolver
     * @param filterValue filter's {@link String} value
     * @return a JPA's {@link Predicate}
     */
    public static <T extends Number> Predicate matchesNumber(CriteriaBuilder cb, Expression<T> path, Function<String, Optional<T>> mapper, String filterValue) {
        return mapper.apply(filterValue).map(mappedFilterValue -> cb.equal(path, mappedFilterValue)).orElse(cb.disjunction());
    }

    /**
     * Specialization of {@link #matchesNumber} to be used against columns of
     * type {@link BigDecimal}
     *
     * @param cb query's {@link CriteriaBuilder}
     * @param path resolved column value
     * @param filterValue filter's {@link String} value
     * @return a JPA's {@link Predicate}
     */
    public static Predicate matchesBigDecimal(CriteriaBuilder cb, Expression<BigDecimal> path, String filterValue) {
        return matchesNumber(cb, path, val -> {
            try {
                return Optional.of(new BigDecimal(val));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }, filterValue);
    }

    /**
     * Specialization of {@link #matchesNumber} to be used against columns of
     * type {@link Integer}
     *
     * @param cb query's {@link CriteriaBuilder}
     * @param path resolved column value
     * @param filterValue filter's {@link String} value
     * @return a JPA's {@link Predicate}
     */
    public static Predicate matchesInt(CriteriaBuilder cb, Expression<Integer> path, String filterValue) {
        return matchesNumber(cb, path, val -> {
            try {
                return Optional.of(Integer.parseInt(val));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }, filterValue);
    }

    /**
     * Specialization of {@link #matchesNumber} to be used against columns of
     * type {@link Long}
     *
     * @param cb query's {@link CriteriaBuilder}
     * @param path resolved column value
     * @param filterValue filter's {@link String} value
     * @return a JPA's {@link Predicate}
     */
    public static Predicate matchesLong(CriteriaBuilder cb, Expression<Long> path, String filterValue) {
        return matchesNumber(cb, path, val -> {
            try {
                return Optional.of(Long.parseLong(val));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }, filterValue);
    }
}
