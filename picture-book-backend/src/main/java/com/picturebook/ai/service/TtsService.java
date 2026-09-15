package com.picturebook.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 语音合成服务 TTS（M30）
 * - 多角色音色输出（10+音色）
 * - 流式合成（首字延迟 ≤ 300ms）
 * - 风格切换（温柔/活泼/沉稳）
 * - 离线缓存与预加载下一页
 *
 * 当前为 Mock 实现，等 M14 模型配置接入真实服务商后切换。
 *
 * @author Phase2
 */
public interface TtsService {

    /**
     * 同步合成：返回音频URL
     * @param text 待合成文本
     * @param voiceId 音色ID（如 gentle_female / cheerful_male / calm_male）
     * @param style 风格（gentle / lively / calm）
     * @return {audioUrl, durationMs, voiceId, cached, provider}
     */
    Map<String, Object> synthesize(String text, String voiceId, String style);

    /**
     * 按角色ID合成（解析角色音色映射后调用 synthesize）
     * @param roleId 角色ID
     * @param text 待合成文本
     * @param style 风格
     */
    Map<String, Object> synthesizeByRole(Long roleId, String text, String style);

    /**
     * 流式合成：分块返回音频段URL列表（Mock 仅返回一段）
     */
    List<Map<String, Object>> streamSynthesize(String text, String voiceId, String style);

    /**
     * 预加载下一页音频（讲读流程调用）
     */
    Map<String, Object> preloadNextPage(Long taskId, String text, String voiceId);

    /**
     * 获取所有支持的音色列表
     */
    List<Map<String, Object>> listVoices();

    /**
     * 从缓存读取音频（命中返回URL，未命中返回null）
     */
    String getFromCache(String cacheKey);

    /**
     * 写入缓存
     */
    void putToCache(String cacheKey, String audioUrl, long ttlSeconds);
}
