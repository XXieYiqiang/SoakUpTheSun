package org.hgc.suts.gateway.mq.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 消息发送扩展属性实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseSendExtendDTO {
    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签
     */
    private String tag;

    /**
     * 业务唯一标识
     */
    private String keys;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 发送超时时间
     */
    private Long sentTimeout;

    /**
     * 延迟级别/时间 (毫秒)，若不为空则发送延迟消息
     */
    private Long delayTime;
}