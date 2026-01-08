
package org.hgc.suts.gateway.common.constant;

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
    public static final String USER_LOGIN_KEY = "suts:login:";




    /**
     * 用户登陆key
     */
    public static final String USER_LOGIN_KEY_USER_TO_TOKEN = "suts:login:UserToToken:";
    public static final String USER_LOGIN_KEY_TOKEN_TO_USER = "suts:login:TokenToUser:";


    /**
     * chat用户上下文key
     */
    public static final String GATEWAY_CHAT_HISTORY_KEY = "suts:gateway:chat:history:%s";

    /**
     * 分析图片视觉上下文key
     */
    public static final String GATEWAY_VISION_CONTEXT_KEY = "suts:gateway:vision:context:%s";
}
