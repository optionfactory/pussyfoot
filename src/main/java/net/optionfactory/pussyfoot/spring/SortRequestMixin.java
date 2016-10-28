package net.optionfactory.pussyfoot.spring;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.optionfactory.pussyfoot.SortRequest.Direction;

public abstract class SortRequestMixin {
    
    @JsonCreator
    public SortRequestMixin(@JsonProperty("name") String name, @JsonProperty("direction") Direction direction) {
    }    

}
