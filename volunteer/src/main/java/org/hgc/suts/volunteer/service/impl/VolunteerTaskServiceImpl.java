package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.common.biz.user.UserContext;
import org.hgc.suts.volunteer.common.enums.VolunteerTaskSendTypeEnum;
import org.hgc.suts.volunteer.common.enums.VolunteerTaskStatusEnum;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.dao.entity.VolunteerTaskDO;
import org.hgc.suts.volunteer.dto.req.VolunteerCreateTaskReq;
import org.hgc.suts.volunteer.mq.event.VolunteerTaskExecuteEvent;
import org.hgc.suts.volunteer.mq.producer.VolunteerTaskActualExecuteProducer;
import org.hgc.suts.volunteer.service.VolunteerTaskService;
import org.hgc.suts.volunteer.dao.mapper.VolunteerTaskMapper;
import org.hgc.suts.volunteer.service.handler.excel.RowCountListener;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hgc.suts.volunteer.common.constant.RedisCacheConstant.TASK_SEND_GUARANTEE_QUEUE;

/**
* @author 谢毅强
* @description 针对表【volunteer_task(志愿者任务表)】的数据库操作Service实现
* @createDate 2025-12-01 17:51:35
*/
@Service
@RequiredArgsConstructor
public class VolunteerTaskServiceImpl extends ServiceImpl<VolunteerTaskMapper, VolunteerTaskDO>  implements VolunteerTaskService{

    private final VolunteerTaskMapper volunteerTaskMapper;
    private final RedissonClient redissonClient;


    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() << 1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.DiscardPolicy()
    );
    private final VolunteerTaskActualExecuteProducer volunteerTaskActualExecuteProducer;


    @Override
    public void createVolunteerTask(VolunteerCreateTaskReq requestParam) {
        if (requestParam.getFileAddress() == null) {
            throw new ClientException("Excel文件不能为空");
        }
        VolunteerTaskDO volunteerTaskDO = BeanUtil.copyProperties(requestParam, VolunteerTaskDO.class);

        // 标识批次和操作人,插入数据库中
        volunteerTaskDO.setBatchId(IdUtil.getSnowflakeNextId());
        volunteerTaskDO.setOperatorId(UserContext.getUserId());
        volunteerTaskDO.setStatus(
                Objects.equals(requestParam.getSendType(), VolunteerTaskSendTypeEnum.IMMEDIATE.getType())
                        ? VolunteerTaskStatusEnum.IN_PROGRESS.getStatus()
                        : VolunteerTaskStatusEnum.PENDING.getStatus()
        );
        volunteerTaskMapper.insert(volunteerTaskDO);


        JSONObject delayJsonObject = JSONObject.of("fileAddress", requestParam.getFileAddress(),"volunteerTaskId", volunteerTaskDO.getId());

        // 异步获取excel的行数
        executorService.execute(() -> refreshVolunteerTaskSendNum(delayJsonObject));

        //加入延迟队列中进行兜底
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque(TASK_SEND_GUARANTEE_QUEUE);
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        delayedQueue.offer(delayJsonObject, 1, TimeUnit.SECONDS);

        VolunteerTaskExecuteEvent volunteerTaskExecuteEvent = VolunteerTaskExecuteEvent.builder()
                .volunteerTaskId(volunteerTaskDO.getId())
                .build();

        volunteerTaskActualExecuteProducer.sendMessage(volunteerTaskExecuteEvent);
    }
    @Override
    public void refreshVolunteerTaskSendNum(JSONObject delayJsonObject) {
        // 通过 EasyExcel 监听器获取 Excel 中所有行数
        RowCountListener listener = new RowCountListener();
        try {
            EasyExcel.read(delayJsonObject.getString("fileAddress"), listener).sheet().doRead();
        } catch (Exception ex) {
            throw new ClientException("读取文件错误,请检查一下文件地址");
        }
        int totalRows = listener.getRowCount();

        // 刷新志愿者推送记录中发送行数
        VolunteerTaskDO volunteerTaskDO = VolunteerTaskDO.builder()
                .id(delayJsonObject.getLong("volunteerTaskId"))
                .sendNum(totalRows)
                .build();
        volunteerTaskMapper.updateById(volunteerTaskDO);
    }

}




