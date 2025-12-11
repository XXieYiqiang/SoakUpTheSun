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
 * @TableName volunteer_prizes_grab
 */
@TableName(value ="t_volunteer_prizes_grab")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VolunteerPrizesGrabDO {
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
     * 领取次数
     */
    private Integer receiveCount;

    /**
     * 有效期截至时间
     */
    private Date validEndTime;

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