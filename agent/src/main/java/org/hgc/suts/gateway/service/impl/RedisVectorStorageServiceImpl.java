package org.hgc.suts.gateway.service.impl;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.common.constant.RedisCacheConstant;
import org.hgc.suts.gateway.service.VectorStorageService;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量存储服务 (Jedis 实现版)
 * 简洁、稳定、原生支持 RediSearch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisVectorStorageServiceImpl implements VectorStorageService {

    private final JedisPooled jedis;

    @Override
    public void saveMemory(Long userId, String content, List<Double> vector) {
        String key = RedisCacheConstant.VECTOR_DATA_KEY_PREFIX + userId + ":" + IdUtil.fastSimpleUUID();

        // 1. 转换向量
        byte[] vectorBytes = toByteArray(vector);

        // 2. 准备数据 Map
        Map<String, Object> fields = new HashMap<>();
        fields.put("userId", String.valueOf(userId));
        fields.put("content", content);
        // Jedis 会自动处理 byte[]
        fields.put("vector", vectorBytes);

        // 3. 存入 Redis Hash
        Map<byte[], byte[]> byteFields = new HashMap<>();
        byteFields.put("userId".getBytes(), String.valueOf(userId).getBytes());
        byteFields.put("content".getBytes(), content.getBytes());
        byteFields.put("vector".getBytes(), vectorBytes);

        jedis.hset(key.getBytes(), byteFields);

        log.debug("已保存记忆: {}", key);
    }

    @Override
    public List<String> searchMemory(Long userId, List<Double> queryVector, int limit) {
        try {
            // 1. 转换查询向量
            byte[] vectorBytes = toByteArray(queryVector);

            // 2. 构建查询语句,例 @userId:{1001}=>[KNN 3 @vector $blob AS score]
            String queryStr = String.format("@userId:{%d}=>[KNN %d @vector $blob AS score]", userId, limit);

            Query query = new Query(queryStr)
                    // 传入二进制参数
                    .addParam("blob", vectorBytes)
                    // 只返回这些字段
                    .returnFields("content", "score")
                    // 按相似度排序
                    .setSortBy("score", true)
                    .dialect(2);

            // 3. 执行搜索
            SearchResult result = jedis.ftSearch(RedisCacheConstant.VECTOR_INDEX_NAME, query);

            // 4. 解析结果
            List<String> memories = new ArrayList<>();
            for (Document doc : result.getDocuments()) {
                String content = doc.getString("content");
                if (content != null) {
                    memories.add(content);
                }
            }
            return memories;

        } catch (Exception e) {
            log.error("向量检索失败", e);
            return Collections.emptyList();
        }
    }

    private byte[] toByteArray(List<Double> doubles) {
        ByteBuffer buffer = ByteBuffer.allocate(doubles.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (Double d : doubles) buffer.putFloat(d.floatValue());
        return buffer.array();
    }
}