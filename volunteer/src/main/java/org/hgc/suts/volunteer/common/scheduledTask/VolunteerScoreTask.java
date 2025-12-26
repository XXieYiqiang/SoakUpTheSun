package org.hgc.suts.volunteer.common.scheduledTask;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.volunteer.dao.entity.VolunteerRatingDO;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerRatingMapper;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolunteerScoreTask {

    private final VolunteerRatingMapper volunteerRatingMapper;
    private final VolunteerUserMapper volunteerUserMapper;
    private final ElasticsearchClient elasticsearchClient;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    private static final String ES_INDEX_NAME = "volunteer_index";
    private static final int BATCH_SIZE = 2000;

    @PostConstruct
    public void init() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // 凌晨2点运行
    @Scheduled(cron = "0 0 2 * * ?")
    public void calculateDailyScore() {
        log.info(">>> [ScoreTask] 开始执行评分结算 (Double精度: 1.0/0.7)...");
        long start = System.currentTimeMillis();
        long lastId = 0L;
        int totalProcessed = 0;

        while (true) {
            // 1. 游标拉取数据，防止内存溢出
            List<VolunteerRatingDO> ratingList = volunteerRatingMapper.selectUncalculatedBatch(lastId, BATCH_SIZE);
            if (CollUtil.isEmpty(ratingList)) {
                break;
            }

            lastId = ratingList.get(ratingList.size() - 1).getId();

            // 2. 事务处理本批次
            try {
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        processBatch(ratingList);
                    }
                });
                totalProcessed += ratingList.size();
                log.info("定时任务[评分更新]-批次完成, 当前游标ID: {}, 累计处理: {}", lastId, totalProcessed);
            } catch (Exception e) {
                log.error("定时任务[评分更新]-批次异常回滚, lastId={}", lastId, e);
                break;
            }
        }
        log.info("定时任务[评分更新]-结算结束, 耗时: {}ms", System.currentTimeMillis() - start);
    }

    private void processBatch(List<VolunteerRatingDO> ratingList) {
        // 1. 改成在内存中聚合，减少mysql压力
        Map<Long, Double> userScoreMap = new HashMap<>();
        List<Long> ratingIds = new ArrayList<>();

        for (VolunteerRatingDO rating : ratingList) {
            // 默认无好评
            double scoreToAdd = 0.7;
            if (rating.getRating() != null && rating.getRating() == 1) {
                // 好评加分
                scoreToAdd = 1.0;
            }
            // 合并
            userScoreMap.merge(rating.getUserId(), scoreToAdd, Double::sum);
            ratingIds.add(rating.getId());
        }

        if (MapUtil.isEmpty(userScoreMap)) return;

        // 2. 批量更新 MySQL (执行 score = score + 加分)
        volunteerUserMapper.batchAddScore(userScoreMap);

        // 3. 标记已结算
        if (CollUtil.isNotEmpty(ratingIds)) {
            volunteerRatingMapper.batchUpdateCalculatedStatus(ratingIds);
        }

        // 4. 同步 ES
        syncToEsSafely(userScoreMap.keySet());
    }

    private void syncToEsSafely(Set<Long> userIds) {
        try {
            if (CollUtil.isEmpty(userIds)) return;
            // 查出最新的分数
            List<VolunteerUserDO> users = volunteerUserMapper.selectBatchIds(userIds);

            BulkRequest.Builder br = new BulkRequest.Builder();
            for (VolunteerUserDO user : users) {
                br.operations(op -> op
                        .index(idx -> idx
                                .index(ES_INDEX_NAME)
                                .id(user.getId().toString())
                                .document(user)
                        )
                );
            }
            elasticsearchClient.bulk(br.build());
        } catch (Exception e) {
            log.error("定时任务[评分更新]- ES同步异常,请管理员进行修复", e);
        }
    }
}