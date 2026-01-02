package org.hgc.suts.gateway.mq.base;

import lombok.*;
import java.io.Serializable;

/**
 * 消息体包装器
 * <p>
 * 统一泛型封装，确保所有消息都带有 Keys 和 时间戳。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@RequiredArgsConstructor
public final class MessageWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息发送 Keys (用于消息幂等去重或追踪)
     */
    @NonNull
    private String keys;

    /**
     * 真实业务消息体
     */
    @NonNull
    private T message;

    /**
     * 消息发送时间
     */
    @Builder.Default
    private Long timestamp = System.currentTimeMillis();
}