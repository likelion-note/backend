package com.likelion.notebook.global.auth;

import com.likelion.notebook.global.common.ErrorCode;
import com.likelion.notebook.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @CurrentUser Long userId 에 로그인한 사용자 id를 넣어 줌.
 * 1) Authorization: Bearer <access token>  ← 프론트·배포는 이것만
 * 2) X-User-Id: 3                          ← app.auth.dev-mode=true(local 프로필)일 때만. Swagger·Postman 테스트용
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;
    private final boolean devMode;

    public CurrentUserArgumentResolver(JwtProvider jwtProvider,
                                       @Value("${app.auth.dev-mode}") boolean devMode) {
        this.jwtProvider = jwtProvider;
        this.devMode = devMode;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String auth = webRequest.getHeader("Authorization");
        if (auth != null && auth.startsWith(BEARER)) {
            return jwtProvider.parseAccessToken(auth.substring(BEARER.length()).trim());
        }

        if (devMode) {
            String header = webRequest.getHeader("X-User-Id");
            if (header != null && !header.isBlank()) {
                try {
                    return Long.parseLong(header.trim());
                } catch (NumberFormatException ignored) {
                    // 아래에서 UNAUTHORIZED
                }
            }
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}