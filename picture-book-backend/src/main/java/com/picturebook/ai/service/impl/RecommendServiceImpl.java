package com.picturebook.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picturebook.ai.service.RecommendService;
import com.picturebook.book.domain.Book;
import com.picturebook.book.service.BookService;
import com.picturebook.common.constant.CommonConstants;
import com.picturebook.user.domain.ChildProfile;
import com.picturebook.user.domain.HabitCheckin;
import com.picturebook.user.mapper.ChildProfileMapper;
import com.picturebook.user.mapper.HabitCheckinMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐服务实现
 * - 算法：协同过滤（基于已读绘本主题相似度）+ 内容画像（按兴趣标签匹配）
 * - 时段策略：21:00 后推荐 睡前/情绪 主题；白天推荐 勇气/想象/动物
 * - 难度自适应：reading_level 1-3=shallow, 4-7=middle, 8-10=deep
 *
 * @author Phase2
 */
@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    private ChildProfileMapper childProfileMapper;
    @Autowired
    private HabitCheckinMapper checkinMapper;
    @Autowired
    private BookService bookService;

    /** 睡前舒缓主题 */
    private static final Set<String> NIGHT_THEMES = new HashSet<>(Arrays.asList("情绪", "情绪安抚", "睡前故事"));
    /** 白天活跃主题 */
    private static final Set<String> DAY_THEMES = new HashSet<>(Arrays.asList("勇气", "想象", "动物", "自然", "友谊"));

    @Override
    public Map<String, Object> buildProfile(Long childId) {
        ChildProfile child = childProfileMapper.selectById(childId);
        if (child == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("childId", child.getChildId());
        profile.put("nickname", child.getNickname());
        profile.put("ageGroup", child.getAgeGroup());
        profile.put("readingLevel", child.getReadingLevel());
        profile.put("interestsRaw", child.getInterests());

        // 解析兴趣标签
        List<String> interestList = parseCsv(child.getInterests());
        profile.put("interests", interestList);

        // 历史行为：最近30天打卡记录
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        List<HabitCheckin> recent = checkinMapper.selectList(new LambdaQueryWrapper<HabitCheckin>()
                .eq(HabitCheckin::getChildId, childId)
                .eq(HabitCheckin::getDelFlag, "0")
                .ge(HabitCheckin::getCheckinDate, cal.getTime()));
        profile.put("recentCheckinCount", recent.size());
        int totalMinutes = recent.stream()
                .mapToInt(c -> c.getReadingMinutes() == null ? 0 : c.getReadingMinutes())
                .sum();
        profile.put("recentMinutes", totalMinutes);

        // 已读绘本主题统计（用于画像 + 协同过滤）
        List<Long> readBookIds = recent.stream()
                .map(HabitCheckin::getBookId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        profile.put("readBookIds", readBookIds);
        profile.put("readBookCount", readBookIds.size());

        // 主题偏好统计
        Map<String, Integer> themeCount = new HashMap<>();
        for (Long bid : readBookIds) {
            try {
                Book b = bookService.getById(bid);
                if (b != null && StringUtils.hasText(b.getTheme())) {
                    themeCount.merge(b.getTheme(), 1, Integer::sum);
                }
            } catch (Exception ignore) { /* 绘本可能下架 */ }
        }
        List<Map<String, Object>> topThemes = themeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("theme", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
        profile.put("topThemes", topThemes);

        // 薄弱项：模拟计算（基于表达判定 + 阅读能力等级）
        // TODO: 接入 M26 互动统计后真实计算；目前 Mock
        List<String> weaknesses = new ArrayList<>();
        if (child.getReadingLevel() != null && child.getReadingLevel() <= 3) {
            weaknesses.add("词汇量");
        }
        if (recent.size() < 5) {
            weaknesses.add("阅读习惯");
        }
        profile.put("weaknesses", weaknesses);

        // 阅读偏好：基于时段+主题
        String pref;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 20 || hour < 6) {
            pref = "睡前舒缓";
        } else if (hour >= 14 && hour < 17) {
            pref = "午后探索";
        } else {
            pref = "日常学习";
        }
        profile.put("preference", pref);

        return profile;
    }

    @Override
    public List<Map<String, Object>> recommendBooks(Long childId, Integer limit) {
        if (limit == null || limit <= 0) limit = 6;
        Map<String, Object> profile = buildProfile(childId);
        if (profile.isEmpty()) {
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        List<Long> readBookIds = (List<Long>) profile.getOrDefault("readBookIds", Collections.emptyList());
        Set<Long> readSet = new HashSet<>(readBookIds);
        @SuppressWarnings("unchecked")
        List<String> interests = (List<String>) profile.getOrDefault("interests", Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topThemes = (List<Map<String, Object>>) profile.getOrDefault("topThemes", Collections.emptyList());

        // 全部已上架绘本
        List<Book> allBooks = bookService.list(new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, CommonConstants.STATUS_ENABLED)
                .eq(Book::getAuditStatus, CommonConstants.AUDIT_PASS));
        if (CollectionUtils.isEmpty(allBooks)) {
            return Collections.emptyList();
        }

        // 评分：兴趣匹配 + 主题热度 + 评分 + 年龄匹配
        String ageGroup = (String) profile.get("ageGroup");
        List<ScoredBook> scored = new ArrayList<>();
        for (Book b : allBooks) {
            if (readSet.contains(b.getBookId())) continue; // 已读跳过
            double score = 0.0;
            StringBuilder reason = new StringBuilder();
            // 评分基础分
            score += (b.getRating() == null ? 5.0 : b.getRating().doubleValue()) * 2;
            // 兴趣匹配
            List<String> bookTags = parseCsv(b.getTags());
            for (String t : bookTags) {
                if (interests.contains(t)) {
                    score += 5;
                    if (reason.length() == 0) reason.append("匹配兴趣「").append(t).append("」");
                }
            }
            // 主题匹配孩子历史主题偏好
            for (Map<String, Object> tt : topThemes) {
                if (b.getTheme() != null && b.getTheme().equals(tt.get("theme"))) {
                    int cnt = (int) tt.get("count");
                    score += cnt * 3;
                    if (reason.length() == 0) reason.append("延续喜欢的「").append(b.getTheme()).append("」主题");
                    break;
                }
            }
            // 年龄段匹配
            if (ageGroup != null && ageGroup.equals(b.getAgeGroup())) {
                score += 3;
            }
            // 主题热度：睡前/白天
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if ((hour >= 20 || hour < 6) && NIGHT_THEMES.contains(b.getTheme())) {
                score += 4;
            } else if (hour >= 6 && hour < 20 && DAY_THEMES.contains(b.getTheme())) {
                score += 2;
            }
            scored.add(new ScoredBook(b, score, reason.toString()));
        }
        scored.sort(Comparator.comparingDouble(ScoredBook::getScore).reversed());
        return scored.stream().limit(limit).map(s -> toView(s)).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> todayRecommend(Long childId, Integer limit) {
        if (limit == null || limit <= 0) limit = 3;
        // 今日推荐：基于时段+兴趣+去重
        List<Map<String, Object>> recs = recommendBooks(childId, limit * 2);
        if (recs.isEmpty()) {
            return Collections.emptyList();
        }
        // 时段强化排序
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean night = hour >= 20 || hour < 6;
        recs.sort((a, b) -> {
            String ta = (String) a.getOrDefault("theme", "");
            String tb = (String) b.getOrDefault("theme", "");
            int sa = (night ? NIGHT_THEMES : DAY_THEMES).contains(ta) ? 1 : 0;
            int sb = (night ? NIGHT_THEMES : DAY_THEMES).contains(tb) ? 1 : 0;
            return Integer.compare(sb, sa);
        });
        return recs.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> difficultyAdaptation(Long childId, Long bookId) {
        Map<String, Object> r = new HashMap<>();
        ChildProfile child = childProfileMapper.selectById(childId);
        int level = child != null && child.getReadingLevel() != null ? child.getReadingLevel() : 1;
        String depth;
        String reason;
        String prompt;
        if (level <= 3) {
            depth = "shallow";
            reason = "孩子阅读能力等级 " + level + "，建议浅层讲解：词汇简单，节奏慢，多鼓励";
            prompt = "请用2-3岁能理解的词汇讲解，每个段落不超过3句，多用拟声词和重复结构。";
        } else if (level <= 7) {
            depth = "middle";
            reason = "孩子阅读能力等级 " + level + "，建议中层讲解：适当引入新词汇，互动提问中等难度";
            prompt = "请用4-6岁能理解的词汇讲解，段落可引入1-2个新词并解释，互动提问为开放式。";
        } else {
            depth = "deep";
            reason = "孩子阅读能力等级 " + level + "，建议深层讲解：可拓展主题关联与逻辑思考";
            prompt = "请用7-9岁能理解的词汇讲解，可拓展主题关联、引导孩子做逻辑推理与情感分析。";
        }
        r.put("depth", depth);
        r.put("level", level);
        r.put("reason", reason);
        r.put("suggestedPrompt", prompt);
        return r;
    }

    // ==================== 工具 ====================

    private List<String> parseCsv(String csv) {
        if (!StringUtils.hasText(csv)) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toView(ScoredBook s) {
        Book b = s.book;
        Map<String, Object> m = new HashMap<>();
        m.put("bookId", b.getBookId());
        m.put("title", b.getTitle());
        m.put("cover", b.getCoverImage());
        m.put("theme", b.getTheme());
        m.put("ageGroup", b.getAgeGroup());
        m.put("duration", b.getReadDuration() == null ? null : b.getReadDuration() + "分钟");
        m.put("rating", b.getRating());
        m.put("tags", b.getTags());
        m.put("score", Math.round(s.score * 10) / 10.0);
        m.put("reason", s.reason);
        return m;
    }

    private static class ScoredBook {
        Book book;
        double score;
        String reason;
        ScoredBook(Book b, double s, String r) { this.book = b; this.score = s; this.reason = r; }
        double getScore() { return score; }
    }
}
