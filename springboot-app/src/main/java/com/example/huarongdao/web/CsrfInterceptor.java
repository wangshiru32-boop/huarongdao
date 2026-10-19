package com.example.huarongdao.web;

import com.example.huarongdao.auth.ApiException;
import com.example.huarongdao.auth.CsrfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 所有 POST API 都必须携带页面注入的 X-CSRF-Token。 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {
    private final CsrfService csrf;

    public CsrfInterceptor(CsrfService csrf) {
        this.csrf = csrf;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        HttpSession session = request.getSession(false);
        if (!csrf.matches(session, request.getHeader("X-CSRF-Token"))) {
            throw new ApiException(403, "CSRF Token 无效，请刷新页面后重试。");
        }
        return true;
    }
}
