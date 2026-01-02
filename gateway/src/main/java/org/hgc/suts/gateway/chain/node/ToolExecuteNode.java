package org.hgc.suts.gateway.chain.node;

import cn.hutool.core.util.StrUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.chain.AbstractChainNode;
import org.hgc.suts.gateway.chain.AiChatContext;
import org.hgc.suts.gateway.common.result.Result;
import org.hgc.suts.gateway.dto.resp.TargetRoomLinkInfoRespDTO;
import org.hgc.suts.gateway.remote.VolunteerRemoteService;
import org.springframework.stereotype.Component;

/**
 * 责任链节点 2,工具执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolExecuteNode extends AbstractChainNode {

    private final VolunteerRemoteService volunteerRemoteService;

    @Override
    protected void execute(AiChatContext context) {
        String toolName = context.getToolName();

        // 1. 如果没有工具要调，或者 Node-1 已经给出了最终回复(闲聊)，直接跳过
        if (StrUtil.isBlank(toolName) || StrUtil.isNotBlank(context.getFinalAnswer())) {
            log.info("无需执行工具，跳过");
            return;
        }

        log.info("开始执行工具: {}", toolName);

        try {
            // 2. 根据工具名进行路由 (Switch-Case 模式)
            switch (toolName) {
                case "create_volunteer_help":
                    executeCreateHelpTask(context);
                    break;
                default:
                    log.warn("未知的工具名: {}", toolName);
                    context.setToolExecutionResult("系统错误：找不到名为 " + toolName + " 的工具");
            }
        } catch (Exception e) {
            log.error("工具执行异常", e);
            context.setToolExecutionResult("执行失败：" + e.getMessage());
        }
    }

    /**
     * 具体执行逻辑：创建志愿请求
     */
    private void executeCreateHelpTask(AiChatContext context) {
        // 调用 Feign
        Result<TargetRoomLinkInfoRespDTO> result = volunteerRemoteService.createHelpTask();
        
        if (result!=null) {
            context.setToolExecutionResult("成功：志愿帮助请求已发布，正在等待志愿者响应。");
            log.info("志愿请求发布成功");
        }
    }
}