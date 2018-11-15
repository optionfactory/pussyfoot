package net.optionfactory.pussyfoot.hibernate;

import java.util.function.Function;
import net.emaze.dysfunctional.tuples.Pair;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;

/**
 * Defines everything that is needed to respond to a {@link PageRequest}'s
 * {@link FilterRequest}
 *
 * @param <TRoot> the type of the query's {@link Root} object
 * @param <TFilterRawValue> The type of the {@link FilterRequest#value} this
 * processor applies to
 * @param <TFilterValue> the type of the filter's value, after adaptation, to be
 * applied against the database
 */
public class FilterRequestProcessor<TRoot, TFilterRawValue, TFilterValue> {

    protected final Pair<String, Class<TFilterRawValue>> filterRequestKey;
    protected final Function<TFilterRawValue, TFilterValue> filterValueAdapter;
    protected final FilterExecutor<TRoot, TFilterValue> filterExecutor;

    /**
     * Default constructor
     *
     * @param filterRequestKey when to apply this filter (as in: this processor
     * reacts to requests with {@link FilterRequest#name} and the type of
     * {@link FilterRequest#value} matching the pair provided)
     * @param filterValueAdapter how to adapt (e.g.: deserialize) the
     * {@link FilterRequest#value} before applying it against the database
     * @param filterExecutor the actual function being applied
     * @param <TRoot> the type of the query's {@link Root} object
     * @param <TFilterRawValue> The type of the {@link FilterRequest#value} this
     * processor applies to
     * @param <TFilterValue> the type of the filter's value, after adaptation,
     * to be applied against the database
     */
    public FilterRequestProcessor(
            Pair<String, Class<TFilterRawValue>> filterRequestKey,
            Function<TFilterRawValue, TFilterValue> filterValueAdapter,
            FilterExecutor<TRoot, TFilterValue> filterExecutor) {
        this.filterRequestKey = filterRequestKey;
        this.filterValueAdapter = filterValueAdapter;
        this.filterExecutor = filterExecutor;
    }

}
