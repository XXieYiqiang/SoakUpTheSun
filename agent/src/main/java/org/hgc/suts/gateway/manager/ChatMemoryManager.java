package org.hgc.suts.gateway.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.common.constant.RedisCacheConstant;
import org.hgc.suts.gateway.service.VectorStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于上下文长度的记忆管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMemoryManager {

    private final StringRedisTemplate redisTemplate;
    // 向量存储服务接口
    private final VectorStorageService vectorStorageService;
    // AI 模型管理器
    private final AiModelManager aiModelManager;
    // 向量纬度
    @Value("${suts.ai.vector.dimension}")
    private int vectorDimension;

    // 用户上下文长度
    @Value("${suts.ai.chat.max-context-char-limit}")
    private int maxContextCharLimit;

    // Redis 里最多保留多少条物理数据
    @Value("${suts.ai.chat.redis-physical-limit:50}")
    private int redisPhysicalLimit;

    /**
     * 获取与当前问题最相关的历史记录 (RAG)
     * 替代了旧的 getHistoryText
     * @param userId       用户ID
     * @param currentQuery 用户当前的问题
     * @return 检索到的相关记忆文本
     */
    public String getRelatedHistory(Long userId, String currentQuery) {
        try {
            // 1. 将用户问题转为向量 (Mock 阶段)
            List<Double> queryVector = aiModelManager.embed(currentQuery);

            // 2. 去向量库搜索 Top 3 相关记忆
            List<String> memories = vectorStorageService.searchMemory(userId, queryVector, 10);

            if (CollUtil.isEmpty(memories)) {
                return "";
            }

            // 3. 拼接提示词
            StringBuilder sb = new StringBuilder("\n【长期记忆参考 (Long-term Memory)】:\n");
            for (String mem : memories) {
                sb.append("- ").append(mem).append("\n");
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("获取向量记忆失败: {}", e.getMessage());
            return "";
        }
    }
    /**
     * 保存当前对话 (异步 + 向量存储)
     * @param userId          用户ID
     * @param userDescription 用户的输入
     * @param aiAnswer        AI 的回答
     */
    @Async("aiTaskExecutor")
    public void saveHistory(Long userId, String userDescription, String aiAnswer) {
        try {
            // 1. 不拆分 role 存储，合并成一段完整的对话文本
            String content = "User: " + userDescription + "\nAI: " + aiAnswer;

            // 2. 生成向量
            List<Double> vector = aiModelManager.embed(content);

            // 3. 存入向量数据库
            vectorStorageService.saveMemory(userId, content, vector);

            // 原来的 remove range 逻辑已删除，因为向量库通常不设物理条数限制，而是依赖磁盘容量

        } catch (Exception e) {
            log.error("保存记忆异常", e);
        }
    }

    /**
     * 获取历史记录，截断多余长度
     */
    @Deprecated
    public String getHistoryText(Long userId) {
        String key = String.format(RedisCacheConstant.AGENT_CHAT_HISTORY_KEY, userId);

        // 1. 获取 Redis 中存的所有记录
        List<String> allHistoryJson = redisTemplate.opsForList().range(key, 0, -1);

        if (CollUtil.isEmpty(allHistoryJson)) {
            return "";
        }

        // 2. 倒序遍历 (从新到旧)，累加长度
        List<String> validHistoryBlocks = new ArrayList<>();
        int currentTotalLength = 0;

        // 因为是尾部插入,从尾部拿最新的
        for (int i = allHistoryJson.size() - 1; i >= 0; i--) {
            String jsonStr = allHistoryJson.get(i);
            JSONObject obj = JSONUtil.parseObj(jsonStr);
            String role = obj.getStr("role");
            String content = obj.getStr("content");

            // 估算长度
            int msgLen = (role + content).length() + 5;

            // 3. 判断是否超长,超长丢弃
            if (currentTotalLength + msgLen > maxContextCharLimit) {
                break;
            }

            // 没超长，加入临时列表
            String formattedBlock = role + ": " + content;
            validHistoryBlocks.add(formattedBlock);
            currentTotalLength += msgLen;
        }

        // 4. 反一下，输给大模型的为正常对话顺序，从旧到新
        Collections.reverse(validHistoryBlocks);

        // 5. 拼接最终文本
        StringBuilder sb = new StringBuilder("\n【历史对话记录 (Context)】\n");
        for (String block : validHistoryBlocks) {
            sb.append(block).append("\n");
        }
        sb.append("【历史记录结束】\n");

        log.info("构建历史上下文: 用户ID={}, 引用条数={}, 总字符数={}", userId, validHistoryBlocks.size(), currentTotalLength);
        return sb.toString();
    }
}