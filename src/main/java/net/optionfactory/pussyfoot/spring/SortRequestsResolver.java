package net.optionfactory.pussyfoot.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.optionfactory.pussyfoot.PageRequest;
import net.optionfactory.pussyfoot.SortRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class SortRequestsResolver implements HandlerMethodArgumentResolver {

    private final ObjectMapper mapper;

    public SortRequestsResolver(ObjectMapper mapper) {
        this.mapper = mapper.addMixIn(SortRequest.class, SortRequestMixin.class);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return SortRequest[].class == parameter.getParameterType();
    }

    @Override
    public Object resolveArgument(MethodParameter mp, ModelAndViewContainer mavc, NativeWebRequest req, WebDataBinderFactory wdbf) throws Exception {
        final String value = req.getParameter("sorters");
        if (value == null) {
            return PageRequest.NO_SORTERS;
        }
        return mapper.readValue(value, SortRequest[].class);
    }
}
