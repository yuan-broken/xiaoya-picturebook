package com.picturebook.common.constant;

/**
 * 通用常量定义
 */
public class CommonConstants {

    /** 正常状态 */
    public static final String NORMAL = "0";

    /** 异常状态 */
    public static final String EXCEPTION = "1";

    /** 启用状态（DB: 1=启用 0=禁用） */
    public static final int STATUS_ENABLED = 1;
    public static final int STATUS_DISABLED = 0;

    /** 审核通过 */
    public static final String AUDIT_PASS = "pass";
    public static final String AUDIT_PENDING = "pending";
    public static final String AUDIT_BLOCK = "block";

    /** 删除标志 - 已删除 */
    public static final String DEL_FLAG_DELETED = "2";

    /** 删除标志 - 正常 */
    public static final String DEL_FLAG_NORMAL = "0";

    /** 是 */
    public static final String YES = "Y";

    /** 否 */
    public static final String NO = "N";

    /** 男 */
    public static final String SEX_MALE = "0";

    /** 女 */
    public static final String SEX_FEMALE = "1";

    /** 未知 */
    public static final String SEX_UNKNOWN = "2";

    /** 成功 */
    public static final int SUCCESS = 200;

    /** 失败 */
    public static final int FAIL = 500;

    /** 未授权 */
    public static final int UNAUTHORIZED = 401;

    /** 禁止访问 */
    public static final int FORBIDDEN = 403;

    /** 资源不存在 */
    public static final int NOT_FOUND = 404;

    /** 参数校验错误 */
    public static final int PARAM_ERROR = 400;

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Token 请求头 */
    public static final String TOKEN_HEADER = "Authorization";

    /** UTF-8 字符集 */
    public static final String UTF8 = "UTF-8";

    /** 超级管理员角色 */
    public static final String SUPER_ADMIN_ROLE = "admin";

    /** 默认页面大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;

    private CommonConstants() {
    }
}
