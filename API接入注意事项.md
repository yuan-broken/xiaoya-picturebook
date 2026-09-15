# 小芽绘本 v1.0 — API 接入注意事项（给接手 Agent 的指南）

> 本文档面向**接手 AI/API 真实接入**的 Agent 或开发者。v1.0 版本所有 AI 能力均为 Mock 占位实现，通过 `picturebook.ai.*.provider=mock` 配置开关控制。本文档逐项说明：**接入什么 / 在哪里接入 / 推荐接入什么 / 怎么接入**。

---

## 一、整体接入清单

| 模块 | 用途 | 推荐服务商（按优先级） | 后端接入位置（Service 实现） | 前端调用入口 | 配置开关 |
|---|---|---|---|---|---|
| **LLM 对话/故事生成** | reader.html 互动问答、studio.html AI 故事生成 | ① 通义千问 Qwen（DashScope）② DeepSeek ③ OpenAI GPT-4o-mini | `AiChatService.java` → `generateStoryContent()` / `generateAiReply()` | studio.html、reader.html | `picturebook.ai.llm.provider` |
| **TTS 语音合成** | reader.html 朗读故事、音色切换 | ① 火山引擎语音合成 ② 阿里云 CosyVoice ③ Azure Speech | `TtsServiceImpl.java` → `synthesize()` | reader.html | `picturebook.ai.tts.provider` |
| **ASR 语音识别** | 孩子口语回答、跟读匹配 | ① 火山引擎一句话识别 ② 阿里云一句话识别 ③ 讯飞儿童 ASR | `AsrServiceImpl.java` → `recognize()` | reader.html | `picturebook.ai.asr.provider` |
| **声音复刻** | 自定义音色（家长录声音） | ① 火山引擎声音复刻 ② Azure Custom Neural Voice | `VoiceCloneServiceImpl.java` → `triggerMockTrain()` | reader.html、parent.html | `picturebook.ai.tts.voice-clone.provider`（待新增） |
| **AI 配图** | 绘本插图生成 | ① 通义万相 ② 文心一格 ③ DALL-E 3 | `AiChatService.java` → `createStory()` 中图片生成段（当前未实现） | studio.html | `picturebook.ai.image.provider`（待新增） |
| **支付网关** | 会员充值 | ① 支付宝当面付 ② 微信 JSAPI / Native 支付 | `OrderServiceImpl.java` → `payOrder()` | upgrade.html、parent.html | `picturebook.payment.provider`（待新增） |
| **内容审核** | 故事内容安全过滤 | ① 阿里云内容安全 ② 百度文本审核 | `ContentReviewService.java` | studio.html（自动触发） | `picturebook.ai.review.provider` |

---

## 二、各模块详细接入说明

### 1. LLM 对话 / 故事生成

**接入位置**：
```
picture-book-backend/src/main/java/com/picturebook/ai/service/AiChatService.java
```

**需要替换的方法**：
- `generateStoryContent(StoryCreateRequest request)` — 故事生成（当前返回固定 6 页模板）
- `generateAiReply(String userMessage, Long roleId)` — 角色对话回复（当前返回关键词匹配回复）

**推荐接入：通义千问 Qwen（DashScope）**

配置（application.yml）：
```yaml
picturebook:
  ai:
    llm:
      provider: dashscope          # 改为非 mock
      model: qwen-max              # 或 qwen-plus / qwen-turbo
      api-url: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
      api-key: ${AI_LLM_KEY:}      # 环境变量注入，勿硬编码
      temperature: 0.7
      max-token: 2048
      enabled: true
```

接入代码示例（在 `AiChatService` 中新增 private 方法）：
```java
private String callLlm(String systemPrompt, String userMessage) {
    Map<String, Object> body = new HashMap<>();
    body.put("model", defaultModel);
    body.put("input", Map.of(
        "messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)
        )
    ));
    body.put("parameters", Map.of("temperature", temperature, "max_tokens", maxToken));
    // HTTP POST → api-url，Header: Authorization: Bearer {api-key}
    // 解析 response.output.text 返回
    return restTemplate.postForObject(apiUrl, body, String.class);
}
```

**替代方案**：
- **DeepSeek**：`https://api.deepseek.com/v1/chat/completions`，model=`deepseek-chat`，OpenAI 兼容格式
- **OpenAI GPT-4o-mini**：`https://api.openai.com/v1/chat/completions`，标准 OpenAI 格式

**故事生成 Prompt 模板建议**：
```
你是一位儿童绘本作家。请根据以下信息生成一个适合 {ageGroup} 儿童的绘本故事，共6页：
- 角色：{roleName}
- 主题输入：{storyInput}
要求：每页1-2句话，语言简单温暖，适合亲子共读，包含一个积极的道理。
输出格式：JSON {"pages":["第1页内容","第2页内容",...]}
```

---

### 2. TTS 语音合成

**接入位置**：
```
picture-book-backend/src/main/java/com/picturebook/ai/service/impl/TtsServiceImpl.java
```

**需要替换的方法**：
- `synthesize(String text, String voiceId, String style)` — 核心合成方法（当前 `mockAudioUrl()` 返回占位 URL）
- `streamSynthesize(...)` — 流式合成（当前返回单段）

**推荐接入：火山引擎语音合成**

配置（application.yml）：
```yaml
picturebook:
  ai:
    tts:
      provider: volcengine        # 改为非 mock
      stream: true
      default-voice: gentle_female
      first-token-latency-ms: 300
      enabled: true
      # 新增火山引擎专属配置
      volcengine:
        app-id: ${VOLC_TTS_APP_ID:}
        access-token: ${VOLC_TTS_TOKEN:}
        api-url: https://openspeech.bytedance.com/api/v1/tts
```

**音色映射**（关键！内置 12 个音色 ID，需映射到服务商音色）：

当前 `VOICE_REGISTRY` 已定义以下 voiceId：
| voiceId | voiceName | 说明 |
|---|---|---|
| gentle_female | 温暖姐姐 | 女声-温柔 |
| cheerful_female | 活泼姐姐 | 女声-活泼 |
| calm_male | 憨厚爷爷 | 男声-沉稳 |
| deep_male | 沉稳爸爸 | 男声-浑厚 |
| lively_boy | 调皮男孩 | 男童-活泼 |
| curious_girl | 好奇女孩 | 奸童-好奇 |
| story_mom | 故事妈妈 | 女声-讲故事 |
| animal_squirrel | 小松鼠 | 动物-小松鼠 |
| animal_bear | 憨憨熊 | 动物-熊 |
| animal_rabbit | 兔小白 | 动物-兔子 |
| robot_assistant | AI小助 | 中性-机器人 |
| narrator | 旁白 | 中性-旁白 |

接入时需在 `synthesize()` 中将本地 voiceId 映射到服务商的音色 ID（如火山引擎的 `voice_type` 参数）。

**替代方案**：
- **阿里云 CosyVoice**：`https://dashscope.aliyuncs.com/api/v1/services/audio/tts/...`，支持声音复刻
- **Azure Speech**：`https://<region>.tts.speech.microsoft.com/cognitiveservices/v1`，voice 映射到 Azure 的 `zh-CN-XiaoxiaoNeural` 等

**音频文件存储**：合成后的音频应保存到 `uploads/ai_result/` 目录（已配置 `/uploads/**` 为静态资源映射），通过 `FileStorageService` 上传后返回 URL。

---

### 3. ASR 语音识别

**接入位置**：
```
picture-book-backend/src/main/java/com/picturebook/ai/service/impl/AsrServiceImpl.java
```

**需要替换的方法**：
- `recognize(String audioUrl, String dialect)` — 语音转文字（当前返回随机 Mock 语料）
- `recognizeAndMatch(...)` — 识别 + 跟读匹配（依赖 recognize）

**推荐接入：火山引擎一句话识别**

配置（application.yml）：
```yaml
picturebook:
  ai:
    asr:
      provider: volcengine       # 改为非 mock
      child-finetune: true       # 儿童模型微调
      dialect: zh-CN
      enabled: true
      # 新增火山引擎专属配置
      volcengine:
        app-id: ${VOLC_ASR_APP_ID:}
        access-token: ${VOLC_ASR_TOKEN:}
        api-url: https://openspeech.bytedance.com/api/v1/asr
```

**接入要点**：
- 前端上传音频文件（`/api/file/upload`）→ 返回 audioUrl → 传给 `/api/ai/asr`
- 火山引擎 ASR 接口接收音频流（PCM/WAV/MP3），返回识别文本 + 置信度
- `recognizeAndMatch()` 中的相似度计算逻辑（`calcSimilarity`）已实现 LCS 算法，可保留
- `getFallbackStrategy()` 中的降级策略（重听/转文字/跳过）已实现，可保留

**替代方案**：
- **阿里云一句话识别**：`https://dashscope.aliyuncs.com/api/v1/services/audio/asr/...`
- **讯飞儿童 ASR**：`https://iat-api.xfyun.cn/v2/iat`，专门优化儿童语音

---

### 4. 声音复刻

**接入位置**：
```
picture-book-backend/src/main/java/com/picturebook/ai/service/impl/VoiceCloneServiceImpl.java
```

**需要替换的方法**：
- `triggerMockTrain(Long id)` — 当前用 `Thread.sleep(3000)` 模拟训练，需替换为真实 HTTP 调用

**推荐接入：火山引擎声音复刻**

接入流程：
1. 前端录制 10-30 秒音频 → 上传到 `/api/file/upload` → 获取 sampleAudioUrl
2. 调用 `POST /api/ai/voice/clone` → 触发 `submitClone()` → 异步训练
3. 训练完成后 `voiceId` 字段填入服务商返回的自定义音色 ID
4. TTS 合成时，voiceId=自定义音色 → 走声音复刻音色合成

**替换代码示例**（在 `triggerMockTrain` 中）：
```java
private void triggerRealTrain(Long id, String sampleAudioUrl, String voiceName) {
    new Thread(() -> {
        try {
            // 1. 调用服务商声音复刻 API
            Map<String, Object> body = Map.of(
                "app_id", appId,
                "audio_url", sampleAudioUrl,
                "voice_name", voiceName
            );
            // HTTP POST → 火山引擎声音复刻 API
            String resp = restTemplate.postForObject(cloneApiUrl, body, String.class);
            String realVoiceId = parseVoiceId(resp); // 解析返回的 voice_id

            // 2. 更新状态为 success
            VoiceClone success = new VoiceClone();
            success.setId(id);
            success.setStatus("success");
            success.setVoiceId(realVoiceId);
            voiceCloneMapper.updateById(success);
        } catch (Exception e) {
            VoiceClone failed = new VoiceClone();
            failed.setId(id);
            failed.setStatus("failed");
            failed.setFailReason(e.getMessage());
            voiceCloneMapper.updateById(failed);
        }
    }, "voice-clone-train-" + id).start();
}
```

**替代方案**：
- **Azure Custom Neural Voice**：需训练数据 ≥20 分钟音频，适合企业级
- **阿里云 CosyVoice 复刻**：仅需 3-10 秒音频，零样本复刻

---

### 5. AI 配图（绘本插图）

**接入位置**：
```
picture-book-backend/src/main/java/com/picturebook/ai/service/AiChatService.java → createStory()
```

当前 `createStory()` 只生成文本，未生成插图。需在故事文本生成后，**逐页调用图片生成 API**。

**推荐接入：通义万相**

配置（application.yml，需新增）：
```yaml
picturebook:
  ai:
    image:
      provider: wanx             # 通义万相
      api-url: https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis
      api-key: ${AI_IMAGE_KEY:}
      model: wanx-v1
      size: 1024*1024
      count: 1
```

**接入要点**：
- 通义万相是**异步接口**：提交任务 → 轮询结果 → 返回图片 URL
- 建议在 `createStory()` 中异步生成图片（开新线程），前端轮询 `GET /api/ai/story/{id}/status`
- 图片风格 Prompt 建议加：`children's book illustration, warm watercolor style, simple, child-friendly`
- 生成的图片保存到 `uploads/ai_result/` 目录

**替代方案**：
- **文心一格**：`https://aip.baidubce.com/rpc/2.0/ernievil/v1/txt2img`
- **DALL-E 3**：`https://api.openai.com/v1/images/generations`

---

### 6. 支付网关

**接入位置**：
```
picture-book-backend/src/main/java/com/picturebook/user/service/impl/OrderServiceImpl.java → payOrder()
```

当前 `payOrder()` 直接将订单状态改为已支付（Mock），需替换为真实支付流程。

**推荐接入：支付宝当面付**

配置（application.yml，需新增）：
```yaml
picturebook:
  payment:
    provider: alipay             # 或 wechat
    alipay:
      app-id: ${ALIPAY_APP_ID:}
      private-key: ${ALIPAY_PRIVATE_KEY:}
      public-key: ${ALIPAY_PUBLIC_KEY:}
      gateway: https://openapi.alipay.com/gateway.do
      notify-url: https://your-domain.com/api/order/pay/notify
      return-url: https://your-domain.com/parent.html
    wechat:
      app-id: ${WECHAT_APP_ID:}
      mch-id: ${WECHAT_MCH_ID:}
      api-key: ${WECHAT_API_KEY:}
      notify-url: https://your-domain.com/api/order/pay/notify
```

**接入流程**：
1. 前端 `upgrade.html` → 选择套餐 → `POST /api/order/create` → 创建订单，返回 orderId
2. 前端 → `POST /api/order/pay` → 后端调用支付宝下单接口 → 返回支付二维码/跳转 URL
3. 用户支付 → 支付宝异步回调 `/api/order/pay/notify` → 后端验签 → 更新订单状态为已支付
4. 前端轮询 `GET /api/order/{orderId}/status` → 返回支付状态 → 支付成功后刷新会员信息

**关键改动点**：
- `payOrder()` 改为返回支付参数（二维码 URL / 跳转链接），而非直接改状态
- 新增 `payNotify()` 回调接口处理支付结果通知（需加入安全白名单）
- `MemberOrder` 实体已有 `payType`、`transactionId`、`paidAt` 字段，可直接使用

**替代方案**：
- **微信 JSAPI / Native 支付**：适合微信内 H5 或扫码支付
- **微信 H5 支付**：适合非微信浏览器

---

### 7. 内容审核（可选）

**接入位置**：
```
picture-book-backend/src/main/java/com/picturebook/ai/service/ContentReviewService.java
```

当前 `ContentReviewService` 已有 Mock 实现。故事生成后自动触发审核。

**推荐接入：阿里云内容安全**

配置（application.yml）：
```yaml
picturebook:
  ai:
    review:
      provider: aliyun
      enabled: true
      aes-key: ${PICTUREBOOK_AES_KEY:Xiaoya2026AesKeyForChildData2026}
      aliyun:
        access-key-id: ${ALIYUN_REVIEW_AK:}
        access-key-secret: ${ALIYUN_REVIEW_SK:}
        api-url: https://green.cn-shanghai.aliyuncs.com
```

---

## 三、环境变量清单

接入真实服务后，以下环境变量必须配置（切勿硬编码到代码或 git）：

```bash
# LLM 大模型
AI_LLM_KEY=sk-xxxxxxxxxxxxxxxx          # 通义千问 / DeepSeek / OpenAI 的 API Key

# 火山引擎 TTS + ASR + 声音复刻
VOLC_TTS_APP_ID=xxx                     # TTS 应用 ID
VOLC_TTS_TOKEN=xxx                      # TTS Access Token
VOLC_ASR_APP_ID=xxx                     # ASR 应用 ID
VOLC_ASR_TOKEN=xxx                      # ASR Access Token

# AI 配图
AI_IMAGE_KEY=sk-xxxxxxxxxxxxxxxx         # 通义万相 API Key

# 支付
ALIPAY_APP_ID=20210xxxxx
ALIPAY_PRIVATE_KEY=MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQ...
ALIPAY_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...

# 微信支付（备选）
WECHAT_APP_ID=wxXXXXXXXXXXXXXXX
WECHAT_MCH_ID=1234567890
WECHAT_API_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# 内容审核
ALIYUN_REVIEW_AK=LTAI5tXXXXXXXXXXXX
ALIYUN_REVIEW_SK=XXXXXXXXXXXXXXXXXXXXXXXX

# 数据加密
PICTUREBOOK_AES_KEY=Xiaoya2026AesKeyForChildData2026
```

---

## 四、接入步骤建议

1. **先接入 LLM**（影响面最大，故事生成 + 角色对话都用）
2. **再接入 TTS**（朗读体验核心）
3. **然后接入 ASR**（孩子语音互动）
4. **再接入声音复刻**（依赖 TTS）
5. **接入 AI 配图**（故事页面插图）
6. **最后接入支付**（商业化，建议最后做，先确保功能闭环）
7. **内容审核**（可随时接入，建议与 LLM 同步上线）

---

## 五、前端调用接口对照表

| 前端页面 | 调用接口 | 用途 |
|---|---|---|
| home.html | `GET /api/user/home` | 首页数据聚合 |
| home.html | `POST /api/user/trial` | 开通7天试用 |
| home.html | `POST /api/auth/logout` | 退出登录 |
| login.html | `POST /api/auth/login` | 家长登录 |
| login.html | `POST /api/auth/register` | 家长注册 |
| child-login.html | `POST /api/auth/child/login` | 孩子登录 |
| reader.html | `POST /api/ai/tts` | 语音合成（文本→音频URL） |
| reader.html | `POST /api/ai/asr` | 语音识别（音频URL→文本） |
| reader.html | `GET /api/ai/tts/voices` | 获取音色列表 |
| reader.html | `POST /api/ai/voice/clone` | 提交声音复刻 |
| reader.html | `GET /api/ai/voice/clone/list` | 查询克隆音色列表 |
| studio.html | `POST /api/ai/story/generate` | AI 故事生成 |
| studio.html | `GET /api/ai/story/{id}/status` | 查询故事生成状态 |
| studio.html | `POST /api/ai/chat/session` | 创建对话会话 |
| studio.html | `POST /api/ai/chat/send` | 发送消息 |
| parent.html | `GET /api/user/family` | 获取家庭信息 |
| parent.html | `GET /api/user/children` | 获取孩子列表 |
| parent.html | `POST /api/user/child` | 添加孩子 |
| parent.html | `PUT /api/user/child/{id}` | 修改孩子信息 |
| parent.html | `GET /api/order/list` | 订单列表 |
| upgrade.html | `POST /api/order/create` | 创建订单 |
| upgrade.html | `POST /api/order/pay` | 支付订单 |
| upgrade.html | `GET /api/order/{id}/status` | 查询支付状态 |

---

## 六、数据库表结构对照

关键表（`schema.sql`）：

| 表名 | 用途 | AI 接入相关字段 |
|---|---|---|
| `biz_family` | 家庭账号 | `member_level`、`member_status`、`member_end_at`（会员状态） |
| `biz_child_profile` | 孩子档案 | `username`、`password`、`bind_status`（孩子独立登录+绑定） |
| `biz_member_order` | 会员订单 | `pay_type`、`transaction_id`、`paid_at`（支付回填） |
| `biz_ai_voice_clone` | 声音复刻 | `voice_id`、`status`、`sample_audio_url`（训练结果回填） |
| `biz_story_creation` | AI 故事 | `story_content`、`generation_state`、`generation_model` |
| `biz_chat_session` / `biz_chat_message` | 角色对话 | `model_name`（AI 模型标识） |

---

## 七、注意事项

1. **所有 API Key 走环境变量注入**，不要硬编码到 `application.yml` 或提交到 git。配置文件中使用 `${ENV_VAR:默认值}` 格式。
2. **TTS 音色映射是关键**：内置 12 个 voiceId 必须映射到服务商的真实音色 ID，否则前端切换音色无效。
3. **声音复刻是异步流程**：提交后需轮询状态，前端 `reader.html` 已实现轮询逻辑，后端 `triggerMockTrain` 的异步模式可保留。
4. **支付回调必须验签**：支付宝/微信的异步通知必须验证签名，防止伪造。回调接口需加入 `SecurityConfig` 白名单。
5. **LLM 故事生成建议改为异步**：当前 `createStory()` 是同步等待，真实接入后 LLM 响应需 3-10 秒，建议改为异步 + 前端轮询。
6. **AI 配图是异步接口**：通义万相/文心一格都是提交任务后轮询结果，需实现轮询逻辑。
7. **儿童数据安全**：所有涉及孩子的语音/文本数据，建议启用 `ContentReviewService` 做内容安全过滤。
8. **防沉迷限制已内置**：`application.yml` 中 `picturebook.anti-addiction` 已配置单次 30 分钟/每日 60 分钟限制，接入真实服务后依然生效。
9. **Redis 缓存已就绪**：TTS 合成结果已实现 Redis 缓存（key: `picturebook:tts:cache:{md5}`），接入真实服务后缓存逻辑可保留以降低成本。
10. **文件上传已就绪**：`/api/file/upload` 已实现，支持 multipart 上传，返回 `uploads/` 目录下的 URL，音频文件可直接复用。

---

*本文档由 v1.0 打包时生成，用于指导真实 AI/API 接入。如代码结构有变动，请同步更新本文档。*
