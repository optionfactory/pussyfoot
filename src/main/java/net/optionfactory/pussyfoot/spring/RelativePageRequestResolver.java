package net.optionfactory.pussyfoot.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.optionfactory.pussyfoot.AbsolutePageRequest;
import net.optionfactory.pussyfoot.RelativeSliceRequest;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.SortRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class RelativePageRequestResolver implements HandlerMethodArgumentResolver {

    private final RelativeSliceRequestResolver sliceResolver;
    private final FilterRequestsResolver filtersResolver;
    private final SortRequestsResolver sortersResolver;

    public RelativePageRequestResolver(ObjectMapper mapper) {
        this.sliceResolver = new RelativeSliceRequestResolver();
        this.filtersResolver = new FilterRequestsResolver(mapper);
        this.sortersResolver = new SortRequestsResolver(mapper);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AbsolutePageRequest.class == parameter.getParameterType();
    }

    @Override
    public Object resolveArgument(MethodParameter mp, ModelAndViewContainer mavc, NativeWebRequest req, WebDataBinderFactory wdbf) throws Exception {
        final RelativeSliceRequest slice = (RelativeSliceRequest) sliceResolver.resolveArgument(mp, mavc, req, wdbf);
        final SortRequest[] sorters = (SortRequest[]) sortersResolver.resolveArgument(mp, mavc, req, wdbf);
        final FilterRequest[] filters = (FilterRequest[]) filtersResolver.resolveArgument(mp, mavc, req, wdbf);
        return new AbsolutePageRequest(slice, sorters, filters);
    }

}
