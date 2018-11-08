package net.optionfactory.pussyfoot.extjs;

import java.util.function.Function;

/**
 * Wraps the pair {@link Operator}-value, where the first defines the actual
 * comparison to be performed, and the second the value to be used in the
 * comparison
 *
 * @param <T> The type of the value to compare against
 */
public class Comparator<T> {

    /**
     * The {@link Operator} that defines the actual comparison to be performed
     */
    public Operator operator;

    /**
     * The value to compare against
     */
    public T value;

    /**
     * Static constructor for {@link Comparator}
     *
     * @param <T> The type of the value to compare against
     * @param operator The {@link Operator} that defines the actual comparison
     * to be performed
     * @param value The value to compare against
     * @return a new instance of {@link Comparator}
     */
    public static <T> Comparator<T> of(Operator operator, T value) {
        final Comparator res = new Comparator();
        res.operator = operator;
        res.value = value;
        return res;
    }

    public <R> Comparator<R> map(Function<T, R> valueMapper) {
        return Comparator.of(this.operator, valueMapper.apply(this.value));
    }
}
