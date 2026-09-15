package com.picturebook.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户勋章颁发记录 biz_user_badge
 *
 * @author Phase2
 */
@Data
@TableName("biz_user_badge")
public class UserBadge {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long familyId;
    private Long childId;

    @TableField("badge_code")
    private String badgeCode;

    @TableField("awarded_at")
    private Date awardedAt;

    /** 来源：auto / manual */
    private String source;
    private String remark;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private Date createTime;
}
