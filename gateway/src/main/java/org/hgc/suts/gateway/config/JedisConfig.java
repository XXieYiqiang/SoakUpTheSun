package org.hgc.suts.gateway.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Configuration
public class JedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    public JedisPooled jedisPooled() {
        if (StrUtil.isBlank(password)) {
            // 无密码模式
            return new JedisPooled(host, port);
        } else {
            // 有密码模式
            return new JedisPooled(host, port, null, password);
        }
    }
}