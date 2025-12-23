package org.hgc.suts.volunteer.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Enumeration;

@Configuration
@Slf4j
public class FeignConfig {
    // 透传
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 1. 获取当前请求的上下文
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String tokenName = "token";
                    String token = request.getHeader(tokenName);
                    
                    if (token != null) {
                        // 3. 将 Token 放入 Feign 的请求头中
                        template.header(tokenName, token);
                    }
                } else {
                    log.warn("当前非HTTP请求上下文,无法透传用户Token");
                }
            }
        };
    }
}