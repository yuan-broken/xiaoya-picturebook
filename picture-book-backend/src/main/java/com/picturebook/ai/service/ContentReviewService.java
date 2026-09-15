package com.picturebook.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 内容审核服务（M37）
 *
 * - 关键词过滤
 * - AI 审核占位（Mock）
 * - 不合规拦截与记录
 *
 * 隐私保护：儿童数据加密存储由 AesUtil 负责
 */
@Service
public class ContentReviewService {

    private static final Logger log = LoggerFactory.getLogger(ContentReviewService.class);

    /**
     * 敏感词库（第一期硬编码，后续可从数据库加载）
     */
    private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
            // 暴力
            "杀", "打", "血", "枪", "刀", "死", "暴力",
            // 色情
            "色情", "裸体", "性",
            // 赌博
            "赌博", "彩票", "下注",
            // 毒品
            "毒品", "吸毒",
            // 其他
            "诈骗", "传销"
    );

    /**
     * 文本审核
     *
     * @param content 待审核文本
     * @return 审核结果
     */
    public ReviewResult reviewText(String content) {
        if (content == null || content.isBlank()) {
            return ReviewResult.pass("空内容", null);
        }
        // 1. 关键词过滤
        List<String> hits = SENSITIVE_KEYWORDS.stream()
                .filter(content::contains)
                .toList();
        if (!hits.isEmpty()) {
            return ReviewResult.block("命中敏感词: " + String.join(",", hits), hits);
        }
        // 2. TODO: AI 审核调用（等 M14 模型配置就绪后切换）
        // 当前 Mock：所有通过关键词过滤的内容默认通过
        return ReviewResult.pass("AI审核通过(Mock)", null);
    }

    /**
     * 图片审核（Mock）
     */
    public ReviewResult reviewImage(String url) {
        // TODO: 接入 VLM 视觉模型审核
        return ReviewResult.pass("图片审核通过(Mock)", null);
    }

    /**
     * 审核结果
     */
    public static class ReviewResult {
        private boolean pass;
        private String reason;
        private List<String> hitKeywords;

        public static ReviewResult pass(String reason, List<String> hits) {
            ReviewResult r = new ReviewResult();
            r.pass = true;
            r.reason = reason;
            r.hitKeywords = hits;
            return r;
        }

        public static ReviewResult block(String reason, List<String> hits) {
            ReviewResult r = new ReviewResult();
            r.pass = false;
            r.reason = reason;
            r.hitKeywords = hits;
            return r;
        }

        public boolean isPass() { return pass; }
        public String getReason() { return reason; }
        public List<String> getHitKeywords() { return hitKeywords; }
    }
}
