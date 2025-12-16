package org.hgc.suts.picture.mq.producer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.hgc.suts.picture.mq.base.BaseSendExtendDTO;
import org.hgc.suts.picture.mq.base.MessageWrapper;
import org.hgc.suts.picture.mq.event.UploadPictureAnalysisEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 图片分析执行生产者
 */
@Slf4j
@Component
public class PictureAnalysisSendProducer extends AbstractCommonSendProduceTemplate<UploadPictureAnalysisEvent> {

    private final ConfigurableEnvironment environment;

    public PictureAnalysisSendProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(UploadPictureAnalysisEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("图片分析发送执行")
                .keys(String.valueOf(messageSendEvent.getPictureId()))
                .topic(environment.resolvePlaceholders("picture_analysis_topic"))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(UploadPictureAnalysisEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }

}
