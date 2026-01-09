package org.hgc.suts.gateway.dto.req;

import lombok.Data;

@Data
public class AiChatReqDTO {
    /**
     * 用户描述/语音转文字内容
     */
    private String userDescription;
}