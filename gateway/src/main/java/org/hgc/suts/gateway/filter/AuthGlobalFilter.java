package org.hgc.suts.gateway.filter;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 白名单放行
    private final List<String> whiteList;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单校验
        for (String whitePath : whiteList) {
            if (path.startsWith(whitePath)) {
                return chain.filter(exchange);
            }
        }

        // 2. 拿 Token
        String token = request.getHeaders().getFirst("Authorization");
        if (StrUtil.isBlank(token)) {
            token = request.getQueryParams().getFirst("token");
        }

        // 3. 传递给后端拦截器
        if (StrUtil.isNotBlank(token)) {
            ServerHttpRequest newRequest = request.mutate()
                    .header("Authorization", token)
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}