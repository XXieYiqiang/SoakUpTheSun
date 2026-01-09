package org.hgc.suts.gateway.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ThreadPoolConfig {

    @Bean("aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程
        executor.setCorePoolSize(200);
        // 最大线程
        executor.setMaxPoolSize(200);
        // 队列容量
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("suts-gateway-thread-");

        // 2. 拒绝策略：直接抛出异常
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.initialize();

        // 3. 使用 Alibaba TTL 包装，确保 UserContext 上下文能透传
        return TtlExecutors.getTtlExecutor(executor.getThreadPoolExecutor());
    }
}