package net.optionfactory.pussyfoot.extjs;

public class GenericComparableFilter<T extends Comparable<? super T>> {

    public T value;
    public Operator operator;

    public GenericComparableFilter(T value, Operator operator) {
        this.value = value;
        this.operator = operator;
    }

    public GenericComparableFilter() {
    }

}
