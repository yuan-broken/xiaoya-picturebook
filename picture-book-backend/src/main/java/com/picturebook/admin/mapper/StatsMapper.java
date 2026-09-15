package com.picturebook.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计查询 Mapper（M38）
 * 聚合 biz_reading_task / biz_story_creation / biz_chat_session / biz_family / biz_book / biz_ai_role 数据
 */
@Mapper
public interface StatsMapper {

    /**
     * 今日活跃家庭数（当日发起过讲读/创作的家庭数）
     */
    @Select("SELECT COUNT(DISTINCT t.family_id) FROM biz_reading_task t WHERE CAST(t.create_time AS DATE) = CURRENT_DATE AND t.del_flag = 0")
    long countTodayActiveFamilies();

    /**
     * 今日伴读次数
     */
    @Select("SELECT COUNT(*) FROM biz_reading_task WHERE CAST(create_time AS DATE) = CURRENT_DATE AND del_flag = 0")
    long countTodayReading();

    /**
     * 今日故事创作次数
     */
    @Select("SELECT COUNT(*) FROM biz_story_creation WHERE CAST(create_time AS DATE) = CURRENT_DATE AND del_flag = 0")
    long countTodayStory();

    /**
     * 累计注册家庭
     */
    @Select("SELECT COUNT(*) FROM biz_family WHERE del_flag = 0")
    long countTotalFamilies();

    /**
     * 讲读成功率（finished/total 排除 error）
     */
    @Select("SELECT CASE WHEN COUNT(*) = 0 THEN 0 ELSE ROUND(CAST(SUM(CASE WHEN state='finished' THEN 1 ELSE 0 END) AS FLOAT) / COUNT(*) * 100, 1) END " +
            "FROM biz_reading_task WHERE state != 'error' AND del_flag = 0")
    double readingSuccessRate();

    /**
     * 平均伴读时长（分钟）
     */
    @Select("SELECT CASE WHEN COUNT(*) = 0 THEN 0 ELSE ROUND(AVG(duration_ms) / 60000.0, 1) END " +
            "FROM biz_reading_task WHERE CAST(create_time AS DATE) = CURRENT_DATE AND del_flag = 0")
    double avgReadingMinutes();

    /**
     * 热门绘本 Top5（当日）
     */
    @Select("SELECT b.title AS bookTitle, COUNT(*) AS count " +
            "FROM biz_reading_task t JOIN biz_book b ON t.book_id = b.id " +
            "WHERE CAST(t.create_time AS DATE) = CURRENT_DATE AND t.del_flag = 0 " +
            "GROUP BY b.title ORDER BY count DESC LIMIT 5")
    List<Map<String, Object>> hotBooksToday();

    /**
     * 热门角色 Top5（当日）
     */
    @Select("SELECT r.role_name AS roleName, r.role_emoji AS roleEmoji, (SELECT COUNT(*) FROM biz_story_creation s WHERE s.role_id = r.id AND CAST(s.create_time AS DATE) = CURRENT_DATE AND s.del_flag = 0) " +
            "+ (SELECT COUNT(*) FROM biz_chat_session c WHERE c.role_id = r.id AND CAST(c.create_time AS DATE) = CURRENT_DATE AND c.del_flag = 0) AS count " +
            "FROM biz_ai_role r WHERE r.del_flag = 0 AND r.enabled = 1 ORDER BY count DESC LIMIT 5")
    List<Map<String, Object>> hotRolesToday();

    /**
     * AI对话总数
     */
    @Select("SELECT COUNT(*) FROM biz_chat_message WHERE role = 'user'")
    long countChatMessages();

    /**
     * 累计伴读次数
     */
    @Select("SELECT COUNT(*) FROM biz_reading_task WHERE del_flag = 0")
    long countTotalReading();
}
