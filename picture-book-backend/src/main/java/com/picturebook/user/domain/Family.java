package com.picturebook.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 家庭账号实体 biz_family
 * 对应 PRD D1/D2/D3：家庭账号、家长密码、内容筛选白/黑名单、时长管控。
 *
 * @author Agent-4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_family")
public class Family extends BaseEntity {

    /** 家庭ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long familyId;

    /** 家长登录账号（手机号或邮箱） */
    private String username;

    /** 加密后的密码（BCrypt） */
    private String password;

    /** 家长昵称（DB列名 family_name） */
    @TableField("family_name")
    private String nickname;

    /** 家长姓名（schema无此列，仅内存使用） */
    @TableField(exist = false)
    private String parentName;

    /** 联系电话 */
    private String phone;

    /** 头像URL（schema无此列） */
    @TableField(exist = false)
    private String avatar;

    /** 会员等级：free=免费 / growth=成长版 */
    private String memberLevel;

    /** 订阅状态（schema无此列，仅内存使用） */
    @TableField(exist = false)
    private String memberStatus;

    /** 会员开始时间（schema无此列） */
    @TableField(exist = false)
    private java.util.Date memberStartAt;

    /** 会员到期时间（DB列名 member_expire） */
    @TableField("member_expire")
    private java.util.Date memberEndAt;

    /** 内容筛选白名单（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private String whitelistThemes;

    /** 内容筛选黑名单（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private String blacklistThemes;

    /** 每日阅读上限（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private Integer dailyLimitMin;

    /** 单次阅读上限（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private Integer singleLimitMin;

    /** 防沉迷提醒开关（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private String antiAddictionFlag;

    /** 状态：1=正常 / 0=停用 */
    @TableField("status")
    private Integer status;
}
