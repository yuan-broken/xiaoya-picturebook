package com.picturebook.user.controller;

import com.picturebook.common.core.Result;
import com.picturebook.user.domain.Badge;
import com.picturebook.user.domain.Favorite;
import com.picturebook.user.domain.HabitCheckin;
import com.picturebook.user.domain.UserBadge;
import com.picturebook.user.service.HabitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 激励与习惯 Controller（用户端）
 * 对应 PRD M36：阅读打卡、勋章、收藏夹。
 *
 * @author Phase2
 */
@RestController
@RequestMapping("/api/habit")
public class HabitController {

    @Autowired
    private HabitService habitService;

    // ==================== 打卡 ====================

    /**
     * 阅读打卡（伴读完成时调用）
     * POST /api/habit/checkin
     * body: {childId, readingMinutes, bookId, taskId}
     */
    @PostMapping("/checkin")
    public Result<Map<String, Object>> checkin(@RequestBody Map<String, Object> body) {
        Long familyId = currentFamilyId();
        Long childId = toLong(body.get("childId"));
        Integer minutes = toInt(body.get("readingMinutes"));
        Long bookId = toLong(body.get("bookId"));
        Long taskId = toLong(body.get("taskId"));
        return Result.ok(habitService.checkin(familyId, childId, minutes, bookId, taskId));
    }

    /**
     * 查询打卡记录
     * GET /api/habit/checkin/list?childId=&days=
     */
    @GetMapping("/checkin/list")
    public Result<List<HabitCheckin>> listCheckins(@RequestParam Long childId,
                                                    @RequestParam(required = false, defaultValue = "30") Integer days) {
        return Result.ok(habitService.listCheckins(childId, days));
    }

    /**
     * 打卡统计
     * GET /api/habit/checkin/stats?childId=
     */
    @GetMapping("/checkin/stats")
    public Result<Map<String, Object>> getCheckinStats(@RequestParam Long childId) {
        return Result.ok(habitService.getCheckinStats(childId));
    }

    // ==================== 勋章 ====================

    /**
     * 全部勋章定义
     * GET /api/habit/badge/list
     */
    @GetMapping("/badge/list")
    public Result<List<Badge>> listBadges() {
        return Result.ok(habitService.listBadges());
    }

    /**
     * 我的勋章
     * GET /api/habit/badge/my?childId=
     */
    @GetMapping("/badge/my")
    public Result<List<UserBadge>> listMyBadges(@RequestParam Long childId) {
        return Result.ok(habitService.listUserBadges(childId));
    }

    /**
     * 手动颁发勋章（家长颁发）
     * POST /api/habit/badge/award  body: {childId, badgeCode, remark}
     */
    @PostMapping("/badge/award")
    public Result<Void> awardBadge(@RequestBody Map<String, Object> body) {
        Long familyId = currentFamilyId();
        Long childId = toLong(body.get("childId"));
        String code = (String) body.get("badgeCode");
        String remark = (String) body.get("remark");
        habitService.awardBadge(familyId, childId, code, remark);
        return Result.ok();
    }

    /**
     * 检查并颁发勋章（手动触发）
     * POST /api/habit/badge/check  body: {childId}
     */
    @PostMapping("/badge/check")
    public Result<List<String>> checkBadges(@RequestBody Map<String, Object> body) {
        Long familyId = currentFamilyId();
        Long childId = toLong(body.get("childId"));
        return Result.ok(habitService.checkAndAwardBadges(familyId, childId));
    }

    // ==================== 收藏夹 ====================

    /**
     * 添加收藏
     * POST /api/habit/favorite
     */
    @PostMapping("/favorite")
    public Result<Long> addFavorite(@RequestBody Favorite fav) {
        fav.setFamilyId(currentFamilyId());
        return Result.ok(habitService.addFavorite(fav));
    }

    /**
     * 取消收藏
     * DELETE /api/habit/favorite/{id}
     */
    @DeleteMapping("/favorite/{id}")
    public Result<Void> removeFavorite(@PathVariable Long id) {
        habitService.removeFavorite(currentFamilyId(), id);
        return Result.ok();
    }

    /**
     * 切换收藏状态
     * POST /api/habit/favorite/toggle
     */
    @PostMapping("/favorite/toggle")
    public Result<Map<String, Object>> toggleFavorite(@RequestBody Favorite fav) {
        fav.setFamilyId(currentFamilyId());
        return Result.ok(habitService.toggleFavorite(fav));
    }

    /**
     * 收藏列表
     * GET /api/habit/favorite/list?targetType=&groupName=&childId=
     */
    @GetMapping("/favorite/list")
    public Result<List<Favorite>> listFavorites(@RequestParam(required = false) Long childId,
                                                @RequestParam(required = false) String targetType,
                                                @RequestParam(required = false) String groupName) {
        return Result.ok(habitService.listFavorites(currentFamilyId(), childId, targetType, groupName));
    }

    // ==================== 工具 ====================

    private Long currentFamilyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long) {
            return (Long) auth.getDetails();
        }
        return 1L; // 兜底演示账号
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }
}
