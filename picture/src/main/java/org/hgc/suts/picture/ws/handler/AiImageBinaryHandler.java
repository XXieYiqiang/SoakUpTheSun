package org.hgc.suts.picture.ws.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.picture.mq.event.VideoFrameAnalysisEvent;
import org.hgc.suts.picture.mq.producer.VideoFrameProducer;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiImageBinaryHandler extends AbstractWebSocketHandler {

    private final VideoFrameProducer videoFrameProducer;

    public static final Map<String, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("WEBSOCKET_USER_ID");
        if (userId != null) {
            USER_SESSION_MAP.put(userId, session);
            session.setBinaryMessageSizeLimit(500 * 1024);
            log.info("AI 连接: {}", userId);
        } else {
            try {
                session.close();
            } catch (Exception ignored) {

            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            String userId = (String) session.getAttributes().get("WEBSOCKET_USER_ID");
            if (userId == null) return;

            ByteBuffer buffer = message.getPayload();
            byte[] imageBytes = new byte[buffer.remaining()];
            buffer.get(imageBytes);

            VideoFrameAnalysisEvent event = VideoFrameAnalysisEvent.builder()
                    .userId(userId)
                    .imageData(imageBytes)
                    .timestamp(System.currentTimeMillis())
                    .build();

            // 发送到实时分析消息队列
            videoFrameProducer.sendRealTimeFrame(event);

        } catch (Exception e) {
            log.error("WS 错误", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("WEBSOCKET_USER_ID");
        if (userId != null) {
            USER_SESSION_MAP.remove(userId);
        }
    }
}