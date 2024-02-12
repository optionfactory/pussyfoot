package net.optionfactory.pussyfoot.hibernate;

import java.util.function.Function;
import net.optionfactory.pussyfoot.Pair;
import net.optionfactory.pussyfoot.hibernate.HibernatePsf.Builder;

public class FilterRequestProcessorPreBuilder<TRoot, TFilterRawValue, TFilterValue> {

    private final Builder<TRoot> builder;
    private final Pair<String, Class<TFilterRawValue>> filterKey;
    private final Function<TFilterRawValue, TFilterValue> filterValueAdapter;

    public FilterRequestProcessorPreBuilder(Builder<TRoot> builder, Pair<String, Class<TFilterRawValue>> filterKey) {
        this.builder = builder;
        this.filterKey = filterKey;
        this.filterValueAdapter = (TFilterRawValue x) -> (TFilterValue) x;
    }

    public FilterRequestProcessorPreBuilder(Builder<TRoot> builder, Pair<String, Class<TFilterRawValue>> filterKey, Function<TFilterRawValue, TFilterValue> filterValueAdapter) {
        this.builder = builder;
        this.filterKey = filterKey;
        this.filterValueAdapter = filterValueAdapter;
    }

    public Builder<TRoot> applyFilterExecutor(FilterExecutor<TRoot, TFilterValue> function) {
        return builder.withFilterRequestProcessor(new FilterRequestProcessor<>(filterKey, filterValueAdapter, function));
    }

    public <TCol> FilterRequestProcessorBuilder<TRoot, TFilterRawValue, TCol, TFilterValue> applyExecutor(SimpleExecutor<TCol, TFilterValue> function) {
        return new FilterRequestProcessorBuilder<>(builder, filterKey, filterValueAdapter, function);
    }

    public <TNewFilterValue> FilterRequestProcessorPreBuilder<TRoot, TFilterRawValue, TNewFilterValue> mappedTo(Function<TFilterValue, TNewFilterValue> valueMapper) {
        return new FilterRequestProcessorPreBuilder<>(builder, filterKey, this.filterValueAdapter.andThen(valueMapper));
    }
}
