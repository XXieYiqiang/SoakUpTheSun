package org.hgc.suts.picture.mq.producer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.hgc.suts.picture.mq.base.BaseSendExtendDTO;
import org.hgc.suts.picture.mq.base.MessageWrapper;
import org.hgc.suts.picture.mq.event.VideoFrameAnalysisEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 实时视频流生产者
 */
@Slf4j
@Component
public class VideoFrameProducer extends AbstractCommonSendProduceTemplate<VideoFrameAnalysisEvent> {

    private final ConfigurableEnvironment environment;
    private final RocketMQTemplate rocketMQTemplate;

    public VideoFrameProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.rocketMQTemplate = rocketMQTemplate;
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(VideoFrameAnalysisEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("实时视频流分析")
                // 使用 UserId 作为 Key，使得同一用户的为队列形式.
                .keys(messageSendEvent.getUserId())
                .topic(environment.resolvePlaceholders("picture_video_frame_topic"))
                .tag("realtime")
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(VideoFrameAnalysisEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper<>(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }

    /**
     * 使用不需要等待 Broker 确认的队列，降低 WebSocket 延迟，调用 sendOneWay。
     */
    public void sendRealTimeFrame(VideoFrameAnalysisEvent event) {
        try {
            BaseSendExtendDTO sendParam = buildBaseSendExtendParam(event);
            Message<?> message = buildMessage(event, sendParam);

            // 拼接 Topic:Tag
            String destination = sendParam.getTopic() + ":" + sendParam.getTag();

            // 使用 OneWay 发送
            rocketMQTemplate.sendOneWay(destination, message);

        } catch (Exception e) {
            // 偶尔丢几帧没关系，不抛出异常打断 WebSocket
            log.warn("视频流发送 MQ 失败: {}", e.getMessage());
        }
    }
}