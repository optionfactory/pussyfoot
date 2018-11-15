package net.optionfactory.pussyfoot.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.optionfactory.pussyfoot.AbsolutePageRequest;
import net.optionfactory.pussyfoot.AbsoluteSliceRequest;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.SortRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AbsolutePageRequestResolver implements HandlerMethodArgumentResolver {

    private final AbsoluteSliceRequestResolver sliceResolver;
    private final FilterRequestsResolver filtersResolver;
    private final SortRequestsResolver sortersResolver;

    public AbsolutePageRequestResolver(ObjectMapper mapper) {
        this.sliceResolver = new AbsoluteSliceRequestResolver();
        this.filtersResolver = new FilterRequestsResolver(mapper);
        this.sortersResolver = new SortRequestsResolver(mapper);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AbsolutePageRequest.class == parameter.getParameterType();
    }

    @Override
    public Object resolveArgument(MethodParameter mp, ModelAndViewContainer mavc, NativeWebRequest req, WebDataBinderFactory wdbf) throws Exception {
        final AbsoluteSliceRequest slice = (AbsoluteSliceRequest) sliceResolver.resolveArgument(mp, mavc, req, wdbf);
        final SortRequest[] sorters = (SortRequest[]) sortersResolver.resolveArgument(mp, mavc, req, wdbf);
        final FilterRequest[] filters = (FilterRequest[]) filtersResolver.resolveArgument(mp, mavc, req, wdbf);
        return new AbsolutePageRequest(slice, sorters, filters);
    }

}
