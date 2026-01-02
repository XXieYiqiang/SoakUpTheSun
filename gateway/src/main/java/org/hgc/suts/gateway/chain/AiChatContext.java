package org.hgc.suts.gateway.chain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * AI 对话责任链上下文
 */
@Data
@Accessors(chain = true)
public class AiChatContext {

    /**
     * 当前操作用户ID (来自 UserContext)
     */
    private Long userId;
    /**
     * 用户原始提问
     */
    private String userQuestion;

    /**
     * 决策要调用的工具名
     * 若为 null，则代表不需要调工具
     */
    private String toolName;
    
    /**
     * 工具参数 (JSON String)
     */
    private String toolArgsJson;

    /**
     * 工具执行后的原始返回结果
     */
    private String toolExecutionResult;

    /**
     * 最终返回给前端的回答
     */
    private String finalAnswer;
}