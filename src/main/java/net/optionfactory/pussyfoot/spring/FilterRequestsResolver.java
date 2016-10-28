package net.optionfactory.pussyfoot.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.optionfactory.pussyfoot.FilterRequest;
import net.optionfactory.pussyfoot.PageRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class FilterRequestsResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper mapper;

    public FilterRequestsResolver(ObjectMapper mapper) {
        this.mapper = mapper.addMixIn(FilterRequest.class, FilterRequestMixin.class);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return FilterRequest[].class == parameter.getParameterType();
    }

    @Override
    public Object resolveArgument(MethodParameter mp, ModelAndViewContainer mavc, NativeWebRequest req, WebDataBinderFactory wdbf) throws Exception {
        final String value = req.getParameter("filters");
        if (value == null) {
            return PageRequest.NO_FILTERS;
        }
        return mapper.readValue(value, FilterRequest[].class);
    }
}
