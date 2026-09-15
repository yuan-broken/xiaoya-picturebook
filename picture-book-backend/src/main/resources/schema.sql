-- ============================================================
-- 小芽绘本 AI 伴读成长空间 数据库 Schema
-- 维护人：Agent-5（公共文件）
-- 数据库：H2（开发）/ MySQL（生产）
-- ============================================================

-- 通用字段说明：
-- create_by VARCHAR(64) 创建者
-- create_time TIMESTAMP 创建时间
-- update_by VARCHAR(64) 更新者
-- update_time TIMESTAMP 更新时间
-- del_flag INT 逻辑删除（0=正常 1=删除）

-- ============================================================
-- 一、基础设施表（Agent-5 维护）
-- ============================================================

-- 1. 系统配置表（M20）
CREATE TABLE IF NOT EXISTS sys_config (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key  VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value TEXT        COMMENT '配置值',
    config_type VARCHAR(32)  DEFAULT 'string' COMMENT '类型 string/int/json',
    remark      VARCHAR(255) COMMENT '说明',
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0,
    UNIQUE (config_key)
);

-- 2. AI 模型配置表（M14）
CREATE TABLE IF NOT EXISTS sys_ai_model (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    capability_type       VARCHAR(16)  NOT NULL COMMENT '能力类型 llm/tts/asr/vlm/emotion',
    provider              VARCHAR(64)  NOT NULL COMMENT '服务商 如阿里云/OpenAI',
    model_name            VARCHAR(128) NOT NULL COMMENT '模型名 如 Qwen2.5',
    api_url               VARCHAR(255) COMMENT 'API地址',
    api_key               VARCHAR(512) COMMENT 'API Key（AES加密存储）',
    temperature           DECIMAL(3,2) DEFAULT 0.70 COMMENT '温度',
    max_token             INT         DEFAULT 2048,
    stream_enabled        TINYINT(1)  DEFAULT 0 COMMENT '是否流式',
    default_voice         VARCHAR(64) COMMENT '默认音色',
    first_token_latency_ms INT        DEFAULT 300 COMMENT '首字延迟',
    dialect_list          VARCHAR(255) COMMENT '方言列表 逗号分隔',
    enabled               TINYINT(1)  DEFAULT 0 COMMENT '是否启用',
    sort_order            INT         DEFAULT 0,
    remark                VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- 3. 内容审核记录表（M37/M18）
CREATE TABLE IF NOT EXISTS biz_content_review (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type      VARCHAR(32) NOT NULL COMMENT '业务类型 book/story/chat/upload',
    biz_id        BIGINT      COMMENT '业务记录ID',
    content       TEXT        COMMENT '被审核内容',
    review_status VARCHAR(16) DEFAULT 'pending' COMMENT 'pending/ai_pass/ai_block/manual_pass/manual_block',
    review_method VARCHAR(16) DEFAULT 'ai' COMMENT 'ai/manual',
    hit_keywords  VARCHAR(512) COMMENT '命中关键词',
    reviewer      VARCHAR(64),
    review_time   TIMESTAMP,
    remark        VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- 4. 文件存储记录表（M32）
CREATE TABLE IF NOT EXISTS biz_file_storage (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path   VARCHAR(512) NOT NULL COMMENT '存储相对路径',
    file_url    VARCHAR(512) COMMENT '访问URL',
    file_type   VARCHAR(32)  COMMENT '文件类型 cover/illustration/audio/ai_result/avatar',
    file_ext    VARCHAR(16)  COMMENT '扩展名',
    file_size   BIGINT       COMMENT '字节数',
    mime_type   VARCHAR(128),
    storage_type VARCHAR(16) DEFAULT 'local' COMMENT 'local/oss/minio',
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- ============================================================
-- 二、用户与家长（Agent-4 维护，此处DDL由 Agent-5 统一维护）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_family (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_name   VARCHAR(64) NOT NULL,
    username      VARCHAR(64) NOT NULL UNIQUE,
    password      VARCHAR(128) NOT NULL COMMENT 'BCrypt加密',
    phone         VARCHAR(32),
    member_level  VARCHAR(32) DEFAULT 'free' COMMENT 'free/family',
    member_expire TIMESTAMP COMMENT '会员到期时间',
    status        TINYINT(1) DEFAULT 1,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_child_profile (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL COMMENT '绑定的家长账号ID（1:1）',
    username        VARCHAR(64) UNIQUE COMMENT '孩子登录账号',
    password        VARCHAR(128) COMMENT 'BCrypt加密密码',
    child_name      VARCHAR(64),
    birth_date      DATE,
    age_group       VARCHAR(16) COMMENT 'toddler/preschool/lowerPrimary/upperPrimary',
    interests       VARCHAR(255) COMMENT '兴趣标签 逗号分隔',
    reading_level   INT DEFAULT 1 COMMENT '阅读能力等级',
    bind_status     INT DEFAULT 1 COMMENT '1=已绑定 0=待确认',
    last_login_at   TIMESTAMP,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- ============================================================
-- 三、绘本内容（Agent-1 维护，DDL 由 Agent-5 统一）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_book (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(128) NOT NULL,
    author      VARCHAR(64),
    age_group   VARCHAR(16) COMMENT '2-4/5-7/8-12',
    theme       VARCHAR(64),
    language    VARCHAR(32) COMMENT '中文原创/英文启蒙/国际精选',
    cover_url   VARCHAR(255),
    description TEXT,
    page_count  INT DEFAULT 0,
    duration    INT DEFAULT 8 COMMENT '伴读时长(分钟)',
    rating      DECIMAL(2,1) DEFAULT 5.0,
    tags        VARCHAR(255) COMMENT '标签 逗号分隔',
    sort_order  INT DEFAULT 0,
    status      TINYINT(1) DEFAULT 1 COMMENT '0=下架 1=上架',
    audit_status VARCHAR(16) DEFAULT 'pending' COMMENT 'pending/pass/block',
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_book_page (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id         BIGINT NOT NULL,
    page_num        INT NOT NULL,
    illustration_url VARCHAR(255),
    narration       TEXT COMMENT '讲读文本',
    role_emoji      VARCHAR(16),
    bg_color        VARCHAR(32),
    decoration      VARCHAR(255),
    sort_order      INT DEFAULT 0,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_interaction_point (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    page_id         BIGINT NOT NULL,
    book_id         BIGINT NOT NULL,
    interaction_type VARCHAR(32) COMMENT 'open/select/judge/imagine',
    question        VARCHAR(512) NOT NULL,
    options         TEXT COMMENT '选项 JSON数组',
    feedback        VARCHAR(512),
    encourage       VARCHAR(512),
    guide           VARCHAR(512),
    sort_order      INT DEFAULT 0,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- ============================================================
-- 四、AI 角色/主题/Prompt（Agent-3 维护，DDL 由 Agent-5）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_ai_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(64) NOT NULL,
    role_emoji  VARCHAR(16),
    role_intro  VARCHAR(255),
    role_personality TEXT,
    prompt      TEXT COMMENT '角色Prompt',
    voice_config VARCHAR(255),
    bg_color    VARCHAR(32),
    sort_order  INT DEFAULT 0,
    enabled     TINYINT(1) DEFAULT 1,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_story_theme (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    theme_name  VARCHAR(64) NOT NULL,
    theme_desc  VARCHAR(255),
    theme_color VARCHAR(32),
    prompt      TEXT,
    sort_order  INT DEFAULT 0,
    enabled     TINYINT(1) DEFAULT 1,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_sys_prompt (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt_key  VARCHAR(64) NOT NULL,
    prompt_type VARCHAR(32) COMMENT 'system/role/book/style',
    prompt_content TEXT NOT NULL,
    remark      VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- ============================================================
-- 五、讲读/故事/对话记录（Agent-2/3 维护，DDL 由 Agent-5）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_reading_task (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL,
    child_id        BIGINT,
    book_id         BIGINT NOT NULL,
    book_title      VARCHAR(128),
    current_page    INT DEFAULT 1,
    total_pages     INT DEFAULT 0,
    state           VARCHAR(16) DEFAULT 'idle' COMMENT 'idle/loading/reading/interactive/paused/finished/error',
    reading_style   VARCHAR(32) DEFAULT 'gentle',
    duration_ms     BIGINT DEFAULT 0 COMMENT '讲读毫秒',
    error_msg       VARCHAR(255),
    model_name      VARCHAR(128),
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_interaction_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT NOT NULL,
    page_id         BIGINT,
    interaction_id BIGINT,
    question        VARCHAR(512),
    response        TEXT COMMENT '孩子回应',
    ai_feedback     VARCHAR(512),
    intent          VARCHAR(16) COMMENT 'correct/partial/wrong/irrelevant',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS biz_story_creation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL,
    role_id         BIGINT,
    theme_id        BIGINT,
    inspiration     TEXT,
    content         TEXT COMMENT 'AI生成故事内容',
    model_name      VARCHAR(128),
    state           VARCHAR(16) DEFAULT 'pending',
    audit_status    VARCHAR(16) DEFAULT 'pending',
    duration_ms     BIGINT,
    error_msg       VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_chat_session (
    session_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_no      VARCHAR(64),
    family_id       BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    role_name       VARCHAR(64),
    message_count   INT DEFAULT 0,
    last_message    VARCHAR(500),
    status          VARCHAR(8) DEFAULT '0' COMMENT '0=正常 1=已结束',
    model_name      VARCHAR(128),
    create_by       VARCHAR(64),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag        INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_chat_message (
    message_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT NOT NULL,
    sender_type     VARCHAR(16) NOT NULL COMMENT 'user/ai',
    message_type    VARCHAR(16) DEFAULT 'text' COMMENT 'text/voice/image',
    content         TEXT NOT NULL,
    media_url       VARCHAR(255),
    duration_ms     INT,
    model_name      VARCHAR(128),
    exception_status VARCHAR(8) DEFAULT '0',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 六、会员订单（Agent-4 维护，DDL 由 Agent-5）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_member_order (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL,
    plan_id         VARCHAR(32),
    amount          DECIMAL(8,2),
    payment_method  VARCHAR(32),
    pay_status      VARCHAR(16) DEFAULT 'pending' COMMENT 'pending/paid/refunded',
    order_no        VARCHAR(64) UNIQUE,
    start_time      TIMESTAMP,
    expire_time     TIMESTAMP,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- ============================================================
-- 七、管理员（Agent-5 维护）
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_admin (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(64) NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt加密',
    nickname    VARCHAR(64),
    role        VARCHAR(32) DEFAULT 'admin',
    status      TINYINT(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- ============================================================
-- 八、激励与习惯（二期 M36 维护）
-- ============================================================

-- 1. 阅读打卡记录
CREATE TABLE IF NOT EXISTS biz_habit_checkin (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id         BIGINT NOT NULL,
    child_id          BIGINT NOT NULL,
    checkin_date      DATE NOT NULL COMMENT '打卡日期',
    reading_minutes   INT DEFAULT 0 COMMENT '本次阅读分钟',
    book_id           BIGINT COMMENT '关联绘本',
    task_id           BIGINT COMMENT '关联讲读任务',
    continuous_days   INT DEFAULT 1 COMMENT '打卡后连续天数',
    note              VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0,
    UNIQUE (child_id, checkin_date)
);

-- 2. 勋章定义表
CREATE TABLE IF NOT EXISTS biz_badge (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    badge_code      VARCHAR(64) NOT NULL UNIQUE COMMENT '勋章编码',
    badge_name      VARCHAR(64) NOT NULL,
    badge_icon      VARCHAR(32) COMMENT 'emoji图标',
    badge_color     VARCHAR(32),
    category        VARCHAR(32) COMMENT '类别: reading/expression/streak/creation',
    description     VARCHAR(255),
    rule_json       TEXT COMMENT '颁发规则 JSON',
    sort_order      INT DEFAULT 0,
    enabled         TINYINT(1) DEFAULT 1,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

-- 3. 用户勋章颁发记录
CREATE TABLE IF NOT EXISTS biz_user_badge (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL,
    child_id        BIGINT NOT NULL,
    badge_code      VARCHAR(64) NOT NULL,
    awarded_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source          VARCHAR(64) COMMENT '来源: auto/manual',
    remark          VARCHAR(255),
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (child_id, badge_code)
);

-- 4. 收藏夹
CREATE TABLE IF NOT EXISTS biz_favorite (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id       BIGINT NOT NULL,
    child_id        BIGINT,
    target_type     VARCHAR(16) NOT NULL COMMENT 'book/story',
    target_id       BIGINT NOT NULL,
    target_title    VARCHAR(128),
    target_cover    VARCHAR(255),
    group_name      VARCHAR(64) DEFAULT '默认',
    sort_order      INT DEFAULT 0,
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0,
    UNIQUE (family_id, target_type, target_id)
);

-- ============================================================
-- 九、远程共读（二期 M34 维护）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_coread_room (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_code       VARCHAR(16) NOT NULL UNIQUE COMMENT '6位房间码',
    family_id       BIGINT NOT NULL COMMENT '发起家长家庭ID',
    child_id        BIGINT,
    book_id         BIGINT NOT NULL,
    book_title      VARCHAR(128),
    task_id         BIGINT COMMENT '关联讲读任务',
    host_role       VARCHAR(16) DEFAULT 'parent' COMMENT 'parent/child/both',
    mode            VARCHAR(16) DEFAULT 'two_way' COMMENT 'one_way/two_way',
    state           VARCHAR(16) DEFAULT 'waiting' COMMENT 'waiting/active/paused/finished/closed',
    current_page    INT DEFAULT 1,
    total_pages     INT DEFAULT 0,
    host_joined     TINYINT(1) DEFAULT 0,
    guest_joined    TINYINT(1) DEFAULT 0,
    last_sync_at    TIMESTAMP,
    expire_at       TIMESTAMP COMMENT '房间码过期时间',
    create_by   VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by   VARCHAR(64),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag    INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_coread_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_code       VARCHAR(16) NOT NULL,
    family_id       BIGINT,
    child_id        BIGINT,
    event_type      VARCHAR(32) NOT NULL COMMENT 'join/leave/page_next/page_prev/pause/resume/voice_insert/state_sync',
    event_data      TEXT COMMENT '事件JSON',
    sender_role     VARCHAR(16) COMMENT 'parent/child/system',
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 十、声音克隆（VoiceClone，二期 AI 模块维护）
-- ============================================================

CREATE TABLE IF NOT EXISTS biz_voice_clone (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    family_id         BIGINT NOT NULL COMMENT '所属家庭',
    voice_name        VARCHAR(64) NOT NULL COMMENT '自定义音色名',
    sample_audio_url  VARCHAR(512) NOT NULL COMMENT '录音样本URL',
    status            VARCHAR(16) DEFAULT 'pending' COMMENT 'pending/training/success/failed',
    voice_id          VARCHAR(64) COMMENT '训练后返回的音色ID',
    fail_reason       VARCHAR(255),
    create_by         VARCHAR(64),
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64),
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag          INT DEFAULT 0
);
