package net.optionfactory.pussyfoot.spring;

import net.optionfactory.pussyfoot.SliceRequest;
import java.util.Optional;
import net.optionfactory.pussyfoot.AbsoluteSliceRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AbsoluteSliceRequestResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AbsoluteSliceRequest.class == parameter.getParameterType();
    }

    @Override
    public Object resolveArgument(MethodParameter mp, ModelAndViewContainer mavc, NativeWebRequest req, WebDataBinderFactory wdbf) throws Exception {
        final Optional<String> start = Optional.ofNullable(req.getParameter("reference"));
        final int limit = tryParse(req.getParameter("limit")).orElse(SliceRequest.UNLIMITED);
        return AbsoluteSliceRequest.of(start, limit);
    }

    private static Optional<Integer> tryParse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(value));
    }
}
