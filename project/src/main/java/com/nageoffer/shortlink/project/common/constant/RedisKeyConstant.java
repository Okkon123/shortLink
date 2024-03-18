package com.nageoffer.shortlink.project.common.constant;

/**
 * Redis Key 常量类
 */
public class RedisKeyConstant {
    /**
     * 短链接跳转前缀 KEY
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link_goto_";

    /**
     * 短链接跳转锁 KEY
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_";

    /**
     * 短链接空值跳转 KEY
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link_is_null_goto_%s";
}