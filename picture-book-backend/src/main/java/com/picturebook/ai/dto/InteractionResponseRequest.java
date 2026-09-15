package com.picturebook.ai.dto;

import lombok.Data;

/**
 * 互动回应请求
 */
@Data
public class InteractionResponseRequest {

    /** 任务ID */
    private Long taskId;

    /** 互动点ID */
    private Long pointId;

    /** 页码 */
    private Integer pageNum;

    /** 回应类型：click/voice/choice */
    private String responseType;

    /** 孩子回应内容 */
    private String childResponse;
}
