package com.picturebook.admin.service;

import com.picturebook.admin.mapper.StatsMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计服务（M38）
 *
 * 提供管理后台首页（M08）与数据统计模块（M19）的统计指标：
 *   - 今日活跃家庭
 *   - 今日伴读次数
 *   - 今日故事创作
 *   - 累计注册家庭
 *   - 讲读成功率
 *   - 平均时长
 *   - 热门绘本/角色排行
 *   - AI 对话统计
 */
@Service
public class StatsService {

    private final StatsMapper statsMapper;

    public StatsService(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    /**
     * 数据概览（M08 首页）
     */
    public Map<String, Object> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("todayActive", statsMapper.countTodayActiveFamilies());
        result.put("todayReading", statsMapper.countTodayReading());
        result.put("todayStory", statsMapper.countTodayStory());
        result.put("totalRegister", statsMapper.countTotalFamilies());
        result.put("readingSuccessRate", statsMapper.readingSuccessRate());
        result.put("avgReadingMinutes", statsMapper.avgReadingMinutes());
        return result;
    }

    /**
     * 热门绘本排行
     */
    public List<Map<String, Object>> hotBooks(int limit) {
        // 当日 Top5（简化版）
        return statsMapper.hotBooksToday();
    }

    /**
     * 热门角色排行
     */
    public List<Map<String, Object>> hotRoles(int limit) {
        return statsMapper.hotRolesToday();
    }

    /**
     * 完整统计（M19 数据统计）
     */
    public Map<String, Object> fullStats() {
        Map<String, Object> result = overview();
        result.put("hotBooks", statsMapper.hotBooksToday());
        result.put("hotRoles", statsMapper.hotRolesToday());
        result.put("totalChatMessages", statsMapper.countChatMessages());
        result.put("totalReading", statsMapper.countTotalReading());
        return result;
    }
}
