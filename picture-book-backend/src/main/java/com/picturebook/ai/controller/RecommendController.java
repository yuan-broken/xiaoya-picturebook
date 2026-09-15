package com.picturebook.ai.controller;

import com.picturebook.common.core.Result;
import com.picturebook.ai.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个性化推荐 Controller（用户端）
 * 对应 PRD M35：孩子画像、推荐绘本、今日推荐、难度自适应。
 *
 * @author Phase2
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 孩子画像
     * GET /api/recommend/profile?childId=
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(@RequestParam Long childId) {
        return Result.ok(recommendService.buildProfile(childId));
    }

    /**
     * 推荐绘本列表
     * GET /api/recommend/books?childId=&limit=
     */
    @GetMapping("/books")
    public Result<List<Map<String, Object>>> books(@RequestParam Long childId,
                                                    @RequestParam(required = false, defaultValue = "6") Integer limit) {
        return Result.ok(recommendService.recommendBooks(childId, limit));
    }

    /**
     * 今日推荐（首页推荐位）
     * GET /api/recommend/today?childId=&limit=
     */
    @GetMapping("/today")
    public Result<List<Map<String, Object>>> today(@RequestParam Long childId,
                                                    @RequestParam(required = false, defaultValue = "3") Integer limit) {
        return Result.ok(recommendService.todayRecommend(childId, limit));
    }

    /**
     * 难度自适应
     * GET /api/recommend/depth?childId=&bookId=
     */
    @GetMapping("/depth")
    public Result<Map<String, Object>> depth(@RequestParam Long childId, @RequestParam Long bookId) {
        return Result.ok(recommendService.difficultyAdaptation(childId, bookId));
    }
}
