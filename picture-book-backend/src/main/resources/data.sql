-- ============================================================
-- 初始化数据（开发环境）
-- Agent-5 维护
-- ============================================================

-- 默认管理员（用户名 admin / 密码 admin123 的 BCrypt 哈希）
INSERT INTO sys_admin (username, password, nickname, role, status, del_flag) VALUES
('admin', '$2a$10$4Bv.EW6wohockq2JKHg2sOjioRIRMz5cfPF7ZlEVxD.eO9L9.vW5u', '超级管理员', 'admin', 1, 0);

-- 系统配置初始数据（M20）
INSERT INTO sys_config (config_key, config_value, config_type, remark, del_flag) VALUES
('product.name',          '小芽绘本',                        'string', '产品名称', 0),
('product.slogan',        'AI 伴读成长空间',                  'string', '产品slogan', 0),
('product.logo',          '/uploads/logo.png',               'string', 'Logo路径', 0),
('product.homeText',      '让每一次翻页，都变成孩子的奇妙对话。', 'string', '首页主文案', 0),
('upload.maxSize',        '52428800',                        'int',    '上传限制(字节)', 0),
('upload.allowedTypes',   'jpg,jpeg,png,gif,webp,mp3,wav,m4a', 'string', '允许格式', 0),
('ai.defaultModel',       'qwen2.5',                         'string', '默认LLM', 0),
('ai.defaultTtsVoice',    'gentle_female',                   'string', '默认TTS音色', 0),
('ai.defaultPrompt',      '保持适合儿童的温和语气，互动提问为开放式，给予正向鼓励。', 'string', '默认系统Prompt', 0),
('privacy.policyText',    '小芽绘本严格遵守《个人信息保护法》《儿童个人信息网络保护规定》', 'string', '隐私政策', 0),
('antiAddiction.single',  '30',                              'int',    '单次分钟', 0),
('antiAddiction.daily',   '60',                              'int',    '每日分钟', 0),
('antiAddiction.nightStart', '21:00',                        'string', '夜间模式开始', 0),
('antiAddiction.nightBrightness', '30',                      'int',    '夜间亮度', 0);

-- AI 模型配置初始数据（M14）- 全部 Mock 占位
INSERT INTO sys_ai_model (capability_type, provider, model_name, api_url, api_key, temperature, max_token, stream_enabled, default_voice, first_token_latency_ms, dialect_list, enabled, sort_order, remark, del_flag) VALUES
('llm',     'mock',        'qwen2.5',   'https://dashscope.aliyuncs.com', '', 0.70, 2048, 0, NULL, NULL, NULL, 1, 1, '默认LLM（Mock占位）', 0),
('tts',     'mock',        'cosyvoice', 'https://api.volcengine.com',     '', NULL, NULL, 1, 'gentle_female', 300, NULL, 1, 1, '默认TTS（Mock）', 0),
('asr',     'mock',        'child-asr', '',                                 '', NULL, NULL, 0, NULL, NULL, 'zh-CN', 1, 1, '默认ASR（Mock）', 0),
('vlm',     'mock',        'qwen-vl',   'https://dashscope.aliyuncs.com', '', NULL, NULL, 0, NULL, NULL, NULL, 0, 2, '视觉模型（未启用）', 0),
('emotion', 'mock',        'emotion-rec', '',                              '', NULL, NULL, 0, NULL, NULL, NULL, 0, 2, '情绪识别（未启用）', 0);

-- AI 角色初始数据
INSERT INTO biz_ai_role (role_name, role_emoji, role_intro, role_personality, prompt, voice_config, bg_color, sort_order, enabled, del_flag) VALUES
('森林小狐', '🦊', '嗨！我会陪你勇敢地探索每一个新地方。', '活泼勇敢，鼓励探索', '你是森林小狐，语气活泼勇敢，常用"我们一起出发吧"等引导语。', 'gentle_female', '#fff7f3', 1, 1, 0),
('星星兔',   '🐰', '一起来数星星吧！', '温柔好奇', '你是星星兔，语气温柔好奇。', 'gentle_female', '#fff0da', 2, 1, 0),
('海底鲸',   '🐳', '潜入海底，发现大世界。', '沉稳包容', '你是海底鲸，语气沉稳包容。', 'calm_male', '#e0f0f8', 3, 1, 0),
('云朵龙',   '🐲', '我们一起飞上天看看吧！', '豪爽热情', '你是云朵龙，语气豪爽热情。', 'cheerful_male', '#eee9f7', 4, 1, 0),
('月亮猫',   '🐱', '晚上好，月亮在等你。', '安静神秘', '你是月亮猫，语气安静神秘。', 'gentle_female', '#d9d1ee', 5, 1, 0);

-- 故事主题初始数据
INSERT INTO biz_story_theme (theme_name, theme_desc, theme_color, prompt, sort_order, enabled, del_flag) VALUES
('去森林找彩虹', '和伙伴在森林里寻找消失的彩虹', '#e5f3ed', '生成一个关于森林冒险和寻找彩虹的故事', 1, 1, 0),
('第一次上学',   '缓解入学焦虑的温暖故事',         '#fff0da', '生成一个关于第一次上学的温暖故事',   2, 1, 0),
('月球野餐会',   '和伙伴去月球野餐的奇幻故事',     '#eee9f7', '生成一个关于月球野餐会的奇幻故事',   3, 1, 0);

-- 绘本初始数据（示例 4 本）
INSERT INTO biz_book (title, author, age_group, theme, language, cover_url, description, page_count, duration, rating, tags, sort_order, status, audit_status, del_flag) VALUES
('小熊和最勇敢的风', '佚名', '5-7岁', '勇气', '中文原创', '', '小熊在风中找回勇气的故事', 12, 8, 4.9, '新入库,互动剧情', 1, 1, 'pass', 0),
('云朵鲸鱼的心情',   '佚名', '3-5岁', '情绪', '英文启蒙', '', '云朵鲸鱼学习表达心情',     10, 6, 4.8, '高共读,睡前故事', 2, 1, 'pass', 0),
('月亮邮差的礼物',   '佚名', '7-9岁', '想象', '国际精选', '', '月亮邮差送来神秘礼物',     15, 10, 4.9, '互动剧情', 3, 1, 'pass', 0),
('藏在云朵里的小房子','佚名', '2-4岁', '情绪', '情绪安抚', '', '寻找藏在云朵里的家',       8, 5, 4.7, '睡前故事', 4, 1, 'pass', 0);

-- 默认家庭账号（用户名 family / 密码 family123）
INSERT INTO biz_family (family_name, username, password, phone, member_level, status, del_flag) VALUES
('林小满家', 'family', '$2a$10$1QSFZP5cbZ/tF43MxfWd0OIBCfIMTAedbJBxJXq0URcb4PmXHw7Sa', '13800000000', 'family', 1, 0);

-- 测试家庭账号（用户名 demo / 密码 123456）
INSERT INTO biz_family (family_name, username, password, phone, member_level, status, del_flag) VALUES
('林小满家(测试)', 'demo', '$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC', '138****8888', 'family', 1, 0);

-- 默认孩子档案（family_id=1 林小满）
-- 密码均为 123456 的 BCrypt 哈希
INSERT INTO biz_child_profile (family_id, username, password, child_name, birth_date, age_group, interests, reading_level, bind_status, del_flag) VALUES
(1, 'xiaoman', '$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC', '林小满', '2020-05-12', 'preschool', '森林,勇气,动物', 3, 1, 0),
(1, 'xiaoyu',  '$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC', '林小雨', '2022-09-08', 'toddler',   '情绪,睡前', 1, 1, 0),
-- demo 账号的孩子档案（family_id=2）
(2, 'kid001',  '$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC', '林小满', '2020-05-12', 'preschool', '森林,勇气,动物', 3, 1, 0);

-- ============================================================
-- 二期：勋章定义初始化（M36-T002）
-- ============================================================
INSERT INTO biz_badge (badge_code, badge_name, badge_icon, badge_color, category, description, rule_json, sort_order, enabled, del_flag) VALUES
('reading_first',     '初读绘本',    '📖', '#f8c7a1', 'reading',    '完成第一本绘本伴读',           '{"type":"reading_count","threshold":1}',       1, 1, 0),
('reading_10',        '阅读小达人',  '📚', '#ef765e', 'reading',    '累计完成10本绘本',             '{"type":"reading_count","threshold":10}',      2, 1, 0),
('reading_30',        '绘本探索家',  '🗺️', '#66a990', 'reading',    '累计完成30本绘本',             '{"type":"reading_count","threshold":30}',      3, 1, 0),
('streak_7',          '坚持一周',    '🔥', '#e3a14f', 'streak',     '连续阅读7天',                  '{"type":"streak_days","threshold":7}',         4, 1, 0),
('streak_30',         '月度坚持',    '🏆', '#c4a76e', 'streak',     '连续阅读30天',                 '{"type":"streak_days","threshold":30}',        5, 1, 0),
('expression_a',      '表达小能手',  '💬', '#7c9cbf', 'expression', '5次互动中4次主动表达',         '{"type":"expression_rate","threshold":0.8}',   6, 1, 0),
('expression_master',  '故事演说家',  '🎤', '#9b6bcc', 'expression', '累计10次优秀互动反馈',         '{"type":"expression_excellent","threshold":10}',7, 1, 0),
('creation_first',    '小小创作家',  '✨', '#edaa4f', 'creation',   '第一次完成AI故事创作',          '{"type":"creation_count","threshold":1}',     8, 1, 0),
('creation_5',        '故事大王',    '👑', '#d9b3ff', 'creation',   '累计创作5个故事',              '{"type":"creation_count","threshold":5}',     9, 1, 0),
('thinking_deep',     '思考小哲学家','🦉', '#83908b', 'expression', '深度思考3次（互动判定为优秀）', '{"type":"thinking_deep","threshold":3}',     10, 1, 0);

-- 默认打卡记录（最近3天，断签测试）
INSERT INTO biz_habit_checkin (family_id, child_id, checkin_date, reading_minutes, book_id, continuous_days, del_flag) VALUES
(1, 1, CURRENT_DATE - 3, 15, 1, 1, 0),
(1, 1, CURRENT_DATE - 2, 22, 2, 2, 0),
(1, 1, CURRENT_DATE - 1, 18, 3, 3, 0);

-- 默认勋章颁发记录（模拟已获2枚）
INSERT INTO biz_user_badge (family_id, child_id, badge_code, source, remark) VALUES
(1, 1, 'reading_first', 'auto', '完成第一本绘本'),
(1, 1, 'streak_7',      'auto', '连续阅读7天达成');

-- ============================================================
-- 绘本页面内容（每本书的故事文本）
-- ============================================================

-- 《小熊和最勇敢的风》 book_id=1, 12页
INSERT INTO biz_book_page (book_id, page_num, narration, role_emoji, bg_color, decoration, sort_order, del_flag) VALUES
(1, 1,  '森林的早晨，小熊从树洞里探出头来。风呼呼地吹着，树叶沙沙作响。', '🐻', '#f6d9b9', '✦ ✧', 1, 0),
(1, 2,  '"今天的风好大呀！"小熊抱紧了手臂。可是妈妈说，风其实是大地在呼吸。', '🐻', '#f6d9b9', '🍃', 2, 0),
(1, 3,  '风那么大，小熊要怎样才能把勇气带回家呢？', '🐻', '#f6d9b9', '✦ ✧', 3, 0),
(1, 4,  '小熊遇到了小松鼠。"你为什么不怕风呢？"小熊问。"因为我知道风在和我玩呢！"', '🐻', '#e9a26e', '🐿️', 4, 0),
(1, 5,  '小松鼠教小熊张开双臂，感受风从指间穿过。"你看，风在拥抱你呢！"', '🐿️', '#f9eccd', '🍃', 5, 0),
(1, 6,  '小熊试着张开双臂。风真的从指间穿过了，暖暖的，像妈妈的手。', '🐻', '#f6d9b9', '✦', 6, 0),
(1, 7,  '"原来风不是可怕的怪物，是大地的呼吸呀！"小熊开心地笑了。', '🐻', '#f9eccd', '☀️', 7, 0),
(1, 8,  '小熊继续往前走，遇到了一只正在筑巢的小鸟。风把她的巢吹散了。', '🐦', '#c7e7de', '🌿', 8, 0),
(1, 9,  '"别怕，我来帮你！"小熊用大大的身体挡住了风，让小鸟重新筑巢。', '🐻', '#c7e7de', '🪺', 9, 0),
(1, 10, '小鸟说："谢谢你，小熊！你真的很勇敢。"小熊脸红了。', '🐦', '#e5f3ed', '✦', 10, 0),
(1, 11, '小熊带着满满的勇气回到家。妈妈问："今天有没有被风吓到？"', '🐻', '#f6d9b9', '🏠', 11, 0),
(1, 12, '"没有！"小熊大声说，"风是大地的呼吸，我还帮小鸟挡住了风呢！"妈妈笑了。', '🐻', '#f9eccd', '⭐', 12, 0);

-- 《云朵鲸鱼的心情》 book_id=2, 10页
INSERT INTO biz_book_page (book_id, page_num, narration, role_emoji, bg_color, decoration, sort_order, del_flag) VALUES
(2, 1,  '高高的天上，住着一条云朵鲸鱼。它比山还大，比风还轻。', '🐋', '#c7e7de', '☁️', 1, 0),
(2, 2,  '云朵鲸鱼今天不太开心。它觉得心里沉甸甸的，像装满了雨水。', '🐋', '#d9d1ee', '💧', 2, 0),
(2, 3,  '"你怎么了？"一只小海鸥飞过来问。"我也不知道，就是想哭。"', '🐋', '#d9d1ee', '🌊', 3, 0),
(2, 4,  '小海鸥说："想哭就哭吧，哭出来就舒服了。大海也是这样，下雨的时候就是在哭。"', '鸥', '#c7e7de', '🌊', 4, 0),
(2, 5,  '云朵鲸鱼试了试，一滴大大的眼泪落了下来，变成了雨。', '🐋', '#b4d4e8', '🌧️', 5, 0),
(2, 6,  '雨水落到了干裂的大地上。小花、小草、小树都抬起头来。', '🌸', '#e5f3ed', '🌿', 6, 0),
(2, 7,  '"谢谢你，云朵鲸鱼！"地上的小伙伴们一起喊。', '🌿', '#e5f3ed', '🌈', 7, 0),
(2, 8,  '云朵鲸鱼惊讶地发现，自己的眼泪帮助了这么多的朋友。', '🐋', '#c7e7de', '☀️', 8, 0),
(2, 9,  '"原来，难过的心情也可以变成好事呢！"云朵鲸鱼笑了。', '🐋', '#f9eccd', '✦', 9, 0),
(2, 10, '从那以后，云朵鲸鱼不再害怕难过了。它知道，每一滴眼泪都可能是一朵花的开始。', '🐋', '#c7e7de', '⭐', 10, 0);

-- 《月亮邮差的礼物》 book_id=3, 15页
INSERT INTO biz_book_page (book_id, page_num, narration, role_emoji, bg_color, decoration, sort_order, del_flag) VALUES
(3, 1,  '月亮上住着一位邮差。他每天晚上都会给地上的孩子们送信。', '🌙', '#d9d1ee', '⭐', 1, 0),
(3, 2,  '今晚，月亮邮差收到了一封特别的信——是一个叫小明的小朋友写的。', '🌙', '#d9d1ee', '✉️', 2, 0),
(3, 3,  '小明在信里说："亲爱的月亮邮差，我想要一个朋友。"', '🌙', '#b4d4e8', '📝', 3, 0),
(3, 4,  '月亮邮差想了想，从口袋里掏出了一颗星星种子。', '🌙', '#d9d1ee', '🌟', 4, 0),
(3, 5,  '他把星星种子包在信封里，写上小明的地址，然后放飞了。', '🌙', '#d9d1ee', '💌', 5, 0),
(3, 6,  '信封飘飘悠悠，穿过云层，越过山头，落在了小明的窗台上。', '🌙', '#c7e7de', '☁️', 6, 0),
(3, 7,  '小明打开信封，发现了一颗亮闪闪的种子。"这是什么？"', '👦', '#f9eccd', '🌟', 7, 0),
(3, 8,  '他把种子种在了花盆里，每天浇水、唱歌、讲故事。', '👦', '#e5f3ed', '🪴', 8, 0),
(3, 9,  '一天晚上，花盆里长出了一棵小小的发光的树。', '🌳', '#d9d1ee', '✨', 9, 0),
(3, 10, '树上结出了一颗小小的星星果实，果实开口说话了："你好呀，我是小星！"', '⭐', '#f9eccd', '🌟', 10, 0),
(3, 11, '小明高兴极了！他终于有了一个朋友，一个从月亮上来的朋友。', '👦', '#f9eccd', '⭐', 11, 0),
(3, 12, '月亮邮差在天上看到了这一切，满意地笑了。', '🌙', '#d9d1ee', '😊', 12, 0),
(3, 13, '后来，小明和小星一起，把星星种子分享给了更多的孩子。', '👦', '#e5f3ed', '🌳', 13, 0),
(3, 14, '每个小朋友都种出了自己的星星树，结出了自己的星星朋友。', '🌳', '#d9d1ee', '🌟', 14, 0),
(3, 15, '从此以后，每当夜晚来临，地上就亮起了无数的小星星。那是月亮邮差送给所有孩子的礼物。', '🌙', '#d9d1ee', '⭐', 15, 0);

-- 《藏在云朵里的小房子》 book_id=4, 8页
INSERT INTO biz_book_page (book_id, page_num, narration, role_emoji, bg_color, decoration, sort_order, del_flag) VALUES
(4, 1,  '有一只小兔子，它一直在寻找一个家。听说，有一个小房子藏在云朵里。', '🐰', '#fff0da', '☁️', 1, 0),
(4, 2,  '小兔子抬头看天，云朵白白的、软软的，像棉花糖一样。', '🐰', '#c7e7de', '☁️', 2, 0),
(4, 3,  '"家在哪里呢？"小兔子一边走一边问。风轻轻地推着它往前走。', '🐰', '#e5f3ed', '🍃', 3, 0),
(4, 4,  '它遇到了一只老猫头鹰。"你要找云朵里的家？"老猫头鹰眨眨眼。', '🦉', '#d9d1ee', '🌙', 4, 0),
(4, 5,  '"是的，您知道在哪里吗？""当你闭上眼睛，用心去感受，就能找到了。"', '🦉', '#d9d1ee', '✦', 5, 0),
(4, 6,  '小兔子闭上眼睛，深呼吸。它感觉到了温暖，像被什么包围着。', '🐰', '#f9eccd', '✨', 6, 0),
(4, 7,  '睁开眼，它发现自己已经站在了一个软软的云朵房子前面。门是开着的。', '🐰', '#fff0da', '🏠', 7, 0),
(4, 8,  '"我找到家了！"小兔子跳了进去。原来，家一直在心里，只是需要闭上眼睛才能看见。', '🐰', '#f9eccd', '⭐', 8, 0);

-- ============================================================
-- 互动提问点（根据故事内容生成的AI提问）
-- ============================================================

-- 《小熊和最勇敢的风》互动提问
INSERT INTO biz_interaction_point (page_id, book_id, interaction_type, question, options, feedback, encourage, guide, sort_order, del_flag) VALUES
-- page_id 通过子查询绑定到对应页；这里用固定ID（H2自增从1开始）
(3, 1, 'open', '你觉得小熊会怎么做？', '["鼓起勇气，迎着风往前走","先回家找妈妈商量","我有一个不一样的想法"]', '每一种选择都很勇敢，小芽想听听你的理由。', '你的想法真棒！', '引导孩子表达自己的选择和理由', 1, 0),
(5, 1, 'imagine', '如果你是小熊，张开双臂时你会感受到什么？', '["暖暖的风","凉凉的雨","什么也没有"]', '风有时候暖暖的，有时候凉凉的，就像心情一样。', '你的想象力真丰富！', '鼓励孩子用感官描述感受', 1, 0),
(9, 1, 'judge', '小熊帮小鸟挡住了风，这是勇敢吗？', '["是的，因为帮助别人需要勇气","不是，这只是本能","我觉得不一定"]', '帮助别人确实需要勇气，特别是当你自己也有点害怕的时候。', '你说得真好！', '引导孩子理解勇敢的多种形式', 1, 0),
(12, 1, 'open', '如果你是小熊，你会怎么和妈妈说今天的故事？', '["大声地说出来","画一幅画给妈妈看","悄悄地告诉妈妈"]', '分享故事的方式有很多种，你最喜欢哪一种呢？', '你的表达方式真独特！', '鼓励孩子用自己的方式分享经历', 1, 0);

-- 《云朵鲸鱼的心情》互动提问
INSERT INTO biz_interaction_point (page_id, book_id, interaction_type, question, options, feedback, encourage, guide, sort_order, del_flag) VALUES
(3, 2, 'open', '云朵鲸鱼为什么不开心呢？你有没有过这样的感觉？', '["有时候就是会难过","因为它太大了，没人陪","我不知道"]', '难过的时候，说出来就好了。小芽陪你一起。', '谢谢你愿意分享！', '引导孩子识别和表达情绪', 1, 0),
(6, 2, 'imagine', '如果你是云朵鲸鱼的眼泪，你会落在哪里？', '["干干的土地上","大海里","花园里"]', '眼泪落在哪里，哪里就会开出花来。', '好美的想象！', '鼓励孩子用比喻表达情感', 1, 0),
(10, 2, 'open', '难过的心情可以变成好事吗？你能想到一个例子吗？', '["可以，比如...","好像不太能","我没想过"]', '每一朵乌云都镶着银边，难过之后总会有好事发生。', '你的想法很特别！', '引导孩子正面理解负面情绪', 1, 0);

-- 《月亮邮差的礼物》互动提问
INSERT INTO biz_interaction_point (page_id, book_id, interaction_type, question, options, feedback, encourage, guide, sort_order, del_flag) VALUES
(3, 3, 'open', '如果你能给月亮邮差写一封信，你会写什么？', '["我想要一个朋友","我想去看看月亮","我有一个秘密"]', '月亮邮差会认真读每一封信的。', '你的信一定很特别！', '鼓励孩子表达内心愿望', 1, 0),
(10, 3, 'imagine', '星星果实开口说话了，你觉得它说的第一句话是什么？', '["你好呀！","我终于长出来了！","我们一起玩吧！"]', '每一个新朋友的第一句话，都值得记住。', '你的想象真有趣！', '引导孩子想象初次见面的对话', 1, 0),
(15, 3, 'open', '如果每个孩子都有一颗星星种子，世界会变成什么样？', '["到处都是光","天上和地上一样亮","会有很多朋友"]', '分享让世界更亮，你也是一颗小星星。', '你的世界真美好！', '引导孩子理解分享的力量', 1, 0);

-- 《藏在云朵里的小房子》互动提问
INSERT INTO biz_interaction_point (page_id, book_id, interaction_type, question, options, feedback, encourage, guide, sort_order, del_flag) VALUES
(5, 4, 'open', '小兔子要怎样才能找到云朵里的家？', '["闭上眼睛用心感受","一直往前走","问别人"]', '有时候，用心比用眼睛看得更清楚。', '你体会到了！', '引导孩子理解内在感受', 1, 0),
(8, 4, 'imagine', '如果你闭上眼睛，你的家是什么样子的？', '["暖暖的、有妈妈的味道","有好多玩具","我还没想过"]', '家不只是房子，更是心里的温暖。', '你的家一定很温暖！', '鼓励孩子描述对家的感受', 1, 0);

-- ============================================================
-- 声音克隆预置 demo 数据
-- ============================================================
INSERT INTO biz_voice_clone (family_id, voice_name, sample_audio_url, status, voice_id, create_by) VALUES
(2, '妈妈的声音', '/uploads/voice_clone/demo_mom_voice.wav', 'success', 'clone_demo_mom_001', 'demo');
