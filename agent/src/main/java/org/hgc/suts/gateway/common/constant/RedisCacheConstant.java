
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
    public static final String AGENT_CHAT_HISTORY_KEY = "suts:agent:chat:history:%s";

    /**
     * 分析图片视觉上下文key
     */
    public static final String AGENT_VISION_CONTEXT_KEY = "suts:agent:vision:context:%s";

    /**
     * 向量索引的名称
      */
    public static final String VECTOR_INDEX_NAME = "suts:vector:index:memory";

    /**
     * 向量数据的 Key 前缀 如 suts:vector:data:memory:10086:uuid
     */
    public static final String VECTOR_DATA_KEY_PREFIX = "suts:vector:data:memory:";
}
