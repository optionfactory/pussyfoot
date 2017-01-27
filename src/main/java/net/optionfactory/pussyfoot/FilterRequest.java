package net.optionfactory.pussyfoot;

public class FilterRequest {

    public final String name;
    public final String value;

    public FilterRequest(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "FilterRequest{" + "name=" + name + ", value=" + value + '}';
    }

}
