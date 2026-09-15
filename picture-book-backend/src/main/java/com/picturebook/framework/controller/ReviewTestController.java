package com.picturebook.framework.controller;

import com.picturebook.ai.service.ContentReviewService;
import com.picturebook.common.core.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 内容审核测试接口（M37 验证用，可删除）
 */
@RestController
@RequestMapping("/api/test/review")
public class ReviewTestController {

    private final ContentReviewService reviewService;

    public ReviewTestController(ContentReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 测试文本审核
     * GET /api/test/review/text?content=xxx
     */
    @GetMapping("/text")
    public Result<Map<String, Object>> testText(@RequestParam String content) {
        ContentReviewService.ReviewResult r = reviewService.reviewText(content);
        Map<String, Object> data = new HashMap<>();
        data.put("pass", r.isPass());
        data.put("reason", r.getReason());
        data.put("hitKeywords", r.getHitKeywords());
        data.put("input", content);
        return Result.ok(data);
    }

    /**
     * 测试图片审核
     */
    @GetMapping("/image")
    public Result<Map<String, Object>> testImage(@RequestParam String url) {
        ContentReviewService.ReviewResult r = reviewService.reviewImage(url);
        Map<String, Object> data = new HashMap<>();
        data.put("pass", r.isPass());
        data.put("reason", r.getReason());
        return Result.ok(data);
    }
}
