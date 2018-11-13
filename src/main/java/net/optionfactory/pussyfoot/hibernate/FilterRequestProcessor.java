package net.optionfactory.pussyfoot.hibernate;


import java.util.function.Function;
import net.emaze.dysfunctional.tuples.Pair;
import net.optionfactory.pussyfoot.hibernate.PredicateBuilder;

public class FilterRequestProcessor<TRoot, TFilterRawValue, TFilterValue> {
    
    public final Pair<String, Class<?>> filterRequestKey;
    public final Function<TFilterRawValue, TFilterValue> filterValueAdapter;
    public final PredicateBuilder<TRoot, TFilterValue> predicateBuilder;

    public FilterRequestProcessor(
            Pair<String, Class<?>> filterRequestKey, 
            Function<TFilterRawValue, TFilterValue> filterValueAdapter, 
            PredicateBuilder<TRoot, TFilterValue> predicateBuilder) {
        this.filterRequestKey = filterRequestKey;
        this.filterValueAdapter = filterValueAdapter;
        this.predicateBuilder = predicateBuilder;
    }
    
}
