package org.hgc.suts.shortlink.dao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务接单记录
 * @TableName t_short_link_help_log
 */
@TableName(value ="t_short_link_help_log")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShortLinkHelpLogDO {
    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 外键，ShortLinkHelpDO
     */
    private Long requestId;

    /**
     * 志愿者ID
     */
    private Long volunteerId;

    /**
     * 进入房间时间
     */
    private Date joinTime;

    /**
     * 离开时间
     */
    private Date leaveTime;

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