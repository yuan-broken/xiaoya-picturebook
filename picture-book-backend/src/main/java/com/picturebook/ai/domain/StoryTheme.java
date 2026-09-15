package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 故事主题表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_story_theme")
public class StoryTheme extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long themeId;

    private String themeName;
    private String themeDesc;
    private String themeColor;
    @TableField(exist = false)
    private String bgColor;
    @TableField("prompt")
    private String aiPrompt;
    private Integer sortOrder;
    /** 启用状态（1=启用 0=禁用） */
    @TableField("enabled")
    private Integer status;
}
