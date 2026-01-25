package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务失败记录表
 * @TableName volunteer_task_fail
 */
@TableName(value ="t_volunteer_task_fail")
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
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}