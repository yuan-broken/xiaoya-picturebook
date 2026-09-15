package com.picturebook.ai.service;

import java.util.Map;

/**
 * 语音识别服务 ASR（M31）
 * - 儿童语音识别（支持语料微调）
 * - 方言口音友好识别
 * - 跟读复述识别
 * - 识别结果置信度评估
 * - 重听重答降级处理
 *
 * 当前为 Mock 实现，等 M14 模型配置接入真实服务商后切换。
 *
 * @author Phase2
 */
public interface AsrService {

    /**
     * 识别音频文件，返回文本与置信度
     * @param audioUrl 音频URL
     * @param dialect 方言（zh-CN/zh-SC-四川话/zh-Cantonese 等，可空）
     * @return {text, confidence, dialect, provider, durationMs}
     */
    Map<String, Object> recognize(String audioUrl, String dialect);

    /**
     * 识别音频并判定意图匹配度（用于互动问答跟读场景）
     * @param audioUrl 音频URL
     * @param expectedText 期望文本（跟读场景）
     * @return {text, confidence, similarity, intent, dialect}
     *   intent: correct/partial/wrong/irrelevant
     */
    Map<String, Object> recognizeAndMatch(String audioUrl, String expectedText, String dialect);

    /**
     * 直接对文本做置信度评估（用于纯文本输入降级场景）
     */
    Map<String, Object> evaluateTextConfidence(String text);

    /**
     * 重听重答降级处理：返回降级建议
     */
    Map<String, Object> getFallbackStrategy(String reason);
}
