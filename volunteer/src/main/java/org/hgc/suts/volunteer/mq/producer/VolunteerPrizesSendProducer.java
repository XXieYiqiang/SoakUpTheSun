package org.hgc.suts.volunteer.mq.producer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.hgc.suts.volunteer.mq.base.BaseSendExtendDTO;
import org.hgc.suts.volunteer.mq.base.MessageWrapper;
import org.hgc.suts.volunteer.mq.event.VolunteerPrizesSendEvent;
import org.hgc.suts.volunteer.mq.event.VolunteerTaskExecuteEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 志愿者任务执行生产者
 */
@Slf4j
@Component
public class VolunteerPrizesSendProducer extends AbstractCommonSendProduceTemplate<VolunteerPrizesSendEvent> {

    private final ConfigurableEnvironment environment;

    public VolunteerPrizesSendProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(VolunteerPrizesSendEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("志愿者奖品发送执行")
                .keys(String.valueOf(messageSendEvent.getBatchId()))
                .topic(environment.resolvePlaceholders("volunteer_prizes_topic"))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(VolunteerPrizesSendEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }

}
