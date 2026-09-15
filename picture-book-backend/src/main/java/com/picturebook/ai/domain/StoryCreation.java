package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
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

    @TableId(type = IdType.AUTO)
    private Long creationId;

    private Long familyId;
    private Long roleId;
    private String roleName;
    private Long themeId;
    private String storyInput;
    private String storyContent;
    private Integer storyPages;
    /** 生成状态（pending/generating/success/failed） */
    private String generationState;
    /** 审核状态（0=待审核 1=通过 2=拒绝） */
    private String auditStatus;
    private String generationModel;
    private Long durationMs;
    private String errorMsg;
}
