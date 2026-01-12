package org.hgc.suts.shortlink.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.shortlink.common.constant.RedisCacheConstant;
import org.hgc.suts.shortlink.utils.RandomUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkCodeService {


    private final StringRedisTemplate stringRedisTemplate;
    private final RandomUtils randomUtils;

    // 最多存储的code量
    private final int MAX_POOL_SIZE = 1000;
    // 补code阈值
    private final int REFILL_THRESHOLD = 200;

    // 标记当前是否执行补充code
    private final AtomicBoolean isRefilling = new AtomicBoolean(false);

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
        if (isRefilling.get()) {
            return;
        }
        // 分布式锁 key
        String lockKey = RedisCacheConstant.SHORT_LINK_REFILL_LOCK_KEY;
        String lockValue = UUID.randomUUID().toString();

        // 尝试获取锁（SETNX + TTL）
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofSeconds(30));
        // 未拿到锁直接返回，避免多实例重复补充
        if (Boolean.FALSE.equals(locked)) {
            return;
        }
        try {
            // 检查数量
            Long currentSize = stringRedisTemplate.opsForSet().size(RedisCacheConstant.SHORT_LINK_CODE_POOL_KEY);
            if (currentSize != null && currentSize < REFILL_THRESHOLD) {
                // 直接触发补充（锁已保证互斥）
                refillPoolAsync(currentSize.intValue());
            }
        } finally {
            // 释放锁，仅当 value 匹配时删除
            String releaseScript =
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('DEL', KEYS[1]) " +
                            "else return 0 end";
            DefaultRedisScript<Long> releaseLockScript = new DefaultRedisScript<>();
            releaseLockScript.setScriptText(releaseScript);
            releaseLockScript.setResultType(Long.class);
            stringRedisTemplate.execute(
                    releaseLockScript,
                    Collections.singletonList(lockKey),
                    lockValue
            );
        }
    }

    /**
     * 异步补充code
     */
    @Async
    public void refillPoolAsync(int currentSize) {
        try {
            log.info("触发code补充，当前剩余: {}", currentSize);
            int needCount = MAX_POOL_SIZE - currentSize;

            java.util.List<String> codes = new java.util.ArrayList<>(needCount);
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
            // 释放锁
            isRefilling.set(false);
        }
    }


}