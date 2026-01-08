package org.hgc.suts.gateway.common.biz.user;

import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson.JSON;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.hgc.suts.gateway.common.constant.RedisCacheConstant;
import org.hgc.suts.gateway.common.exception.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从 header 或 parameter 获取 token（兼容前后端约定）
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        }
        if (StrUtil.isBlank(token)) {
            throw new ClientException("未登录，请先登录");
        }

        // 放入token
        UserContext.setToken(token);
        String account = stringRedisTemplate.opsForValue()
                .get(RedisCacheConstant.USER_LOGIN_KEY_TOKEN_TO_USER + token);

        if (StrUtil.isBlank(account)) {
            throw new ClientException("登录已过期，请重新登录");
        }

        // 3. 从 Hash 中取出该 token 对应的用户信息
        String userJson = (String) stringRedisTemplate.opsForHash()
                .get(RedisCacheConstant.USER_LOGIN_KEY_USER_TO_TOKEN + account, token);

        if (StrUtil.isBlank(userJson)) {
            // 理论上不应该发生，但防止脏数据
            throw new ClientException("登录状态异常，请重新登录");
        }

        // 4. 放入当前线程上下文
        UserInfoDTO userRespDTO = JSON.parseObject(userJson, UserInfoDTO.class);
        UserContext.setUser(userRespDTO);



        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清除，防止内存泄漏和用户串号！
        UserContext.remove();
    }
}