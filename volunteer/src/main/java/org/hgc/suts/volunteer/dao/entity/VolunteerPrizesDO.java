package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 志愿者奖品表
 * @TableName volunteer_prizes
 */
@TableName(value ="volunteer_prizes")
@Data
public class VolunteerPrizesDO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 奖品名称
     */
    private String name;

    /**
     * 有效期开始时间
     */
    private Date validStartTime;

    /**
     * 有效期结束时间
     */
    private Date validEndTime;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 状态 0：未发放 1:执行中  2：已结束
     */
    private Integer status;

    /**
     * 比例，前？%
     */
    private Integer proportion;

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