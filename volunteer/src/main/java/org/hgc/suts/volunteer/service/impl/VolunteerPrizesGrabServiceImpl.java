package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.hgc.suts.volunteer.common.biz.user.UserContext;
import org.hgc.suts.volunteer.common.constant.RedisCacheConstant;
import org.hgc.suts.volunteer.common.enums.VolunteerPrizesGrabStutsEnum;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.common.exception.RemoteException;
import org.hgc.suts.volunteer.common.exception.ServiceException;
import org.hgc.suts.volunteer.dao.entity.VolunteerPrizesGrabDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerPrizesMapper;
import org.hgc.suts.volunteer.dto.req.redeemVolunteerPrizesDTO;
import org.hgc.suts.volunteer.dto.resp.VolunteerPrizesRespDTO;
import org.hgc.suts.volunteer.mq.event.VolunteerPrizesGrabDBSyncEvent;
import org.hgc.suts.volunteer.mq.producer.VolunteerPrizesDBSyncProducer;
import org.hgc.suts.volunteer.mq.producer.VolunteerPrizesSendProducer;
import org.hgc.suts.volunteer.service.VolunteerPrizesGrabService;
import org.hgc.suts.volunteer.dao.mapper.VolunteerPrizesGrabMapper;
import org.hgc.suts.volunteer.service.VolunteerPrizesService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;

/**
* @author 谢毅强
* @description 针对表【volunteer_prizes_grab】的数据库操作Service实现
* @createDate 2025-12-09 14:45:16
*/
@Service
@Slf4j
@AllArgsConstructor
public class VolunteerPrizesGrabServiceImpl extends ServiceImpl<VolunteerPrizesGrabMapper, VolunteerPrizesGrabDO> implements VolunteerPrizesGrabService{

    private final VolunteerPrizesService volunteerPrizesService;
    // lua脚本，用于同步库存和限领
    private final DefaultRedisScript<Long> redeemVolunteerPrizesStockSynchronize;
    // lua脚本，用于回滚库存
    private final DefaultRedisScript<Long> rollbackPrizesStock;
    private final StringRedisTemplate stringRedisTemplate;

    private final VolunteerPrizesDBSyncProducer volunteerPrizesDBSyncProducer;
    @Override
    public void redeemVolunteerPrizes(redeemVolunteerPrizesDTO requestParam){
        VolunteerPrizesRespDTO volunteerPrizes = volunteerPrizesService.findVolunteerPrizes(requestParam.getPrizesId());
        // 判断是否为有效时间
        boolean isInTime = DateUtil.isIn(new Date(), volunteerPrizes.getValidStartTime(), volunteerPrizes.getValidEndTime());
        if (!isInTime) {
            throw new ClientException("不满足奖品领取时间");
        }

        String prizesKey = String.format(RedisCacheConstant.VOLUNTEER_PRIZES_KEY, requestParam.getPrizesId());
        String volunteerPrizesGrabKey = String.format(RedisCacheConstant.VOLUNTEER_PRIZES_VOLUNTEER_GRAB_KEY, requestParam.getVolunteerId());

        // 返回状态 0 完成 1 库存不足 2 已领过
        long status = stringRedisTemplate.execute(
                redeemVolunteerPrizesStockSynchronize,
                ListUtil.of(prizesKey, volunteerPrizesGrabKey),
                String.valueOf(volunteerPrizes.getValidEndTime().getTime())
        );

        // 根据状态返回错误类型
        if (VolunteerPrizesGrabStutsEnum.isFail(status)) {
            throw new ServiceException(VolunteerPrizesGrabStutsEnum.fromType(status));
        }
        // 构造发送消息
        VolunteerPrizesGrabDBSyncEvent volunteerPrizesGrabDBSyncEvent = VolunteerPrizesGrabDBSyncEvent.builder()
                .volunteerPrizesDBSyncDTO(volunteerPrizes)
                .volunteerId(requestParam.getVolunteerId()).build();
        try {
            // 生产者发送消息
            SendResult sendResult = volunteerPrizesDBSyncProducer.sendMessage(volunteerPrizesGrabDBSyncEvent);
            // 检查发送状态
            if (sendResult == null || !StrUtil.equals(sendResult.getSendStatus().name(), "SEND_OK")) {
                // 主动抛出异常，进入 catch 块执行回滚
                throw new RemoteException("消息未正常发送，兑换失败");
            }

        } catch (Exception e) {
            log.warn("抢购成功但MQ发送失败，开始回滚。原因: {}", e.getMessage());
            try {
                // 执行库存回滚
                stringRedisTemplate.execute(rollbackPrizesStock, Arrays.asList(prizesKey, volunteerPrizesGrabKey));
                log.info("库存回滚成功。PrizesId: {}", requestParam.getPrizesId());
            } catch (Exception rollbackEx) {
                // 正常来说MQ和redis同时都挂了的概率太低了
                log.warn("库存回滚失败，PrizesId: {},VolunteerId: {}", requestParam.getPrizesId(), requestParam.getVolunteerId());
            }

            throw new ClientException("网络繁忙，兑换奖品失败，请稍后重试");
        }
    }
}




