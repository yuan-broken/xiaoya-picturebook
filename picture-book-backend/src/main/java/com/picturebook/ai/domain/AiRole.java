package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI角色表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_ai_role")
public class AiRole extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long roleId;

    private String roleName;
    private String roleEmoji;
    @TableField(exist = false)
    private String roleImage;
    private String roleIntro;
    @TableField("role_personality")
    private String personality;
    @TableField("prompt")
    private String aiPrompt;
    private String voiceConfig;
    private String bgColor;
    private Integer sortOrder;
    /** 启用状态（1=启用 0=禁用） */
    @TableField("enabled")
    private Integer status;
}
