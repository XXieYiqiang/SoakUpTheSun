package org.hgc.suts.user.dto.req;

import lombok.Data;

/**
 * 用户登陆请求参数
 */
@Data
public class UserLoginReqDTO {

    /**
     * 用户名
     */
    private String userAccount;

    /**
     * 密码
     */
    private String password;
}
