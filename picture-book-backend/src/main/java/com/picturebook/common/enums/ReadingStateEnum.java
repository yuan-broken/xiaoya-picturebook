package com.picturebook.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 讲读状态枚举（讲读状态机）
 */
@Getter
@AllArgsConstructor
public enum ReadingStateEnum {

    /** 空闲 */
    IDLE("idle", "空闲"),

    /** 加载中 */
    LOADING("loading", "加载中"),

    /** 讲读中 */
    READING("reading", "讲读中"),

    /** 互动中 */
    INTERACTIVE("interactive", "互动中"),

    /** 已暂停 */
    PAUSED("paused", "已暂停"),

    /** 已完成 */
    FINISHED("finished", "已完成"),

    /** 加载失败 */
    ERROR("error", "加载失败");

    private final String code;
    private final String desc;

    public static ReadingStateEnum fromCode(String code) {
        for (ReadingStateEnum state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return IDLE;
    }
}
