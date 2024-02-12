package net.optionfactory.pussyfoot;

import java.util.Objects;
import java.util.function.Function;

public class Pair<T1, T2> {

    private final T1 first;
    private final T2 second;

    public Pair(T1 f, T2 l) {
        this.first = f;
        this.second = l;
    }

    public T1 first() {
        return first;
    }

    public T2 second() {
        return second;
    }

    public Pair<T2, T1> flip() {
        return Pair.of(second, first);
    }

    public <R1, R2> Pair<R1, R2> map(Function<T1, R1> withFirst, Function<T2, R2> withSecond) {
        return Pair.of(withFirst.apply(first), withSecond.apply(second));
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof Pair == false) {
            return false;
        }
        final Pair<T1, T2> other = (Pair<T1, T2>) rhs;
        return Objects.equals(this.first, other.first)
                && Objects.equals(this.second, other.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return String.format("(%s,%s)", first, second);
    }

    public static <T1, T2> Pair<T1, T2> of(T1 first, T2 second) {
        return new Pair<T1, T2>(first, second);
    }
}
