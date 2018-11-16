package net.optionfactory.pussyfoot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RelativePageResponse<T> {

    /**
     * The actual list of records
     */
    public final List<T> data;

    public Optional<String> prevPageToken;
    public Optional<String> nextPageToken;

    public RelativePageResponse(List<T> data, Optional<String> prevPageToken, Optional<String> nextPageToken) {
        this.data = data;
        this.prevPageToken = prevPageToken;
        this.nextPageToken = nextPageToken;
    }

    public static <T> RelativePageResponse<T> of(List<T> data, Optional<String> prevPageToken, Optional<String> nextPageToken) {
        return new RelativePageResponse<>(data, prevPageToken, nextPageToken);
    }

    public <R> RelativePageResponse<R> map(Function<? super T, ? extends R> mapper) {
        return RelativePageResponse.<R>of(this.data.stream().map(mapper).collect(Collectors.toList()), this.prevPageToken, this.nextPageToken);
    }

    @Override
    public String toString() {
        return "AbsolutePageResponse{" + "data=" + data + ", prevPageToken=" + prevPageToken + ", nextPageToken=" + nextPageToken + '}';
    }

}
