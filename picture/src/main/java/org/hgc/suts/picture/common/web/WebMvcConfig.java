package org.hgc.suts.picture.common.web;

import lombok.AllArgsConstructor;

import org.hgc.suts.picture.common.biz.user.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AllArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {


    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/api/user/login",
                    "/api/user/register",
                    "/api/picture/createPictureSpace",
                    "/swagger-ui/**", "/v3/api-docs/**", "/doc.html"
                );
    }
}