package org.hgc.suts.picture.mq.consumer;

import cn.hutool.core.io.resource.InputStreamResource;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.hgc.suts.picture.mq.base.MessageWrapper; // 必须引入你的 Wrapper
import org.hgc.suts.picture.mq.event.VideoFrameAnalysisEvent;
import org.hgc.suts.picture.ws.handler.AiImageBinaryHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@Slf4j
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "picture_video_frame_topic",
        consumerGroup = "picture_video_frame_cg"
)
// 注意泛型：接收的是 MessageWrapper
public class VideoFrameConsumer implements RocketMQListener<MessageWrapper<VideoFrameAnalysisEvent>> {
    // 暂时写死，后续更改为实时处理地址
    private final String aiApiUrl = "http://127.0.0.1:20000/api/vision-qa/qwen";

    @Override
    public void onMessage(MessageWrapper<VideoFrameAnalysisEvent> wrapper) {
        // 1. 拿到事件
        VideoFrameAnalysisEvent event = wrapper.getMessage();

        // 报平安
        log.info("[消费者] 实时视频流分析正式执行 - 执行消费逻辑，消息userid：{}", JSON.toJSONString(wrapper.getMessage().getUserId()));

        // 2. 时间间隔太久,直接丢弃即可
        if (System.currentTimeMillis() - event.getTimestamp() > 3000) {
            return;
        }
        // 获取处理信息
        String userId = event.getUserId();
        byte[] imageData = event.getImageData();

        if (imageData == null || imageData.length == 0) return;

        try {
            // 3. 包装成文件流
            InputStream inputStream = new ByteArrayInputStream(imageData);
            InputStreamResource resource = new InputStreamResource(inputStream, "frame.jpg");

            // 4. HTTP 请求 Python
            HttpResponse response = HttpRequest.post(aiApiUrl)
                    .form("file", resource)
                    .timeout(2000)
                    .execute();

            if (response.isOk()) {
                String aiResultJson = response.body();
                // 5. 推送回前端
                pushResultToUser(userId, aiResultJson);
            } else {
                // 没有拿到则报错
                log.error("AI 端错误: {}", response.getStatus());
            }

        } catch (Exception e) {
            log.error("[消费者] 实时视频流分析正式执行 - 消费失败，AI端错误", e);
        }
    }

    private void pushResultToUser(String userId, String aiResultText) {
        WebSocketSession session = AiImageBinaryHandler.USER_SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            try {
                JSONObject resp = new JSONObject();
                resp.put("type", "REALTIME_AI_RESULT");
                resp.put("data", aiResultText);
                resp.put("timestamp", System.currentTimeMillis());

                synchronized (session) {
                    session.sendMessage(new TextMessage(resp.toJSONString()));
                }
            } catch (Exception e) {
                log.error("推送消息失败", e);
            }
        }
    }
}