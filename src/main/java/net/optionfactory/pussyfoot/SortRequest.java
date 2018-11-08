package net.optionfactory.pussyfoot;

/**
 * Used to specify a desired sorting of the {@link PageResponse}'s records, to
 * be applied before taking the slice according to the {@link SliceRequest}
 */
public class SortRequest {

    /**
     * Name of the Sorting function to be applied
     */
    public final String name;
    /**
     * {@link Direction} of the sorting
     */
    public final Direction direction;

    public SortRequest(String name, Direction direction) {
        this.name = name;
        this.direction = direction;
    }

    /**
     * Specifies whether the desired sorting is Ascending or Descending
     */
    public enum Direction {

        /**
         * Ascending (smaller to bigger) ordering
         */
        ASC, 
        /**
         * Descending (bigger to smaller) ordering
         */
         DESC;
    }

    @Override
    public String toString() {
        return "SortRequest{" + "name=" + name + ", direction=" + direction + '}';
    }

}
