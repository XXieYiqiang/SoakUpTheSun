package org.hgc.suts.shortlink.dao.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 求助短链路由表
 * @TableName short_link_help
 */
@TableName(value ="t_short_link_help")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShortLinkHelpDO {
    /**
     * 唯一ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 短链接码
     */
    private String shortCode;

    /**
     * 完整短链接
     */
    private String fullShortLink;

    /**
     * 房间链接
     */
    private String targetRoomLink;

    /**
     * 透传给IM服务的鉴权票据/Token
     */
    private String authTicket;

    /**
     * 用户需输入的邀请码
     */
    private String inviteCode;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0:等待中 1:进行中 2:已结束
     */
    private Integer status;

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