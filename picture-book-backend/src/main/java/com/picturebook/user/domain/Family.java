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
 * 所有字段均持久化到数据库，不使用内存临时字段。
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

    /** 家长真实姓名 */
    private String parentName;

    /** 联系电话 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 会员等级：free=免费 / growth=成长版 / family=家庭版 */
    private String memberLevel;

    /** 订阅状态：0=未开通 1=已开通 2=已过期 */
    private String memberStatus;

    /** 会员开始时间 */
    @TableField("member_start")
    private java.util.Date memberStartAt;

    /** 会员到期时间（DB列名 member_expire） */
    @TableField("member_expire")
    private java.util.Date memberEndAt;

    /** 内容筛选白名单（逗号分隔） */
    private String whitelistThemes;

    /** 内容筛选黑名单（逗号分隔） */
    private String blacklistThemes;

    /** 每日阅读上限（分钟） */
    private Integer dailyLimitMin;

    /** 单次阅读上限（分钟） */
    private Integer singleLimitMin;

    /** 防沉迷提醒开关：0=关 1=开 */
    private String antiAddictionFlag;

    /** 状态：1=正常 / 0=停用 */
    private Integer status;
}
