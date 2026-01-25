package org.hgc.suts.gateway.manager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.common.constant.RedisCacheConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

import java.util.Map;

/**
 * 向量索引管理器 (Jedis 版)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorIndexManager {

    private final JedisPooled jedis;

    @Value("${suts.ai.vector.dimension:1024}")
    private int vectorDimension;

    @PostConstruct
    public void initIndex() {
        String indexName = RedisCacheConstant.VECTOR_INDEX_NAME;
        try {
            // 1. 检查索引是否存在
            jedis.ftInfo(indexName);
            log.info("Redis向量索引 [{}] 检查正常。", indexName);
        } catch (Exception e) {
            // 如果报错通常说明索引不存在
            log.info("索引 [{}] 不存在，开始创建...", indexName);
            createIndex(indexName);
        }
    }

    private void createIndex(String indexName) {
        try {
            // 2. 定义 Schema
            Schema schema = new Schema()
                    // 过滤字段
                    .addTagField("userId")
                    // 文本字段
                    .addTextField("content", 1.0)
                    .addVectorField("vector", Schema.VectorField.VectorAlgo.HNSW,
                            Map.of(
                                    "TYPE", "FLOAT32",
                                    "DIM", vectorDimension,
                                    "DISTANCE_METRIC", "COSINE"
                            ));

            // 3. 定义索引规则
            IndexDefinition def = new IndexDefinition()
                    .setPrefixes(RedisCacheConstant.VECTOR_DATA_KEY_PREFIX);

            // 4. 执行创建
            jedis.ftCreate(indexName, IndexOptions.defaultOptions().setDefinition(def), schema);

            log.info("Redis向量索引创建成功！");
        } catch (Exception e) {
            log.error("创建向量索引失败", e);
        }
    }
}