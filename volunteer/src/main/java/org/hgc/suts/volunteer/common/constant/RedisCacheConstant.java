package org.hgc.suts.volunteer.common.constant;

/**
 * Redis 缓存常量类
 */
public class RedisCacheConstant {

    /**
     * 用户注册分布式锁
     */
    public static final String LOCK_USER_REGISTER_KEY = "suts:lock_user-register:";

    /**
     * 用户登陆key
     */
    public static final String USER_LOGIN_KEY_USER_TO_TOKEN = "suts:login:UserToToken:";
    public static final String USER_LOGIN_KEY_TOKEN_TO_USER = "suts:login:TokenToUser:";

    /**
     * 志愿者新增任务时兜底机制的延迟队列key
     */
    public static final String TASK_SEND_GUARANTEE_QUEUE = "Volunteer_TASK_SEND_NUM_DELAY_QUEUE";

}
