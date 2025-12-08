package org.hgc.suts.volunteer.mq.consumer;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.hgc.suts.volunteer.common.constant.DistributionRedisConstant;
import org.hgc.suts.volunteer.common.constant.RedisCacheConstant;
import org.hgc.suts.volunteer.common.enums.VolunteerPrizesSendTypeEnum;
import org.hgc.suts.volunteer.common.enums.VolunteerTaskStatusEnum;
import org.hgc.suts.volunteer.dao.entity.*;
import org.hgc.suts.volunteer.dao.mapper.*;
import org.hgc.suts.volunteer.mq.base.MessageWrapper;
import org.hgc.suts.volunteer.mq.event.VolunteerPrizesSendEvent;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "volunteer_prizes_topic",
        consumerGroup = "volunteer_prizes_cg"
)
@Slf4j
public class VolunteerPrizesSendConsumer implements RocketMQListener<MessageWrapper<VolunteerPrizesSendEvent>> {

    private final VolunteerPrizesMapper volunteerPrizesMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final VolunteerPrizesSendLogMapper volunteerPrizesSendLogMapper;
    private final VolunteerPrizesSendFailLogMapper volunteerPrizesSendFailLogMapper;
    private final RedissonClient redissonClient;
    @Override
    public void onMessage(MessageWrapper<VolunteerPrizesSendEvent> messageWrapper) {
        // 获取奖品id
        var volunteerPrizesBatchId = messageWrapper.getMessage().getBatchId();
        List<Long> userIdList = messageWrapper.getMessage().getUserList();

        if (userIdList == null || userIdList.isEmpty()) {
            log.info("[消费者] 用户列表为空，直接结束。BatchId: {}", volunteerPrizesBatchId);
            return;
        }

        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 志愿者推送任务正式执行 - 执行消费逻辑，消息体批次：{}", JSON.toJSONString(messageWrapper.getMessage().getBatchId()));

        // 判断志愿者任务发送状态是否为执行中，如果不是有可能是被取消状态
        Integer status = getStatusWithCache(volunteerPrizesBatchId);
        if (ObjectUtil.notEqual(status, VolunteerPrizesSendTypeEnum.SUCCESS.getStatus())) {
            log.warn("[消费者] 志愿者推送任务正式执行 - 推送任务记录状态异常：{}，已终止推送", status);
            return ;
        }


        List<VolunteerPrizesSendLogDO> allVolunteerPrizesSendLogDOList=new ArrayList<>();
        List<VolunteerPrizesSendLogDO> VolunteerPrizesSendSuccessList = new ArrayList<>();
        // 存入redis的键
        String VolunteerPrizesSendLogKey=String.format(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_KEY, volunteerPrizesBatchId);;
        for (Long userId : userIdList) {
            VolunteerPrizesSendLogDO volunteerPrizesSendLogDO=new VolunteerPrizesSendLogDO();
            volunteerPrizesSendLogDO.setVolunteerId(userId);
            volunteerPrizesSendLogDO.setPrizesId(volunteerPrizesBatchId);
            // 设置CDK
            String CDK =IdUtil.simpleUUID();
            volunteerPrizesSendLogDO.setCdk(CDK);
            allVolunteerPrizesSendLogDOList.add(volunteerPrizesSendLogDO);
        }
        try {
            volunteerPrizesSendLogMapper.insert(allVolunteerPrizesSendLogDOList,allVolunteerPrizesSendLogDOList.size());
            // 如果全部成功，就全部加入到成功列表
            VolunteerPrizesSendSuccessList.addAll(allVolunteerPrizesSendLogDOList);
        } catch (Exception ignore) {
            List<VolunteerPrizesSendFailLogDO> volunteerPrizesSendFailDOList = new ArrayList<>();
            allVolunteerPrizesSendLogDOList.forEach(each->{
                try {
                    volunteerPrizesSendLogMapper.insert(each);
                    // 加入成功的
                    VolunteerPrizesSendSuccessList.add(each);
                } catch (Exception e) {
                    Map<Object, Object> objectMap = MapUtil.builder()
                            .put("volunteerId", each.getVolunteerId())
                            .put("cause", e.getMessage())
                            .build();
                    VolunteerPrizesSendFailLogDO volunteerTaskFailDO = VolunteerPrizesSendFailLogDO.builder()
                            .prizesId(each.getPrizesId())
                            .jsonObject(JSON.toJSONString(objectMap))
                            .build();
                    volunteerPrizesSendFailDOList.add(volunteerTaskFailDO);
                }
            });
            // 不为空时，插入
            if (!volunteerPrizesSendFailDOList.isEmpty()) {
                volunteerPrizesSendFailLogMapper.insert(volunteerPrizesSendFailDOList,volunteerPrizesSendFailDOList.size());
            }



        }
        // 转换为 Map
        Map<String, String> redisDataMap = MapUtil.newHashMap(VolunteerPrizesSendSuccessList.size());
        VolunteerPrizesSendSuccessList.forEach(each -> {
            redisDataMap.put(String.valueOf(each.getVolunteerId()), each.getCdk());
        });

        if (!redisDataMap.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(VolunteerPrizesSendLogKey, redisDataMap);
            stringRedisTemplate.expire(VolunteerPrizesSendLogKey, 72L, TimeUnit.HOURS);
            log.info("[消费者] 发放记录写入成功，数量：{}", VolunteerPrizesSendSuccessList.size());
        }

    }


    private Integer getStatusWithCache(Long batchId) {
        // 1. 查缓存
        Object statusObj = stringRedisTemplate.opsForHash().get(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_STATUS_KEY, String.valueOf(batchId));
        if (statusObj != null) {
            try {
                return Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // 2. 缓存未命中，加锁查库
        RLock lock = redissonClient.getLock(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_STATUS_LOCK_KEY + batchId);

        try {
            // 如果获取锁失败，应该抛出异常,重试
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
                try {
                    // 二次查找，防止堆积在锁外的请求进来后再次访问数据库，降低效率
                    statusObj = stringRedisTemplate.opsForHash().get(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_STATUS_KEY, String.valueOf(batchId));
                    if (statusObj != null) {
                        return Integer.parseInt(statusObj.toString());
                    }

                    VolunteerPrizesDO prizesDO = volunteerPrizesMapper.selectById(batchId);
                    if (prizesDO == null) {
                        // 数据库也没查到，说明 ID 错误，终止任务
                        return null;
                    }

                    Integer status = prizesDO.getStatus();
                    stringRedisTemplate.opsForHash().put(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_STATUS_KEY, String.valueOf(batchId), String.valueOf(status));
                    stringRedisTemplate.expire(RedisCacheConstant.VOLUNTEER_PRIZES_SEND_STATUS_KEY, 72L, TimeUnit.HOURS);

                    return status;
                } finally {
                    lock.unlock();
                }
            } else {
                // 获取锁失败
                throw new RuntimeException("获取锁失败，触发重试");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程中断，触发重试", e);
        }
    }
}

