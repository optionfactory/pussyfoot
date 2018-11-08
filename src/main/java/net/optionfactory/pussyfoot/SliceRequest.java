package net.optionfactory.pussyfoot;

/**
 * Defines the size and position of the slice of records to return
 */
public class SliceRequest {

    /**
     * Placeholder for slice limit special value
     */
    public static final int UNLIMITED = -1;

    /**
     * After applying the necessary filters, skip the first N records before
     * taking the slice
     */
    public final int start;

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
    public SliceRequest(int start, int limit) {
        this.start = start;
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
    public static SliceRequest of(int start, int limit) {
        return new SliceRequest(start, limit);
    }

    /**
     * Static constructor, equivalent to creating a new {@link SliceRequest}
     * with {@link SliceRequest#start} = 0 and
     * {@link SliceRequest#limit} = {@link SliceRequest#UNLIMITED}
     *
     * @return a new instance of {@link SliceRequest}
     */
    public static SliceRequest unlimited() {
        return new SliceRequest(0, UNLIMITED);
    }

    @Override
    public String toString() {
        return "SliceRequest{" + "start=" + start + ", limit=" + limit + '}';
    }

}
