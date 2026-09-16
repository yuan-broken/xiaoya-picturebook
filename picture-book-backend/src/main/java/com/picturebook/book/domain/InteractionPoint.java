package com.picturebook.book.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 互动点表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_interaction_point")
public class InteractionPoint extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long pointId;

    private Long pageId;
    private Long bookId;
    /** 互动类型（open/choice/judge/imagine） */
    @TableField("interaction_type")
    @JsonProperty("interactionType")
    private String pointType;
    private String question;
    @TableField("options")
    @JsonProperty("options")
    private String optionsJson;
    @TableField("feedback")
    @JsonProperty("feedback")
    private String feedbackText;
    @TableField("encourage")
    @JsonProperty("encourage")
    private String encourageText;
    @TableField("guide")
    @JsonProperty("guide")
    private String guideText;
    private Integer sortOrder;
}
