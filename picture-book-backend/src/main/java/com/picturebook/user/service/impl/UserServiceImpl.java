package com.picturebook.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picturebook.common.exception.BusinessException;
import com.picturebook.common.utils.JwtUtil;
import com.picturebook.common.utils.SecurityUtil;
import com.picturebook.user.domain.ChildProfile;
import com.picturebook.user.domain.Family;
import com.picturebook.user.mapper.ChildProfileMapper;
import com.picturebook.user.mapper.FamilyMapper;
import com.picturebook.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户与家长服务实现
 *
 * @author Agent-4
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private FamilyMapper familyMapper;

    @Autowired
    private ChildProfileMapper childProfileMapper;

    @Value("${picturebook.jwt.secret}")
    private String jwtSecret;

    @Value("${picturebook.jwt.expire}")
    private long jwtExpireSeconds;

    // 免费家庭最多 1 个孩子，成长版 3 个（PRD 会员权益）
    private static final int FREE_CHILD_LIMIT = 1;
    private static final int GROWTH_CHILD_LIMIT = 3;

    // 免费家庭每日 30 分钟，单次 15 分钟；成长版 60 / 30
    private static final int FREE_DAILY_MIN = 30;
    private static final int FREE_SINGLE_MIN = 15;
    private static final int GROWTH_DAILY_MIN = 60;
    private static final int GROWTH_SINGLE_MIN = 30;

    // ==================== 认证相关 ====================

    @Override
    public Map<String, Object> login(String username, String password) {
        // 1. 按账号查家庭
        Family family = familyMapper.selectOne(new LambdaQueryWrapper<Family>()
                .eq(Family::getUsername, username));
        if (family == null) {
            throw new BusinessException(401, "账号不存在");
        }
        if (family.getStatus() != null && family.getStatus() == 0) {
            throw new BusinessException(403, "账号已停用，请联系管理员");
        }
        // 2. 校验密码（BCrypt）
        if (!SecurityUtil.matchesPassword(password, family.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }
        // 3. 生成真实 JWT Token
        String token = JwtUtil.generateToken(family.getFamilyId(), family.getUsername(), "parent", jwtSecret, jwtExpireSeconds * 1000);
        Map<String, Object> result = new HashMap<>();
        result.put("familyId", family.getFamilyId());
        result.put("nickname", family.getNickname());
        result.put("memberLevel", family.getMemberLevel());
        result.put("phone", family.getPhone());
        result.put("token", token);
        return result;
    }

    @Override
    public Long register(Family family) {
        // 1. 校验账号唯一
        Long exist = familyMapper.selectCount(new LambdaQueryWrapper<Family>()
                .eq(Family::getUsername, family.getUsername())
                .eq(Family::getDelFlag, "0"));
        if (exist != null && exist > 0) {
            throw new BusinessException(400, "账号已存在");
        }
        // 2. 加密密码
        family.setPassword(SecurityUtil.encryptPassword(family.getPassword()));
        // 3. 初始化字段
        family.setMemberLevel("free");
        family.setMemberStatus("0");
        family.setDailyLimitMin(FREE_DAILY_MIN);
        family.setSingleLimitMin(FREE_SINGLE_MIN);
        family.setAntiAddictionFlag("0");
        family.setStatus(1);
        family.setDelFlag("0");
        // 审计字段
        family.setCreateBy(family.getUsername());
        family.setCreateTime(new Date());
        // 4. 写入
        familyMapper.insert(family);
        return family.getFamilyId();
    }

    @Override
    public void changePassword(Long familyId, String oldPassword, String newPassword) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException(401, "账号不存在");
        }
        if (!SecurityUtil.matchesPassword(oldPassword, family.getPassword())) {
            throw new BusinessException(401, "原密码错误");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BusinessException(400, "新密码至少 6 位");
        }
        Family update = new Family();
        update.setFamilyId(familyId);
        update.setPassword(SecurityUtil.encryptPassword(newPassword));
        update.setUpdateBy(family.getUsername());
        update.setUpdateTime(new Date());
        familyMapper.updateById(update);
    }

    @Override
    public void logout(Long familyId) {
        // JWT 无状态：后端无需维护 token 黑名单
        // 仅记录最后活跃时间，前端清理 localStorage 中的 token
        if (familyId == null) {
            return;
        }
        try {
            Family update = new Family();
            update.setFamilyId(familyId);
            update.setUpdateTime(new Date());
            familyMapper.updateById(update);
        } catch (Exception ignore) {
            // 退出登录容错：即使数据库更新失败也返回成功
        }
    }

    // ==================== 首页聚合 ====================

    @Override
    public Map<String, Object> getHomeData(Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        List<ChildProfile> children = listChildren(familyId);

        Map<String, Object> data = new HashMap<>();
        data.put("family", buildFamilyView(family));
        data.put("children", children);

        // 1. 成长概览：连续伴读天数 / 本月阅读分钟 / 新兴趣数
        Map<String, Object> growth = new HashMap<>();
        // TODO: 接入 M33 成长档案真实数据（来自 M23 讲读记录聚合）
        growth.put("streakDays", 12); // Mock
        growth.put("monthMinutes", 186); // Mock
        growth.put("newInterests", 8); // Mock
        growth.put("recentInterests", "森林,勇气"); // Mock
        data.put("growth", growth);

        // 2. 最近一次伴读任务（供首页 "继续上次伴读" 按钮）
        Map<String, Object> recentTask = new HashMap<>();
        // TODO: 接入 M23 讲读编排 GET /api/reading/task?familyId=&childId=
        recentTask.put("taskId", null); // Mock：无进行中任务
        recentTask.put("bookTitle", "小熊和最勇敢的风"); // Mock
        recentTask.put("cover", "🐻"); // Mock
        data.put("recentTask", recentTask);

        // 3. 今日推荐绘本 3 本
        // TODO: M35 个性化推荐上线后切换；当前走 M24 全量绘本按 sortOrder 取前 3
        List<Map<String, Object>> recommendedBooks = new ArrayList<>();
        // Mock 占位，等 M24 绘本服务就绪后调用 BookService.listTopByChildProfile
        Map<String, Object> b1 = new HashMap<>();
        b1.put("bookId", 1001);
        b1.put("title", "小熊和最勇敢的风");
        b1.put("cover", "🐻");
        b1.put("theme", "勇气主题");
        b1.put("ageGroup", "5-7岁");
        b1.put("duration", "8分钟");
        b1.put("themeColor", "#c7e7de");
        recommendedBooks.add(b1);
        Map<String, Object> b2 = new HashMap<>();
        b2.put("bookId", 1002);
        b2.put("title", "云朵鲸鱼的心情");
        b2.put("cover", "🐋");
        b2.put("theme", "情绪认知");
        b2.put("ageGroup", "3-5岁");
        b2.put("duration", "6分钟");
        b2.put("themeColor", "#f8d6a5");
        recommendedBooks.add(b2);
        Map<String, Object> b3 = new HashMap<>();
        b3.put("bookId", 1003);
        b3.put("title", "月亮邮差的礼物");
        b3.put("cover", "🚀");
        b3.put("theme", "想象力");
        b3.put("ageGroup", "7-9岁");
        b3.put("duration", "10分钟");
        b3.put("themeColor", "#d9d1ee");
        recommendedBooks.add(b3);
        data.put("recommendedBooks", recommendedBooks);

        // 4. 会员信息（供 home.html 显示升级按钮/已开通标识）
        Map<String, Object> member = new HashMap<>();
        member.put("level", family.getMemberLevel());
        member.put("status", family.getMemberStatus());
        member.put("endAt", family.getMemberEndAt());
        data.put("member", member);

        return data;
    }

    private Map<String, Object> buildFamilyView(Family family) {
        Map<String, Object> v = new HashMap<>();
        v.put("familyId", family.getFamilyId());
        v.put("nickname", family.getNickname());
        v.put("parentName", family.getParentName());
        v.put("avatar", family.getAvatar());
        v.put("memberLevel", family.getMemberLevel());
        v.put("memberStatus", family.getMemberStatus());
        return v;
    }

    // ==================== 家庭与孩子档案 ====================

    @Override
    public Family getFamily(Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        // 脱敏：不返回密码
        family.setPassword(null);
        return family;
    }

    @Override
    public void updateFamily(Family family) {
        Family exist = familyMapper.selectById(family.getFamilyId());
        if (exist == null) {
            throw new BusinessException("家庭不存在");
        }
        // 仅允许更新基础信息，不允许通过此接口改密码/会员状态
        Family update = new Family();
        update.setFamilyId(family.getFamilyId());
        update.setNickname(family.getNickname());
        update.setParentName(family.getParentName());
        update.setAvatar(family.getAvatar());
        update.setPhone(family.getPhone());
        update.setUpdateBy(exist.getUsername());
        update.setUpdateTime(new Date());
        familyMapper.updateById(update);
    }

    @Override
    public List<ChildProfile> listChildren(Long familyId) {
        return childProfileMapper.selectList(new LambdaQueryWrapper<ChildProfile>()
                .eq(ChildProfile::getFamilyId, familyId)
                .eq(ChildProfile::getDelFlag, "0")
                .orderByAsc(ChildProfile::getChildId));
    }

    @Override
    public Long addChild(ChildProfile child) {
        Family family = familyMapper.selectById(child.getFamilyId());
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        int limit = ("growth".equals(family.getMemberLevel()) || "family".equals(family.getMemberLevel())) ? GROWTH_CHILD_LIMIT : FREE_CHILD_LIMIT;
        int current = listChildren(child.getFamilyId()).size();
        if (current >= limit) {
            throw new BusinessException("已达到当前会员等级的孩子档案上限 (" + limit + " 个)，请升级会员");
        }
        // 校验孩子账号唯一性
        if (StringUtils.hasText(child.getUsername())) {
            Long existCount = childProfileMapper.selectCount(new LambdaQueryWrapper<ChildProfile>()
                    .eq(ChildProfile::getUsername, child.getUsername())
                    .eq(ChildProfile::getDelFlag, "0"));
            if (existCount != null && existCount > 0) {
                throw new BusinessException("孩子账号 '" + child.getUsername() + "' 已被使用，请换一个");
            }
            // BCrypt 加密密码
            if (StringUtils.hasText(child.getPassword())) {
                child.setPassword(SecurityUtil.encryptPassword(child.getPassword()));
            }
        }
        // 按生日计算年龄段
        child.setAgeGroup(calcAgeGroup(child.getBirthday()));
        child.setReadingLevel(child.getReadingLevel() == null ? 1 : child.getReadingLevel());
        child.setBindStatus(1); // 家长直接创建=已绑定
        child.setStatus("0");
        child.setDelFlag("0");
        child.setCreateBy(family.getUsername());
        child.setCreateTime(new Date());
        childProfileMapper.insert(child);
        return child.getChildId();
    }

    @Override
    public Map<String, Object> childLogin(String username, String password) {
        // 1. 按账号查孩子档案
        ChildProfile child = childProfileMapper.selectOne(new LambdaQueryWrapper<ChildProfile>()
                .eq(ChildProfile::getUsername, username)
                .eq(ChildProfile::getDelFlag, "0"));
        if (child == null) {
            throw new BusinessException(401, "孩子账号不存在");
        }
        if (child.getBindStatus() == null || child.getBindStatus() == 0) {
            throw new BusinessException(403, "账号尚未绑定家长，请联系家长确认");
        }
        // 2. 校验密码（BCrypt）
        if (!StringUtils.hasText(child.getPassword()) || !SecurityUtil.matchesPassword(password, child.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }
        // 3. 生成 JWT Token（role=child）
        String token = JwtUtil.generateToken(child.getChildId(), child.getUsername(), "child", jwtSecret, jwtExpireSeconds * 1000);
        // 4. 更新最后登录时间
        ChildProfile update = new ChildProfile();
        update.setChildId(child.getChildId());
        update.setLastLoginAt(new Date());
        childProfileMapper.updateById(update);
        // 5. 返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("childId", child.getChildId());
        result.put("familyId", child.getFamilyId());
        result.put("username", child.getUsername());
        result.put("nickname", child.getNickname());
        result.put("ageGroup", child.getAgeGroup());
        result.put("role", "child");
        result.put("token", token);
        // 6. 加入家长会员状态（孩子会员权限跟着家长走；解绑后 familyId=null 自然 free）
        if (child.getFamilyId() != null) {
            Family family = familyMapper.selectById(child.getFamilyId());
            if (family != null) {
                result.put("memberLevel", family.getMemberLevel() != null ? family.getMemberLevel() : "free");
                result.put("memberEndAt", family.getMemberEndAt());
            } else {
                result.put("memberLevel", "free");
                result.put("memberEndAt", null);
            }
        } else {
            result.put("memberLevel", "free");
            result.put("memberEndAt", null);
        }
        return result;
    }

    @Override
    public void updateChild(ChildProfile child) {
        ChildProfile exist = childProfileMapper.selectById(child.getChildId());
        if (exist == null) {
            throw new BusinessException("孩子档案不存在");
        }
        child.setAgeGroup(calcAgeGroup(child.getBirthday()));
        child.setUpdateBy(exist.getCreateBy());
        child.setUpdateTime(new Date());
        childProfileMapper.updateById(child);
    }

    @Override
    public void removeChild(Long childId) {
        ChildProfile exist = childProfileMapper.selectById(childId);
        if (exist == null) {
            throw new BusinessException("孩子档案不存在");
        }
        // 用 deleteById 触发 MyBatis-Plus 逻辑删除（自动 SET del_flag=1）
        // 注意：不能用 updateById + setDelFlag，因为 @TableLogic 字段不会进入 SET 子句
        childProfileMapper.deleteById(childId);
    }

    // ==================== 内容筛选与时长管控 ====================

    @Override
    public Map<String, Object> getSettings(Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        Map<String, Object> settings = new HashMap<>();
        settings.put("whitelistThemes", family.getWhitelistThemes());
        settings.put("blacklistThemes", family.getBlacklistThemes());
        settings.put("dailyLimitMin", family.getDailyLimitMin());
        settings.put("singleLimitMin", family.getSingleLimitMin());
        settings.put("antiAddictionFlag", family.getAntiAddictionFlag());
        return settings;
    }

    @Override
    public void updateSettings(Long familyId, String whitelistThemes, String blacklistThemes,
                               Integer dailyLimitMin, Integer singleLimitMin, String antiAddictionFlag) {
        Family exist = familyMapper.selectById(familyId);
        if (exist == null) {
            throw new BusinessException("家庭不存在");
        }
        // 校验时长上限范围（PRD H4：默认单次30/每日60，家长可调但不可超过 120/240）
        int daily = dailyLimitMin == null ? exist.getDailyLimitMin() : dailyLimitMin;
        int single = singleLimitMin == null ? exist.getSingleLimitMin() : singleLimitMin;
        if (daily < 5 || daily > 240) {
            throw new BusinessException("每日时长上限需在 5-240 分钟之间");
        }
        if (single < 5 || single > 120) {
            throw new BusinessException("单次时长上限需在 5-120 分钟之间");
        }
        if (single > daily) {
            throw new BusinessException("单次上限不能大于每日上限");
        }
        Family update = new Family();
        update.setFamilyId(familyId);
        update.setWhitelistThemes(whitelistThemes);
        update.setBlacklistThemes(blacklistThemes);
        update.setDailyLimitMin(daily);
        update.setSingleLimitMin(single);
        update.setAntiAddictionFlag("1".equals(antiAddictionFlag) ? "1" : "0");
        update.setUpdateBy(exist.getUsername());
        update.setUpdateTime(new Date());
        familyMapper.updateById(update);
    }

    @Override
    public List<String> filterThemesByFamilySetting(Long familyId, List<String> themeIds) {
        if (CollectionUtils.isEmpty(themeIds)) {
            return Collections.emptyList();
        }
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            return themeIds; // 找不到家庭配置时不阻塞，原样返回
        }
        Set<String> whitelist = parseCsv(family.getWhitelistThemes());
        Set<String> blacklist = parseCsv(family.getBlacklistThemes());
        return themeIds.stream()
                .filter(id -> !blacklist.contains(id))
                .filter(id -> whitelist.isEmpty() || whitelist.contains(id))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> checkReadingLimit(Long familyId, Long childId, Integer requestMinutes) {
        Map<String, Object> result = new HashMap<>();
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            result.put("allowed", false);
            result.put("reason", "家庭不存在");
            return result;
        }
        int singleLimit = family.getSingleLimitMin() == null ? FREE_SINGLE_MIN : family.getSingleLimitMin();
        int dailyLimit = family.getDailyLimitMin() == null ? FREE_DAILY_MIN : family.getDailyLimitMin();

        if (requestMinutes != null && requestMinutes > singleLimit) {
            result.put("allowed", false);
            result.put("reason", "本次阅读时长超过单次上限 " + singleLimit + " 分钟");
            return result;
        }
        // TODO: 接入 M23 讲读记录聚合今日累计时长，校验是否超过 dailyLimit
        result.put("allowed", true);
        result.put("singleLimitMin", singleLimit);
        result.put("dailyLimitMin", dailyLimit);
        result.put("usedTodayMin", 0); // Mock
        return result;
    }

    private Set<String> parseCsv(String csv) {
        if (!StringUtils.hasText(csv)) {
            return Collections.emptySet();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }
}
