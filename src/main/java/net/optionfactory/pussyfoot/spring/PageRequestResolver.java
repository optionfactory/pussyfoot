package net.optionfactory.pussyfoot.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.SliceRequest;
import net.optionfactory.pussyfoot.SortRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PageRequestResolver implements HandlerMethodArgumentResolver {

    private final SliceRequestResolver sliceResolver;
    private final FilterRequestsResolver filtersResolver;
    private final SortRequestsResolver sortersResolver;

    public PageRequestResolver(ObjectMapper mapper) {
        this.sliceResolver = new SliceRequestResolver();
        this.filtersResolver = new FilterRequestsResolver(mapper);
        this.sortersResolver = new SortRequestsResolver(mapper);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PageRequest.class == parameter.getParameterType();
    }

    @Override
    public Object resolveArgument(MethodParameter mp, ModelAndViewContainer mavc, NativeWebRequest req, WebDataBinderFactory wdbf) throws Exception {
        final SliceRequest slice = (SliceRequest) sliceResolver.resolveArgument(mp, mavc, req, wdbf);
        final SortRequest[] sorters = (SortRequest[]) sortersResolver.resolveArgument(mp, mavc, req, wdbf);
        final FilterRequest[] filters = (FilterRequest[]) filtersResolver.resolveArgument(mp, mavc, req, wdbf);
        return new PageRequest(slice, sorters, filters);
    }

}
