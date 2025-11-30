
package org.hgc.suts.shortlink.config;


import org.hgc.suts.shortlink.common.web.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;


public class WebAutoConfiguration {

    /**
     * 构建全局异常拦截器组件 Bean
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
