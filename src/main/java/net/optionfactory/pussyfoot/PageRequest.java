package net.optionfactory.pussyfoot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PageRequest {

    public static final SortRequest[] NO_SORTERS = new SortRequest[0];
    public static final FilterRequest<?>[] NO_FILTERS = new FilterRequest[0];

    public final SliceRequest slice;
    public final SortRequest[] sorters;
    public final FilterRequest<?>[] filters;

    public PageRequest(SliceRequest slice, SortRequest[] sorters, FilterRequest<?>[] filters) {
        this.slice = slice;
        this.sorters = sorters;
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "PageRequest{" + "slice=" + slice + ", sorters=" + Arrays.toString(sorters) + ", filters=" + Arrays.toString(filters) + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(PageRequest onTopOfRequest) {
        return new Builder(onTopOfRequest);
    }

    public static class Builder {

        private PageRequest building;

        public Builder() {
            building = new PageRequest(SliceRequest.unlimited(), PageRequest.NO_SORTERS, PageRequest.NO_FILTERS);
        }

        public Builder(PageRequest pr) {
            building = pr;
        }

        public Builder withSlice(SliceRequest slice) {
            building = new PageRequest(slice, building.sorters, building.filters);
            return this;
        }

        /**
         * Add a new filter to the current ones
         *
         * @param filter the new filter to be added
         * @return the Builder
         */
        public <T> Builder addFilter(FilterRequest<T> filter) {
            final List<FilterRequest> filters = new ArrayList<>(Arrays.asList(building.filters));
            filters.add(filter);
            building = new PageRequest(building.slice, building.sorters, filters.toArray(new FilterRequest[0]));
            return this;
        }

        /**
         * Add new filters to the current ones
         *
         * @param filters the new filters to be added
         * @return the Builder
         */
        public Builder addFilters(Collection<FilterRequest> filters) {
            final List<FilterRequest> existingfilters = new ArrayList<>(Arrays.asList(building.filters));
            existingfilters.addAll(filters);
            building = new PageRequest(building.slice, building.sorters, existingfilters.toArray(new FilterRequest[0]));
            return this;
        }

        /**
         * Add a new sorter to the current ones
         *
         * @param sorter the new sorter to be added
         * @return the Builder
         */
        public Builder addSorter(SortRequest sorter) {
            final List<SortRequest> sorters = new ArrayList<>(Arrays.asList(building.sorters));
            sorters.add(sorter);
            building = new PageRequest(building.slice, sorters.toArray(new SortRequest[0]), building.filters);
            return this;
        }

        /**
         * Replaces any sorters with the current one
         *
         * @param sorter the new sorter to use
         * @return the Builder
         */
        public Builder withSorter(SortRequest sorter) {
            building = new PageRequest(building.slice, new SortRequest[]{sorter}, building.filters
            );
            return this;
        }

        public PageRequest build() {
            return building;
        }

    }
}
