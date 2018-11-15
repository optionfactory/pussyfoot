package net.optionfactory.pussyfoot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Request for a page of records, with the ability to specify filters, sorters,
 * and page start/page size parameters
 */
public class AbsolutePageRequest {

    /**
     * Placeholder for a no-sorters definition
     */
    public static final SortRequest[] NO_SORTERS = new SortRequest[0];
    /**
     * Placeholder for a no-filters definition
     */
    public static final FilterRequest<?>[] NO_FILTERS = new FilterRequest[0];

    /**
     * Defines the slice of data (in terms of page start and page size) to
     * return
     */
    public final AbsoluteSliceRequest slice;
    /**
     * Defines the sorters to be applied before paging the records
     */
    public final SortRequest[] sorters;
    /**
     * Defines the filters to be applied before paging the records
     */
    public final FilterRequest<?>[] filters;

    /**
     *
     * @param slice the slice of data (in terms of page start and page size) to
     * return
     * @param sorters the sorters to be applied before paging the records
     * @param filters the filters to be applied before paging the records
     */
    public AbsolutePageRequest(AbsoluteSliceRequest slice, SortRequest[] sorters, FilterRequest<?>[] filters) {
        this.slice = slice;
        this.sorters = sorters;
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "AbsolutePageRequest{" + "slice=" + slice + ", sorters=" + sorters + ", filters=" + filters + '}';
    }


    /**
     * Static method to instantiate a {@link Builder} to easily create a new
     * {@link PageRequest}
     *
     * @return a new {@link PageRequest}'s {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Static method to instantiate a {@link Builder} to easily expand an
     * existing {@link PageRequest}
     *
     * @return {@link PageRequest}'s {@link Builder} instance capable of
     * building on top of a previously existing {@link PageRequest}
     */
    public static Builder builder(AbsolutePageRequest onTopOfRequest) {
        return new Builder(onTopOfRequest);
    }

    /**
     * A builder class to help chain-construct a {@link PageRequest}
     */
    public static class Builder {

        private AbsolutePageRequest building;

        /**
         * Default constructor, to be used when builing a {@link PageRequest}
         * from scratch
         */
        public Builder() {
            building = new AbsolutePageRequest(AbsoluteSliceRequest.unbound(), AbsolutePageRequest.NO_SORTERS, AbsolutePageRequest.NO_FILTERS);
        }

        /**
         * Alternative constructor, to be used to enrich or modify an existing
         * {@link PageRequest}
         */
        public Builder(AbsolutePageRequest pr) {
            building = pr;
        }

        /**
         * Adds or replaces the {@link PageRequest#slice} portion of a
         * {@link PageRequest}
         *
         * @param slice the new {@link SliceRequest} value to use
         * @return the {@link Builders} itself, for further chaining
         */
        public Builder withSlice(AbsoluteSliceRequest slice) {
            building = new AbsolutePageRequest(slice, building.sorters, building.filters);
            return this;
        }

        /**
         * Add a new filter to the current ones
         *
         * @param <T> The type of the value contained in the filter
         * @param filter the new filter to be added
         * @return the {@link Builders} itself, for further chaining
         */
        public <T> Builder addFilter(FilterRequest<T> filter) {
            final List<FilterRequest> filters = new ArrayList<>(Arrays.asList(building.filters));
            filters.add(filter);
            building = new AbsolutePageRequest(building.slice, building.sorters, filters.toArray(new FilterRequest[0]));
            return this;
        }

        /**
         * Add new filters to the current ones
         *
         * @param filters the new filters to be added
         * @return the {@link Builders} itself, for further chaining
         */
        public Builder addFilters(Collection<FilterRequest> filters) {
            final List<FilterRequest> existingfilters = new ArrayList<>(Arrays.asList(building.filters));
            existingfilters.addAll(filters);
            building = new AbsolutePageRequest(building.slice, building.sorters, existingfilters.toArray(new FilterRequest[0]));
            return this;
        }

        /**
         * Add a new sorter to the current ones
         *
         * @param sorter the new sorter to be added
         * @return the {@link Builders} itself, for further chaining
         */
        public Builder addSorter(SortRequest sorter) {
            final List<SortRequest> sorters = new ArrayList<>(Arrays.asList(building.sorters));
            sorters.add(sorter);
            building = new AbsolutePageRequest(building.slice, sorters.toArray(new SortRequest[0]), building.filters);
            return this;
        }

        /**
         * Replaces any previously defined sorters with the current one
         *
         * @param sorter the new sorter to use
         * @return the {@link Builders} itself, for further chaining
         */
        public Builder withSorter(SortRequest sorter) {
            building = new AbsolutePageRequest(building.slice, new SortRequest[]{sorter}, building.filters
            );
            return this;
        }

        /**
         * Completes the build and returns the constructed {@link PageRequest}
         * @return a {@link PageRequest} constructed as requested
         */
        public AbsolutePageRequest build() {
            return building;
        }

    }
}
