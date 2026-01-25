package org.hgc.suts.volunteer.service.impl.delayQueue;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.volunteer.dao.entity.VolunteerTaskDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerTaskMapper;
import org.hgc.suts.volunteer.service.VolunteerTaskService;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hgc.suts.volunteer.common.constant.RedisCacheConstant.TASK_SEND_GUARANTEE_QUEUE;

/**
 * 对异步进行的操作进行保底机制，假如存在未消费的，则再次消费
 */
@Service
@RequiredArgsConstructor
@Slf4j
class VolunteerTaskDelayQueueRunner implements CommandLineRunner {

    private final VolunteerTaskMapper volunteerTaskMapper;
    private final RedissonClient redissonClient;
    private final VolunteerTaskService volunteerTaskService;
    private final Executor taskExecutor;
    // 控制运行状态
    private volatile boolean running = true;
    @Override
    public void run(String... args) {
        taskExecutor.execute(this::processDelayQueue);
    }
    private void processDelayQueue() {
        RBlockingDeque<JSONObject> blockingDeque = redissonClient.getBlockingDeque(TASK_SEND_GUARANTEE_QUEUE);
        log.info("延迟队列消费者已启动...");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 阻塞获取，设置一个超时时间，防止永久死锁无法响应中断
                JSONObject delayJsonObject = blockingDeque.poll(5, TimeUnit.SECONDS);

                if (delayJsonObject != null) {
                    processTask(delayJsonObject);
                }
            } catch (InterruptedException e) {
                log.warn("消费者线程被中断，准备退出");
                // 恢复中断状态
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费任务处理异常", e);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ignored) {
                    // 休眠期间如果被中断，恢复标记并退出
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("延迟队列消费者已停止");
    }

    // 处理任务
    private void processTask(JSONObject json) {
        Long taskId = json.getLong("volunteerTaskId");
        VolunteerTaskDO taskDO = volunteerTaskMapper.selectById(taskId);
        if (taskDO != null && taskDO.getSendNum() == null) {
            volunteerTaskService.refreshVolunteerTaskSendNum(json);
        }
    }

    @PreDestroy
    public void stop() {
        this.running = false;
    }
}