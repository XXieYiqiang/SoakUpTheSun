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
                    "【模式 1：实时避障/看路】(WebSocket模式)\n" +
                    "   - 触发: 用户说 '我要走路了'、'开启避障'、'前面安全吗'、'帮我看路'。\n" +
                    "   - 动作: 需要开启摄像头实时流，检测路况。\n" +
                    "   - 返回: {\"tool\": null, \"reply\": \"避障模式已启动，请注意脚下。\", \"command\": \"CONNECT_WS\"}\n" +
                    "   - 关闭: 用户说 '关闭监测' -> 返回 command: \"DISCONNECT_WS\"\n" +
                    "\n" +
                    "【模式 2：图片场景分析】(HTTP拍照模式)\n" +
                    "   - 触发: 用户说 '这是什么'、'念一下上面的字'、'这瓶药过期没'、'看看手里的东西'。\n" +
                    "   - 动作: 需要拍一张高清照片进行精细分析。\n" +
                    "   - 返回: {\"tool\": null, \"reply\": \"好的，正在为您分析图片内容。\", \"command\": \"CAPTURE_UPLOAD\"}\n" +
                    "\n" +
                    "【模式 3：志愿求助】(后台服务模式)\n" +
                    "   - 触发: 用户说 '发布求助'、'招募志愿者'、'我摔倒了'。\n" +
                    "   - 返回: {\"tool\": \"create_volunteer_help\", \"args\": {\"description\": \"提取用户描述\"}}\n" +
                    "--------------------------------------------------\n" +
                    "【规则】\n" +
                    "1. 如果是普通闲聊，command 填 \"NONE\"。\n" +
                    "2. 严禁输出 Markdown 标记，直接输出纯 JSON 字符串。";

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