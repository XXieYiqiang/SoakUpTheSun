package org.hgc.suts.volunteer.mq.consumer;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.hgc.suts.volunteer.common.constant.RedisCacheConstant;
import org.hgc.suts.volunteer.common.enums.VolunteerTaskStatusEnum;
import org.hgc.suts.volunteer.dao.entity.VolunteerPrizesGrabDO;
import org.hgc.suts.volunteer.dao.mapper.*;
import org.hgc.suts.volunteer.dto.resp.VolunteerPrizesRespDTO;
import org.hgc.suts.volunteer.mq.base.MessageWrapper;
import org.hgc.suts.volunteer.mq.consumer.easyExcel.volunteerCreateExcel.ReadExcelDistributionListener;
import org.hgc.suts.volunteer.mq.consumer.easyExcel.volunteerCreateExcel.VolunteerExcelObject;
import org.hgc.suts.volunteer.mq.event.VolunteerPrizesGrabDBSyncEvent;
import org.hgc.suts.volunteer.mq.event.VolunteerTaskExecuteEvent;
import org.hgc.suts.volunteer.mq.producer.VolunteerUserEsSyncProducer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "volunteerPrizes_grab_DBSync_topic",
        consumerGroup = "volunteerPrizes_grab_DBSync_cg"
)
@Slf4j
public class VolunteerPrizesGrabDBSyncConsumer implements RocketMQListener<MessageWrapper<VolunteerPrizesGrabDBSyncEvent>> {

    private final VolunteerPrizesMapper  volunteerPrizesMapper;
    private final VolunteerPrizesGrabMapper  volunteerPrizesGrabMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(MessageWrapper<VolunteerPrizesGrabDBSyncEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 志愿者推送任务正式执行 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        VolunteerPrizesRespDTO volunteerPrizesDBSyncDTO =messageWrapper.getMessage().getVolunteerPrizesDBSyncDTO();
        Long volunteerId =messageWrapper.getMessage().getVolunteerId();

        int decremented = volunteerPrizesMapper.decrementPrizesStock(volunteerPrizesDBSyncDTO.getId(), 1L);
        if (!SqlHelper.retBool(decremented)) {
            log.warn("[消费者] 用户领取奖品 - 执行消费逻辑，扣减奖品数据库库存失败，消息体：{}", JSON.toJSONString(messageWrapper));
            return;
        }
        String cdk = IdUtil.simpleUUID();
        Date now = new Date();

        // 插入领取记录
        VolunteerPrizesGrabDO volunteerPrizesGrabDO = VolunteerPrizesGrabDO.builder()
                .prizesId(volunteerPrizesDBSyncDTO.getId())
                .cdk(cdk)
                .validEndTime(volunteerPrizesDBSyncDTO.getValidEndTime())
                .volunteerId(volunteerId)
                .receiveCount(1)
                .build();
        volunteerPrizesGrabMapper.insert(volunteerPrizesGrabDO);


        // 2. Redis (Write-Read-Write) 防止指令丢失
        String volunteerPrizesListKey = String.format(RedisCacheConstant.VOLUNTEER_PRIZES_LIST, volunteerId);

        // 构造cdk信息
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("prizesId", volunteerPrizesDBSyncDTO.getId());
        valueMap.put("prizeName", volunteerPrizesDBSyncDTO.getName());
        valueMap.put("cdk", cdk);
        valueMap.put("time", now.getTime());
        String redisValue = JSON.toJSONString(valueMap);
        stringRedisTemplate.opsForHash().put(volunteerPrizesListKey, cdk, redisValue);
        stringRedisTemplate.expire(volunteerPrizesListKey, 30L, TimeUnit.DAYS);
        Object valueInRedis;
        try {
            // 查询是否存在
            valueInRedis = stringRedisTemplate.opsForHash().get(volunteerPrizesListKey, cdk);

            // 如果查不到（可能因主从切换导致丢失），则重试写入
            if (valueInRedis == null) {
                stringRedisTemplate.opsForHash().put(volunteerPrizesListKey, cdk, redisValue);
                stringRedisTemplate.expire(volunteerPrizesListKey, 30L, TimeUnit.DAYS);
            }
        } catch (Throwable ex) {
            log.warn("[消费者] Redis异常监测 - Hash数据可能丢失，触发兜底写入，错误信息：{}", ex.getMessage());
            // 异常兜底：再次写入
            stringRedisTemplate.opsForHash().put(volunteerPrizesListKey, cdk, redisValue);
            stringRedisTemplate.expire(volunteerPrizesListKey, 30L, TimeUnit.DAYS);
        }



    }
}
