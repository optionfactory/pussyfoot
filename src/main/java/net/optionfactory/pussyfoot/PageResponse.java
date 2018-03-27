package net.optionfactory.pussyfoot;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageResponse<T> {

    public final List<T> data;
    public final Map<String, Object> summary;
    public final long total;

    public PageResponse(long total, List<T> data, Map<String, Object> summary) {
        this.total = total;
        this.data = data;
        this.summary = summary;
    }

    public static <T> PageResponse<T> of(long total, List<T> data, Map<String, Object> summary) {
        return new PageResponse<>(total, data, summary);
    }

    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        return PageResponse.<R>of(this.total, this.data.stream().map(mapper).collect(Collectors.toList()), this.summary);
    }

    @Override
    public String toString() {
        return "PageResponse{" + "data=" + data + ", reductions=" + summary + ", total=" + total + '}';
    }

}
