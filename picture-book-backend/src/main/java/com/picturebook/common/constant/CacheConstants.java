package com.picturebook.common.constant;

/**
 * 缓存相关常量
 */
public class CacheConstants {

    /** 登录Token Redis Key 前缀 */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /** 验证码 Redis Key 前缀 */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /** 绘本列表缓存前缀 */
    public static final String BOOK_LIST_KEY = "book:list:";

    /** 绘本详情缓存前缀 */
    public static final String BOOK_DETAIL_KEY = "book:detail:";

    /** 角色列表缓存前缀 */
    public static final String ROLE_LIST_KEY = "role:list:";

    /** 系统配置缓存前缀 */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /** 讲读会话缓存前缀 */
    public static final String READING_SESSION_KEY = "reading:session:";

    /** 对话会话缓存前缀 */
    public static final String CHAT_SESSION_KEY = "chat:session:";

    private CacheConstants() {
    }
}
