package org.hgc.suts.volunteer.common.scheduledTask;

import cn.hutool.core.collection.CollUtil; // 引入 Hutool 集合工具
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerRatingMapper;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolunteerScoreTask {

    private final VolunteerRatingMapper volunteerRatingMapper;
    private final VolunteerUserMapper volunteerUserMapper;
    private final ElasticsearchClient elasticsearchClient;

    private static final String ES_INDEX_NAME = "volunteer_index";
    // 定义批处理大小，防止 SQL 过长或内存溢出
    private static final int BATCH_SIZE = 1000;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void calculateDailyScore() {
        log.info("================ 开始执行志愿者评分结算任务 ================");
        long start = System.currentTimeMillis();

        // 1. 获得每个人要加的分
        List<Map<String, Object>> aggList = volunteerRatingMapper.selectAggregatedUncalculatedRatings();

        if (aggList == null || aggList.isEmpty()) {
            log.info("无待结算数据");
            return;
        }

        List<Long> allRatingIds = new ArrayList<>();
        List<VolunteerUserDO> usersToSyncEs = new ArrayList<>();

        for (Map<String, Object> map : aggList) {
            try {
                Long userId = Convert.toLong(map.get("userId"));
                Double totalAddScore = Convert.toDouble(map.get("totalAddScore"));
                String idListStr = Convert.toStr(map.get("idListStr"));

                if (userId == null) continue;

                // 2. 更新 MySQL 分数
                if (totalAddScore > 0) {
                    volunteerUserMapper.addScore(userId, totalAddScore);
                }

                // 记录成功处理的评价 ID
                if (StrUtil.isNotBlank(idListStr)) {
                    List<Long> ids = Arrays.stream(idListStr.split(","))
                            .map(Long::parseLong)
                            .toList();
                    allRatingIds.addAll(ids);
                }

                // 准备 ES 数据 (为了保证数据一致性，重新查一次最新数据)
                VolunteerUserDO latestUser = volunteerUserMapper.selectById(userId);
                if (latestUser != null) {
                    usersToSyncEs.add(latestUser);
                }

                if (usersToSyncEs.size() >= BATCH_SIZE) {
                    syncToEsBatch(usersToSyncEs);
                    usersToSyncEs.clear(); // 释放内存
                }

            } catch (Exception e) {
                // 单个用户失败不应该中断任务
                log.error("================ 结算异常 userId={} ================", map.get("userId"), e);
            }
        }

        // 处理剩余未同步的 ES 数据
        if (!usersToSyncEs.isEmpty()) {
            syncToEsBatch(usersToSyncEs);
        }

        if (!allRatingIds.isEmpty()) {
            // 使用 Hutool 将大 List 切割成多个小 List
            List<List<Long>> splitIds = CollUtil.split(allRatingIds, BATCH_SIZE);
            for (List<Long> batchIds : splitIds) {
                volunteerRatingMapper.batchUpdateCalculatedStatus(batchIds);
            }
        }

        log.info("================ 结算结束，耗时: {}ms，处理评价数: {} ================",
                System.currentTimeMillis() - start, allRatingIds.size());
    }

    private void syncToEsBatch(List<VolunteerUserDO> userList) {
        if (CollUtil.isEmpty(userList)) return;
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (VolunteerUserDO user : userList) {
                br.operations(op -> op
                        .index(idx -> idx
                                .index(ES_INDEX_NAME)
                                .id(user.getId().toString())
                                .document(user)
                        )
                );
            }
            BulkResponse result = elasticsearchClient.bulk(br.build());
            if (result.errors()) {
                log.error("ES批量同步存在失败");
            }
        } catch (Exception e) {
            log.error("ES批量同步异常", e);
        }
    }
}