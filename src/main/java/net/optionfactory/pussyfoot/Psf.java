package net.optionfactory.pussyfoot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Defines the outer interface of pussyfoot
 *
 * @param <T> the type of the records contained in the {@link PageResponse}
 */
public interface Psf<T> {

    /**
     * given a {@link PageRequest}, returns a {@link PageResponse} constructed
     * according to the request's specification
     *
     * @param request a {@link PageRequest} containing a set of instructions
     * defining how to retrieve the data (how many, starting from which one,
     * filtered in a certain way, ordered according to a specified set of rules)
     * @return a {@link PageResponse} containing the requested page of records
     */
    PageResponse<T> queryForPage(PageRequest request);

    PageResponse<T> queryForPageInfiniteScrolling(PageRequest request);

    RelativePageResponse<T> queryForRelativePage(AbsolutePageRequest request,ObjectMapper mapper) throws JsonProcessingException;

}
