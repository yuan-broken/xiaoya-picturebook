package com.picturebook.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建讲读任务请求
 */
@Data
public class ReadingTaskCreateRequest {

    @NotNull(message = "绘本ID不能为空")
    private Long bookId;

    /** 孩子档案ID（可选） */
    private Long childId;

    /** 讲读风格：gentle/lively/calm，默认 gentle */
    private String readingStyle;

    /** 音色配置（可选） */
    private String voiceConfig;

    /** 互动开关，默认 Y */
    private String interactionEnabled;

    /** 字幕开关，默认 Y */
    private String subtitleEnabled;
}
