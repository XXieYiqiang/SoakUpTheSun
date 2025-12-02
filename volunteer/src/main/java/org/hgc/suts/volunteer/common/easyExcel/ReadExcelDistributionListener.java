package org.hgc.suts.volunteer.common.easyExcel;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.executor.BatchExecutorException;
import org.hgc.suts.volunteer.common.constant.DistributionRedisConstant;
import org.hgc.suts.volunteer.dao.entity.VolunteerTaskDO;
import org.hgc.suts.volunteer.dao.entity.VolunteerTaskFailDO;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerTaskFailMapper;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.hgc.suts.volunteer.mq.event.VolunteerTaskExecuteEvent;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ReadExcelDistributionListener extends AnalysisEventListener<VolunteerExcelObject> {

    private final VolunteerTaskDO volunteerTaskDO;


    private final StringRedisTemplate stringRedisTemplate;

    private final VolunteerUserMapper volunteerUserMapper;

    private final VolunteerTaskFailMapper volunteerTaskFailMapper;

    private final VolunteerTaskExecuteEvent event;

    private final List<VolunteerUserDO> volunteerUserDOList=new ArrayList<>();

    private int rowCount = 1;


    @Override
    public void invoke(VolunteerExcelObject data, AnalysisContext analysisContext) {

        Long volunteerTaskId = volunteerTaskDO.getId();

        // 获取当前进度，判断是否已经执行过。如果已执行，则跳过即可，防止执行到一半应用宕机
        String TaskExecuteProgressKey = String.format(DistributionRedisConstant.VOLUNTEER_TASK_EXECUTE_PROGRESS_KEY, volunteerTaskId);
        String progress = stringRedisTemplate.opsForValue().get(TaskExecuteProgressKey);
        if (StrUtil.isNotBlank(progress) && Integer.parseInt(progress) >= rowCount) {
            ++rowCount;
            return;
        }
        VolunteerUserDO userDO = BeanUtil.toBean(data, VolunteerUserDO.class);
        volunteerUserDOList.add(userDO);
        // 记录当前执行到第几个了


        // 假如列表中存储了500个，就新增到数据库中
        if (rowCount%500==0){
            batchSaveVolunteer();
        }
        stringRedisTemplate.opsForValue().set(TaskExecuteProgressKey, String.valueOf(rowCount));
        rowCount++;
    }

    // 批量新增用户
    private void batchSaveVolunteer() {
        try {
            volunteerUserMapper.insert(volunteerUserDOList,volunteerUserDOList.size());
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BatchExecutorException) {
                // 添加到 t_volunteer_task_fail 并标记错误原因，方便后续查看未成功发送的原因和记录
                List<VolunteerTaskFailDO> volunteerTaskFailDOList = new ArrayList<>();
                List<VolunteerUserDO> toRemove = new ArrayList<>();

                // 调用批量新增失败后，为了避免大量重复失败，我们通过新增单条记录方式执行
                volunteerUserDOList.forEach(each -> {
                    try {
                        volunteerUserMapper.insert(each);
                    } catch (Exception ignored) {
                        boolean hasReceived = volunteerUserMapper.selectById(each.getId())!=null;
                        if (hasReceived) {
                            // 添加到 t_volunteer_task_fail 并标记错误原因，方便后续查看未成功发送的原因和记录
                            Map<Object, Object> objectMap = MapUtil.builder()
                                    .put("phone", each.getPhone())
                                    .put("cause", "该手机号已经注册了")
                                    .build();
                            VolunteerTaskFailDO volunteerTaskFailDO = VolunteerTaskFailDO.builder()
                                    .batchId(event.getVolunteerTaskId())
                                    .jsonObject(JSON.toJSONString(objectMap))
                                    .build();
                            volunteerTaskFailDOList.add(volunteerTaskFailDO);
                            // 从 volunteerUserDOList 中删除已经存在的记录
                            toRemove.add(each);
                        }
                    }
                });

                // 批量新增 t_volunteer_task_fail 表
                volunteerTaskFailMapper.insert(volunteerTaskFailDOList, volunteerTaskFailDOList.size());

                // 删除已经重复的内容
                volunteerUserDOList.removeAll(toRemove);
            }

            throw ex;
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        batchSaveVolunteer();
    }
}

