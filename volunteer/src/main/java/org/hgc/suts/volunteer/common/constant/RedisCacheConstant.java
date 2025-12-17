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
    public static final String TASK_SEND_GUARANTEE_QUEUE = "suts:Volunteer_TASK_SEND_NUM_DELAY_QUEUE";

    /**
     * 奖品信息key
     */
    public static final String VOLUNTEER_PRIZES_KEY = "suts:volunteer:prizes:%s";

    /**
     * 奖品信息key
     */
    public static final String VOLUNTEER_PRIZES_VOLUNTEER_GRAB_KEY = "suts:volunteer:prizes:%s";

    /**
     * 奖品信息获取lock
     */
    public static final String VOLUNTEER_PRIZES_LOCK_KEY = "suts:volunteer:prizes:lock:%s";
    /**
     * 发放奖品给志愿者领取
     */
    public static final String VOLUNTEER_PRIZES_SEND_KEY = "suts:volunteer:prizes:send:%s";

    /**
     * 发放奖品信息状态
     */
    public static final String VOLUNTEER_PRIZES_SEND_STATUS_KEY = "suts:volunteer:prizes:status";

    /**
     * 发放奖品信息状态锁
     */
    public static final String VOLUNTEER_PRIZES_SEND_STATUS_LOCK_KEY = "suts:volunteer:prizes:status";

    /**
     * 用户已领取奖品 Key
     */
    public static final String VOLUNTEER_PRIZES_LIST = "suts:volunteer-prizes-list:%s";

    /**
     * 活跃志愿者匹配 GEO 池
     */
    public static final String VOLUNTEER_MATCH_ACTIVE_GEO_KEY = "suts:volunteer:match:active:geo";

    /**
     * 活跃志愿者画像信息key
     */
    public static final String VOLUNTEER_MATCH_ACTIVE_INFO_PREFIX_KEY = "suts:volunteer:match:active:info:";

    /**
     * 志愿者匹配冷却/频控前缀 key
     */
    public static final String VOLUNTEER_MATCH_COOLDOWN_PREFIX_KEY = "suts:volunteer:match:cooldown:";


}
