package org.hgc.suts.user.dto.resp;

import lombok.Data;

/**
 * 用户返回参数响应
 */
@Data
public class UserRespDTO {

    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 角色
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private String editTime;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;
}
