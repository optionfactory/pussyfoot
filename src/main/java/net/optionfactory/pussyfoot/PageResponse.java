package net.optionfactory.pussyfoot;

import java.util.List;

public class PageResponse<T> {

    public final List<T> data;
    public final long total;

    public PageResponse(long total, List<T> data) {
        this.total = total;
        this.data = data;
    }

    public static <T> PageResponse<T> of(long total, List<T> data) {
        return new PageResponse<>(total, data);
    }

    @Override
    public String toString() {
        return "PageResponse{" + "data=" + data + ", total=" + total + '}';
    }

}
