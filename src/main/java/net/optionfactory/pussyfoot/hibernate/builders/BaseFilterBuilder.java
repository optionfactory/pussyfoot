package net.optionfactory.pussyfoot.hibernate.builders;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf;

/**
 * Defines the basis of a FilterBuilder, and provides all the user-convenient
 * overloads to select one or more columns, while leaving the filter-specific
 * logic to the subclass specific implementation
 *
 * @param <TRoot> The type of the root object the query starts from
 * @param <TRawValue> The type of the value of the {@link PageRequest}'s
 * {@link FilterRequest}, before adaptation (e.g.: received from an API)
 * @param <TValue> The type of the value of the {@link PageRequest}'s
 * {@link FilterRequest}, after being adapted (e.g.: deserialized)
 * @param <TCol> The type of the column this filter will apply to
 */
public abstract class BaseFilterBuilder<TRoot, TRawValue, TValue, TCol> {

    private final HibernatePsf.Builder<TRoot> builder;
    private final String filterName;
    private final Function<TRawValue, TValue> filterValueAdapter;

    /**
     * Main (and only) constructor
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TRawValue> The type of the value of the {@link PageRequest}'s
     * {@link FilterRequest}, before adaptation (e.g.: received from an API)
     * @param <TValue> The type of the value of the {@link PageRequest}'s
     * {@link FilterRequest}, after being adapted (e.g.: deserialized)
     * @param builder The external builder which is going to be modified (and
     * ultimately returned)
     * @param filterName The name of the filter, once built
     * @param filterValueAdapter The function that converts Raw values of type
     * TRawValue to values in the final form of type TValue, before being passed
     * to the inheriting filter builder implementation
     */
    protected BaseFilterBuilder(HibernatePsf.Builder<TRoot> builder, String filterName, Function<TRawValue, TValue> filterValueAdapter) {
        this.builder = builder;
        this.filterName = filterName;
        this.filterValueAdapter = filterValueAdapter;
    }

    /**
     * The method to be overridden to implement the filter-specific logic
     *
     * @param <TCol> The type of the column this filter will apply to
     * @param <TValue> The type of the value of the {@link PageRequest}'s
     * {@link FilterRequest}, after being adapted (e.g.: deserialized)
     * @param cb JPA's {@link javax.persistence.criteria.CriteriaBuilder}
     * @param path An expression of type TCol (straight column or calculated
     * value) to be filtered against
     * @param filterValue The value of the filter (after potential conversion
     * from raw to actual value) to be used for filtering
     * @return A JPA's {@link javax.persistence.criteria.Predicate}
     */
    protected abstract Predicate createPredicate(CriteriaBuilder cb, final Expression<TCol> path, final TValue filterValue);

    /**
     * Applies the filter to the value resolved by the function provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param pathResolver The function that, given a
     * {@link javax.persistence.criteria.CriteriaBuilder} and the query's
     * {@link  javax.persistence.criteria.Root} provides the value (e.g.:
     * column) to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> on(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver) {
        return builder.withFilter(this.filterName, (CriteriaBuilder cb, Root<TRoot> root, TRawValue filterRawValue) -> {
            final Expression<TCol> path = pathResolver.apply(cb, root);
            final TValue filterValue = filterValueAdapter.apply(filterRawValue);
            return createPredicate(cb, path, filterValue);
        });
    }

    /**
     * Applies the filter to the value resolved by the function provided. When
     * the value is null (e.g.: nullable column), uses the provided value
     * instead
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param pathResolver The function that, given a
     * {@link javax.persistence.criteria.CriteriaBuilder} and the query's
     * {@link  javax.persistence.criteria.Root} provides the value (e.g.:
     * column) to be filtered against
     * @param defaultIfNull The value to be used instead of a null value
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onNullable(BiFunction<CriteriaBuilder, Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return on((CriteriaBuilder cb, Root<TRoot> r) -> {
            final Expression<TCol> path = pathResolver.apply(cb, r);
            return cb.coalesce(path, defaultIfNull);
        });
    }

    /**
     * Applies the filter to the list of values resolved by the function
     * provided - at least one of which must match
     *
     * @param pathsResolver The function that, given a
     * {@link javax.persistence.criteria.CriteriaBuilder} and the query's
     * {@link  javax.persistence.criteria.Root} provides the values (e.g.:
     * columns) to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onEither(BiFunction<CriteriaBuilder, Root<TRoot>, List<Expression<TCol>>> pathsResolver) {
        return this.builder.withFilter(this.filterName, (CriteriaBuilder cb, Root<TRoot> root, TRawValue filterRawValue) -> {
            final List<Expression<TCol>> paths = pathsResolver.apply(cb, root);
            final TValue filterValue = filterValueAdapter.apply(filterRawValue);
            return paths.stream().map((path) -> createPredicate(cb, path, filterValue)).collect(Collectors.reducing(cb.disjunction(), (p1, p2) -> cb.or(p1, p2)));
        });
    }

    /**
     * Applies the filter to the value resolved by the function provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param pathResolver The function that, given the query's
     * {@link  javax.persistence.criteria.Root} provides the value (e.g.:
     * column) to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> on(Function<Root<TRoot>, Expression<TCol>> pathResolver) {
        return on((cb, r) -> pathResolver.apply(r));
    }

    /**
     * Applies the filter to the value resolved by the function provided. When
     * the value is null (e.g.: nullable column), uses the provided value
     * instead
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param pathResolver The function that, given the query's
     * {@link  javax.persistence.criteria.Root} provides the value (e.g.:
     * column) to be filtered against
     * @param defaultIfNull The value to be used instead of a null value
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onNullable(Function<Root<TRoot>, Expression<TCol>> pathResolver, TCol defaultIfNull) {
        return onNullable((cb, r) -> pathResolver.apply(r), defaultIfNull);
    }

    /**
     * Applies the filter to the list of values resolved by the function
     * provided - at least one of which must match
     *
     * @param pathsResolver The function that, given the query's
     * {@link  javax.persistence.criteria.Root} provides the values (e.g.:
     * columns) to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onEither(Function<Root<TRoot>, List<Expression<TCol>>> pathsResolver) {
        return onEither((cb, r) -> pathsResolver.apply(r));
    }

    /**
     * Applies the filter to the column provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param column The column to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> on(SingularAttribute<TRoot, TCol> column) {
        return on((cb, r) -> r.get(column));
    }

    /**
     * Applies the filter to the column provided. When the column's value is
     * null (e.g.: nullable column), uses the provided value instead
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param column The column to be filtered against
     * @param defaultIfNull The value to be used instead of a null value
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onNullable(SingularAttribute<TRoot, TCol> column, TCol defaultIfNull) {
        return onNullable((cb, r) -> r.get(column), defaultIfNull);
    }

    /**
     * Applies the filter to the list of columns provided - at least one of
     * which must match
     *
     * @param columns The columns to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onEither(List<SingularAttribute<TRoot, TCol>> columns) {
        return onEither((CriteriaBuilder cb, Root<TRoot> r) -> columns.stream().map((p) -> r.get(p)).collect(Collectors.toList()));
    }

    /**
     * Applies the filter to the column that matches the name provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param columnName The name of the column to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> on(String columnName) {
        return on((cb, r) -> r.get(columnName));
    }

    /**
     * Applies the filter to the column that matches the filter name previously
     * provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onColumnWithSameName() {
        return on((cb, r) -> r.get(filterName));
    }

    /**
     * Applies the filter to the column that matches the name provided. When the
     * column's value is null (e.g.: nullable column), uses the provided value
     * instead
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param columnName The name of the column to be filtered against
     * @param defaultIfNull The value to be used instead of a null value
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onNullable(String columnName, TCol defaultIfNull) {
        return onNullable((cb, r) -> r.get(columnName), defaultIfNull);
    }

    /**
     * Applies the filter to the column that matches the filter name previously
     * provided. When the column's value is null (e.g.: nullable column), uses
     * the provided value instead
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param defaultIfNull The value to be used instead of a null value
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onNullableColumnWithSameName(TCol defaultIfNull) {
        return onNullable((cb, r) -> r.get(filterName), defaultIfNull);
    }

    /**
     * Applies the filter to the second-level column identified through the
     * chain of columns provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param <TCHILD> The type of the first-level column traversed to get to
     * the leaf column
     * @param firstLevelColumn The first-level column traversed to get to the
     * leaf column
     * @param leafColumn The (second-level) column to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public <TCHILD> HibernatePsf.Builder<TRoot> on(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, TCol> leafColumn) {
        return on((cb, r) -> r.get(firstLevelColumn).get(leafColumn));
    }

    /**
     * Applies the filter to the third-level column identified through the chain
     * of columns provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param <TCHILD> The type of the first-level column traversed to get to
     * the leaf column
     * @param <TGRANDCHILD> The type of the second-level column traversed to get
     * to the leaf column
     * @param firstLevelColumn The first-level column traversed to get to the
     * leaf column
     * @param secondLevelColumn The second-level column traversed to get to the
     * leaf column
     * @param leafColumn The (second-level) column to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public <TCHILD, TGRANDCHILD> HibernatePsf.Builder<TRoot> on(SingularAttribute<TRoot, TCHILD> firstLevelColumn, SingularAttribute<TCHILD, TGRANDCHILD> secondLevelColumn, SingularAttribute<TGRANDCHILD, TCol> leafColumn) {
        return on((cb, r) -> r.get(firstLevelColumn).get(secondLevelColumn).get(leafColumn));
    }

    /**
     * Applies the filter to the column identified through the chain of column
     * names provided
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param firstColumnName The name of the first column on the chain
     * @param columnNames the list of column names to be traversed (in order) to
     * ultimately retrieve the column to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> on(String firstColumnName, String... columnNames) {
        return on((CriteriaBuilder cb, Root<TRoot> r)
                -> Stream.of(columnNames)
                        .reduce(r.get(firstColumnName), Path::get, (p1, p2) -> {
                            throw new IllegalStateException("Can't parallelize this!");
                        }));
    }

    /**
     * Applies the filter to the list of columns provided (retrieved on a
     * name-basis) - at least one of which must match
     *
     * @param <TRoot> The type of the root object the query starts from
     * @param <TCol> The type of the column this filter will apply to
     * @param columnNames The names of the columns to be filtered against
     * @return The main builder, in order to chain further filters/sorterers
     */
    public HibernatePsf.Builder<TRoot> onEitherByName(List<String> columnNames) {
        return onEither((cb, r) -> columnNames
                .stream()
                .map((cn) -> r.<TCol>get(cn))
                .collect(Collectors.toList()));
    }

}
