package net.optionfactory.pussyfoot;

public class PageRequest {

    public static final SortRequest[] NO_SORTERS = new SortRequest[0];
    public static final FilterRequest[] NO_FILTERS = new FilterRequest[0];

    public final SliceRequest slice;
    public final SortRequest[] sorters;
    public final FilterRequest[] filters;

    public PageRequest(SliceRequest slice, SortRequest[] sorters, FilterRequest[] filters) {
        this.slice = slice;
        this.sorters = sorters;
        this.filters = filters;
    }

    @Override
    public String toString() {
        return "PageRequest{" + "slice=" + slice + ", sorters=" + sorters + ", filters=" + filters + '}';
    }

}
