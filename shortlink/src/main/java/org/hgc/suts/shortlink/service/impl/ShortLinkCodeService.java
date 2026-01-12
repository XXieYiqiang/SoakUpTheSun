package org.hgc.suts.shortlink.service.impl;

import groovy.lang.Lazy;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.hgc.suts.shortlink.common.constant.RedisCacheConstant;
import org.hgc.suts.shortlink.utils.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkCodeService {
    // 注入自己，防止代码运行在主线程
    private final ApplicationContext applicationContext;
    private final StringRedisTemplate stringRedisTemplate;
    private final RandomUtils randomUtils;
    // 默认池子最大大小
    @Value("${short-link.code-pool.max-size}")
    private int maxPoolSize;
    // 补充阈值
    @Value("${short-link.code-pool.refill-threshold}")
    private int refillThreshold;

    // 标记当前是否执行补充code
    private final AtomicBoolean isRefilling = new AtomicBoolean(false);

    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new BasicThreadFactory.Builder().namingPattern("short-link-refill-").daemon(true).build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 获取一个code
     */
    public String takeCode() {
        // 1. 拿走一个code
        String code = stringRedisTemplate.opsForSet().pop(RedisCacheConstant.SHORT_LINK_CODE_POOL_KEY);

        // 2. 是否需要补充code
        checkAndRefillAsync();

        // 3. 假如没有获取到code，直接生成一个返回
        if (code == null) {
            log.warn("code池耗尽，降级为同步生成");
            return randomUtils.generateShortCode(6);
        }
        return code;
    }

    /**
     * 检查并触发异步补充code
     */
    private void checkAndRefillAsync() {
        // 判断锁，执行时跳过
        if (!isRefilling.compareAndSet(false, true)) {
            return;
        }

        // 未拿到锁直接返回，避免多实例重复补充

        try {
            // 分布式锁 key
            String lockKey = RedisCacheConstant.SHORT_LINK_REFILL_LOCK_KEY;
            String lockValue = UUID.randomUUID().toString();

            // 尝试获取锁（SETNX + TTL）
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(30));
            if (Boolean.FALSE.equals(locked)) {
                // 未获取锁，重置本地标记
                isRefilling.set(false);
                return;
            }
            // 获取成功，检查数量
            Long currentSize = stringRedisTemplate.opsForSet().size(RedisCacheConstant.SHORT_LINK_CODE_POOL_KEY);
            if (currentSize != null && currentSize < refillThreshold) {
                // 容器中动态获取当前的代理对象触发异步，同时传输锁
                executorService.execute(() -> {
                    refillPoolTask(currentSize.intValue(), lockKey, lockValue);
                });
            }else {
                // 不需要补池，主线程立刻释放锁
                unlockRefill(lockKey, lockValue);
                isRefilling.set(false);
            }
        } catch (Exception e) {
            // 同步阶段异常兜底
            isRefilling.set(false);
        }
    }

    /**
     * 异步补充code
     */
    @Async
    public void refillPoolTask(int currentSize, String lockKey, String lockValue) {
        try {
            log.info("触发code补充，当前剩余: {}", currentSize);
            int needCount = maxPoolSize - currentSize;

            if (needCount <= 0) return;

            List<String> codes = new ArrayList<>(needCount);
            for (int i = 0; i < needCount; i++) {
                String code = randomUtils.generateShortCode(6);
                codes.add(code);
            }
            stringRedisTemplate.opsForSet().add(
                    RedisCacheConstant.SHORT_LINK_CODE_POOL_KEY,
                    codes.toArray(new String[0])
            );

            log.info("补充code完成，补充了 {} 个code", needCount);

        } catch (Exception e) {
            log.error("补充code异常", e);
        } finally {
            // 释放分布式锁
            unlockRefill(lockKey, lockValue);
            // 重置本地标记
            isRefilling.set(false);
        }
    }

    /**
     * Lua 脚本释放锁
     * 避免代码重复
     */
    private void unlockRefill(String key, String value) {
        String releaseScript =
                "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('DEL', KEYS[1]) " +
                        "else return 0 end";
        try {
            DefaultRedisScript<Long> releaseLockScript = new DefaultRedisScript<>();
            releaseLockScript.setScriptText(releaseScript);
            releaseLockScript.setResultType(Long.class);
            stringRedisTemplate.execute(
                    releaseLockScript,
                    Collections.singletonList(key),
                    value
            );
        } catch (Exception e) {
            log.warn("释放锁异常", e);
        }
    }
}