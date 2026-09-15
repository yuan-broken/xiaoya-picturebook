package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * AI讲读任务表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_reading_task")
public class ReadingTask extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long taskId;

    private String taskNo;
    private Long familyId;
    private Long childId;
    private Long bookId;
    private String bookTitle;
    private Integer currentPage;
    private Integer totalPages;
    /** 讲读状态（idle/loading/reading/interactive/paused/finished/error） */
    private String readingState;
    /** 讲读风格（gentle/lively/calm） */
    private String readingStyle;
    private String voiceConfig;
    private String interactionEnabled;
    private String subtitleEnabled;
    private Integer totalDuration;
    private String readingModel;
    private String errorMsg;
    private Date startTime;
    private Date endTime;
}
