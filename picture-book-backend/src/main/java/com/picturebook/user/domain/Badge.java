package com.picturebook.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 勋章定义 biz_badge
 *
 * @author Phase2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_badge")
public class Badge extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("badge_code")
    private String badgeCode;

    @TableField("badge_name")
    private String badgeName;

    @TableField("badge_icon")
    private String badgeIcon;

    @TableField("badge_color")
    private String badgeColor;

    /** 类别: reading / expression / streak / creation */
    private String category;

    private String description;

    /** 颁发规则 JSON */
    @TableField("rule_json")
    private String ruleJson;

    private Integer sortOrder;

    /** 1=启用 0=禁用 */
    private Integer enabled;
}
