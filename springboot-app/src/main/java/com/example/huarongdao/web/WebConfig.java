package com.example.huarongdao.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC 配置：CSRF 只拦截 /api/**，静态资源不拦截。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final CsrfInterceptor csrfInterceptor;

    public WebConfig(CsrfInterceptor csrfInterceptor) {
        this.csrfInterceptor = csrfInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(csrfInterceptor).addPathPatterns("/api/**");
    }
}
