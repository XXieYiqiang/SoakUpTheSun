package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 
 * @TableName volunteer_rating
 */
@TableName(value ="volunteer_rating")
@Data
public class VolunteerRatingDO {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 志愿者id
     */
    private Long volunteerId;

    /**
     * 附加分(0无 1有)
     */
    private Integer rating;

    /**
     * 是否已计算入总分 0-否 1-是
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer isCalculated;

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