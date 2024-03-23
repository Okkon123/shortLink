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
}
