package com.nageoffer.shortlink.project.common.constant;

/**
 * Redis Key 常量类
 */
public class RedisKeyConstant {
    /**
     * 短链接跳转前缀 KEY
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link_goto_%s";

    /**
     * 短链接跳转锁 KEY
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_%s";

    /**
     * 短链接空值跳转 KEY
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link_is_null_goto_%s";

    /**
     * 用户是否首次登录标识
     */
    public static final String UV_FIRST_KEY = "short-link_stats:uv:%s";

    /**
     * 用户IP首次登录标识
     */
    public static final String UIP_FIRST_KEY = "short-link_stats:uip:$s";

    /**
     * 短链接修改分组 ID 锁前缀 Key
     */
    public static final String LOCK_GID_UPDATE_KEY = "short-link:lock:update-gid:%s";
    /**
     * 短链接监控消息保存队列 Group 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_GROUP_KEY = "short_link:stats-stream:only-group";
    /**
     * 短链接监控消息保存队列 Topic 缓存标识
     */
    public static final String SHORT_LINK_STATS_STREAM_TOPIC_KEY = "short_link:stats-stream";
    /**
     * 短链接延迟队列消费统计 Key
     */
    public static final String DELAY_QUEUE_STATS_KEY = "short-link_delay-queue:stats";
}
