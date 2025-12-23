package org.hgc.suts.picture.ws.interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.picture.common.biz.user.UserInfoDTO;
import org.hgc.suts.picture.common.constant.RedisCacheConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 安全握手拦截器
 */
@Slf4j
@Component
public class AiAuthHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //  Token -> Redis -> UserInfo -> UserId -> Session
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();

            // 2. 获取 Token
            String token = httpRequest.getParameter("token");

            if (StrUtil.isBlank(token)) {
                log.warn("WebSocket 握手拒绝: Token 为空");
                // 拒绝连接
                return false;
            }

            try {
                // 3. 身份验证
                // 3.1 校验 Token 是否过期，拿到 Account
                String account = stringRedisTemplate.opsForValue().get(RedisCacheConstant.USER_LOGIN_KEY_TOKEN_TO_USER + token);

                if (StrUtil.isBlank(account)) {
                    log.warn("WebSocket 握手拒绝: Token 已过期或不存在");
                    return false;
                }

                // 3.2 通过 Account + Token 拿到用户信息 JSON
                Object userJsonObj = stringRedisTemplate.opsForHash().get(RedisCacheConstant.USER_LOGIN_KEY_USER_TO_TOKEN + account, token);

                String userJson = (userJsonObj != null) ? userJsonObj.toString() : null;

                if (StrUtil.isBlank(userJson)) {
                    log.warn("WebSocket 握手拒绝: 用户信息异常");
                    return false;
                }

                // 4. 获取真实的 UserId
                UserInfoDTO userInfoDTO = JSON.parseObject(userJson, UserInfoDTO.class);

                if (userInfoDTO != null && userInfoDTO.getId() != null) {
                    // 把校验过的可信 UserId 放入 WebSocket Session 属性中
                    String userId = String.valueOf(userInfoDTO.getId());
                    attributes.put("WEBSOCKET_USER_ID", userId);

                    log.info("用户 [{}] (ID: {}) WebSocket 连接认证成功", userInfoDTO.getUserName(), userId);
                    return true;
                }

            } catch (Exception e) {
                log.error("WebSocket 认证过程发生未捕获异常", e);
                return false;
            }
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 正在考虑要不要记录用户使用记录
    }
}