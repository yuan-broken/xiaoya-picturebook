package com.picturebook.ai.controller;

import com.picturebook.ai.service.AsrService;
import com.picturebook.ai.service.TtsService;
import com.picturebook.common.core.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 能力（TTS/ASR）测试 Controller
 * 用于前端验证语音合成/识别功能。
 *
 * @author Phase2
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiCapabilityController {

    @Autowired
    private TtsService ttsService;

    @Autowired
    private AsrService asrService;

    @Value("${picturebook.ai.image.provider:mock}")
    private String imageProvider;

    @Value("${picturebook.ai.image.model:black-forest-labs/FLUX.1-schnell}")
    private String imageModel;

    @Value("${picturebook.ai.image.api-url:https://api.siliconflow.cn/v1}")
    private String imageApiUrl;

    @Value("${picturebook.ai.image.api-key:}")
    private String imageApiKey;

    @Value("${picturebook.ai.image.size:1024x1024}")
    private String imageSize;

    /**
     * AI 图片生成（角色头像）
     * POST /api/ai/image/generate
     * body: {prompt}
     * 优先调用硅基流动，失败则回退到 pollinations.ai（免费无需 key）
     */
    @PostMapping("/image/generate")
    public Result<Map<String, Object>> generateImage(@RequestBody Map<String, String> body) {
        String prompt = body.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            return Result.fail("提示词不能为空");
        }

        // 1. 优先调用硅基流动图片 API
        if ("dashscope".equals(imageProvider) && imageApiKey != null && !imageApiKey.isEmpty()) {
            try {
                String imageUrl = callImageApi(prompt);
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("url", imageUrl);
                    r.put("prompt", prompt);
                    return Result.ok(r);
                }
            } catch (Exception e) {
                log.warn("硅基流动图片生成失败，回退到 pollinations.ai: {}", e.getMessage());
            }
        }

        // 2. 回退到 pollinations.ai（免费，无需 key，直接返回图片 URL）
        try {
            String size = imageSize != null ? imageSize : "1024x1024";
            String[] wh = size.split("x");
            int w = wh.length >= 1 ? Integer.parseInt(wh[0]) : 1024;
            int h = wh.length >= 2 ? Integer.parseInt(wh[1]) : 1024;
            // 加随机 seed，确保每次生成不同图片（相同 prompt 不再返回缓存图）
            long seed = System.currentTimeMillis() % 1000000 + (long)(Math.random() * 1000000);
            // 直接使用前端传入的 prompt（已包含角色名称和 Q 版风格描述），不再叠加后缀避免干扰内容匹配
            String encoded = java.net.URLEncoder.encode(prompt, "UTF-8");
            String pollinationsUrl = String.format(
                "https://image.pollinations.ai/prompt/%s?width=%d&height=%d&seed=%d&nologo=true&model=flux&enhance=true",
                encoded, w, h, seed);
            Map<String, Object> r = new HashMap<>();
            r.put("url", pollinationsUrl);
            r.put("prompt", prompt);
            r.put("provider", "pollinations");
            r.put("seed", seed);
            return Result.ok(r);
        } catch (Exception e) {
            log.error("pollinations.ai 图片生成也失败，降级为占位图", e);
        }

        // 3. 最终 Mock fallback
        Map<String, Object> r = new HashMap<>();
        r.put("url", "https://api.dicebear.com/7.x/shapes/svg?seed=" + java.net.URLEncoder.encode(prompt));
        r.put("prompt", prompt);
        r.put("mock", true);
        return Result.ok(r);
    }

    /**
     * 调用硅基流动图片生成 API
     */
    private String callImageApi(String prompt) throws Exception {
        URL url = new URL(imageApiUrl + "/images/generations");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + imageApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        String body = "{\"model\":\"" + imageModel + "\","
            + "\"prompt\":\"" + escapeJson(prompt) + "\","
            + "\"image_size\":\"" + imageSize + "\"}";

        conn.getOutputStream().write(body.getBytes("UTF-8"));

        int code = conn.getResponseCode();
        String responseStr;
        if (code == 200) {
            responseStr = new String(conn.getInputStream().readAllBytes(), "UTF-8");
        } else {
            responseStr = new String(conn.getErrorStream().readAllBytes(), "UTF-8");
            log.error("图片生成 API 错误: {} - {}", code, responseStr);
            return null;
        }

        // 解析响应，提取 images[0].url
        return parseImageUrlFromResponse(responseStr);
    }

    private String parseImageUrlFromResponse(String json) {
        int idx = json.indexOf("\"url\":\"");
        if (idx < 0) return null;
        int start = idx + "\"url\":\"".length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == '"') sb.append('"');
                else if (c == '\\') sb.append('\\');
                else if (c == '/') sb.append('/');
                else sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * TTS 合成
     * POST /api/ai/tts/synthesize
     * body: {text, voiceId, style}
     */
    @PostMapping("/tts/synthesize")
    public Result<Map<String, Object>> ttsSynthesize(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String voiceId = body.get("voiceId");
        String style = body.get("style");
        return Result.ok(ttsService.synthesize(text, voiceId, style));
    }

    /**
     * 按角色合成
     * POST /api/ai/tts/synthesize-by-role
     * body: {roleId, text, style}
     */
    @PostMapping("/tts/synthesize-by-role")
    public Result<Map<String, Object>> ttsSynthesizeByRole(@RequestBody Map<String, Object> body) {
        Long roleId = body.get("roleId") == null ? null : Long.valueOf(body.get("roleId").toString());
        String text = (String) body.get("text");
        String style = (String) body.get("style");
        return Result.ok(ttsService.synthesizeByRole(roleId, text, style));
    }

    /**
     * 流式合成（Mock 返回单段）
     * POST /api/ai/tts/stream
     */
    @PostMapping("/tts/stream")
    public Result<List<Map<String, Object>>> ttsStream(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        String voiceId = body.get("voiceId");
        String style = body.get("style");
        return Result.ok(ttsService.streamSynthesize(text, voiceId, style));
    }

    /**
     * 全部音色列表
     * GET /api/ai/tts/voices
     */
    @GetMapping("/tts/voices")
    public Result<List<Map<String, Object>>> ttsVoices() {
        return Result.ok(ttsService.listVoices());
    }

    /**
     * ASR 识别
     * POST /api/ai/asr/recognize
     * body: {audioUrl, dialect}
     */
    @PostMapping("/asr/recognize")
    public Result<Map<String, Object>> asrRecognize(@RequestBody Map<String, String> body) {
        String audioUrl = body.get("audioUrl");
        String dialect = body.get("dialect");
        return Result.ok(asrService.recognize(audioUrl, dialect));
    }

    /**
     * ASR 识别 + 跟读匹配
     * POST /api/ai/asr/match
     * body: {audioUrl, expectedText, dialect}
     */
    @PostMapping("/asr/match")
    public Result<Map<String, Object>> asrMatch(@RequestBody Map<String, String> body) {
        return Result.ok(asrService.recognizeAndMatch(
                body.get("audioUrl"),
                body.get("expectedText"),
                body.get("dialect")));
    }

    /**
     * ASR 降级策略
     * GET /api/ai/asr/fallback?reason=low_confidence
     */
    @GetMapping("/asr/fallback")
    public Result<Map<String, Object>> asrFallback(@RequestParam String reason) {
        return Result.ok(asrService.getFallbackStrategy(reason));
    }

    /**
     * ASR 支持方言
     * GET /api/ai/asr/dialects
     */
    @GetMapping("/asr/dialects")
    public Result<Map<String, Object>> asrDialects() {
        // Mock dialects
        Map<String, Object> r = new HashMap<>();
        r.put("zh-CN",         "普通话");
        r.put("zh-SC",         "四川话");
        r.put("zh-Cantonese", "粤语");
        r.put("zh-SH",         "上海话");
        r.put("zh-Northeast", "东北话");
        r.put("en-US",         "美式英语");
        return Result.ok(r);
    }
}
