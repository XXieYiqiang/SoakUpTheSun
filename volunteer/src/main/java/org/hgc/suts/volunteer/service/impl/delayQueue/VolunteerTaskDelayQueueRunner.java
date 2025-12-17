package org.hgc.suts.volunteer.service.impl.delayQueue;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.dao.entity.VolunteerTaskDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerTaskMapper;
import org.hgc.suts.volunteer.service.VolunteerTaskService;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;

import static org.hgc.suts.volunteer.common.constant.RedisCacheConstant.TASK_SEND_GUARANTEE_QUEUE;

/**
 * 对异步进行的操作进行保底机制，假如存在未消费的，则再次消费
 */
@Service
@RequiredArgsConstructor
class VolunteerTaskDelayQueueRunner implements CommandLineRunner {

    private final VolunteerTaskMapper volunteerTaskMapper;
    private final RedissonClient redissonClient;
    private final VolunteerTaskService volunteerTaskService;

    @Override
    public void run(String... args) throws Exception {
        Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable);
                            thread.setName("delay_volunteer-task_send-num_consumer");
                            thread.setDaemon(Boolean.TRUE);
                            return thread;
                        })
                .execute(() -> {
                    RBlockingDeque<JSONObject> blockingDeque = redissonClient.getBlockingDeque(TASK_SEND_GUARANTEE_QUEUE);
                    for (; ; ) {
                        try {
                            // 获取延迟队列已到达时间元素
                            JSONObject delayJsonObject = blockingDeque.take();
                            if (delayJsonObject != null) {
                                // 获取志愿者推送记录，查看发送条数是否已经有值，有的话代表上面线程池已经处理完成，无需再处理
                                VolunteerTaskDO volunteerTaskDO = volunteerTaskMapper.selectById(delayJsonObject.getLong("volunteerTaskId"));
                                if (volunteerTaskDO.getSendNum() == null) {
                                    volunteerTaskService.refreshVolunteerTaskSendNum(delayJsonObject);
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                });
    }
}