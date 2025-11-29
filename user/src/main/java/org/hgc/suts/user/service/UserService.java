package org.hgc.suts.user.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.user.dao.entity.UserDO;
import org.hgc.suts.user.dto.req.UserLoginReqDTO;
import org.hgc.suts.user.dto.req.UserRegisterReqDTO;
import org.hgc.suts.user.dto.req.UserUpdateReqDTO;
import org.hgc.suts.user.dto.resp.UserLoginRespDTO;
import org.hgc.suts.user.dto.resp.UserRespDTO;


/**
* @author 谢毅强
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-11-27 22:52:37
*/
public interface UserService extends IService<UserDO> {


    /**
     * 注册账号
     * @param requestParam 注册参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 返回是否注册该账号
     * @param username 用户名
     * @return 存在 true 不存在 false
     */
    Boolean hasUserAccount(String username);


    /**
     * 用户登录
     *
     * @param requestParam 用户登录请求参数
     * @return 用户登录返回参数 Token
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);


    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername(String username);


    /**
     * 根据用户名修改用户
     *
     * @param requestParam 修改用户请求参数
     */
    void updateUser(UserUpdateReqDTO requestParam);

    /**
     * 对密码进行加盐加密
     * @param userPassword 用户密码
     * @return 返回加盐密码
     */
    String getEncryptPassword(String userPassword);
}
