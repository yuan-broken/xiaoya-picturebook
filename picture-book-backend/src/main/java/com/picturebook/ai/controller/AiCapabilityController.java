package com.picturebook.ai.controller;

import com.picturebook.ai.service.AsrService;
import com.picturebook.ai.service.TtsService;
import com.picturebook.common.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 能力（TTS/ASR）测试 Controller
 * 用于前端验证语音合成/识别功能。
 *
 * @author Phase2
 */
@RestController
@RequestMapping("/api/ai")
public class AiCapabilityController {

    @Autowired
    private TtsService ttsService;

    @Autowired
    private AsrService asrService;

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
