package org.hgc.suts.volunteer.mq.producer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.hgc.suts.volunteer.mq.base.BaseSendExtendDTO;
import org.hgc.suts.volunteer.mq.base.MessageWrapper;
import org.hgc.suts.volunteer.mq.event.VolunteerTaskExecuteEvent;
import org.hgc.suts.volunteer.mq.event.VolunteerUserEsSyncEvent; // 导入 ES 同步事件
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 志愿者用户ES同步生产者
 * 负责将MySQL写入成功的批量用户数据异步发送到ES同步队列。
 */
@Slf4j
@Component
public class VolunteerUserEsSyncProducer extends AbstractCommonSendProduceTemplate<VolunteerUserEsSyncEvent> {

    private final ConfigurableEnvironment environment;

    public VolunteerUserEsSyncProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(VolunteerUserEsSyncEvent messageSendEvent) {
        
        return BaseSendExtendDTO.builder()
                .eventName("志愿者用户ES同步执行")
                .keys(String.valueOf(messageSendEvent.getBatchId()))
                .topic(environment.resolvePlaceholders("volunteerTask_excel_es_topic"))
                .sentTimeout(3000L) // 适当延长发送超时时间
                .build();
    }

    @Override
    protected Message<?> buildMessage(VolunteerUserEsSyncEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        // 使用请求参数中的 Keys，如果为空则生成 UUID
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        
        // 封装消息负载和设置消息头
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys) // 设置 RocketMQ 的业务键
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag()) // 设置 Tag
                .build();
    }

}
