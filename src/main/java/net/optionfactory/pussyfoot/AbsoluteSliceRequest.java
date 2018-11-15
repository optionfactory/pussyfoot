package net.optionfactory.pussyfoot;

import java.util.Optional;

/**
 * Defines the size and position of the slice of records to return
 */
public class AbsoluteSliceRequest {

    /**
     * Placeholder for slice limit special value
     */
    public static final int UNLIMITED = -1;

    /**
     * After applying the necessary filters, skip the first N records before
     * taking the slice
     */
    public final Optional<String> reference;

    /**
     * How many records the interesting slice of records is composed of
     */
    public final int limit;

    /**
     * Default constructor for a {@link SliceRequest}
     *
     * @param start how many records to skip
     * @param limit how many records to take
     */
    public AbsoluteSliceRequest(Optional<String> reference, int limit) {
        this.reference = reference;
        this.limit = limit;
    }

    /**
     * Static constructor, equivalent to
     * {@link SliceRequest#SliceRequest(int, int)}
     *
     * @param start how many records to skip
     * @param limit how many records to take
     * @return a new instance of {@link SliceRequest}
     */
    public static AbsoluteSliceRequest of(Optional<String> reference, int limit) {
        return new AbsoluteSliceRequest(reference, limit);
    }

    /**
     * Static constructor, equivalent to creating a new {@link SliceRequest}
     * with {@link SliceRequest#start} = 0 and
     * {@link SliceRequest#limit} = {@link SliceRequest#UNLIMITED}
     *
     * @return a new instance of {@link SliceRequest}
     */
    public static AbsoluteSliceRequest unbound() {
        return new AbsoluteSliceRequest(Optional.empty(), UNLIMITED);
    }

    @Override
    public String toString() {
        return "AbsoluteSliceRequest{" + "reference=" + reference + ", limit=" + limit + '}';
    }

}
