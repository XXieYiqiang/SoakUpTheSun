package org.hgc.suts.gateway.chain.node;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.chain.AbstractChainNode;
import org.hgc.suts.gateway.chain.AiChatContext;
import org.hgc.suts.gateway.manager.AiModelManager;
import org.springframework.stereotype.Component;

/**
 * 责任链节点 3: 结果总结与润色
 * <p>
 * 职责：将工具执行的“原始结果”转换为“自然语言回复”。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolSummaryNode extends AbstractChainNode {

    private final AiModelManager aiModelManager;

    // 总结的提示词
    private static final String SUMMARY_PROMPT_TEMPLATE = 
        "你是一个 SUTS 志愿平台的贴心小助手。\n" +
        "当前用户ID: %d。\n" +
        "用户的问题是: \"%s\"\n" +
        "刚才系统调用的工具是: [%s]\n" +
        "工具执行的原始结果是: \"%s\"\n\n" +
        "请根据执行结果，用**温暖、热情、鼓励**的语气给用户生成一个最终回复。\n" +
        "如果执行成功，请感谢用户的善举；如果执行失败，请安抚用户。\n" +
        "注意：不要暴露底层技术细节（如 JSON、Exception），直接说人话。字数控制在 50 字以内。";

    @Override
    protected void execute(AiChatContext context) {
        // 1. 检查是否有执行结果,如果 toolExecutionResult 为空，说明前面压根没调工具（是闲聊），或者执行节点挂了
        if (StrUtil.isBlank(context.getToolExecutionResult())) {
            log.info("无执行结果，跳过总结");
            return;
        }

        log.info("开始润色结果...");

        // 2. 构建 Prompt
        String systemPrompt = String.format(SUMMARY_PROMPT_TEMPLATE,
                context.getUserId(),
                context.getUserQuestion(),
                context.getToolName(),
                context.getToolExecutionResult()
        );

        // 3. 调用 AI 生成人话
        // 这里不需要 JSON 格式了，直接要文本
        String humanAnswer = aiModelManager.chat(systemPrompt, "请生成回复");

        // 4. 设置最终回答
        context.setFinalAnswer(humanAnswer);
        log.info("最终回复生成: {}", humanAnswer);
    }
}