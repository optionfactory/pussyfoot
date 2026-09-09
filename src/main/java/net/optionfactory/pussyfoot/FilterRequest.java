package net.optionfactory.pussyfoot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Used to request filtered data.
 * @param <T> The type of the filter's value
 */
public class FilterRequest<T> {

    public final String name;
    public final T value;

    /**
     *
     * @param name
     *  The name of the filter to be applied
     * @param value
     *  The value to be passed to the filter referenced
     */
    @JsonCreator
    public FilterRequest(@JsonProperty("name") String name, @JsonProperty("value") T value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public String toString() {
        return "FilterRequest{" + "name=" + name + ", value=" + value + '}';
    }

}
