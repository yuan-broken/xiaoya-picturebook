package com.picturebook.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 个性化推荐服务
 * 对应 PRD M35：孩子画像、协同过滤+内容画像、今日推荐、难度自适应。
 *
 * @author Phase2
 */
public interface RecommendService {

    /**
     * 构建孩子画像（聚合档案+行为历史）
     * 字段：ageGroup, interests, readingLevel, weaknesses, preference, historyCount, topThemes
     */
    Map<String, Object> buildProfile(Long childId);

    /**
     * 推荐绘本（混合：协同过滤 + 内容画像）
     * @param childId 孩子ID
     * @param limit 数量上限
     * @return 推荐绘本列表，每项含 bookId/title/cover/theme/ageGroup/duration/score/reason
     */
    List<Map<String, Object>> recommendBooks(Long childId, Integer limit);

    /**
     * 今日推荐（带时段策略：睡前推荐舒缓，白天推荐活跃）
     */
    List<Map<String, Object>> todayRecommend(Long childId, Integer limit);

    /**
     * 难度自适应：同绘本按孩子能力返回讲解深度
     * @return {depth: shallow/middle/deep, reason, suggestedPrompt}
     */
    Map<String, Object> difficultyAdaptation(Long childId, Long bookId);
}
