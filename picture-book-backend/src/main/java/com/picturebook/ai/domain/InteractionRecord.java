package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 互动记录表
 */
@Data
@TableName("biz_interaction_record")
public class InteractionRecord {

    @TableId(type = IdType.AUTO)
    private Long recordId;

    private Long taskId;
    private Long bookId;
    private Integer pageNum;
    private Long pointId;
    private String question;
    /** 回应类型（click/voice/choice） */
    private String responseType;
    private String childResponse;
    /** 反馈类型（encourage/guide） */
    private String feedbackType;
    private String feedbackText;
    private Integer responseTime;
    private Date createTime;
}
