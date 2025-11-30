package org.hgc.suts.shortlink.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式 Redis 缓存配置属性
 */
@Data
@ConfigurationProperties(prefix = RedisDistributedProperties.PREFIX)
public class RedisDistributedProperties {

    public static final String PREFIX = "framework.cache.redis";

    /**
     * Key 前缀
     */
    private String prefix;

    /**
     * Key 前缀字符集
     */
    private String prefixCharset = "UTF-8";
}
