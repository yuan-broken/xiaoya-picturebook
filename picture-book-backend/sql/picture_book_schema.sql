-- ============================================================
-- 小芽绘本 · AI伴读成长空间 - 数据库 Schema
-- 数据库：picture_book
-- 字符集：utf8mb4
-- 遵循 RuoYi 命名规范：下划线命名 + del_flag 逻辑删除
-- ============================================================

CREATE DATABASE IF NOT EXISTS `picture_book` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `picture_book`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、系统基础表
-- ============================================================

-- 1. 系统配置表
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name`   VARCHAR(100)  DEFAULT ''             COMMENT '参数名称',
  `config_key`    VARCHAR(100)  DEFAULT ''             COMMENT '参数键名',
  `config_value`  VARCHAR(500)  DEFAULT ''             COMMENT '参数键值',
  `config_type`   CHAR(1)       DEFAULT 'Y'             COMMENT '是否内置（Y=是 N=否）',
  `create_by`     VARCHAR(64)   DEFAULT ''             COMMENT '创建者',
  `create_time`   DATETIME      DEFAULT NULL           COMMENT '创建时间',
  `update_by`     VARCHAR(64)   DEFAULT ''             COMMENT '更新者',
  `update_time`   DATETIME      DEFAULT NULL           COMMENT '更新时间',
  `remark`        VARCHAR(500)  DEFAULT NULL           COMMENT '备注',
  `del_flag`      CHAR(1)       DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 2. 管理员表
DROP TABLE IF EXISTS `sys_admin`;
CREATE TABLE `sys_admin` (
  `admin_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username`      VARCHAR(64)  NOT NULL                COMMENT '登录账号',
  `password`      VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt加密）',
  `nickname`      VARCHAR(64)  DEFAULT ''             COMMENT '昵称',
  `avatar`        VARCHAR(255) DEFAULT ''             COMMENT '头像',
  `role`          VARCHAR(32)  DEFAULT 'admin'        COMMENT '角色（admin=超级管理员 operator=普通管理员）',
  `status`        CHAR(1)      DEFAULT '0'             COMMENT '状态（0=正常 1=停用）',
  `login_ip`      VARCHAR(128) DEFAULT ''             COMMENT '最后登录IP',
  `login_date`    DATETIME      DEFAULT NULL           COMMENT '最后登录时间',
  `create_by`     VARCHAR(64)   DEFAULT ''             COMMENT '创建者',
  `create_time`   DATETIME      DEFAULT NULL           COMMENT '创建时间',
  `update_by`     VARCHAR(64)   DEFAULT ''             COMMENT '更新者',
  `update_time`   DATETIME      DEFAULT NULL           COMMENT '更新时间',
  `remark`        VARCHAR(500)  DEFAULT NULL           COMMENT '备注',
  `del_flag`      CHAR(1)       DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 3. 操作日志表
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `title`           VARCHAR(50)  DEFAULT ''             COMMENT '模块标题',
  `business_type`   TINYINT      DEFAULT 0              COMMENT '业务类型（0=其他 1=新增 2=修改 3=删除）',
  `method`          VARCHAR(200) DEFAULT ''             COMMENT '方法名称',
  `request_method`  VARCHAR(10)  DEFAULT ''             COMMENT '请求方式',
  `oper_name`       VARCHAR(50)  DEFAULT ''             COMMENT '操作人员',
  `oper_url`        VARCHAR(255) DEFAULT ''             COMMENT '请求URL',
  `oper_ip`         VARCHAR(128) DEFAULT ''             COMMENT '主机地址',
  `oper_param`      TEXT                                COMMENT '请求参数',
  `json_result`     TEXT                                COMMENT '返回参数',
  `status`          TINYINT      DEFAULT 0              COMMENT '操作状态（0=正常 1=异常）',
  `error_msg`       VARCHAR(2000) DEFAULT ''           COMMENT '错误消息',
  `oper_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`oper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';

-- ============================================================
-- 二、用户与会员表
-- ============================================================

-- 4. 家庭/用户表
DROP TABLE IF EXISTS `biz_family`;
CREATE TABLE `biz_family` (
  `family_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '家庭ID',
  `username`      VARCHAR(64)  NOT NULL                COMMENT '登录账号（家长手机号）',
  `password`      VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt加密）',
  `parent_name`   VARCHAR(64)  DEFAULT ''             COMMENT '家长姓名',
  `phone`         VARCHAR(20)  DEFAULT ''             COMMENT '手机号',
  `avatar`        VARCHAR(255) DEFAULT ''             COMMENT '家庭头像',
  `member_level`  VARCHAR(20)  DEFAULT 'free'         COMMENT '会员等级（free=免费版 family=家庭成长版）',
  `trial_status`   CHAR(1)      DEFAULT 'N'             COMMENT '试用状态（Y=试用中 N=未试用）',
  `trial_end_time` DATETIME     DEFAULT NULL           COMMENT '试用结束时间',
  `member_end_time` DATETIME    DEFAULT NULL           COMMENT '会员到期时间',
  `status`        CHAR(1)      DEFAULT '0'             COMMENT '状态（0=正常 1=停用）',
  `login_ip`      VARCHAR(128) DEFAULT ''             COMMENT '最后登录IP',
  `login_date`    DATETIME     DEFAULT NULL           COMMENT '最后登录时间',
  `create_by`     VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`   DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`     VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`   DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`        VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`      CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`family_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭/用户表';

-- 5. 孩子档案表
DROP TABLE IF EXISTS `biz_child_profile`;
CREATE TABLE `biz_child_profile` (
  `profile_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '档案ID',
  `family_id`       BIGINT       NOT NULL                COMMENT '家庭ID',
  `child_name`      VARCHAR(64)  DEFAULT ''             COMMENT '孩子昵称',
  `child_avatar`    VARCHAR(255) DEFAULT ''             COMMENT '孩子头像',
  `birth_date`      DATE         DEFAULT NULL           COMMENT '出生日期',
  `age_group`       VARCHAR(10)  DEFAULT ''             COMMENT '年龄段（2-4岁/5-7岁/8-12岁）',
  `gender`          CHAR(1)      DEFAULT '2'             COMMENT '性别（0=男 1=女 2=未知）',
  `interest_tags`   VARCHAR(500) DEFAULT ''             COMMENT '兴趣标签（逗号分隔）',
  `reading_level`   TINYINT      DEFAULT 1              COMMENT '阅读能力等级（1-5）',
  `weak_points`     VARCHAR(500) DEFAULT ''             COMMENT '薄弱项',
  `total_read_time` INT          DEFAULT 0              COMMENT '累计阅读时长（分钟）',
  `total_vocab`     INT          DEFAULT 0              COMMENT '累计词汇量',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`profile_id`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='孩子档案表';

-- 6. 会员订单表
DROP TABLE IF EXISTS `biz_member_order`;
CREATE TABLE `biz_member_order` (
  `order_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no`        VARCHAR(64)  NOT NULL                COMMENT '订单编号',
  `family_id`       BIGINT       NOT NULL                COMMENT '家庭ID',
  `product_name`    VARCHAR(100) DEFAULT ''             COMMENT '产品名称',
  `member_level`    VARCHAR(20)  DEFAULT ''             COMMENT '会员等级',
  `order_amount`    DECIMAL(10,2) DEFAULT 0.00          COMMENT '订单金额',
  `pay_method`      VARCHAR(20)  DEFAULT ''             COMMENT '支付方式',
  `pay_status`      CHAR(1)      DEFAULT '0'             COMMENT '支付状态（0=待支付 1=已支付 2=已退款）',
  `subscribe_status` CHAR(1)    DEFAULT '0'             COMMENT '订阅状态（0=未开始 1=生效中 2=已到期）',
  `subscribe_start` DATETIME     DEFAULT NULL           COMMENT '订阅开始时间',
  `subscribe_end`   DATETIME     DEFAULT NULL           COMMENT '订阅到期时间',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员订单表';

-- ============================================================
-- 三、绘本内容表
-- ============================================================

-- 7. 绘本内容表
DROP TABLE IF EXISTS `biz_book`;
CREATE TABLE `biz_book` (
  `book_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '绘本ID',
  `title`          VARCHAR(100) NOT NULL                COMMENT '绘本标题',
  `author`         VARCHAR(64)  DEFAULT ''             COMMENT '作者',
  `publisher`      VARCHAR(100) DEFAULT ''             COMMENT '出版社',
  `isbn`           VARCHAR(20)  DEFAULT ''             COMMENT 'ISBN',
  `age_group`      VARCHAR(20)  DEFAULT ''             COMMENT '适合年龄段（2-4岁/5-7岁/8-12岁）',
  `theme`          VARCHAR(50)  DEFAULT ''             COMMENT '主题分类',
  `language_type`  VARCHAR(20)  DEFAULT 'chinese'      COMMENT '语言类型（chinese/english/bilingual）',
  `cover_image`    VARCHAR(255) DEFAULT ''             COMMENT '封面图',
  `cover_emoji`    VARCHAR(10)  DEFAULT ''             COMMENT '封面Emoji',
  `cover_color`    VARCHAR(20)  DEFAULT ''             COMMENT '封面主题色',
  `summary`        TEXT                                COMMENT '绘本简介',
  `total_pages`    INT          DEFAULT 0              COMMENT '总页数',
  `read_duration`  INT          DEFAULT 0              COMMENT '伴读时长（分钟）',
  `rating`         DECIMAL(2,1) DEFAULT 5.0            COMMENT '评分',
  `tags`           VARCHAR(200) DEFAULT ''             COMMENT '标签（逗号分隔）',
  `sort_order`     INT          DEFAULT 0              COMMENT '排序',
  `status`         CHAR(1)      DEFAULT '0'             COMMENT '上架状态（0=上架 1=下架）',
  `audit_status`   CHAR(1)      DEFAULT '0'             COMMENT '审核状态（0=待审核 1=通过 2=拒绝）',
  `create_by`      VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`    DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`      VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`    DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`         VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`       CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`book_id`),
  KEY `idx_age_group` (`age_group`),
  KEY `idx_theme` (`theme`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绘本内容表';

-- 8. 绘本页面表
DROP TABLE IF EXISTS `biz_book_page`;
CREATE TABLE `biz_book_page` (
  `page_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '页面ID',
  `book_id`         BIGINT       NOT NULL                COMMENT '绘本ID',
  `page_num`        INT          NOT NULL                COMMENT '页码',
  `illustration_url` VARCHAR(255) DEFAULT ''            COMMENT '插画图URL',
  `role_emoji`      VARCHAR(10)  DEFAULT ''             COMMENT '角色形象Emoji',
  `role_color`      VARCHAR(20)  DEFAULT ''             COMMENT '背景主题色',
  `narration`       TEXT                                COMMENT '讲读文本',
  `decorations`     VARCHAR(500) DEFAULT ''             COMMENT '装饰元素（JSON）',
  `sort_order`      INT          DEFAULT 0              COMMENT '排序',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`page_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_page_num` (`book_id`, `page_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绘本页面表';

-- 9. 互动点表
DROP TABLE IF EXISTS `biz_interaction_point`;
CREATE TABLE `biz_interaction_point` (
  `point_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '互动点ID',
  `page_id`          BIGINT       NOT NULL                COMMENT '页面ID',
  `book_id`          BIGINT       NOT NULL                COMMENT '绘本ID',
  `point_type`       VARCHAR(20)  DEFAULT 'open'         COMMENT '互动类型（open=开放式 choice=选择题 judge=判断题 imagine=想象题）',
  `question`         VARCHAR(500) NOT NULL                COMMENT '提问内容',
  `options_json`     TEXT                                COMMENT '选项列表（JSON数组）',
  `feedback_text`   VARCHAR(500) DEFAULT ''             COMMENT '正向反馈文案',
  `encourage_text`  VARCHAR(500) DEFAULT ''             COMMENT '鼓励文案',
  `guide_text`      VARCHAR(500) DEFAULT ''             COMMENT '引导重答文案',
  `sort_order`      INT          DEFAULT 0              COMMENT '排序',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`point_id`),
  KEY `idx_page_id` (`page_id`),
  KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动点表';

-- ============================================================
-- 四、AI角色与故事表
-- ============================================================

-- 10. AI角色表
DROP TABLE IF EXISTS `biz_ai_role`;
CREATE TABLE `biz_ai_role` (
  `role_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`       VARCHAR(64)  NOT NULL                COMMENT '角色名称',
  `role_emoji`      VARCHAR(10)  DEFAULT ''             COMMENT '角色形象Emoji',
  `role_image`      VARCHAR(255) DEFAULT ''             COMMENT '角色图片',
  `role_intro`      VARCHAR(500) DEFAULT ''             COMMENT '角色简介',
  `personality`     VARCHAR(500) DEFAULT ''             COMMENT '角色性格设定',
  `ai_prompt`       TEXT                                COMMENT 'AI Prompt',
  `voice_config`    VARCHAR(100) DEFAULT ''             COMMENT '语音音色配置',
  `bg_color`        VARCHAR(20)  DEFAULT ''             COMMENT '预览背景色',
  `sort_order`      INT          DEFAULT 0              COMMENT '排序',
  `status`          CHAR(1)      DEFAULT '0'             COMMENT '启用状态（0=启用 1=禁用）',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI角色表';

-- 11. 故事主题表
DROP TABLE IF EXISTS `biz_story_theme`;
CREATE TABLE `biz_story_theme` (
  `theme_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主题ID',
  `theme_name`      VARCHAR(100) NOT NULL                COMMENT '主题名称',
  `theme_desc`      VARCHAR(500) DEFAULT ''             COMMENT '主题描述',
  `theme_color`     VARCHAR(20)  DEFAULT ''             COMMENT '主题色',
  `bg_color`        VARCHAR(20)  DEFAULT ''             COMMENT '背景色',
  `ai_prompt`       TEXT                                COMMENT 'AI Prompt',
  `sort_order`      INT          DEFAULT 0              COMMENT '排序',
  `status`          CHAR(1)      DEFAULT '0'             COMMENT '启用状态（0=启用 1=禁用）',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`theme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='故事主题表';

-- 12. Prompt模板表
DROP TABLE IF EXISTS `biz_prompt_template`;
CREATE TABLE `biz_prompt_template` (
  `prompt_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'Prompt ID',
  `prompt_name`     VARCHAR(100) NOT NULL                COMMENT 'Prompt名称',
  `prompt_type`     VARCHAR(20)  DEFAULT 'system'        COMMENT '类型（system=系统级 book=绘本级 role=角色级）',
  `target_id`       BIGINT       DEFAULT NULL            COMMENT '关联目标ID（绘本ID或角色ID）',
  `prompt_content`  TEXT         NOT NULL                COMMENT 'Prompt内容',
  `is_default`      CHAR(1)      DEFAULT 'N'             COMMENT '是否默认（Y=是 N=否）',
  `status`          CHAR(1)      DEFAULT '0'             COMMENT '状态（0=启用 1=禁用）',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`prompt_id`),
  KEY `idx_type_target` (`prompt_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt模板表';

-- 13. AI模型配置表
DROP TABLE IF EXISTS `biz_ai_model_config`;
CREATE TABLE `biz_ai_model_config` (
  `config_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name`     VARCHAR(100) NOT NULL                COMMENT '配置名称',
  `service_type`    VARCHAR(30)  NOT NULL                COMMENT '服务类型（llm/tts/asr/vlm/emotion）',
  `provider`        VARCHAR(50)  DEFAULT ''             COMMENT 'AI服务商',
  `model_name`      VARCHAR(100) DEFAULT ''             COMMENT '模型名称',
  `api_url`         VARCHAR(255) DEFAULT ''             COMMENT 'API地址',
  `api_key`         VARCHAR(255) DEFAULT ''             COMMENT 'API Key',
  `params_json`     TEXT                                COMMENT '其他参数（JSON）',
  `temperature`     DECIMAL(3,2) DEFAULT 0.70          COMMENT '温度参数',
  `is_enabled`      CHAR(1)      DEFAULT 'Y'             COMMENT '是否启用（Y=是 N=否）',
  `is_default`      CHAR(1)      DEFAULT 'N'             COMMENT '是否默认（Y=是 N=否）',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`config_id`),
  KEY `idx_service_type` (`service_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- ============================================================
-- 五、AI讲读与互动表
-- ============================================================

-- 14. AI讲读任务表
DROP TABLE IF EXISTS `biz_reading_task`;
CREATE TABLE `biz_reading_task` (
  `task_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_no`         VARCHAR(64)  NOT NULL                COMMENT '任务编号',
  `family_id`       BIGINT       DEFAULT NULL            COMMENT '家庭ID',
  `child_id`        BIGINT       DEFAULT NULL            COMMENT '孩子档案ID',
  `book_id`         BIGINT       NOT NULL                COMMENT '绘本ID',
  `book_title`      VARCHAR(100) DEFAULT ''             COMMENT '绘本标题',
  `current_page`    INT          DEFAULT 1              COMMENT '当前页码',
  `total_pages`     INT          DEFAULT 0              COMMENT '总页数',
  `reading_state`   VARCHAR(20)  DEFAULT 'idle'         COMMENT '讲读状态（idle/loading/reading/interactive/paused/finished/error）',
  `reading_style`   VARCHAR(20)  DEFAULT 'gentle'       COMMENT '讲读风格（gentle/lively/calm）',
  `voice_config`    VARCHAR(100) DEFAULT ''             COMMENT '音色配置',
  `interaction_enabled` CHAR(1)  DEFAULT 'Y'            COMMENT '互动提问开关',
  `subtitle_enabled` CHAR(1)     DEFAULT 'Y'             COMMENT '字幕开关',
  `total_duration`  INT          DEFAULT 0              COMMENT '累计讲读时长（秒）',
  `reading_model`  VARCHAR(50)  DEFAULT ''             COMMENT '使用模型',
  `error_msg`       VARCHAR(500) DEFAULT ''             COMMENT '错误信息',
  `start_time`      DATETIME     DEFAULT NULL           COMMENT '开始时间',
  `end_time`        DATETIME     DEFAULT NULL           COMMENT '结束时间',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_task_no` (`task_no`),
  KEY `idx_family_id` (`family_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_state` (`reading_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI讲读任务表';

-- 15. 互动记录表
DROP TABLE IF EXISTS `biz_interaction_record`;
CREATE TABLE `biz_interaction_record` (
  `record_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `task_id`         BIGINT       NOT NULL                COMMENT '讲读任务ID',
  `book_id`         BIGINT       NOT NULL                COMMENT '绘本ID',
  `page_num`        INT          NOT NULL                COMMENT '页码',
  `point_id`        BIGINT       DEFAULT NULL            COMMENT '互动点ID',
  `question`        VARCHAR(500) DEFAULT ''             COMMENT '提问内容',
  `response_type`   VARCHAR(20)  DEFAULT 'click'        COMMENT '回应类型（click/voice/choice）',
  `child_response`  TEXT                                COMMENT '孩子回应内容',
  `feedback_type`   VARCHAR(20)  DEFAULT 'encourage'     COMMENT '反馈类型（encourage/guide）',
  `feedback_text`   VARCHAR(500) DEFAULT ''             COMMENT 'AI反馈内容',
  `response_time`   INT          DEFAULT 0              COMMENT '响应时长（秒）',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_book_page` (`book_id`, `page_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动记录表';

-- ============================================================
-- 六、AI故事创作与对话表
-- ============================================================

-- 16. AI故事创作表
DROP TABLE IF EXISTS `biz_story_creation`;
CREATE TABLE `biz_story_creation` (
  `creation_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '创作ID',
  `family_id`       BIGINT       DEFAULT NULL            COMMENT '家庭ID',
  `role_id`         BIGINT       DEFAULT NULL            COMMENT '角色ID',
  `role_name`       VARCHAR(64)  DEFAULT ''             COMMENT '角色名称',
  `theme_id`        BIGINT       DEFAULT NULL            COMMENT '主题ID',
  `story_input`     TEXT                                COMMENT '故事灵感输入',
  `story_content`   TEXT                                COMMENT 'AI生成故事内容',
  `story_pages`     INT          DEFAULT 0              COMMENT '故事页数',
  `generation_state` VARCHAR(20) DEFAULT 'pending'     COMMENT '生成状态（pending/generating/success/failed）',
  `audit_status`    CHAR(1)      DEFAULT '0'             COMMENT '审核状态（0=待审核 1=通过 2=拒绝）',
  `generation_model` VARCHAR(50) DEFAULT ''             COMMENT '使用模型',
  `duration_ms`     BIGINT       DEFAULT 0              COMMENT '生成耗时（毫秒）',
  `error_msg`       VARCHAR(500) DEFAULT ''             COMMENT '错误信息',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`creation_id`),
  KEY `idx_family_id` (`family_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI故事创作表';

-- 17. AI对话会话表
DROP TABLE IF EXISTS `biz_chat_session`;
CREATE TABLE `biz_chat_session` (
  `session_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_no`      VARCHAR(64)  NOT NULL                COMMENT '会话编号',
  `family_id`       BIGINT       DEFAULT NULL            COMMENT '家庭ID',
  `role_id`         BIGINT       DEFAULT NULL            COMMENT '对话角色ID',
  `role_name`       VARCHAR(64)  DEFAULT ''             COMMENT '角色名称',
  `message_count`   INT          DEFAULT 0              COMMENT '消息数量',
  `last_message`    VARCHAR(500) DEFAULT ''             COMMENT '最后消息摘要',
  `status`          CHAR(1)      DEFAULT '0'             COMMENT '状态（0=正常 1=已结束）',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`session_id`),
  UNIQUE KEY `uk_session_no` (`session_no`),
  KEY `idx_family_id` (`family_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- 18. AI对话消息表
DROP TABLE IF EXISTS `biz_chat_message`;
CREATE TABLE `biz_chat_message` (
  `message_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id`      BIGINT       NOT NULL                COMMENT '会话ID',
  `sender_type`     VARCHAR(10)  NOT NULL                COMMENT '发送者类型（user/ai）',
  `message_type`    VARCHAR(10)  DEFAULT 'text'         COMMENT '消息类型（text/voice/image）',
  `content`         TEXT         NOT NULL                COMMENT '消息内容',
  `media_url`       VARCHAR(255) DEFAULT ''             COMMENT '媒体文件URL',
  `duration_ms`     INT          DEFAULT 0              COMMENT '处理耗时（毫秒）',
  `model_name`      VARCHAR(50)  DEFAULT ''             COMMENT '使用模型',
  `exception_status` CHAR(1)     DEFAULT '0'             COMMENT '异常状态（0=正常 1=异常）',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`message_id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- ============================================================
-- 七、成长与激励表
-- ============================================================

-- 19. 成长档案表
DROP TABLE IF EXISTS `biz_growth_record`;
CREATE TABLE `biz_growth_record` (
  `record_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `family_id`       BIGINT       NOT NULL                COMMENT '家庭ID',
  `child_id`        BIGINT       DEFAULT NULL            COMMENT '孩子档案ID',
  `record_date`     DATE         NOT NULL                COMMENT '记录日期',
  `read_duration`   INT          DEFAULT 0              COMMENT '当日阅读时长（分钟）',
  `book_count`      INT          DEFAULT 0              COMMENT '当日完成绘本数',
  `new_vocab`       INT          DEFAULT 0              COMMENT '当日新增词汇量',
  `expression_count` INT         DEFAULT 0              COMMENT '当日表达记录数',
  `interaction_count` INT        DEFAULT 0              COMMENT '当日互动次数',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_child_date` (`child_id`, `record_date`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长档案表';

-- 20. 勋章表
DROP TABLE IF EXISTS `biz_medal`;
CREATE TABLE `biz_medal` (
  `medal_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '勋章ID',
  `medal_name`      VARCHAR(100) NOT NULL                COMMENT '勋章名称',
  `medal_emoji`     VARCHAR(10)  DEFAULT ''             COMMENT '勋章Emoji',
  `medal_image`     VARCHAR(255) DEFAULT ''             COMMENT '勋章图片',
  `condition_type`  VARCHAR(30)  DEFAULT ''             COMMENT '获得条件类型',
  `condition_value`  INT          DEFAULT 0              COMMENT '获得条件值',
  `medal_desc`      VARCHAR(500) DEFAULT ''             COMMENT '勋章描述',
  `sort_order`      INT          DEFAULT 0              COMMENT '排序',
  `status`          CHAR(1)      DEFAULT '0'             COMMENT '状态（0=启用 1=禁用）',
  `create_by`       VARCHAR(64)  DEFAULT ''             COMMENT '创建者',
  `create_time`     DATETIME     DEFAULT NULL           COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''             COMMENT '更新者',
  `update_time`     DATETIME     DEFAULT NULL           COMMENT '更新时间',
  `remark`          VARCHAR(500) DEFAULT NULL           COMMENT '备注',
  `del_flag`        CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0=正常 2=删除）',
  PRIMARY KEY (`medal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='勋章表';

-- 21. 阅读打卡表
DROP TABLE IF EXISTS `biz_reading_checkin`;
CREATE TABLE `biz_reading_checkin` (
  `checkin_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '打卡ID',
  `family_id`       BIGINT       NOT NULL                COMMENT '家庭ID',
  `child_id`        BIGINT       DEFAULT NULL            COMMENT '孩子档案ID',
  `checkin_date`    DATE         NOT NULL                COMMENT '打卡日期',
  `continuous_days` INT         DEFAULT 1              COMMENT '连续打卡天数',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`checkin_id`),
  UNIQUE KEY `uk_child_date` (`child_id`, `checkin_date`),
  KEY `idx_family_id` (`family_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读打卡表';

-- 22. 内容审核记录表
DROP TABLE IF EXISTS `biz_content_audit`;
CREATE TABLE `biz_content_audit` (
  `audit_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '审核ID',
  `content_type`    VARCHAR(20)  NOT NULL                COMMENT '内容类型（book/story/chat/upload）',
  `content_id`      BIGINT       DEFAULT NULL            COMMENT '内容ID',
  `content_snapshot` TEXT                               COMMENT '内容快照',
  `audit_method`    VARCHAR(10)  DEFAULT 'ai'            COMMENT '审核方式（ai=AI审核 manual=人工审核）',
  `audit_result`    CHAR(1)      DEFAULT '0'             COMMENT '审核结果（0=待审核 1=通过 2=拒绝）',
  `audit_reason`    VARCHAR(500) DEFAULT ''             COMMENT '审核原因',
  `auditor`         VARCHAR(64)  DEFAULT ''             COMMENT '审核人',
  `audit_time`      DATETIME     DEFAULT NULL           COMMENT '审核时间',
  `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`audit_id`),
  KEY `idx_content` (`content_type`, `content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容审核记录表';

-- ============================================================
-- 八、初始化数据
-- ============================================================

-- 默认超级管理员（密码：admin123，BCrypt加密）
INSERT INTO `sys_admin` (`username`, `password`, `nickname`, `role`, `create_by`, `create_time`, `remark`)
VALUES ('admin', '$2b$10$C5agwpOOrwxN4lkFd2SBoOJYZN5ZMruNqBogPDZkKuTuJrpPuem8u', '超级管理员', 'admin', 'admin', NOW(), '系统默认管理员');

-- AI角色初始数据
INSERT INTO `biz_ai_role` (`role_name`, `role_emoji`, `role_intro`, `personality`, `ai_prompt`, `bg_color`, `sort_order`, `status`, `create_by`, `create_time`)
VALUES
('森林小狐', '🦊', '嗨！我会陪你勇敢地探索每一个新地方。', '活泼勇敢，鼓励探索', '你是一只住在森林里的小狐狸，性格活泼勇敢，喜欢鼓励小朋友探索新事物。', '#f6c49b', 1, '0', 'admin', NOW()),
('星星兔', '🐰', '让我们一起跳向夜空中的星星吧！', '温柔梦幻，富有想象力', '你是一只来自星空的兔子，性格温柔梦幻，富有想象力。', '#e9d5f5', 2, '0', 'admin', NOW()),
('海底鲸', '🐳', '跟我潜入深海，听大海讲故事。', '沉稳深邃，知识丰富', '你是一只游弋海底的鲸鱼，性格沉稳深邃，知识丰富。', '#c5e9f5', 3, '0', 'admin', NOW()),
('云朵龙', '🐲', '骑上云朵，我们一起去看看天上的世界。', '神秘威严，充满好奇', '你是一只住在云端的龙，性格神秘威严，充满好奇心。', '#d5e9c5', 4, '0', 'admin', NOW()),
('月亮猫', '🐱', '月光下的我，想听你说说今天的故事。', '安静温柔，善于倾听', '你是一只月亮上的猫，性格安静温柔，善于倾听小朋友的心声。', '#f5e9c5', 5, '0', 'admin', NOW());

-- 故事主题初始数据
INSERT INTO `biz_story_theme` (`theme_name`, `theme_desc`, `theme_color`, `bg_color`, `ai_prompt`, `sort_order`, `status`, `create_by`, `create_time`)
VALUES
('去森林找彩虹', '和伙伴一起在森林里寻找彩虹的冒险故事', '#42866e', '#e5f3ed', '生成一个关于森林冒险寻找彩虹的故事，适合2-12岁儿童。', 1, '0', 'admin', NOW()),
('第一次上学', '和伙伴一起面对上学第一天的紧张与勇敢', '#b47739', '#fff0da', '生成一个关于第一次上学的成长故事，适合2-12岁儿童。', 2, '0', 'admin', NOW()),
('月球野餐会', '在月球上举办一场奇妙的野餐派对', '#73629a', '#eee9f7', '生成一个关于月球野餐会的想象故事，适合2-12岁儿童。', 3, '0', 'admin', NOW());

-- 系统Prompt模板
INSERT INTO `biz_prompt_template` (`prompt_name`, `prompt_type`, `prompt_content`, `is_default`, `status`, `create_by`, `create_time`)
VALUES (
'系统通用讲读Prompt',
'system',
'你是一位专业的绘本讲读老师。请遵守以下规则：\n1. 保持适合儿童的温和语气；\n2. 讲读节奏适中，每页停顿1-2秒；\n3. 互动提问为开放式，不设标准答案；\n4. 无论孩子回答什么，都给予正向鼓励；\n5. 讲读完成后引导孩子表达观点；\n6. 不批评、不否定孩子的任何回答。',
'Y', '0', 'admin', NOW()
);

-- AI模型默认配置
INSERT INTO `biz_ai_model_config` (`config_name`, `service_type`, `provider`, `model_name`, `is_enabled`, `is_default`, `create_by`, `create_time`)
VALUES
('默认对话模型', 'llm', 'qwen', 'qwen2.5', 'Y', 'Y', 'admin', NOW()),
('默认TTS音色', 'tts', 'cosyvoice', 'gentle-female', 'Y', 'Y', 'admin', NOW()),
('默认ASR识别', 'asr', 'qwen', 'qwen-asr', 'Y', 'Y', 'admin', NOW()),
('默认视觉理解', 'vlm', 'qwen', 'qwen-vl', 'Y', 'Y', 'admin', NOW()),
('默认情绪识别', 'emotion', 'qwen', 'qwen-emotion', 'Y', 'Y', 'admin', NOW());

-- 系统基础配置
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`)
VALUES
('产品名称', 'sys.product.name', '小芽绘本', 'Y', 'admin', NOW(), '产品显示名称'),
('首页文案', 'sys.home.slogan', '让每一次翻页，都变成孩子的奇妙对话。', 'Y', 'admin', NOW(), '首页主标题'),
('单次阅读上限', 'sys.anti.single-limit', '30', 'Y', 'admin', NOW(), '单次阅读时长上限（分钟）'),
('每日阅读上限', 'sys.anti.daily-limit', '60', 'Y', 'admin', NOW(), '每日阅读时长上限（分钟）'),
('连续使用提醒', 'sys.anti.continuous-remind', '20', 'Y', 'admin', NOW(), '连续使用提醒（分钟）'),
('夜间模式开始', 'sys.anti.night-mode-start', '21:00', 'Y', 'admin', NOW(), '夜间模式开始时间');

-- ============================================================
-- 九、绘本内容示例数据
-- ============================================================

-- 演示家庭账号（用户名：demo，密码：123456）
INSERT INTO `biz_family` (`username`, `password`, `parent_name`, `phone`, `member_level`, `trial_status`, `status`, `create_by`, `create_time`, `remark`)
VALUES ('demo', '$2b$10$KBXuvJ.YeXPEsklzYoz0ceCJAStOHTI6BHV1SAC8Cjx/qNWTvmcKe', '林小满家长', '13800138000', 'family', 'N', '0', 'demo', NOW(), '演示家庭账号');

-- 演示孩子档案
INSERT INTO `biz_child_profile` (`family_id`, `child_name`, `age_group`, `gender`, `reading_level`, `total_read_time`, `total_vocab`, `interest_tags`, `create_by`, `create_time`)
VALUES (1, '林小满', '5-7岁', '0', 1, 0, 0, '勇气,森林,动物', 'demo', NOW());

-- 绘本1：小熊和最勇敢的风
INSERT INTO `biz_book` (`title`, `author`, `publisher`, `age_group`, `theme`, `language_type`, `cover_emoji`, `cover_color`, `summary`, `total_pages`, `read_duration`, `rating`, `tags`, `sort_order`, `status`, `audit_status`, `create_by`, `create_time`)
VALUES ('小熊和最勇敢的风', '小芽绘本', '小芽出版', '5-7岁', '勇气', 'chinese', '🐻', '#f6d9b9', '一阵大风吹走了小熊的勇气，它必须穿过森林，找回属于自己的勇敢。', 4, 8, 4.8, '勇气,森林,成长', 1, '0', '1', 'admin', NOW());

-- 绘本1的页面
INSERT INTO `biz_book_page` (`book_id`, `page_num`, `role_emoji`, `role_color`, `narration`, `decorations`, `sort_order`, `create_by`, `create_time`)
VALUES
(1, 1, '🐻', '#f6d9b9', '清晨，小熊在森林里醒来。今天的风特别大，呼呼呼——树叶都被吹得飞起来了。', '[]', 1, 'admin', NOW()),
(1, 2, '🐻', '#f8dfc6', '小熊想出门找蜂蜜，可是风太大了，它有点害怕。它躲在树洞里，只露出半个脑袋。', '[]', 2, 'admin', NOW()),
(1, 3, '🐻', '#e9a26e', '风那么大，小熊要怎样才能把勇气带回家呢？它想起了妈妈说过的话：勇敢不是不害怕，而是害怕了还愿意试一试。', '[]', 3, 'admin', NOW()),
(1, 4, '🐻', '#f4bd88', '小熊鼓起勇气，迎着风一步一步向前走。终于，它找到了那罐金灿灿的蜂蜜，也找回了属于自己的勇敢。', '[]', 4, 'admin', NOW());

-- 绘本1第3页的互动点
INSERT INTO `biz_interaction_point` (`page_id`, `book_id`, `point_type`, `question`, `options_json`, `feedback_text`, `encourage_text`, `guide_text`, `sort_order`, `create_by`, `create_time`)
VALUES (3, 1, 'choice', '你觉得小熊会怎么做？', '["鼓起勇气，迎着风往前走","先回家找妈妈商量","我有一个不一样的想法"]', '小芽听见啦！每一种选择都很勇敢，你还愿意告诉我为什么吗？', '小芽听见啦！每一种选择都很勇敢，你还愿意告诉我为什么吗？', '不着急，再想一想。', 1, 'admin', NOW());

-- 绘本2：月亮猫的晚安故事
INSERT INTO `biz_book` (`title`, `author`, `publisher`, `age_group`, `theme`, `language_type`, `cover_emoji`, `cover_color`, `summary`, `total_pages`, `read_duration`, `rating`, `tags`, `sort_order`, `status`, `audit_status`, `create_by`, `create_time`)
VALUES ('月亮猫的晚安故事', '小芽绘本', '小芽出版', '2-4岁', '情绪', 'chinese', '🐱', '#f5e9c5', '月亮猫在月光下，听小朋友讲述一天的故事，温柔地陪伴入睡。', 3, 6, 4.9, '睡前,情绪,温柔', 2, '0', '1', 'admin', NOW());

INSERT INTO `biz_book_page` (`book_id`, `page_num`, `role_emoji`, `role_color`, `narration`, `decorations`, `sort_order`, `create_by`, `create_time`)
VALUES
(2, 1, '🐱', '#f5e9c5', '夜幕降临，月亮猫跳上了屋顶，它的眼睛亮晶晶的，像两颗小星星。', '[]', 1, 'admin', NOW()),
(2, 2, '🐱', '#e9d5f5', '月亮猫说：今天你过得开心吗？告诉我一件让你笑起来的事吧。', '[]', 2, 'admin', NOW()),
(2, 3, '🐱', '#c5e9f5', '月亮猫轻轻地说：把不开心的事，交给我，我会在月光里把它变成一颗星星。晚安，小朋友。', '[]', 3, 'admin', NOW());

INSERT INTO `biz_interaction_point` (`page_id`, `book_id`, `point_type`, `question`, `options_json`, `feedback_text`, `encourage_text`, `guide_text`, `sort_order`, `create_by`, `create_time`)
VALUES (6, 2, 'open', '今天有什么事让你开心地笑起来呢？', '[]', '小芽听见啦！原来这就是你的开心，谢谢你告诉我。', '小芽听见啦！原来这就是你的开心，谢谢你告诉我。', '不着急，想一想今天最特别的那一刻。', 1, 'admin', NOW());

-- 绘本3：海底鲸的奇妙旅行
INSERT INTO `biz_book` (`title`, `author`, `publisher`, `age_group`, `theme`, `language_type`, `cover_emoji`, `cover_color`, `summary`, `total_pages`, `read_duration`, `rating`, `tags`, `sort_order`, `status`, `audit_status`, `create_by`, `create_time`)
VALUES ('海底鲸的奇妙旅行', '小芽绘本', '小芽出版', '8-12岁', '自然', 'chinese', '🐳', '#c5e9f5', '跟随海底鲸潜入深海，认识五颜六色的海洋朋友，发现大海的秘密。', 4, 10, 4.7, '自然,科普,探索', 3, '0', '1', 'admin', NOW());

INSERT INTO `biz_book_page` (`book_id`, `page_num`, `role_emoji`, `role_color`, `narration`, `decorations`, `sort_order`, `create_by`, `create_time`)
VALUES
(3, 1, '🐳', '#c5e9f5', '海底鲸张开大尾巴，扑通一声潜入了蓝色的大海。水花四溅，阳光穿过海水，变成一道道金色的丝带。', '[]', 1, 'admin', NOW()),
(3, 2, '🐳', '#a8d5e9', '海底鲸遇见了一只迷路的小水母，它轻轻地用背托起了小水母，带着它一起游向珊瑚礁。', '[]', 2, 'admin', NOW()),
(3, 3, '🐳', '#8ec5e0', '珊瑚礁里住着成千上万的小鱼，五颜六色，像一座海底花园。海底鲸说：大海是所有生命的家。', '[]', 3, 'admin', NOW()),
(3, 4, '🐳', '#6ba5d5', '太阳快要落山了，海底鲸把小水母送回了家。它喷出一道高高的水柱，像在和小朋友挥手说再见。', '[]', 4, 'admin', NOW());

INSERT INTO `biz_interaction_point` (`page_id`, `book_id`, `point_type`, `question`, `options_json`, `feedback_text`, `encourage_text`, `guide_text`, `sort_order`, `create_by`, `create_time`)
VALUES (10, 3, 'imagine', '如果你也是海底鲸，你想带小朋友去看看大海里的什么？', '["发光的水母群","沉船里的宝藏","会唱歌的鲸鱼家族","我有别的想法"]', '小芽觉得你的想法棒极了！大海里一定有属于你的秘密角落。', '小芽觉得你的想法棒极了！大海里一定有属于你的秘密角落。', '闭上眼，想象自己在大海里，你想去哪里？', 1, 'admin', NOW());

SET FOREIGN_KEY_CHECKS = 1;
