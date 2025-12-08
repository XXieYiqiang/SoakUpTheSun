package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;
}