package org.hgc.suts.gateway.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.chain.AiChatContext;
import org.hgc.suts.gateway.chain.node.ToolAnalysisNode;
import org.hgc.suts.gateway.chain.node.ToolExecuteNode;
import org.hgc.suts.gateway.chain.node.ToolSummaryNode;
import org.hgc.suts.gateway.common.biz.user.UserContext;
import org.hgc.suts.gateway.common.constant.RedisCacheConstant;
import org.hgc.suts.gateway.dto.resp.AiChatRespDTO;
import org.hgc.suts.gateway.manager.ChatMemoryManager;
import org.hgc.suts.gateway.service.AiChatService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ToolAnalysisNode analysisNode;
    private final ToolExecuteNode executeNode;
    private final ToolSummaryNode summaryNode;
    private final StringRedisTemplate redisTemplate;
    private final ChatMemoryManager chatMemoryManager;

    @PostConstruct
    public void initChain() {
        analysisNode.setNextNode(executeNode);
        executeNode.setNextNode(summaryNode);
    }

    @Override
    public AiChatRespDTO executeChat(String userDescription) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new RuntimeException("用户未登录");

        //1. 获取视觉记忆
        String visionKey = String.format(RedisCacheConstant.GATEWAY_VISION_CONTEXT_KEY, userId);
        String lastVisionJson = redisTemplate.opsForValue().get(visionKey);

        //2. 获取对话记忆
        String historyText = chatMemoryManager.getHistoryText(userId);

        //3. 拼接输入
        String finalInput = userDescription;
        if (StrUtil.isNotBlank(lastVisionJson)) {
            try {
                JSONObject visionObj = JSONUtil.parseObj(lastVisionJson);
                String visionText = visionObj.getStr("text");
                finalInput += String.format("\n系统视觉记忆：用户刚才上传的图片分析结果为: \"%s\"】", visionText);
            } catch (Exception e) {
                // 兼容逻辑：如果是纯文本
                finalInput += String.format("\n系统视觉记忆：用户刚才上传的图片分析结果为: \"%s\"】", lastVisionJson);
            }
        }

        // 4. 启动责任链
        AiChatContext context = new AiChatContext()
                .setUserId(userId)
                .setUserDescription(finalInput)
                .setHistoryText(historyText);

        analysisNode.doChain(context);

        // 5. 保存本轮对话
        if (StrUtil.isNotBlank(context.getFinalAnswer())) {
            // 存入原始描述，节省 Token
            chatMemoryManager.saveHistory(userId, userDescription, context.getFinalAnswer());
        }

        return AiChatRespDTO.builder()
                .text(context.getFinalAnswer())
                .clientCommand(context.getClientCommand())
                .build();
    }
}