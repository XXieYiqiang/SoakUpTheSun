package org.hgc.suts.gateway.chain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AiChatContext {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 用户描述
     */
    private String userDescription;

    /**
     * 历史对话
     */
    private String historyText;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具参数
     */
    private String toolArgsJson;

    /**
     * 用于指挥前端,默认为NONE
     */
    private String clientCommand = "NONE";

    /**
     * 执行结果
     */
    private String toolExecutionResult;

    /**
     * 最终产出
      */
    private String finalAnswer;
}