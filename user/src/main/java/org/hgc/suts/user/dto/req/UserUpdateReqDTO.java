package org.hgc.suts.user.dto.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserUpdateReqDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

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

}
