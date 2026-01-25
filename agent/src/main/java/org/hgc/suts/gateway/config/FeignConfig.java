package org.hgc.suts.gateway.config;

import cn.hutool.core.util.StrUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.common.biz.user.UserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 1. 优先从 UserContext 获取 Token
                String token = UserContext.getToken();
                // 2. 如果 UserContext 里没有，再尝试从 HTTP 上下文拿
                if (StrUtil.isBlank(token)) {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        token = attributes.getRequest().getHeader("token");
                        if (StrUtil.isBlank(token)) {
                            token = attributes.getRequest().getParameter("token");
                        }
                    }
                }
                // 3. 只要拿到了 Token，就透传给下游
                if (StrUtil.isNotBlank(token)) {
                    template.header("token", token);
                } else {
                    log.debug("Feign拦截器检测到 Token 为空，下游调用可能会失败");
                }
            }
        };
    }
}