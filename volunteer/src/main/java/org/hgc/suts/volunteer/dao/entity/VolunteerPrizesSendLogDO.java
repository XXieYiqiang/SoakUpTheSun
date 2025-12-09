package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 
 * @TableName volunteer_prizes_send_log
 */
@TableName(value ="volunteer_prizes_send_log")
@Data
public class VolunteerPrizesSendLogDO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 志愿者id
     */
    private Long volunteerId;

    /**
     * 奖品id
     */
    private Long prizesId;

    /**
     * 奖品领取码
     */
    private String cdk;

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

    /**
     * 逻辑删除 0/1 存在/删除
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}