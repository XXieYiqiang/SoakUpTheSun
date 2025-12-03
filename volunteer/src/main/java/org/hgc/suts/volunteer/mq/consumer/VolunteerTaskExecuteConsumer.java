package org.hgc.suts.volunteer.mq.consumer;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.hgc.suts.volunteer.common.easyExcel.ReadExcelDistributionListener;
import org.hgc.suts.volunteer.common.easyExcel.VolunteerExcelObject;
import org.hgc.suts.volunteer.common.enums.VolunteerTaskStatusEnum;
import org.hgc.suts.volunteer.dao.mapper.VolunteerTaskFailMapper;
import org.hgc.suts.volunteer.dao.mapper.VolunteerTaskMapper;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.hgc.suts.volunteer.mq.base.MessageWrapper;
import org.hgc.suts.volunteer.mq.event.VolunteerTaskExecuteEvent;
import org.hgc.suts.volunteer.mq.producer.VolunteerUserEsSyncProducer;
import org.hgc.suts.volunteer.service.VolunteerUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "volunteerTask_excel_topic",
        consumerGroup = "volunteerTask_excel_cg"
)
@Slf4j
public class VolunteerTaskExecuteConsumer implements RocketMQListener<MessageWrapper<VolunteerTaskExecuteEvent>> {

    private final VolunteerTaskMapper volunteerTaskMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final VolunteerUserMapper volunteerUserMapper;
    private final VolunteerTaskFailMapper volunteerTaskFailMapper;
    private final VolunteerUserEsSyncProducer volunteerUserEsSyncProducer;

    @Override
    public void onMessage(MessageWrapper<VolunteerTaskExecuteEvent> messageWrapper) {
// 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 志愿者推送任务正式执行 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        // 判断志愿者任务发送状态是否为执行中，如果不是有可能是被取消状态
        var volunteerTaskId = messageWrapper.getMessage().getVolunteerTaskId();
        var volunteerTaskDO = volunteerTaskMapper.selectById(volunteerTaskId);
        if (ObjectUtil.notEqual(volunteerTaskDO.getStatus(), VolunteerTaskStatusEnum.IN_PROGRESS.getStatus())) {
            log.warn("[消费者] 志愿者推送任务正式执行 - 推送任务记录状态异常：{}，已终止推送", volunteerTaskDO.getStatus());
            return ;
        }

        // 正式开始执行志愿者任务
        var readExcelDistributionListener = new ReadExcelDistributionListener(
                volunteerTaskDO,
                stringRedisTemplate,
                volunteerUserMapper,
                volunteerTaskFailMapper,
                volunteerUserEsSyncProducer
        );
        EasyExcel.read(volunteerTaskDO.getFileAddress(), VolunteerExcelObject.class, readExcelDistributionListener).sheet().doRead();

    }
}
