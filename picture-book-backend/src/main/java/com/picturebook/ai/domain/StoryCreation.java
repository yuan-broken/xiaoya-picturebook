package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI故事创作表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_story_creation")
public class StoryCreation extends BaseEntity {

    @TableId(type = IdType.AUTO, value = "id")
    private Long creationId;

    private Long familyId;
    private Long roleId;
    private String roleName;
    private Long themeId;

    @TableField("inspiration")
    private String storyInput;

    @TableField("content")
    private String storyContent;

    @TableField(exist = false)
    private Integer storyPages;

    /** 生成状态（pending/generating/success/failed） */
    @TableField("state")
    private String generationState;

    /** 审核状态（0=待审核 1=通过 2=拒绝） */
    private String auditStatus;

    @TableField("model_name")
    private String generationModel;

    private Long durationMs;
    private String errorMsg;
}
