package com.picturebook.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 孩子档案实体 biz_child_profile
 * 对应 PRD D1/E1：孩子画像数据（年龄、兴趣、阅读能力等级、薄弱项、阅读偏好）。
 *
 * @author Agent-4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_child_profile")
public class ChildProfile extends BaseEntity {

    /** 孩子档案ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long childId;

    /** 所属家庭ID（1:1 绑定家长账号） */
    private Long familyId;

    /** 孩子登录账号（全局唯一） */
    @TableField("username")
    private String username;

    /** BCrypt 加密密码 */
    @TableField("password")
    private String password;

    /** 绑定状态：1=已绑定 0=待确认 */
    @TableField("bind_status")
    private Integer bindStatus;

    /** 最后登录时间 */
    @TableField("last_login_at")
    private java.util.Date lastLoginAt;

    /** 孩子昵称（DB列名 child_name） */
    @TableField("child_name")
    private String nickname;

    /** 孩子头像（schema无此列） */
    @TableField(exist = false)
    private String avatar;

    /** 性别（schema无此列） */
    @TableField(exist = false)
    private String sex;

    /** 出生日期（DB列名 birth_date） */
    @TableField("birth_date")
    private java.util.Date birthday;

    /** 年龄段：toddler=2-3 / preschool=4-6 / lowerPrimary=7-9 / upperPrimary=10-12 */
    private String ageGroup;

    /** 阅读能力等级 1-10，由系统按行为日志自动更新 */
    private Integer readingLevel;

    /** 兴趣标签（多个用逗号分隔，如 森林,勇气,动物） */
    private String interests;

    /** 薄弱项（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private String weaknesses;

    /** 阅读偏好（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private String preference;

    /** 状态（schema无此列，仅内存使用） */
    @TableField(exist = false)
    private String status;
}
