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

    //
    private static final String SYSTEM_PROMPT_TEMPLATE = 
        "你是一个 SUTS (Soak Up The Sun) 志愿服务平台的智能中控助手。\n" +
        "当前操作用户ID为: %d。\n\n" +
        "你的任务是精准分析用户的提问\n" +
        "--------------------------------------------------\n" +
        "【可用工具列表】\n" +
        "1. 工具名: [create_volunteer_help]\n" +
        "   - 触发场景: 用户想要发布任务、招募志愿者、请求帮助、发起活动、派发急救单等。\n" +
        "   - 示例输入: '帮我发一个招募令'、'这里需要志愿者'、'发布志愿任务'、'快去派单'。\n" +
        "   - 参数: description (String, 提取用户关于任务的描述，如果没具体描述则填'用户发起的紧急招募')。\n" +
        "--------------------------------------------------\n" +
        "【输出规则】\n" +
        "1. 如果用户意图匹配上述工具，请 **仅** 返回如下 JSON 格式：\n" +
        "{\"tool\": \"create_volunteer_help\", \"args\": {\"description\": \"...\"}}\n\n" +
        "2. 如果用户只是闲聊 (如 '你好', '你是谁')，请返回：\n" +
        "{\"tool\": null, \"reply\": \"你的直接回复\"}\n\n" +
        "注意：严禁输出 Markdown 标记 (如 ```json)，直接输出纯 JSON 字符串。";

    @Override
    protected void execute(AiChatContext context) {
        log.info("开始分析意图, User: {}, Input: {}", context.getUserId(), context.getUserQuestion());

        // 1. 注入 UserId 到 System Prompt，用于获取上下文
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context.getUserId());

        // 2. 调用 DeepSeek 大脑
        String aiRawResponse = aiModelManager.chat(systemPrompt, context.getUserQuestion());
        
        // 3. 结果清洗
        String cleanJson = StrUtil.replace(aiRawResponse, "```json", "").replace("```", "").trim();

        try {
            // 4. 解析 JSON 决策
            JSONObject decision = JSONUtil.parseObj(cleanJson);
            String tool = decision.getStr("tool");

            if (StrUtil.isNotBlank(tool) && "create_volunteer_help".equals(tool)) {

                context.setToolName(tool);

                // 提取参数,虽然现在后端是无参调用，但保留提取能力
                JSONObject args = decision.getJSONObject("args");
                context.setToolArgsJson(args != null ? args.toString() : "{}");
                
                log.info("命中工具 [create_volunteer_help], 意图明确");
            } else {
                // 普通闲聊
                String reply = decision.getStr("reply");
                // 直接把回复填入，后续节点就不跑了
                context.setFinalAnswer(reply);
                log.info("AI 决策: 普通对话");
            }

        } catch (Exception e) {
            log.error("解析 AI 决策失败, 原始响应: {}", aiRawResponse, e);
            // 如果 AI 无法恢复，返回一个安全回复
            context.setFinalAnswer("AI 中控台暂时无法解析您的指令，请稍后再试。");
        }
    }
}