package org.hgc.suts.gateway.controller;

import cn.hutool.json.JSONUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.threadpool.TtlExecutors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.gateway.common.biz.user.UserContext;
import org.hgc.suts.gateway.common.biz.user.UserInfoDTO;
import org.hgc.suts.gateway.common.exception.ClientException;
import org.hgc.suts.gateway.common.result.Result;
import org.hgc.suts.gateway.common.web.Results;
import org.hgc.suts.gateway.dto.resp.AiChatRespDTO;
import org.hgc.suts.gateway.service.AiChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.*;

@Slf4j
@RestController
@Tag(name = "AI 对话模块")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    private final Executor aiTaskExecutor;

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


    @PostMapping("/api/gateway/chat/async")
    @Operation(summary = "AI 智能对话 (异步高并发版)")
    public DeferredResult<Result<AiChatRespDTO>> chatAsync(@RequestBody String userDescription) {

        // 设置 60秒 超时
        DeferredResult<Result<AiChatRespDTO>> deferredResult = new DeferredResult<>(60 * 1000L);

        if (StrUtil.isBlank(userDescription)) {
            // 构造错误
            deferredResult.setErrorResult(new Result<AiChatRespDTO>().setCode("400").setMessage("输入不能为空"));
            return deferredResult;
        }

        // 超时回调
        String finalDesc = userDescription;
        deferredResult.onTimeout(() -> {
            log.warn("AI 异步响应超时: {}", finalDesc);
            deferredResult.setErrorResult(
                    new Result<AiChatRespDTO>().setCode("504").setMessage("AI 思考太久了，请稍后再试")
            );
        });

        try {
            // 使用线程池执行任务
            aiTaskExecutor.execute(() -> {
                try {
                    log.info("异步任务执行中...");
                    AiChatRespDTO respDTO = aiChatService.executeChat(finalDesc);
                    // 成功回调
                    deferredResult.setResult(Results.success(respDTO));

                } catch (Exception e) {
                    log.error("异步任务异常", e);
                    deferredResult.setErrorResult(new Result<AiChatRespDTO>().setCode("500").setMessage("系统繁忙"));
                }
            });
        } catch (RejectedExecutionException e) {
            // 捕获线程池满的异常
            log.warn("并发过高，任务拒绝");
            deferredResult.setErrorResult(new Result<AiChatRespDTO>().setCode("503").setMessage("服务火爆，请稍后"));
        }

        return deferredResult;
    }



}