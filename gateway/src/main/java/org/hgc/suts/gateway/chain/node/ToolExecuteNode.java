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
        // 获取前端指令
        String clientCommand = context.getClientCommand();

        // 既没有后端工具，也没有前端指令，跳过
        boolean hasBackendTool = StrUtil.isNotBlank(toolName);
        boolean hasClientCommand = StrUtil.isNotBlank(clientCommand) && !"NONE".equals(clientCommand);
        if ((!hasBackendTool && !hasClientCommand) || StrUtil.isNotBlank(context.getFinalAnswer())) {
            log.info("无需执行工具，跳过");
            return;
        }


        // 1. 如果没有工具要调，或者 Node-1 已经给出了最终回复(闲聊)，直接跳过
        if (StrUtil.isBlank(toolName) || StrUtil.isNotBlank(context.getFinalAnswer())) {
            log.info("无需执行工具，跳过");
            return;
        }

        log.info("开始执行工具: {}", toolName);

        try {
            if (hasBackendTool) {
                switch (toolName) {
                    case "create_volunteer_help":
                        executeCreateHelpTask(context);
                        break;
                    default:
                        log.warn("未知的后端工具名: {}", toolName);
                        // 不阻断流程，防止影响后续对话
                        context.setToolExecutionResult("系统提示：该后台功能暂未上线。");
                }
            }
            // 2. 处理前端指令
            else {
                switch (clientCommand) {
                    case "CONNECT_WS":
                        // 后端不做实质操作，只登记状态
                        context.setToolExecutionResult("系统已切换至实时避障模式，摄像头数据通道已开启。");
                        break;
                    case "CAPTURE_UPLOAD":
                        context.setToolExecutionResult("系统已下发拍照指令，正在等待图像上传与智能分析。");
                        break;
                    case "DISCONNECT_WS":
                        context.setToolExecutionResult("系统已关闭实时避障模式。");
                        break;
                    default:
                        // 其他指令不需特殊文案
                        break;
                }
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