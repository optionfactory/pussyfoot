package net.optionfactory.pussyfoot;

public class FilterRequest<T> {

    public final String name;
    public final T value;

    public FilterRequest(String name, T value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "FilterRequest{" + "name=" + name + ", value=" + value + '}';
    }

}
