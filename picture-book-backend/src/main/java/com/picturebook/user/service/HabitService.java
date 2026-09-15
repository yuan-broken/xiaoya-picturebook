package com.picturebook.user.service;

import com.picturebook.user.domain.Badge;
import com.picturebook.user.domain.Favorite;
import com.picturebook.user.domain.HabitCheckin;
import com.picturebook.user.domain.UserBadge;

import java.util.List;
import java.util.Map;

/**
 * 激励与习惯服务接口
 * 对应 PRD M36：阅读打卡记录、断签提醒、勋章系统、收藏夹管理。
 *
 * @author Phase2
 */
public interface HabitService {

    // ========== 打卡 ==========

    /**
     * 阅读打卡（伴读完成事件触发或孩子主动打卡）
     */
    Map<String, Object> checkin(Long familyId, Long childId, Integer readingMinutes, Long bookId, Long taskId);

    /**
     * 查询打卡记录
     */
    List<HabitCheckin> listCheckins(Long childId, Integer days);

    /**
     * 打卡统计：连续天数、本月天数、累计天数、本月阅读分钟
     */
    Map<String, Object> getCheckinStats(Long childId);

    /**
     * 检查断签并发送提醒（定时任务调用，可由调度器触发）
     * 返回触发提醒的孩子ID列表
     */
    List<Long> checkBreakAndNotify();

    // ========== 勋章 ==========

    /**
     * 全部勋章定义
     */
    List<Badge> listBadges();

    /**
     * 用户已获勋章
     */
    List<UserBadge> listUserBadges(Long childId);

    /**
     * 检查并颁发符合条件勋章（伴读完成时由内部调用）
     * 返回本次新颁发的勋章编码列表
     */
    List<String> checkAndAwardBadges(Long familyId, Long childId);

    /**
     * 手动颁发勋章（管理员/家长颁发）
     */
    void awardBadge(Long familyId, Long childId, String badgeCode, String remark);

    // ========== 收藏夹 ==========

    /**
     * 添加收藏
     */
    Long addFavorite(Favorite favorite);

    /**
     * 取消收藏
     */
    void removeFavorite(Long familyId, Long id);

    /**
     * 收藏列表（按类型/分组过滤）
     */
    List<Favorite> listFavorites(Long familyId, Long childId, String targetType, String groupName);

    /**
     * 切换收藏状态（已收藏则取消，未收藏则添加）
     * 返回 true=已收藏 false=已取消
     */
    Map<String, Object> toggleFavorite(Favorite favorite);
}
