package org.hgc.suts.gateway.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.chain.AiChatContext;
import org.hgc.suts.gateway.chain.node.ToolAnalysisNode;
import org.hgc.suts.gateway.chain.node.ToolExecuteNode;
import org.hgc.suts.gateway.chain.node.ToolSummaryNode;
import org.hgc.suts.gateway.common.biz.user.UserContext;
import org.hgc.suts.gateway.service.AiChatService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ToolAnalysisNode toolAnalysisNode;
    private final ToolExecuteNode toolExecuteNode;
    private final ToolSummaryNode toolSummaryNode;

    /**
     * 初始化责任链
     * 顺序：分析 -> 执行 -> 总结
     */
    @PostConstruct
    public void initChain() {
        toolAnalysisNode.setNextNode(toolExecuteNode);
        toolExecuteNode.setNextNode(toolSummaryNode);
    }

    @Override
    public String executeChat(String userQuestion) {
        Long currentUserId = UserContext.getUserId();
        // 生产环境务必开启登录校验
        if (currentUserId == null) currentUserId = 1001L;

        // 1. 初始化
        AiChatContext context = new AiChatContext()
                .setUserId(currentUserId)
                .setUserQuestion(userQuestion);

        // 2. 启动责任链
        toolAnalysisNode.doChain(context);

        // 3. 返回最终结果
        // 无论经过了几个节点，context.getFinalAnswer() 里永远是最新的回复
        return context.getFinalAnswer();
    }
}