package org.hgc.suts.picture.common.biz.user;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserContext {

    private static final ThreadLocal<UserInfoDTO> USER_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前登陆用户
     */
    public static void setUser(UserInfoDTO user) {
        USER_HOLDER.set(user);
    }

    /**
     * 获取当前登陆用户
     */
    public static UserInfoDTO getUser() {
        return USER_HOLDER.get();
    }

    /**
     * 获取用户id
     */
    public static Long getUserId() {
        UserInfoDTO user = USER_HOLDER.get();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取用户账号
     */
    public static String getUserAccount() {
        UserInfoDTO user = USER_HOLDER.get();
        return user != null ? user.getUserAccount() : null;
    }

    /**
     * 获取用户名
     */
    public static String getUserName() {
        UserInfoDTO user = USER_HOLDER.get();
        return user != null ? user.getUserName() : null;
    }

    /**
     * 获取用户性别
     */
    public static Integer getUserSex() {
        UserInfoDTO user = USER_HOLDER.get();
        return user != null ? user.getSex() : null;
    }

    /**
     * 获取用户性别
     */
    public static LocalDateTime getBirthday() {
        UserInfoDTO user = USER_HOLDER.get();
        return user != null ? user.getBirthday() : null;
    }

    /**
     * 获取用户地址
     */
    public static String getLocation() {
        UserInfoDTO user = USER_HOLDER.get();
        return user != null ? user.getLocation() : null;
    }

    /**
     * 清除账号
     */
    public static void remove() {
        USER_HOLDER.remove();
    }
}