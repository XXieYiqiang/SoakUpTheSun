package org.hgc.suts.user.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户登陆返回信息
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginRespDTO implements Serializable {

    /**
     * 唯一验证
     */
    private String token;

}