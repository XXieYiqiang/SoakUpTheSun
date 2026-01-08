package org.hgc.suts.gateway.controller;

import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.common.exception.ClientException;
import org.hgc.suts.gateway.common.result.Result;
import org.hgc.suts.gateway.common.web.Results;
import org.hgc.suts.gateway.dto.resp.AiChatRespDTO;
import org.hgc.suts.gateway.service.AiChatService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "AI 对话模块")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/api/gateway/chat")
    @Operation(summary = "AI 智能对话")
    public Result<AiChatRespDTO> chat(@RequestBody String userDescription) {
        // 1. 参数校验
        if (StrUtil.isBlank(userDescription)) {
            throw  new ClientException("输入不能为空");
        }

        log.info("收到 AI 请求: {}", userDescription);

        // 2. 调用业务
        AiChatRespDTO respDTO = aiChatService.executeChat(userDescription);

        // 3. 返回成功
        return Results.success(respDTO);
    }
}