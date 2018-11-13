package net.optionfactory.pussyfoot.hibernate;

import java.util.function.Function;
import net.emaze.dysfunctional.tuples.Pair;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;

public class FilterRequestProcessorPreBuilder<TRoot, TFilterRawValue extends Object, TFilterValue> {

    private final Builder<TRoot> builder;
    private final Pair<String, Class<? extends Object>> filterKey;
    private final Function<TFilterRawValue, TFilterValue> filterValueAdapter;

    public FilterRequestProcessorPreBuilder(Builder<TRoot> builder, Pair<String, Class<? extends Object>> filterKey) {
        this.builder = builder;
        this.filterKey = filterKey;
        this.filterValueAdapter = (TFilterRawValue x) -> (TFilterValue) x;
    }

    public FilterRequestProcessorPreBuilder(Builder<TRoot> builder, Pair<String, Class<? extends Object>> filterKey, Function<TFilterRawValue, TFilterValue> filterValueAdapter) {
        this.builder = builder;
        this.filterKey = filterKey;
        this.filterValueAdapter = filterValueAdapter;
    }

    public Builder<TRoot> applyCustomFilter(CustomPredicateBuilder<TRoot, TFilterValue> function) {
        return builder.withFilterRequestProcessor(new FilterRequestProcessor<>(filterKey, filterValueAdapter, function));
    }

    public <TCol> FilterRequestProcessorBuilder<TRoot, TFilterRawValue, TCol, TFilterValue> applyPredicate(SimplePredicateBuilder<TCol, TFilterValue> function) {
        return new FilterRequestProcessorBuilder<>(builder, filterKey, filterValueAdapter, function);
    }

    public <TNewFilterValue> FilterRequestProcessorPreBuilder<TRoot, TFilterRawValue, TNewFilterValue> mappedTo(Function<TFilterValue, TNewFilterValue> valueMapper) {
        return new FilterRequestProcessorPreBuilder<>(builder, filterKey, this.filterValueAdapter.andThen(valueMapper));
    }
}
