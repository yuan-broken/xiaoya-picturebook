package com.picturebook.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picturebook.common.exception.BusinessException;
import com.picturebook.user.domain.Badge;
import com.picturebook.user.domain.Favorite;
import com.picturebook.user.domain.HabitCheckin;
import com.picturebook.user.domain.UserBadge;
import com.picturebook.user.mapper.BadgeMapper;
import com.picturebook.user.mapper.FavoriteMapper;
import com.picturebook.user.mapper.HabitCheckinMapper;
import com.picturebook.user.mapper.UserBadgeMapper;
import com.picturebook.user.service.HabitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 激励与习惯服务实现
 *
 * @author Phase2
 */
@Service
public class HabitServiceImpl implements HabitService {

    private static final Logger log = LoggerFactory.getLogger(HabitServiceImpl.class);

    @Autowired
    private HabitCheckinMapper checkinMapper;
    @Autowired
    private BadgeMapper badgeMapper;
    @Autowired
    private UserBadgeMapper userBadgeMapper;
    @Autowired
    private FavoriteMapper favoriteMapper;

    // ==================== 打卡 ====================

    @Override
    public Map<String, Object> checkin(Long familyId, Long childId, Integer readingMinutes, Long bookId, Long taskId) {
        if (familyId == null || childId == null) {
            throw new BusinessException("familyId/childId 不能为空");
        }
        Date today = stripTime(new Date());
        // 当日是否已打卡
        HabitCheckin exist = checkinMapper.selectOne(new LambdaQueryWrapper<HabitCheckin>()
                .eq(HabitCheckin::getChildId, childId)
                .eq(HabitCheckin::getCheckinDate, today)
                .eq(HabitCheckin::getDelFlag, "0"));
        if (exist != null) {
            // 已打卡，累加分钟
            int newMinutes = (exist.getReadingMinutes() == null ? 0 : exist.getReadingMinutes())
                    + (readingMinutes == null ? 0 : readingMinutes);
            exist.setReadingMinutes(newMinutes);
            exist.setBookId(bookId != null ? bookId : exist.getBookId());
            exist.setTaskId(taskId != null ? taskId : exist.getTaskId());
            exist.setUpdateTime(new Date());
            checkinMapper.updateById(exist);
            Map<String, Object> r = new HashMap<>();
            r.put("id", exist.getId());
            r.put("continuousDays", exist.getContinuousDays());
            r.put("todayMinutes", newMinutes);
            r.put("message", "今日已打卡，已累计阅读 " + newMinutes + " 分钟");
            return r;
        }
        // 计算连续天数：查询昨天是否打卡
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.add(Calendar.DATE, -1);
        Date yesterday = cal.getTime();
        HabitCheckin yesterdayRec = checkinMapper.selectOne(new LambdaQueryWrapper<HabitCheckin>()
                .eq(HabitCheckin::getChildId, childId)
                .eq(HabitCheckin::getCheckinDate, yesterday)
                .eq(HabitCheckin::getDelFlag, "0"));
        int continuousDays = yesterdayRec != null && yesterdayRec.getContinuousDays() != null
                ? yesterdayRec.getContinuousDays() + 1 : 1;

        // 插入
        HabitCheckin rec = new HabitCheckin();
        rec.setFamilyId(familyId);
        rec.setChildId(childId);
        rec.setCheckinDate(today);
        rec.setReadingMinutes(readingMinutes == null ? 0 : readingMinutes);
        rec.setBookId(bookId);
        rec.setTaskId(taskId);
        rec.setContinuousDays(continuousDays);
        rec.setDelFlag("0");
        rec.setCreateBy(String.valueOf(familyId));
        rec.setCreateTime(new Date());
        checkinMapper.insert(rec);

        // 触发勋章检查
        List<String> awarded = checkAndAwardBadges(familyId, childId);

        Map<String, Object> r = new HashMap<>();
        r.put("id", rec.getId());
        r.put("continuousDays", continuousDays);
        r.put("todayMinutes", rec.getReadingMinutes());
        r.put("newBadges", awarded);
        r.put("message", "打卡成功，已连续 " + continuousDays + " 天");
        return r;
    }

    @Override
    public List<HabitCheckin> listCheckins(Long childId, Integer days) {
        if (days == null || days <= 0) {
            days = 30;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -days);
        return checkinMapper.selectList(new LambdaQueryWrapper<HabitCheckin>()
                .eq(HabitCheckin::getChildId, childId)
                .eq(HabitCheckin::getDelFlag, "0")
                .ge(HabitCheckin::getCheckinDate, cal.getTime())
                .orderByDesc(HabitCheckin::getCheckinDate));
    }

    @Override
    public Map<String, Object> getCheckinStats(Long childId) {
        List<HabitCheckin> all = listCheckins(childId, 365);
        Map<String, Object> r = new HashMap<>();
        if (CollectionUtils.isEmpty(all)) {
            r.put("continuousDays", 0);
            r.put("totalDays", 0);
            r.put("monthDays", 0);
            r.put("monthMinutes", 0);
            r.put("todayChecked", false);
            return r;
        }
        // 今日是否打卡
        Date today = stripTime(new Date());
        boolean todayChecked = all.stream().anyMatch(c -> today.equals(stripTime(c.getCheckinDate())));
        // 最新记录的连续天数
        int continuousDays = all.get(0).getContinuousDays() == null ? 0 : all.get(0).getContinuousDays();
        // 如果今日未打卡，连续天数应该是基于昨日
        if (!todayChecked && all.size() >= 2) {
            // 检查昨日是否打卡
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            Date yesterday = stripTime(cal.getTime());
            boolean yesterdayChecked = all.stream().anyMatch(c -> yesterday.equals(stripTime(c.getCheckinDate())));
            if (!yesterdayChecked) {
                continuousDays = 0; // 断签
            } else {
                // 取昨日记录的连续天数
                continuousDays = all.stream()
                        .filter(c -> yesterday.equals(stripTime(c.getCheckinDate())))
                        .findFirst()
                        .map(HabitCheckin::getContinuousDays)
                        .orElse(0);
            }
        }
        // 本月打卡
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        List<HabitCheckin> monthList = all.stream()
                .filter(c -> !c.getCheckinDate().before(monthStart.getTime()))
                .collect(Collectors.toList());
        int monthMinutes = monthList.stream()
                .mapToInt(c -> c.getReadingMinutes() == null ? 0 : c.getReadingMinutes())
                .sum();
        r.put("continuousDays", continuousDays);
        r.put("totalDays", all.size());
        r.put("monthDays", monthList.size());
        r.put("monthMinutes", monthMinutes);
        r.put("todayChecked", todayChecked);
        r.put("recentList", all.stream().limit(30).collect(Collectors.toList()));
        return r;
    }

    @Override
    public List<Long> checkBreakAndNotify() {
        // 简化版：扫描所有孩子，若最近一次打卡非今日非昨日，则视为断签
        // 实际生产应做分页扫描 + 异步发送通知
        log.info("[HabitService] checkBreakAndNotify tick at {}", new Date());
        // TODO: 接入家长通知通道（站内信/微信/短信），目前仅打日志
        return Collections.emptyList();
    }

    // ==================== 勋章 ====================

    @Override
    public List<Badge> listBadges() {
        return badgeMapper.selectList(new LambdaQueryWrapper<Badge>()
                .eq(Badge::getEnabled, 1)
                .eq(Badge::getDelFlag, "0")
                .orderByAsc(Badge::getSortOrder));
    }

    @Override
    public List<UserBadge> listUserBadges(Long childId) {
        return userBadgeMapper.selectList(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getChildId, childId)
                .orderByDesc(UserBadge::getAwardedAt));
    }

    @Override
    public List<String> checkAndAwardBadges(Long familyId, Long childId) {
        List<Badge> badges = listBadges();
        List<UserBadge> owned = listUserBadges(childId);
        Set<String> ownedCodes = owned.stream().map(UserBadge::getBadgeCode).collect(Collectors.toSet());

        // 计算当前指标
        Map<String, Object> stats = getCheckinStats(childId);
        int continuousDays = (int) stats.getOrDefault("continuousDays", 0);
        int totalDays = (int) stats.getOrDefault("totalDays", 0);
        // 完成绘本数：用打卡记录中不同 bookId 数量估算（正式应来自 M23）
        List<HabitCheckin> allCheckins = listCheckins(childId, 3650);
        long readingCount = allCheckins.stream()
                .filter(c -> c.getBookId() != null)
                .map(HabitCheckin::getBookId)
                .distinct()
                .count();

        List<String> newBadges = new ArrayList<>();
        for (Badge b : badges) {
            if (ownedCodes.contains(b.getBadgeCode())) continue;
            boolean meet = false;
            if ("reading_count".equals(extractRuleType(b.getRuleJson()))) {
                int threshold = extractRuleThreshold(b.getRuleJson(), 1);
                meet = readingCount >= threshold;
            } else if ("streak_days".equals(extractRuleType(b.getRuleJson()))) {
                int threshold = extractRuleThreshold(b.getRuleJson(), 1);
                meet = continuousDays >= threshold;
            } else if ("creation_count".equals(extractRuleType(b.getRuleJson()))) {
                // TODO: 接入 M27 故事创作数；Mock 0
                meet = false;
            } else if ("expression_rate".equals(extractRuleType(b.getRuleJson()))) {
                // TODO: 接入 M26 互动统计；Mock false
                meet = false;
            } else if ("expression_excellent".equals(extractRuleType(b.getRuleJson()))) {
                // TODO
                meet = false;
            } else if ("thinking_deep".equals(extractRuleType(b.getRuleJson()))) {
                // TODO
                meet = false;
            }
            if (meet) {
                doAward(familyId, childId, b.getBadgeCode(), "auto", "达成条件自动颁发");
                newBadges.add(b.getBadgeCode());
                log.info("[HabitService] child={} awarded badge={}", childId, b.getBadgeCode());
            }
        }
        return newBadges;
    }

    @Override
    public void awardBadge(Long familyId, Long childId, String badgeCode, String remark) {
        // 校验勋章存在
        Badge badge = badgeMapper.selectOne(new LambdaQueryWrapper<Badge>()
                .eq(Badge::getBadgeCode, badgeCode)
                .eq(Badge::getDelFlag, "0"));
        if (badge == null) {
            throw new BusinessException("勋章编码不存在: " + badgeCode);
        }
        doAward(familyId, childId, badgeCode, "manual", remark);
    }

    private void doAward(Long familyId, Long childId, String badgeCode, String source, String remark) {
        // 防重复
        Long exist = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getChildId, childId)
                .eq(UserBadge::getBadgeCode, badgeCode));
        if (exist != null && exist > 0) return;
        UserBadge ub = new UserBadge();
        ub.setFamilyId(familyId);
        ub.setChildId(childId);
        ub.setBadgeCode(badgeCode);
        ub.setAwardedAt(new Date());
        ub.setSource(source);
        ub.setRemark(remark);
        ub.setCreateBy(String.valueOf(familyId));
        ub.setCreateTime(new Date());
        userBadgeMapper.insert(ub);
    }

    private String extractRuleType(String ruleJson) {
        if (!StringUtils.hasText(ruleJson)) return "";
        // 简单解析 {"type":"xxx","threshold":N}
        int idx = ruleJson.indexOf("\"type\"");
        if (idx < 0) return "";
        int start = ruleJson.indexOf(':', idx) + 1;
        // 找到引号包围的字符串
        int s = ruleJson.indexOf('"', start);
        int e = ruleJson.indexOf('"', s + 1);
        if (s < 0 || e < 0) return "";
        return ruleJson.substring(s + 1, e);
    }

    private int extractRuleThreshold(String ruleJson, int defaultVal) {
        if (!StringUtils.hasText(ruleJson)) return defaultVal;
        int idx = ruleJson.indexOf("\"threshold\"");
        if (idx < 0) return defaultVal;
        int start = ruleJson.indexOf(':', idx) + 1;
        // 找到数字
        int s = start;
        while (s < ruleJson.length() && !Character.isDigit(ruleJson.charAt(s))) s++;
        int e = s;
        while (e < ruleJson.length() && Character.isDigit(ruleJson.charAt(e))) e++;
        if (s >= e) return defaultVal;
        try {
            return Integer.parseInt(ruleJson.substring(s, e));
        } catch (Exception ex) {
            return defaultVal;
        }
    }

    // ==================== 收藏夹 ====================

    @Override
    public Long addFavorite(Favorite favorite) {
        if (favorite.getFamilyId() == null || favorite.getTargetType() == null || favorite.getTargetId() == null) {
            throw new BusinessException("familyId/targetType/targetId 不能为空");
        }
        // 唯一性
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getFamilyId, favorite.getFamilyId())
                .eq(Favorite::getTargetType, favorite.getTargetType())
                .eq(Favorite::getTargetId, favorite.getTargetId())
                .eq(Favorite::getDelFlag, "0"));
        if (exist != null) {
            return exist.getId(); // 已收藏，幂等返回
        }
        if (!StringUtils.hasText(favorite.getGroupName())) {
            favorite.setGroupName("默认");
        }
        favorite.setDelFlag("0");
        favorite.setCreateBy(String.valueOf(favorite.getFamilyId()));
        favorite.setCreateTime(new Date());
        favoriteMapper.insert(favorite);
        return favorite.getId();
    }

    @Override
    public void removeFavorite(Long familyId, Long id) {
        Favorite exist = favoriteMapper.selectById(id);
        if (exist == null) throw new BusinessException("收藏记录不存在");
        if (!exist.getFamilyId().equals(familyId)) throw new BusinessException("无权操作他人收藏");
        Favorite up = new Favorite();
        up.setId(id);
        up.setDelFlag("1");
        up.setUpdateTime(new Date());
        favoriteMapper.updateById(up);
    }

    @Override
    public List<Favorite> listFavorites(Long familyId, Long childId, String targetType, String groupName) {
        LambdaQueryWrapper<Favorite> w = new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getFamilyId, familyId)
                .eq(Favorite::getDelFlag, "0");
        if (childId != null) w.eq(Favorite::getChildId, childId);
        if (StringUtils.hasText(targetType)) w.eq(Favorite::getTargetType, targetType);
        if (StringUtils.hasText(groupName)) w.eq(Favorite::getGroupName, groupName);
        w.orderByDesc(Favorite::getCreateTime);
        return favoriteMapper.selectList(w);
    }

    @Override
    public Map<String, Object> toggleFavorite(Favorite favorite) {
        Favorite exist = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getFamilyId, favorite.getFamilyId())
                .eq(Favorite::getTargetType, favorite.getTargetType())
                .eq(Favorite::getTargetId, favorite.getTargetId())
                .eq(Favorite::getDelFlag, "0"));
        Map<String, Object> r = new HashMap<>();
        if (exist != null) {
            removeFavorite(favorite.getFamilyId(), exist.getId());
            r.put("favorite", false);
            r.put("message", "已取消收藏");
        } else {
            Long id = addFavorite(favorite);
            r.put("favorite", true);
            r.put("id", id);
            r.put("message", "已加入收藏");
        }
        return r;
    }

    // ==================== 工具 ====================

    private Date stripTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
