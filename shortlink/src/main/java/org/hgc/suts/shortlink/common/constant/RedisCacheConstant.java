
package org.hgc.suts.shortlink.common.constant;

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
     * 存储短链接码池 key
     */
    public static final String SHORT_LINK_CODE_POOL_KEY = "suts:shortlink:code:pool";

    /**
     * 短链接key
     */
    public static final String SHORT_LINK_GOTO = "suts:short-link:goto:%s";

    /**
     * 短链接空值key
     */
    public static final String SHORT_LINK_GOTO_IS_NULL_KEY = "suts:short-link:goto-is-null:%s";

    /**
     * 短链接跳转锁
     */
    public static final String SHORT_LINK_GOTO_LOCK_KEY =  "suts:short-link:goto-lock:%s";

    /**
     * 短链补充锁 key
     */
    public static final String SHORT_LINK_REFILL_LOCK_KEY = "suts:shortlink:refill:lock";

}
