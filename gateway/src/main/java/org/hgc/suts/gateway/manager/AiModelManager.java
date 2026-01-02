package org.hgc.suts.gateway.manager;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.config.AiConfig;
import org.springframework.stereotype.Component;

/**
 * 通用 AI 模型管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelManager {

    private final AiConfig aiConfig;

    /**
     * 发起同步对话
     *
     * @param systemPrompt 系统人设
     * @param userContent  用户提问
     * @return AI 的回复内容
     */
    public String chat(String systemPrompt, String userContent) {
        // 1. 拼接接口地址
        String url = StrUtil.removeSuffix(aiConfig.getApiHost(), "/") + "/chat/completions";

        // 2. 构建消息上下文
        JSONArray messages = new JSONArray();

        // 2.1 添加系统人设 System
        if (StrUtil.isNotBlank(systemPrompt)) {
            messages.add(new JSONObject()
                    .set("role", "system")
                    .set("content", systemPrompt));
        }

        // 2.2 添加用户问题User
        messages.add(new JSONObject()
                .set("role", "user")
                .set("content", userContent));

        // 3. 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", aiConfig.getModelName());
        requestBody.set("messages", messages);
        // 暂时关闭流式
        requestBody.set("stream", false);
        requestBody.set("temperature", aiConfig.getTemperature());

        try {
            log.info("请求模型 Model: {}, User: {}", aiConfig.getModelName(), userContent);

            // 4. 发送 HTTP POST 请
            try (HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + aiConfig.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(aiConfig.getTimeout())
                    .execute()) {

                String responseBody = response.body();

                // 5. 错误处理
                if (!response.isOk()) {
                    log.error("LLM Error Status: {}, Body: {}", response.getStatus(), responseBody);
                    throw new RuntimeException("LLM 服务响应异常: " + response.getStatus());
                }

                // 6. 解析标准响应格式
                JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

                // 检查业务错误
                if (jsonResponse.containsKey("error")) {
                    String errorMsg = jsonResponse.getJSONObject("error").getStr("message");
                    throw new RuntimeException("AI API 报错: " + errorMsg);
                }

                // 提取内容
                String content = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getStr("content");

                log.info("Length: {}", content.length());
                return content;
            }

        } catch (Exception e) {
            log.error("LLM 调用失败", e);
            throw new RuntimeException("LLM : " + e.getMessage());
        }
    }
}