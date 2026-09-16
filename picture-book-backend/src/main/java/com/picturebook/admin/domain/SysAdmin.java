package com.picturebook.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员账号实体 sys_admin
 * 对应 PRD M21：超级管理员 + 普通管理员两级，管理员CRUD/启用/禁用/密码修改/登录日志。
 *
 * @author Agent-4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_admin")
public class SysAdmin extends BaseEntity {

    /** 管理员ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long adminId;

    /** 登录账号 */
    private String username;

    /** 加密后的密码（BCrypt） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 角色：super=超级管理员 / operator=普通管理员 */
    private String role;

    /** 联系电话 */
    private String phone;

    /** 最近登录时间 */
    private java.util.Date lastLoginAt;

    /** 最近登录IP */
    private String lastLoginIp;

    /** 状态：0=正常 / 1=停用 */
    private String status;
}
