package org.hgc.suts.gateway.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRespDTO {
    /**
     * 回复文本
     */
    private String text;

    /**
     * 客户端指令
     * 用于控制前端硬件行为：
     * - "NONE": 纯聊天，无硬件动作
     * - "CONNECT_WS": 开启实时避障模式 (前端连接 WebSocket)
     * - "DISCONNECT_WS": 关闭实时避障
     * - "CAPTURE_UPLOAD": 单次拍照分析 (前端拍照并上传)
     */
    private String clientCommand;

    /**
     * 指令附属数据
     */
    private Object commandData;
}