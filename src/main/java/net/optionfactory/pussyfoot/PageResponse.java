package net.optionfactory.pussyfoot;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        return PageResponse.<R>of(this.total, this.data.stream().map(mapper).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "PageResponse{" + "data=" + data + ", total=" + total + '}';
    }

}
