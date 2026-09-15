package com.picturebook.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 讲读会话上下文（缓存在Redis中）
 */
@Data
public class ReadingSession {

    /** 任务ID */
    private Long taskId;
    /** 任务编号 */
    private String taskNo;
    /** 家庭ID */
    private Long familyId;
    /** 孩子档案ID */
    private Long childId;
    /** 绘本ID */
    private Long bookId;
    /** 绘本标题 */
    private String bookTitle;
    /** 总页数 */
    private Integer totalPages;
    /** 当前页码（从1开始） */
    private Integer currentPage;
    /** 讲读状态 */
    private String state;
    /** 讲读风格 */
    private String readingStyle;
    /** 音色配置 */
    private String voiceConfig;
    /** 互动开关 */
    private Boolean interactionEnabled;
    /** 字幕开关 */
    private Boolean subtitleEnabled;
    /** 当前页互动点是否全部完成 */
    private Boolean currentPageInteractionsDone;
    /** 当前页已完成的互动点ID */
    private List<Long> completedPointIds;
    /** 是否正在播放音频 */
    private Boolean isPlaying;
    /** 累计讲读时长（秒） */
    private Integer totalDuration;
    /** 开始时间戳 */
    private Long startTimestamp;
}
