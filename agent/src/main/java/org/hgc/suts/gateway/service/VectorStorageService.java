package org.hgc.suts.gateway.service;

import java.util.List;

/**
 * 向量存储服务接口
 */
public interface VectorStorageService {

    /**
     * 保存记忆
     * @param userId  用户ID (用于数据隔离，不同用户的记忆互不可见)
     * @param content 原始记忆文本 (例如："用户提到他对花生过敏")
     * @param vector  文本对应的向量数据 (Float/Double 列表)
     */
    void saveMemory(Long userId, String content, List<Double> vector);

    /**
     * 语义检索
     * @param userId      用户ID (强制过滤条件)
     * @param queryVector 当前问题的向量表示
     * @param limit       返回条数限制
     * @return 匹配到的文本列表
     */
    List<String> searchMemory(Long userId, List<Double> queryVector, int limit);
}