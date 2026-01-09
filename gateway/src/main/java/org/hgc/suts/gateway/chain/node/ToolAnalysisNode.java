package org.hgc.suts.gateway.chain.node;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.chain.AbstractChainNode;
import org.hgc.suts.gateway.chain.AiChatContext;
import org.hgc.suts.gateway.manager.AiModelManager;
import org.springframework.stereotype.Component;

/**
 * 责任链节点 1: 意图分析与工具决策
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAnalysisNode extends AbstractChainNode {

    private final AiModelManager aiModelManager;

    private static final String SYSTEM_PROMPT_TEMPLATE =
            "你是一个 SUTS 盲人助视眼镜的智能中控助手。当前用户ID: %d。\n" +
                    "请分析用户的语音描述(userDescription)，并返回对应的 JSON 指令。\n" +
                    "--------------------------------------------------\n" +
                    "【🧠 高级智能：混合意图处理逻辑】\n" +
                    "用户经常会同时发出【指令】和【提问】（例如：“帮我找志愿者，顺便告诉我我爱吃什么？”）。\n" +
                    "面对这种情况，你必须同时做两件事：\n" +
                    "1. **处理指令**：根据意图选择正确的 `tool` (如 create_volunteer_help)。\n" +
                    "2. **回答提问**：\n" +
                    "   - 请仔细阅读下方的【长期记忆参考】。\n" +
                    "   - 如果记忆中有答案（比如用户以前说过爱吃苹果），请务必在 `reply` 字段中回答！\n" +
                    "   - 如果记忆中没有，就回答“我还不清楚您的喜好”。\n" +
                    "\n" +
                    "🚫 **禁止行为**：\n" +
                    "   - 严禁在 `reply` 里只复述“正在为您呼叫”，必须包含对提问的回答！\n" +
                    "   - 严禁编造记忆中不存在的信息。\n" +
                    "\n" +
                    "💡 **通用示例**：\n" +
                    "   用户: \"(指令) + (提问)\"\n" +
                    "   返回: {\"tool\": \"对应工具名\", \"reply\": \"(针对提问的回答)\"}\n" +
                    "--------------------------------------------------\n" +
                    "【模式 1：实时避障】(WebSocket)\n" +
                    "   - 触发: '开启避障'、'看路'。\n" +
                    "   - 返回: {\"tool\": null, \"reply\": \"避障模式已启动。\", \"command\": \"CONNECT_WS\"}\n" +
                    "   - 关闭: '关闭监测' -> command: \"DISCONNECT_WS\"\n" +
                    "\n" +
                    "【模式 2：图片分析】(HTTP拍照)\n" +
                    "   - 触发: '这是什么'、'念字'。\n" +
                    "   - 返回: {\"tool\": null, \"reply\": \"正在分析图片。\", \"command\": \"CAPTURE_UPLOAD\"}\n" +
                    "\n" +
                    "【模式 3：志愿求助】(后台服务)\n" +
                    "   - 触发: '发布求助'、'找志愿者'。\n" +
                    "   - 返回: {\"tool\": \"create_volunteer_help\", \"args\": {\"description\": \"用户描述\"}}\n" +
                    "\n" +
                    "【模式 4：纯闲聊/问答】\n" +
                    "   - 触发: '我是谁'、'天气'、'我想吃什么'。\n" +
                    "   - 返回: {\"tool\": null, \"reply\": \"(你的回答)\", \"command\": \"NONE\"}\n" +
                    "--------------------------------------------------\n" +
                    "【格式要求】\n" +
                    "1. 只输出纯 JSON 字符串，不要 Markdown。\n" +
                    "2. 如果是闲聊，command 必须填 \"NONE\"。";
    @Override
    protected void execute(AiChatContext context) {
        log.info("开始分析意图, User: {}, Input: {}", context.getUserId(), context.getUserDescription());

        // 1. 注入 UserId 到 System Prompt，用于获取上下文
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context.getUserId());

        //  注入历史记忆
        if (StrUtil.isNotBlank(context.getHistoryText())) {
            systemPrompt += "\n" + context.getHistoryText();
        }
        // 2. 调用 DeepSeek 大脑
        String aiRawResponse = aiModelManager.chat(systemPrompt, context.getUserDescription());


        // 解析json
        try {
            String cleanJson = StrUtil.replace(aiRawResponse, "```json", "").replace("```", "").trim();
            JSONObject decision = JSONUtil.parseObj(cleanJson);

            // 提取 tool
            if (StrUtil.isNotBlank(decision.getStr("tool"))) {
                context.setToolName(decision.getStr("tool"));
                context.setToolArgsJson(decision.getJSONObject("args") != null ? decision.getJSONObject("args").toString() : "{}");
            }

            // ★ 提取 command
            if (StrUtil.isNotBlank(decision.getStr("command"))) {
                context.setClientCommand(decision.getStr("command"));
            }

            // 提取 reply
            if (StrUtil.isNotBlank(decision.getStr("reply"))) {
                context.setFinalAnswer(decision.getStr("reply"));
            }
        } catch (Exception e) {
            log.error("AI解析失败", e);
            context.setFinalAnswer("抱歉，请再说一遍。");
        }
    }
}