package org.hgc.suts.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模型通用配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "suts.ai")
public class AiConfig {
    /**
     * API 域名地址
     */
    private String apiHost;

    /**
     * 鉴权 Key
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 超时时间
     */
    private Integer timeout = 60000;
    
    /**
     * 温度系数
     */
    private Double temperature = 0.7;

    /**
     * 向量模型地址
     */
    private String embeddingHost;

    /**
     * 向量模型 Key
     */
    private String embeddingApiKey;

    /**
     * 向量模型名称
     */
    private String embeddingModelName;
}