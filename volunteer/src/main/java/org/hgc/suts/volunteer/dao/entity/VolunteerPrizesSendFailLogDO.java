package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @TableName volunteer_prizes_send_fail_log
 */
@TableName(value ="volunteer_prizes_send_fail_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerPrizesSendFailLogDO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 奖品id
     */
    private Long prizesId;

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

    /**
     * 逻辑删除 0/1 存在/删除
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}