package org.hgc.suts.gateway.controller;

import com.alibaba.nacos.api.model.v2.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.service.AiChatService;
import org.springframework.web.bind.annotation.*;

/**
 * gateway控制层
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * AI 对话入口
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody String question) {
        // 校验
        if (question == null || question.trim().isEmpty()) {
            return Result.failure("问题不能为空");
        }
        try {
            // 调用 Service -> 触发责任链
            String answer = aiChatService.executeChat(question);
            return Result.success(answer);
        } catch (Exception e) {
            log.error("AI 处理异常", e);
            return Result.failure("系统繁忙，请稍后再试: " + e.getMessage());
        }
    }
}