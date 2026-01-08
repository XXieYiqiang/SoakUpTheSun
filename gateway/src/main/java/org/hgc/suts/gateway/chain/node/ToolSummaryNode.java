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
            "你是一个服务于视障人士（盲人）的智能语音助手 SUTS。\n" +
                    "请根据用户的输入和工具执行结果，生成一句简短、温暖、清晰的语音回复。\n" +
                    "--------------------------------------------------\n" +
                    "【上下文信息】\n" +
                    "1. 用户说的话: \"%s\"\n" +
                    "2. 调用的工具: \"%s\"\n" +
                    "3. 工具执行结果: \"%s\"\n" +
                    "--------------------------------------------------\n" +
                    "【要求】\n" +
                    "1. 回复要口语化，适合 TTS 语音播报。\n" +
                    "2. 如果工具执行成功，请告知用户后续操作（如“请保持相机稳定”、“志愿者马上就到”）。\n" +
                    "3. 如果工具执行失败，请安抚用户并建议重试。\n" +
                    "4. 字数控制在 30 字以内，不要啰嗦。\n" +
                    "5. 直接输出回复内容，不要包含任何前缀或标记。";

    @Override
    protected void execute(AiChatContext context) {
        String executionResult = context.getToolExecutionResult();
        String currentFinalAnswer = context.getFinalAnswer();
        // 如果没有执行结果，且前面已经有回复（闲聊），则跳过总结
        if (StrUtil.isBlank(executionResult) && StrUtil.isNotBlank(currentFinalAnswer)) {
            log.info("无工具执行结果，且已有回复，跳过总结");
            return;
        }

        // 2. 如果啥结果都没有
        if (StrUtil.isBlank(executionResult)) {
            context.setFinalAnswer("抱歉，我没听清，请再说一遍。");
            return;
        }

        log.info("开始润色结果...");

        // 2. 构建 Prompt
        String systemPrompt = String.format(SUMMARY_PROMPT_TEMPLATE,
                // 用户说的话
                context.getUserDescription(),
                // 工具名
                context.getToolName() != null ? context.getToolName() : context.getClientCommand(),
                // 结果
                executionResult
        );

        // 3. 调用 AI 生成人话
        // 这里不需要 JSON 格式了，直接要文本
        String humanAnswer = aiModelManager.chat(systemPrompt, "请生成回复");

        // 4. 设置最终回答
        context.setFinalAnswer(humanAnswer);
        log.info("最终回复生成: {}", humanAnswer);
    }
}