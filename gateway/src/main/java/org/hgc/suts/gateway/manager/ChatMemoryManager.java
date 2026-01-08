package org.hgc.suts.gateway.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.common.constant.RedisCacheConstant;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    // 用户上下文长度
    private static final int MAX_CONTEXT_CHAR_LIMIT = 4000;

    // Redis 里最多保留多少条物理数据
    private static final int REDIS_PHYSICAL_LIMIT = 50;

    /**
     * 获取历史记录，截断多余长度
     */
    public String getHistoryText(Long userId) {
        String key = String.format(RedisCacheConstant.GATEWAY_CHAT_HISTORY_KEY, userId);

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
            if (currentTotalLength + msgLen > MAX_CONTEXT_CHAR_LIMIT) {
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

    /**
     * 保存当前对话
     * @param userDescription 用户的输入
     * @param aiAnswer AI 的回答
     */
    public void saveHistory(Long userId, String userDescription, String aiAnswer) {
        String key = String.format(RedisCacheConstant.GATEWAY_CHAT_HISTORY_KEY, userId);

        // 1. 构造 User 消息
        if (StrUtil.isNotBlank(userDescription)) {
            JSONObject userMsg = new JSONObject().set("role", "User").set("content", userDescription);
            redisTemplate.opsForList().rightPush(key, userMsg.toString());
        }

        // 2. 构造 AI 消息
        if (StrUtil.isNotBlank(aiAnswer)) {
            JSONObject aiMsg = new JSONObject().set("role", "Assistant").set("content", aiAnswer);
            redisTemplate.opsForList().rightPush(key, aiMsg.toString());
        }

        // 3. 物理兜底清理，虽然 getHistoryText 会逻辑截断，但 Redis 不能存几万条。我们保留最近 50 条作为物理存储.
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > REDIS_PHYSICAL_LIMIT) {
            // 移除最老的记录
            redisTemplate.opsForList().trim(key, -REDIS_PHYSICAL_LIMIT, -1);
        }
    }
}