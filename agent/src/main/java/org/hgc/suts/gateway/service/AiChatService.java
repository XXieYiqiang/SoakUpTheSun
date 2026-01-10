package org.hgc.suts.gateway.service;

import org.hgc.suts.gateway.dto.resp.AiChatRespDTO;

/**
 * AI 对话核心业务接口
 */
public interface AiChatService {

    /**
     * 执行 AI 对话流程,包含,身份校验 -> 意图分析(DeepSeek) -> 工具执行(Feign) -> 结果返回
     * @param userQuestion 用户输入的自然语言
     * @return 最终给用户的回复
     */
    AiChatRespDTO executeChat(String userQuestion);
}