package com.picturebook.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收藏夹 biz_favorite
 *
 * @author Phase2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_favorite")
public class Favorite extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long familyId;
    private Long childId;

    /** book / story */
    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("target_title")
    private String targetTitle;

    @TableField("target_cover")
    private String targetCover;

    @TableField("group_name")
    private String groupName;

    @TableField("sort_order")
    private Integer sortOrder;
}
