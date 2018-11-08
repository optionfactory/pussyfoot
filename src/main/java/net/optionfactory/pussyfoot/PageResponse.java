package net.optionfactory.pussyfoot;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A page of records, created in response to a {@link PageRequest}
 *
 * @param <T> the type of the records contained
 */
public class PageResponse<T> {

    /**
     * The actual list of records
     */
    public final List<T> data;
    /**
     * Additional reductions, indexed by key
     */
    public final Map<String, Object> reductions;

    /**
     * The total number of records, after applying the
     * {@link PageRequest#filters}, but before applying the
     * {@link PageRequest#slice} instructions
     */
    public final long total;

    /**
     * Default constructor
     * 
     * @param total total number of records after filtering
     * @param data actual list of records
     * @param reductions Additional reductions, indexed by key
     */
    public PageResponse(long total, List<T> data, Map<String, Object> reductions) {
        this.total = total;
        this.data = data;
        this.reductions = reductions;
    }

    /**
     * Static constructor for {@link PageResponse#PageResponse(long, java.util.List, java.util.Map)}
     * @param <T> the type of the records contained
     * @param total total number of records after filtering
     * @param data actual list of records
     * @param reductions Additional reductions, indexed by key
     * @return
     */
    public static <T> PageResponse<T> of(long total, List<T> data, Map<String, Object> reductions) {
        return new PageResponse<>(total, data, reductions);
    }

    /**
     * Allows to map the content of a {@link PageResponse#data}, while maintaining all the other aspects of the {@link PageResponse} untouched
     * @param <R> The new type of the records contained in the returned {@link PageResponse}
     * @param mapper The mapping function
     * @return a new {@link PageResponse}, whose records are now of type R
     */
    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        return PageResponse.<R>of(this.total, this.data.stream().map(mapper).collect(Collectors.toList()), this.reductions);
    }

    @Override
    public String toString() {
        return "PageResponse{" + "data=" + data + ", reductions=" + reductions + ", total=" + total + '}';
    }

}
