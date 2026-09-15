package com.picturebook.ai.service.impl;

import com.picturebook.ai.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * TTS 语音合成服务
 *
 * provider=dashscope: 调用阿里云百炼 CosyVoice（非实时语音合成）
 * provider=mock: 返回占位音频URL（降级）
 *
 * @author Phase2
 */
@Service
public class TtsServiceImpl implements TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${picturebook.ai.tts.provider:mock}")
    private String provider;

    @Value("${picturebook.ai.tts.model:cosyvoice-v1}")
    private String ttsModel;

    @Value("${picturebook.ai.tts.voice:longxiaoxia_v2}")
    private String ttsVoice;

    @Value("${picturebook.ai.tts.first-token-latency-ms:300}")
    private int firstTokenLatencyMs;

    @Value("${picturebook.ai.tts.default-voice:gentle_female}")
    private String defaultVoice;

    @Value("${picturebook.ai.llm.api-key:}")
    private String dashscopeApiKey;

    @Value("${picturebook.file.local.base-path:d:/project_work/work/picture-book-backend/uploads}")
    private String localBasePath;

    /** 音色注册表（10+音色，按 PRD M30-T002） */
    private static final Map<String, Map<String, String>> VOICE_REGISTRY = new LinkedHashMap<>();
    static {
        registerVoice("gentle_female",  "温暖姐姐",   "女声-温柔",   "#f8c7a1");
        registerVoice("cheerful_female","活泼姐姐",   "女声-活泼",   "#ef765e");
        registerVoice("calm_male",      "憨厚爷爷",   "男声-沉稳",   "#83908b");
        registerVoice("deep_male",      "沉稳爸爸",   "男声-浑厚",   "#7c9cbf");
        registerVoice("lively_boy",     "调皮男孩",   "男童-活泼",   "#edaa4f");
        registerVoice("curious_girl",   "好奇女孩",   "女童-好奇",   "#f8d6a5");
        registerVoice("story_mom",      "故事妈妈",   "女声-讲故事", "#d9b3ff");
        registerVoice("animal_squirrel","小松鼠",     "动物-小松鼠", "#c4a76e");
        registerVoice("animal_bear",    "憨憨熊",     "动物-熊",     "#a98c7b");
        registerVoice("animal_rabbit",  "兔小白",     "动物-兔子",   "#fff0da");
        registerVoice("robot_assistant","AI小助",     "中性-机器人", "#9aa29d");
        registerVoice("narrator",       "旁白",       "中性-旁白",   "#68746f");
    }

    private static void registerVoice(String id, String name, String desc, String color) {
        Map<String, String> m = new HashMap<>();
        m.put("voiceId", id);
        m.put("voiceName", name);
        m.put("voiceDesc", desc);
        m.put("color", color);
        VOICE_REGISTRY.put(id, m);
    }

    public TtsServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Map<String, Object> synthesize(String text, String voiceId, String style) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyMap();
        }
        if (!StringUtils.hasText(voiceId)) {
            voiceId = defaultVoice;
        }
        if (!StringUtils.hasText(style)) {
            style = "gentle";
        }
        String cacheKey = buildCacheKey(text, voiceId, style);
        // 命中缓存
        String cached = getFromCache(cacheKey);
        boolean isCached = cached != null;
        String audioUrl;
        String usedProvider = provider;
        if (isCached) {
            audioUrl = cached;
            log.debug("[TTS] cache hit key={} url={}", cacheKey, audioUrl);
        } else if ("dashscope".equals(provider) && dashscopeApiKey != null && !dashscopeApiKey.isEmpty()) {
            // 调用阿里云百炼 CosyVoice
            audioUrl = callDashScopeTts(text, voiceId);
            if (audioUrl == null) {
                // API 调用失败，降级为 mock
                audioUrl = mockAudioUrl(voiceId, text);
                usedProvider = "mock(fallback)";
            } else {
                putToCache(cacheKey, audioUrl, 24 * 3600);
            }
        } else {
            // Mock：返回占位音频URL
            audioUrl = mockAudioUrl(voiceId, text);
            putToCache(cacheKey, audioUrl, 24 * 3600);
        }
        Map<String, Object> r = new HashMap<>();
        r.put("audioUrl", audioUrl);
        r.put("durationMs", estimateDurationMs(text));
        r.put("voiceId", voiceId);
        r.put("voiceName", getVoiceName(voiceId));
        r.put("style", style);
        r.put("provider", usedProvider);
        r.put("cached", isCached);
        r.put("firstTokenLatencyMs", firstTokenLatencyMs);
        return r;
    }

    /**
     * 调用阿里云百炼 CosyVoice 非实时语音合成
     * 使用 DashScope RESTful API（同步模式）
     * 返回保存到本地的音频文件 URL
     */
    private String callDashScopeTts(String text, String voiceId) {
        try {
            // 映射内部音色ID到 DashScope 音色
            String dashscopeVoice = mapToDashscopeVoice(voiceId);

            URL url = new URL("https://dashscope.aliyuncs.com/api/v1/services/audio/tts");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + dashscopeApiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            // 构造请求体
            String body = "{\"model\":\"" + ttsModel + "\","
                + "\"input\":{\"text\":\"" + escapeJson(text) + "\"},"
                + "\"parameters\":{\"voice\":\"" + dashscopeVoice + "\","
                + "\"format\":\"mp3\"}}";

            conn.getOutputStream().write(body.getBytes("UTF-8"));

            int code = conn.getResponseCode();
            if (code != 200) {
                String err = new String(conn.getErrorStream().readAllBytes(), "UTF-8");
                log.error("DashScope TTS API 错误: {} - {}", code, err);
                return null;
            }

            // 读取音频二进制数据并保存到本地
            byte[] audioBytes = conn.getInputStream().readAllBytes();
            if (audioBytes.length == 0) {
                log.error("DashScope TTS 返回空音频");
                return null;
            }

            // 保存到 /uploads/ai_result/ 目录
            String fileName = "tts_" + DigestUtils.md5DigestAsHex((text + voiceId).getBytes(StandardCharsets.UTF_8)) + ".mp3";
            String dirPath = localBasePath + "/ai_result";
            Files.createDirectories(Paths.get(dirPath));
            Path filePath = Paths.get(dirPath, fileName);
            Files.write(filePath, audioBytes);

            String audioUrl = "/uploads/ai_result/" + fileName;
            log.info("[TTS] DashScope 合成成功: {} → {}", text.substring(0, Math.min(20, text.length())), audioUrl);
            return audioUrl;
        } catch (Exception e) {
            log.error("DashScope TTS 调用异常", e);
            return null;
        }
    }

    /**
     * 内部音色ID映射到 DashScope CosyVoice 音色
     */
    private String mapToDashscopeVoice(String internalVoiceId) {
        // 优先使用配置的默认音色
        if ("gentle_female".equals(internalVoiceId) || "default".equals(internalVoiceId)) {
            return ttsVoice; // longxiaoxia_v2
        }
        // 其他音色映射到 CosyVoice 可用音色
        Map<String, String> voiceMap = new HashMap<>();
        voiceMap.put("gentle_female",  "longxiaoxia_v2");
        voiceMap.put("cheerful_female","longshu_v2");
        voiceMap.put("calm_male",      "longcheng_v2");
        voiceMap.put("deep_male",      "longcheng_v2");
        voiceMap.put("lively_boy",     "longshu_v2");
        voiceMap.put("curious_girl",   "longxiaoxia_v2");
        voiceMap.put("story_mom",      "longxiaoxia_v2");
        voiceMap.put("animal_squirrel","longshu_v2");
        voiceMap.put("animal_bear",    "longcheng_v2");
        voiceMap.put("animal_rabbit",  "longxiaoxia_v2");
        voiceMap.put("robot_assistant","longcheng_v2");
        voiceMap.put("narrator",       "longcheng_v2");
        return voiceMap.getOrDefault(internalVoiceId, ttsVoice);
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public Map<String, Object> synthesizeByRole(Long roleId, String text, String style) {
        // TODO: 查询 M11 角色-音色映射（biz_ai_role.voice_config 字段）
        // Mock：按 roleId 简单哈希到某个音色
        String voiceId = mapRoleToVoice(roleId);
        return synthesize(text, voiceId, style);
    }

    @Override
    public List<Map<String, Object>> streamSynthesize(String text, String voiceId, String style) {
        // Mock：流式合成应分块返回，这里仅返回1段
        // 真实实现：分句切分，每段独立调用流式接口，首字延迟≤300ms
        List<Map<String, Object>> chunks = new ArrayList<>();
        Map<String, Object> full = synthesize(text, voiceId, style);
        Map<String, Object> chunk = new HashMap<>(full);
        chunk.put("chunkIndex", 0);
        chunk.put("chunkCount", 1);
        chunk.put("textChunk", text);
        chunks.add(chunk);
        return chunks;
    }

    @Override
    public Map<String, Object> preloadNextPage(Long taskId, String text, String voiceId) {
        // 后台异步合成下一页音频
        // Mock：同步返回结果
        Map<String, Object> r = synthesize(text, voiceId, "gentle");
        r.put("taskId", taskId);
        r.put("preloaded", true);
        return r;
    }

    @Override
    public List<Map<String, Object>> listVoices() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, String> v : VOICE_REGISTRY.values()) {
            Map<String, Object> m = new HashMap<>(v);
            result.add(m);
        }
        return result;
    }

    @Override
    public String getFromCache(String cacheKey) {
        try {
            Object v = redisTemplate.opsForValue().get(cacheKey);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            // Redis 未启动时降级
            return null;
        }
    }

    @Override
    public void putToCache(String cacheKey, String audioUrl, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(cacheKey, audioUrl, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            // Redis 未启动时降级，不报错
            log.warn("[TTS] cache write failed (redis unavailable): {}", e.getMessage());
        }
    }

    // ==================== 内部工具 ====================

    private String buildCacheKey(String text, String voiceId, String style) {
        String raw = text + "|" + voiceId + "|" + style;
        String md5 = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        return "picturebook:tts:cache:" + md5;
    }

    private String getVoiceName(String voiceId) {
        Map<String, String> v = VOICE_REGISTRY.get(voiceId);
        return v == null ? voiceId : v.get("voiceName");
    }

    private String mapRoleToVoice(Long roleId) {
        if (roleId == null) return defaultVoice;
        // 按ID 取模 映射到音色列表
        List<String> ids = new ArrayList<>(VOICE_REGISTRY.keySet());
        return ids.get((int) (roleId % ids.size()));
    }

    private String mockAudioUrl(String voiceId, String text) {
        // 占位：前端可直接用此URL播放空音频
        // 真实环境应上传至 /uploads/ai_result/ 目录
        return "/uploads/ai_result/mock_" + voiceId + ".mp3";
    }

    private int estimateDurationMs(String text) {
        // 中文 4字/秒估算；英文 2.5词/秒
        int chars = text.length();
        return Math.max(800, chars * 250);
    }
}
