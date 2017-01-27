package net.optionfactory.pussyfoot;

public class SliceRequest {

    public static final int UNLIMITED = -1;
    public final int start;
    public final int limit;

    public SliceRequest(int start, int limit) {
        this.start = start;
        this.limit = limit;
    }

    public static SliceRequest of(int start, int limit) {
        return new SliceRequest(start, limit);
    }

    public static SliceRequest unlimited() {
        return new SliceRequest(0, UNLIMITED);
    }

    @Override
    public String toString() {
        return "SliceRequest{" + "start=" + start + ", limit=" + limit + '}';
    }

}
