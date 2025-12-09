package org.hgc.suts.volunteer.common.constant;

/**
 * 志愿者新增服务 Redis 缓存常量类
 */
public final class DistributionRedisConstant {

    /**
     * 志愿者批量新增推送执行进度 Key
     */
    public static final String VOLUNTEER_TASK_EXECUTE_PROGRESS_KEY = "suts:template-task-execute-progress:%s";


    /**
     * 防止重复发送奖品 Key
     */
    public static final String VOLUNTEER_PRIZES_DISTRIBUTION_KEY = "suts:volunteer-prizes-distribution:%s";
}
