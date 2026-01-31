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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static java.util.Arrays.copyOfRange;

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
    @Value("${picture.ai.video-frame-url}")
    private String aiApiUrl;

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
        byte[] originalData = event.getImageData();

        if (originalData == null || originalData.length == 0) return;

        try {
            byte[] cleanData = findRealImageStart(originalData);

            // 没找到图片头，可能是坏数据
            if (cleanData.length == 0) {
                log.warn("未能识别有效图片头，丢弃数据");
                return;
            }

            // 3. 包装成文件流
            InputStream inputStream = new ByteArrayInputStream(cleanData);
            InputStreamResource resource = new InputStreamResource(inputStream, "frame.jpg");

            // 4. HTTP 请求 Python
            HttpResponse response = HttpRequest.post(aiApiUrl)
                    .form("image", resource)
                    .form("session_id", userId)
                    .timeout(2000)
                    .execute();

            if (response.isOk()) {
                String aiResultJson = response.body();
                // 5. 推送回前端
                pushResultToUser(userId, aiResultJson);
                log.info("AI 端: status={}, body={}", response.getStatus(), response.body());
            } else {
                // 没有拿到则报错
                log.error("AI 端错误: status={}, body={}", response.getStatus(), response.body());
            }

        } catch (Exception e) {
            log.error("[消费者] 实时视频流分析正式执行 - 消费失败，AI端错误", e);
        }
    }

    private byte[] findRealImageStart(byte[] data) {
        if (data == null || data.length < 10) return new byte[0];

        // 搜索前 50 个字节
        int limit = Math.min(data.length - 1, 50);
        for (int i = 0; i < limit; i++) {
            // 检查 JPEG 头
            if (data[i] == (byte) 0xFF && data[i + 1] == (byte) 0xD8) {
                // 如果发现头不是在第0位，说明前面有垃圾数据，切掉它
                if (i > 0) {
                    return copyOfRange(data, i, data.length);
                }
                // 格式正确，直接返回
                return data;
            }
            // 检查 PNG 头
            if (data[i] == (byte) 0x89 && data[i + 1] == (byte) 0x50) {
                if (i > 0) {
                    return copyOfRange(data, i, data.length);
                }
                return data;
            }
        }
        // 没找到头，返回原数据
        return data;
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