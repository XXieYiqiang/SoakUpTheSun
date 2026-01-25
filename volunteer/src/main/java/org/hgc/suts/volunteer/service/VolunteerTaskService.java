package org.hgc.suts.volunteer.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.volunteer.dao.entity.VolunteerTaskDO;
import org.hgc.suts.volunteer.dto.req.VolunteerCreateTaskReqDTO;

/**
* @author 谢毅强
* @description 针对表【volunteer_task(志愿者任务表)】的数据库操作Service
* @createDate 2025-12-01 17:51:35
*/
public interface VolunteerTaskService extends IService<VolunteerTaskDO> {
    /**
     * 创建志愿者
     * @param requestParam 请求创建任务参数
     */
    void createVolunteerTask(VolunteerCreateTaskReqDTO requestParam);


    /**
     * 查询当前处理的excel的行数
     * @param delayJsonObject excel信息
     */
    void refreshVolunteerTaskSendNum(JSONObject delayJsonObject);
}
