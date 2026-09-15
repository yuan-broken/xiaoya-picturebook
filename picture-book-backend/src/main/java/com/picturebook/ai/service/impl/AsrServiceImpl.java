package com.picturebook.ai.service.impl;

import com.picturebook.ai.service.AsrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * ASR 语音识别服务
 *
 * provider=dashscope: 调用阿里云百炼 Qwen3-ASR-Flash
 * provider=mock: 返回固定儿童常用语（降级）
 *
 * @author Phase2
 */
@Service
public class AsrServiceImpl implements AsrService {

    private static final Logger log = LoggerFactory.getLogger(AsrServiceImpl.class);

    @Value("${picturebook.ai.asr.provider:mock}")
    private String provider;

    @Value("${picturebook.ai.asr.model:qwen3-asr-flash}")
    private String asrModel;

    @Value("${picturebook.ai.llm.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String dashscopeApiUrl;

    @Value("${picturebook.ai.llm.api-key:}")
    private String dashscopeApiKey;

    @Value("${picturebook.ai.asr.child-finetune:true}")
    private boolean childFinetune;

    /** 支持的方言列表（PRD M31-T002） */
    private static final Map<String, String> DIALECT_MAP = new HashMap<>();
    static {
        DIALECT_MAP.put("zh-CN",        "普通话");
        DIALECT_MAP.put("zh-SC",        "四川话");
        DIALECT_MAP.put("zh-Cantonese","粤语");
        DIALECT_MAP.put("zh-SH",       "上海话");
        DIALECT_MAP.put("zh-Northeast","东北话");
        DIALECT_MAP.put("en-US",       "美式英语");
    }

    /** Mock 语料库（儿童常用语） */
    private static final String[] MOCK_CORPUS = {
        "我想再听一遍",
        "这个故事真有趣",
        "我喜欢这个小动物",
        "为什么它会这样呢",
        "我也想试试",
        "妈妈我爱你",
        "今天好开心呀",
        "它去哪里了",
        "再来一次好不好",
        "我不要怕黑"
    };

    @Override
    public Map<String, Object> recognize(String audioUrl, String dialect) {
        long start = System.currentTimeMillis();
        if (!StringUtils.hasText(dialect)) {
            dialect = "zh-CN";
        }

        String text = null;
        double confidence = 0.0;
        String usedProvider = provider;

        if ("dashscope".equals(provider) && dashscopeApiKey != null && !dashscopeApiKey.isEmpty()) {
            try {
                text = callDashScopeAsr(audioUrl, dialect);
                confidence = text != null ? 0.95 : 0.0;
            } catch (Exception e) {
                log.error("DashScope ASR 调用失败，降级为 Mock", e);
                usedProvider = "mock(fallback)";
            }
        }

        // Mock fallback
        if (text == null || text.isEmpty()) {
            text = MOCK_CORPUS[(int) (Math.random() * MOCK_CORPUS.length)];
            confidence = 0.85 + Math.random() * 0.13;
            usedProvider = "mock";
        }

        Map<String, Object> r = new HashMap<>();
        r.put("text", text);
        r.put("confidence", Math.round(confidence * 100) / 100.0);
        r.put("dialect", dialect);
        r.put("dialectName", DIALECT_MAP.getOrDefault(dialect, dialect));
        r.put("provider", usedProvider);
        r.put("childFinetune", childFinetune);
        r.put("durationMs", System.currentTimeMillis() - start);
        r.put("audioUrl", audioUrl);
        return r;
    }

    /**
     * 调用阿里云百炼 Qwen3-ASR-Flash（OpenAI 兼容格式）
     * 音频文件路径或 URL → Base64 → POST /chat/completions
     */
    private String callDashScopeAsr(String audioUrl, String dialect) {
        try {
            // 获取音频 Base64
            String base64Audio = getAudioBase64(audioUrl);
            if (base64Audio == null) {
                log.error("ASR: 无法获取音频数据: {}", audioUrl);
                return null;
            }

            URL url = new URL(dashscopeApiUrl + "/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + dashscopeApiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            // 构造请求体（Qwen3-ASR OpenAI 兼容格式）
            String body = "{\"model\":\"" + asrModel + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":[{"
                + "\"type\":\"input_audio\","
                + "\"input_audio\":\"data:audio/wav;base64," + base64Audio + "\""
                + "}]}]}";

            conn.getOutputStream().write(body.getBytes("UTF-8"));

            int code = conn.getResponseCode();
            String responseStr;
            if (code == 200) {
                responseStr = new String(conn.getInputStream().readAllBytes(), "UTF-8");
            } else {
                responseStr = new String(conn.getErrorStream().readAllBytes(), "UTF-8");
                log.error("DashScope ASR API 错误: {} - {}", code, responseStr);
                return null;
            }

            // 提取 content
            return parseAsrContent(responseStr);
        } catch (Exception e) {
            log.error("DashScope ASR 调用异常", e);
            return null;
        }
    }

    /**
     * 获取音频文件的 Base64 编码
     * 支持: 本地文件路径 / http URL
     */
    private String getAudioBase64(String audioUrl) {
        try {
            byte[] audioBytes;
            if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                // 远程 URL 下载
                URL url = new URL(audioUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                audioBytes = conn.getInputStream().readAllBytes();
            } else {
                // 本地文件路径
                Path path = Paths.get(audioUrl.replaceFirst("file://", ""));
                audioBytes = Files.readAllBytes(path);
            }
            return Base64.getEncoder().encodeToString(audioBytes);
        } catch (Exception e) {
            log.error("获取音频数据失败: {}", audioUrl, e);
            return null;
        }
    }

    /**
     * 从 ASR 响应中提取识别文本
     */
    private String parseAsrContent(String json) {
        int idx = json.indexOf("\"content\":\"");
        if (idx < 0) {
            // 尝试数组格式 content
            idx = json.indexOf("\"content\":[{\"type\":\"text\",\"text\":\"");
            if (idx >= 0) {
                int start = idx + "\"content\":[{\"type\":\"text\",\"text\":\"".length();
                return extractJsonString(json, start);
            }
            return null;
        }
        int start = idx + "\"content\":\"".length();
        return extractJsonString(json, start);
    }

    /**
     * 从 JSON 字符串指定位置提取带转义的字符串值
     */
    private String extractJsonString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(c);
                }
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

    @Override
    public Map<String, Object> recognizeAndMatch(String audioUrl, String expectedText, String dialect) {
        Map<String, Object> rec = recognize(audioUrl, dialect);
        String recognized = (String) rec.get("text");
        double confidence = (double) rec.get("confidence");

        // 计算相似度（简化版：基于字符级 LCS）
        double similarity = calcSimilarity(recognized, expectedText);
        String intent;
        if (similarity >= 0.85) {
            intent = "correct";
        } else if (similarity >= 0.5) {
            intent = "partial";
        } else if (similarity >= 0.2) {
            intent = "wrong";
        } else {
            intent = "irrelevant";
        }

        Map<String, Object> r = new HashMap<>(rec);
        r.put("expectedText", expectedText);
        r.put("similarity", Math.round(similarity * 100) / 100.0);
        r.put("intent", intent);
        r.put("canRetry", confidence < 0.6); // 置信度过低建议重答
        return r;
    }

    @Override
    public Map<String, Object> evaluateTextConfidence(String text) {
        Map<String, Object> r = new HashMap<>();
        r.put("text", text);
        // 文本长度+内容判定置信度
        double conf = 0.7;
        if (text != null && text.length() >= 4) conf = 0.85;
        if (text != null && text.length() >= 8) conf = 0.92;
        r.put("confidence", conf);
        r.put("provider", "text-mode");
        return r;
    }

    @Override
    public Map<String, Object> getFallbackStrategy(String reason) {
        Map<String, Object> r = new HashMap<>();
        if (reason == null) reason = "unknown";
        switch (reason) {
            case "low_confidence":
                r.put("action", "retry_listen");
                r.put("message", "我没有听清楚，可以再说一遍吗？");
                r.put("maxRetries", 3);
                break;
            case "noise":
                r.put("action", "request_text");
                r.put("message", "环境有点吵，要不要用文字告诉我？");
                break;
            case "timeout":
                r.put("action", "skip");
                r.put("message", "没关系，我们继续往下看吧");
                break;
            default:
                r.put("action", "retry_listen");
                r.put("message", "我们再试一次好吗？");
                r.put("maxRetries", 2);
        }
        return r;
    }

    /** 支持方言列表查询 */
    public Map<String, String> getDialectMap() {
        return DIALECT_MAP;
    }

    // ==================== 工具 ====================

    /** 字符级相似度（基于最长公共子序列） */
    private double calcSimilarity(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        int lcs = dp[a.length()][b.length()];
        return (2.0 * lcs) / (a.length() + b.length());
    }
}
