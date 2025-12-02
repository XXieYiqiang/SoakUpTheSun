package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务失败记录表
 * @TableName volunteer_task_fail
 */
@TableName(value ="volunteer_task_fail")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VolunteerTaskFailDO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 批次ID
     */
    private Long batchId;

    /**
     * JSON字符串，存储失败原因、Excel行数等信息
     */
    private String jsonObject;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}