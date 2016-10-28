package net.optionfactory.pussyfoot.spring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class FilterRequestMixin {

    @JsonCreator
    public FilterRequestMixin(@JsonProperty("name") String name, @JsonProperty("value") String value) {
    }

}
