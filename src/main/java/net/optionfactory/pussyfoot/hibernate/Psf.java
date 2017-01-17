package net.optionfactory.pussyfoot.hibernate;

import java.util.function.BiConsumer;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.PageResponse;

public interface Psf<QUERY_TYPE> {

    <T> PageResponse<T> queryForPage(Class<T> klass, PageRequest request, BiConsumer<QUERY_TYPE, QUERY_TYPE> cb);

    default <T> PageResponse<T> queryForPage(Class<T> klass, PageRequest request) {
        return queryForPage(klass, request, (a, b) -> {
        });
    }
}
