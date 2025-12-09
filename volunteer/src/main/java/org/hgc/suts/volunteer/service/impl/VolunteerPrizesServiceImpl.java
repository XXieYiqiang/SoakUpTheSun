package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cursor.Cursor;
import org.hgc.suts.volunteer.common.constant.DistributionRedisConstant;
import org.hgc.suts.volunteer.common.constant.RedisCacheConstant;
import org.hgc.suts.volunteer.common.enums.VolunteerPrizesSendTypeEnum;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.dao.entity.VolunteerPrizesDO;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.hgc.suts.volunteer.dto.req.VolunteerPrizeDistributionReqDTO;
import org.hgc.suts.volunteer.dto.resp.VolunteerPrizesRespDTO;
import org.hgc.suts.volunteer.mq.event.VolunteerPrizesSendEvent;
import org.hgc.suts.volunteer.mq.producer.VolunteerPrizesSendProducer;
import org.hgc.suts.volunteer.service.VolunteerPrizesService;
import org.hgc.suts.volunteer.dao.mapper.VolunteerPrizesMapper;
import org.hgc.suts.volunteer.service.impl.easyExcel.VolunteerPrizes.CreateVolunteerPrizesExcelObject;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
* @author 谢毅强
* @description 针对表【volunteer_prizes(志愿者奖品表)】的数据库操作Service实现
* @createDate 2025-12-07 14:34:07
*/
@Service
@RequiredArgsConstructor
@Slf4j
public class VolunteerPrizesServiceImpl extends ServiceImpl<VolunteerPrizesMapper, VolunteerPrizesDO> implements VolunteerPrizesService{

    private final DefaultRedisScript<Long> savePrizesCacheScript;
    private final VolunteerPrizesMapper volunteerPrizesMapper;
    private final VolunteerUserMapper volunteerUserMapper;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;
    private final VolunteerPrizesSendProducer volunteerPrizesSendProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ExecutorService executorService = new ThreadPoolExecutor(
            // 核心线程
            Runtime.getRuntime().availableProcessors(),
            // 最大线程
            Runtime.getRuntime().availableProcessors() * 2,
            60, TimeUnit.SECONDS,
            // 缓冲队列
            new LinkedBlockingQueue<>(200),
            // 队列满了由主线程执行，防止报错丢失任务
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    // todo 创建奖品
    @Override
    public void createVolunteerPrizes(VolunteerPrizesDO volunteerPrizes) {
        //todo 放入布隆过滤器中
    }

    @SneakyThrows
    @Override
    public VolunteerPrizesRespDTO findVolunteerPrizes(Long prizesId) {
        String prizesKey = String.format(RedisCacheConstant.VOLUNTEER_PRIZES_KEY, prizesId);
        Map<Object, Object> prizesCacheMap = stringRedisTemplate.opsForHash().entries(prizesKey);

        // 1. 缓存命中直接返回
        if (MapUtil.isNotEmpty(prizesCacheMap)) {
            return BeanUtil.mapToBean(prizesCacheMap, VolunteerPrizesRespDTO.class, false, CopyOptions.create());
        }

        // 2. 缓存为空，获取锁
        RLock lock = redissonClient.getLock(RedisCacheConstant.VOLUNTEER_PRIZES_LOCK_KEY + prizesId);

        if (lock.tryLock(1, TimeUnit.SECONDS)) {
            try {
                // 3. 再查缓存，看看前面有没有线程把它放进缓存里面了
                prizesCacheMap = stringRedisTemplate.opsForHash().entries(prizesKey);
                if (MapUtil.isNotEmpty(prizesCacheMap)) {
                    return BeanUtil.mapToBean(prizesCacheMap, VolunteerPrizesRespDTO.class, false, CopyOptions.create());
                }

                // 4. 查询数据库
                VolunteerPrizesDO prizesDO = volunteerPrizesMapper.selectById(prizesId);
                if (prizesDO == null) {
                    // 说明prizesId错误，或者奖品已经被删除
                    throw new ClientException("该奖品不存在");
                }

                // 5. 准备写入 Redis
                VolunteerPrizesRespDTO actualRespDTO = BeanUtil.toBean(prizesDO, VolunteerPrizesRespDTO.class);
                Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);

                Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                        ));

                List<String> keys = Collections.singletonList(prizesKey);
                List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1);
                actualCacheTargetMap.forEach((key, value) -> {
                    args.add(key);
                    args.add(value);
                });

                // 防止数据库因为某种原因导致过期时间为空，给一个默认值
                long expireTime = (prizesDO.getValidEndTime() != null)
                        ? prizesDO.getValidEndTime().getTime() / 1000
                        : System.currentTimeMillis() / 1000 + 86400;

                args.add(String.valueOf(expireTime));

                stringRedisTemplate.execute(
                        savePrizesCacheScript,
                        keys,
                        args.toArray()
                );

                // 返回prizes的信息
                return actualRespDTO;

            } finally {
                lock.unlock();
            }
        } else {
            throw new RuntimeException("当前请求繁忙，请重试");
        }
    }
    @SneakyThrows
    @Override
    public void volunteerPrizeDistribution(VolunteerPrizeDistributionReqDTO requestParam) {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp";

        // todo 检验布隆过滤器
        // 加锁，防止重复发送奖品
        RLock lock = redissonClient.getLock(String.format(DistributionRedisConstant.VOLUNTEER_PRIZES_DISTRIBUTION_KEY, requestParam.getId()));
        // 尝试获取锁

        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new ClientException("该奖品分发任务正在执行中，请稍后再试");
        }
        try {
            // 1.获取奖品信息
            VolunteerPrizesDO volunteerPrizes = volunteerPrizesMapper.selectById(requestParam.getId());

            if (volunteerPrizes == null || volunteerPrizes.getStatus() == VolunteerPrizesSendTypeEnum.IN_PROGRESS.getStatus() ||  volunteerPrizes.getStatus() == VolunteerPrizesSendTypeEnum.SUCCESS.getStatus()) {
                throw new ClientException("奖品不存在 或 正在发放中/已结束");
            }

            // 修改奖品状态，防止重复消费
            LambdaUpdateWrapper<VolunteerPrizesDO> updateWrapper = Wrappers.lambdaUpdate(VolunteerPrizesDO.class)
                    .eq(VolunteerPrizesDO::getId, requestParam.getId())
                    .eq(VolunteerPrizesDO::getStatus, VolunteerPrizesSendTypeEnum.PENDING.getStatus())
                    .eq(VolunteerPrizesDO::getDelFlag, 0)
                    // 修改为 发放中
                    .set(VolunteerPrizesDO::getStatus, VolunteerPrizesSendTypeEnum.IN_PROGRESS.getStatus());
            volunteerPrizesMapper.update(updateWrapper);

            // 2. 计算截断分数线 (Score)
            // 2.1 总人数
            LambdaQueryWrapper<VolunteerUserDO> countWrapper = Wrappers.lambdaQuery(VolunteerUserDO.class)
                    .eq(VolunteerUserDO::getDelFlag, 0);
            long totalCount = volunteerUserMapper.selectCount(countWrapper);

            // 获取人数
            long finalLimit = getFinalLimit(totalCount, volunteerPrizes);

            try {
                executorService.execute(() -> processStreamExportAndMQ(finalLimit, baseDir, volunteerPrizes.getId()));
            } catch (Exception e) {
                log.error("任务提交线程池失败，立即回滚状态, prizeId: {}", volunteerPrizes.getId(), e);
                rollbackStatus(volunteerPrizes.getId());
                throw new ClientException("任务提交失败，请重试");
            }
        }  finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }


    private long getFinalLimit(long totalCount, VolunteerPrizesDO volunteerPrizes) {
        if (totalCount == 0) throw new ClientException("暂无有效志愿者");

        // 2.2 计算截断人数,向上取整
        long limit = (long) Math.ceil(totalCount * (volunteerPrizes.getProportion() / 100.0));

        // 获奖人数不能超过奖品库存, 如果库存只有 100 个，哪怕算出来前 10% 是 500 人，也只能发 100 个
        if (volunteerPrizes.getStock() != null && limit > volunteerPrizes.getStock()) {
            limit = volunteerPrizes.getStock();
        }
        if (limit <= 0) throw new ClientException("计算出的获奖人数为0");
        return limit;
    }



    private void processStreamExportAndMQ(long limit, String baseDir, Long prizeId) {

        // 使用临时文件后缀 .temp，防止生成一半被下载或覆盖
        String tempFileName = "prize_" + prizeId + "_" + System.currentTimeMillis() + ".temp";
        String finalFileName = "prize_" + prizeId + ".xlsx";

        File tempFile = FileUtil.file(baseDir, tempFileName);
        File finalFile = FileUtil.file(baseDir, finalFileName);

        FileUtil.mkdir(tempFile.getParentFile());

        // 清理旧的最终文件（如果存在）
        if (finalFile.exists()) {
            FileUtil.del(finalFile);
        }
        List<Long> allUserIdsForMq = new ArrayList<>();
        log.info("开始处理奖品分发: prizeId={}, limit={}, tempFile={}", prizeId, limit, tempFileName);

        try {
            transactionTemplate.setTimeout(1800);
            // 使用 transactionTemplate,包裹游标查询和文件写入
            transactionTemplate.executeWithoutResult(status -> {
                try (ExcelWriter excelWriter = EasyExcel.write(tempFile, CreateVolunteerPrizesExcelObject.class).build();
                     // Cursor 必须在事务内打开
                     Cursor<VolunteerUserDO> cursor = volunteerUserMapper.scanTopNUsers(limit)) {

                    WriteSheet writeSheet = EasyExcel.writerSheet(0, "获奖名单").build();

                    List<CreateVolunteerPrizesExcelObject> excelBatchList = new ArrayList<>();

                    int batchSize = 1000;

                    for (VolunteerUserDO user : cursor) {
                        excelBatchList.add(BeanUtil.toBean(user, CreateVolunteerPrizesExcelObject.class));
                        // 收集 ID
                        allUserIdsForMq.add(user.getId());
                        // 大于批次，写入
                        if (excelBatchList.size() >= batchSize) {
                            excelWriter.write(excelBatchList, writeSheet);
                            excelBatchList.clear();
                        }
                    }

                    // 处理尾数
                    if (!excelBatchList.isEmpty()) {
                        excelWriter.write(excelBatchList, writeSheet);
                    }

                } catch (Exception e) {
                    log.error("流式导出或MQ发送异常", e);
                    // 标记回滚
                    status.setRollbackOnly();
                    throw new RuntimeException("导出失败", e);
                }
            });

            // 重命名为正式文件
            FileUtil.rename(tempFile, finalFileName, true);

            // 标记为完成态
            LambdaUpdateWrapper<VolunteerPrizesDO> updateWrapper = Wrappers.lambdaUpdate(VolunteerPrizesDO.class)
                    .eq(VolunteerPrizesDO::getId, prizeId)
                    .eq(VolunteerPrizesDO::getStatus, VolunteerPrizesSendTypeEnum.IN_PROGRESS.getStatus())
                    // 修改为完成态
                    .set(VolunteerPrizesDO::getStatus, VolunteerPrizesSendTypeEnum.SUCCESS.getStatus());
            volunteerPrizesMapper.update(updateWrapper);
            sendMqSafely(allUserIdsForMq, prizeId);
            // 放入redis中，防止消息队列消费时一直通过查询数据库获取状态
            stringRedisTemplate.opsForHash().put(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_STATUS_KEY,String.valueOf(prizeId),String.valueOf(VolunteerPrizesSendTypeEnum.SUCCESS.getStatus()));
            stringRedisTemplate.expire(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_STATUS_KEY, 72L, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("奖品分发任务失败，回滚状态", e);
            // 失败时删除临时文件
            if (tempFile.exists()) FileUtil.del(tempFile);

            // 回滚状态为 0 (未发放)，允许重试
            rollbackStatus(prizeId);
        }
    }

    // 批量发送 MQ
    private void sendMqSafely(List<Long> userIds, Long prizeId) {
        if (userIds.isEmpty()) return;

        // 分批发送，避免单次请求过大
        int batchSize = 500;
        for (int i = 0; i < userIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userIds.size());
            // 分割列表
            List<Long> subList = userIds.subList(i, end);
            try {
                sendBatchMQ(subList, prizeId);
            } catch (Exception e) {
                // 只存日志不抛出异常，否则前面的事务会全部回滚
                log.error("MQ发送失败，批次范围[{}-{}]", i, end, e);
            }
        }
    }

    //  模拟批量发送MQ
    private void sendBatchMQ(List<Long> userIds, Long prizeId) {
        if (userIds.isEmpty()) return;
        VolunteerPrizesSendEvent volunteerPrizesSendEvent = VolunteerPrizesSendEvent.builder()
                .batchId(prizeId)
                .userList(userIds).build();
        volunteerPrizesSendProducer.sendMessage(volunteerPrizesSendEvent);
        log.info("批量发送MQ通知，本次包含 {} 人", userIds.size());
    }

    // 回滚事务
    private void rollbackStatus(Long prizeId) {
        LambdaUpdateWrapper<VolunteerPrizesDO> updateWrapper = Wrappers.lambdaUpdate(VolunteerPrizesDO.class)
                .eq(VolunteerPrizesDO::getId, prizeId)
                // 仅回滚“发放中”的任务
                .eq(VolunteerPrizesDO::getStatus, VolunteerPrizesSendTypeEnum.IN_PROGRESS.getStatus())
                .set(VolunteerPrizesDO::getStatus, VolunteerPrizesSendTypeEnum.PENDING.getStatus());
        volunteerPrizesMapper.update(updateWrapper);
    }
}





