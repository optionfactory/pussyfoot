package net.optionfactory.pussyfoot.extjs;

import java.util.function.Function;

/**
 * Wraps the pair {@link Operator}-value, where the first defines the actual
 * comparison to be performed, and the second the value to be used in the
 * comparison
 *
 * @param <T> The type of the value to compare against
 */
public class Comparison<T extends Comparable<? super T>> {

    /**
     * The {@link Operator} that defines the actual comparison to be performed
     */
    public Operator operator;

    /**
     * The value to compare against
     */
    public T value;

    /**
     * Static constructor for {@link Comparison}
     *
     * @param <T> The type of the value to compare against
     * @param operator The {@link Operator} that defines the actual comparison
     * to be performed
     * @param value The value to compare against
     * @return a new instance of {@link Comparison}
     */
    public static <T extends Comparable<? super T>> Comparison<T> of(Operator operator, T value) {
        final Comparison res = new Comparison();
        res.operator = operator;
        res.value = value;
        return res;
    }

    public <R extends Comparable<? super R>> Comparison<R> map(Function<T, R> valueMapper) {
        return Comparison.of(this.operator, valueMapper.apply(this.value));
    }
}
