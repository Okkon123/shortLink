package com.nageoffer.shortlink.project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VailDateTypeEum {
    /**
     * 永久有效
     */
    PERMANENT(0),
    /**
     * 自定义有效
     */
    CUSTOM(1);
    @Getter
    private final int type;
}
