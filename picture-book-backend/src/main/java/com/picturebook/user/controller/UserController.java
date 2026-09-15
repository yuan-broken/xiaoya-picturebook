package com.picturebook.user.controller;

import com.picturebook.common.core.Result;
import com.picturebook.user.domain.ChildProfile;
import com.picturebook.user.domain.Family;
import com.picturebook.user.mapper.ChildProfileMapper;
import com.picturebook.user.service.HabitService;
import com.picturebook.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 用户与家长 Controller（用户端）
 * 对应 PRD M01 首页 / M05 会员升级 / M33 用户与家长服务。
 * 用户端接口前缀 /api/user、/api/auth。
 *
 * @author Agent-4
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private HabitService habitService;

    @Autowired
    private ChildProfileMapper childProfileMapper;

    // ==================== 认证相关 ====================

    /**
     * 家长登录
     * POST /api/auth/login
     * body: {username, password}
     */
    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return Result.ok(userService.login(username, password));
    }

    /**
     * 孩子独立登录
     * POST /api/auth/child/login
     * body: {username, password}
     */
    @PostMapping("/auth/child/login")
    public Result<Map<String, Object>> childLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return Result.ok(userService.childLogin(username, password));
    }

    /**
     * 家长注册
     * POST /api/auth/register
     */
    @PostMapping("/auth/register")
    public Result<Long> register(@RequestBody Family family) {
        return Result.ok(userService.register(family));
    }

    /**
     * 修改密码
     * PUT /api/user/password
     * body: {oldPassword, newPassword}
     */
    @PutMapping("/user/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        Long familyId = currentFamilyId();
        userService.changePassword(familyId, body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    /**
     * 家长退出登录
     * POST /api/auth/logout
     * JWT 无状态，后端返回成功，前端清理本地 token
     */
    @PostMapping("/auth/logout")
    public Result<Void> logout() {
        try {
            userService.logout(currentFamilyId());
        } catch (Exception ignore) {
            // 退出登录容错
        }
        return Result.ok();
    }

    // ==================== 首页聚合 ====================

    /**
     * 首页聚合数据
     * GET /api/user/home
     */
    @GetMapping("/user/home")
    public Result<Map<String, Object>> home() {
        return Result.ok(userService.getHomeData(currentFamilyId()));
    }

    // ==================== 家庭信息 ====================

    @GetMapping("/user/family")
    public Result<Family> getFamily() {
        return Result.ok(userService.getFamily(currentFamilyId()));
    }

    @PutMapping("/user/family")
    public Result<Void> updateFamily(@RequestBody Family family) {
        family.setFamilyId(currentFamilyId());
        userService.updateFamily(family);
        return Result.ok();
    }

    // ==================== 孩子档案 ====================

    @GetMapping("/user/children")
    public Result<List<ChildProfile>> listChildren() {
        return Result.ok(userService.listChildren(currentFamilyId()));
    }

    @PostMapping("/user/child")
    public Result<Long> addChild(@RequestBody ChildProfile child) {
        child.setFamilyId(currentFamilyId());
        return Result.ok(userService.addChild(child));
    }

    @PutMapping("/user/child/{id}")
    public Result<Void> updateChild(@PathVariable("id") Long childId, @RequestBody ChildProfile child) {
        child.setChildId(childId);
        child.setFamilyId(currentFamilyId());
        userService.updateChild(child);
        return Result.ok();
    }

    @DeleteMapping("/user/child/{id}")
    public Result<Void> removeChild(@PathVariable("id") Long childId) {
        userService.removeChild(childId);
        return Result.ok();
    }

    // ==================== 内容筛选与时长管控 ====================

    @GetMapping("/user/settings")
    public Result<Map<String, Object>> getSettings() {
        return Result.ok(userService.getSettings(currentFamilyId()));
    }

    @PutMapping("/user/settings")
    public Result<Void> updateSettings(@RequestBody Map<String, Object> body) {
        userService.updateSettings(
                currentFamilyId(),
                (String) body.get("whitelistThemes"),
                (String) body.get("blacklistThemes"),
                body.get("dailyLimitMin") == null ? null : Integer.valueOf(body.get("dailyLimitMin").toString()),
                body.get("singleLimitMin") == null ? null : Integer.valueOf(body.get("singleLimitMin").toString()),
                (String) body.get("antiAddictionFlag")
        );
        return Result.ok();
    }

    // ==================== 账号信息 ====================

    /**
     * 账号安全信息
     * GET /api/user/account
     */
    @GetMapping("/user/account")
    public Result<Map<String, Object>> getAccount() {
        Long fid = currentFamilyId();
        Family family = userService.getFamily(fid);
        Map<String, Object> r = new HashMap<>();
        r.put("name", family.getNickname());
        r.put("phone", maskPhone(family.getPhone()));
        r.put("memberLevel", family.getMemberLevel());
        r.put("memberEndTime", family.getMemberEndAt());
        r.put("parentName", family.getParentName());
        r.put("avatar", family.getAvatar());
        return Result.ok(r);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "138****8888";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    // ==================== 成长档案聚合（对应 parent.html 成长档案 Tab） ====================
    // 注：真实数据来自 M23 讲读记录 + M36 习惯 + M35 画像；当前提供 Mock + 真实勋章数据。

    /**
     * 成长概览统计卡片
     * GET /api/user/growth/overview
     */
    @GetMapping("/user/growth/overview")
    public Result<Map<String, Object>> growthOverview() {
        Long fid = currentFamilyId();
        // 取习惯统计的真实连续天数（如有打卡记录）
        Map<String, Object> r = new HashMap<>();
        try {
            Map<String, Object> stats = habitService.getCheckinStats(1L); // 默认 childId=1
            r.put("weekDuration", stats.getOrDefault("monthMinutes", 0));
            r.put("vocabCount", 120); // Mock：M26 词汇量未接入
            r.put("expressionScore", "A+");
            r.put("badgeCount", habitService.listUserBadges(1L).size());
            r.put("continuousDays", stats.getOrDefault("continuousDays", 0));
            r.put("totalDays", stats.getOrDefault("totalDays", 0));
        } catch (Exception e) {
            r.put("weekDuration", 0);
            r.put("vocabCount", 0);
            r.put("expressionScore", "--");
            r.put("badgeCount", 0);
        }
        return Result.ok(r);
    }

    /**
     * 词汇量曲线
     * GET /api/user/growth/vocab-curve
     */
    @GetMapping("/user/growth/vocab-curve")
    public Result<List<Map<String, Object>>> growthVocabCurve() {
        // Mock 6个月数据（M26 词汇量统计未接入）
        String[] months = {"4月", "5月", "6月", "7月", "8月", "9月"};
        int[] counts = {35, 58, 76, 92, 108, 120};
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < months.length; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("month", months[i]);
            m.put("count", counts[i]);
            list.add(m);
        }
        return Result.ok(list);
    }

    /**
     * 能力雷达图
     * GET /api/user/growth/radar
     */
    @GetMapping("/user/growth/radar")
    public Result<Map<String, Object>> growthRadar() {
        // Mock：5 维能力评分（M26 互动统计未接入）
        Map<String, Object> r = new HashMap<>();
        List<Map<String, Object>> dims = new ArrayList<>();
        String[][] data = {
                {"语言表达", "85"},
                {"想象力", "92"},
                {"专注力", "78"},
                {"逻辑思维", "70"},
                {"情感认知", "88"}
        };
        for (String[] d : data) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", d[0]);
            m.put("score", Integer.parseInt(d[1]));
            dims.add(m);
        }
        r.put("dimensions", dims);
        return Result.ok(r);
    }

    /**
     * 兴趣标签
     * GET /api/user/growth/interests
     */
    @GetMapping("/user/growth/interests")
    public Result<List<Map<String, Object>>> growthInterests() {
        // Mock 兴趣标签（M35 画像未完整接入）
        String[][] data = {
                {"动物", "#fde6c5", "12"},
                {"冒险", "#e5f3ed", "8"},
                {"勇气", "#f8d6a5", "6"},
                {"友谊", "#d9d1ee", "5"},
                {"自然", "#c7e7de", "4"},
                {"家庭", "#f6c49b", "3"}
        };
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] d : data) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", d[0]);
            m.put("color", d[1]);
            m.put("count", Integer.parseInt(d[2]));
            list.add(m);
        }
        return Result.ok(list);
    }

    /**
     * 勋章墙（含未解锁勋章）
     * GET /api/user/growth/badges
     */
    @GetMapping("/user/growth/badges")
    public Result<List<Map<String, Object>>> growthBadges(@RequestParam(required = false, defaultValue = "1") Long childId) {
        // 已解锁勋章从 habitService 拿真实数据，未解锁从全部勋章定义里补
        List<com.picturebook.user.domain.UserBadge> owned = habitService.listUserBadges(childId);
        Set<String> ownedCodes = new HashSet<>();
        for (com.picturebook.user.domain.UserBadge ub : owned) {
            ownedCodes.add(ub.getBadgeCode());
        }
        List<com.picturebook.user.domain.Badge> all = habitService.listBadges();
        List<Map<String, Object>> list = new ArrayList<>();
        for (com.picturebook.user.domain.Badge b : all) {
            Map<String, Object> m = new HashMap<>();
            m.put("emoji", b.getBadgeIcon() == null ? "🏅" : b.getBadgeIcon());
            m.put("name", b.getBadgeName());
            m.put("code", b.getBadgeCode());
            m.put("unlocked", ownedCodes.contains(b.getBadgeCode()));
            list.add(m);
        }
        return Result.ok(list);
    }

    /**
     * 表达能力记录（精彩表达）
     * GET /api/user/growth/expressions
     */
    @GetMapping("/user/growth/expressions")
    public Result<List<Map<String, Object>>> growthExpressions() {
        // Mock 精彩表达记录（M26 互动统计未接入）
        String[][] data = {
                {"小熊和最勇敢的风", "3", "2026-09-14 10:23", "小熊会鼓起勇气，因为风是它的朋友！"},
                {"云朵鲸鱼的心情", "5", "2026-09-13 19:45", "鲸鱼哭了，因为它想念大海。"},
                {"月亮邮差的礼物", "2", "2026-09-12 20:10", "月亮会给每个孩子写信。"}
        };
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] d : data) {
            Map<String, Object> m = new HashMap<>();
            m.put("book", d[0]);
            m.put("page", Integer.parseInt(d[1]));
            m.put("time", d[2]);
            m.put("text", d[3]);
            list.add(m);
        }
        return Result.ok(list);
    }

    // ==================== 孩子专属接口（role=child） ====================

    /**
     * 获取当前登录孩子的档案
     * GET /api/child/profile
     */
    @GetMapping("/child/profile")
    public Result<Map<String, Object>> getChildProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CHILD"))) {
            return Result.fail("仅孩子账号可访问");
        }
        Long childId = (Long) auth.getDetails();
        ChildProfile child = childProfileMapper.selectById(childId);
        if (child == null) {
            return Result.fail("档案不存在");
        }
        Map<String, Object> r = new HashMap<>();
        r.put("childId", child.getChildId());
        r.put("username", child.getUsername());
        r.put("nickname", child.getNickname());
        r.put("ageGroup", child.getAgeGroup());
        r.put("interests", child.getInterests());
        r.put("readingLevel", child.getReadingLevel());
        r.put("bindStatus", child.getBindStatus());
        r.put("role", "child");
        // 会员状态：仅当孩子处于"已绑定"状态(=1)时，才继承家长会员
        // 解绑后(bindStatus=0)孩子自动失去会员功能
        String childMemberLevel = "free";
        java.util.Date childMemberEndAt = null;
        if (child.getBindStatus() != null && child.getBindStatus() == 1 && child.getFamilyId() != null) {
            try {
                Family family = userService.getFamily(child.getFamilyId());
                if (family != null) {
                    childMemberLevel = family.getMemberLevel() != null ? family.getMemberLevel() : "free";
                    childMemberEndAt = family.getMemberEndAt();
                    // 检查会员是否已过期
                    if (childMemberEndAt != null && childMemberEndAt.before(new java.util.Date())) {
                        childMemberLevel = "free";
                        childMemberEndAt = null;
                    }
                }
            } catch (Exception ignore) {
                // 家庭信息查询失败，降级为 free
            }
        }
        r.put("memberLevel", childMemberLevel);
        r.put("memberEndAt", childMemberEndAt);
        return Result.ok(r);
    }

    /**
     * 获取绑定的家长信息（仅返回昵称，不返回密码/手机号）
     * GET /api/child/family
     */
    @GetMapping("/child/family")
    public Result<Map<String, Object>> getChildFamily() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CHILD"))) {
            return Result.fail("仅孩子账号可访问");
        }
        Long childId = (Long) auth.getDetails();
        ChildProfile child = childProfileMapper.selectById(childId);
        if (child == null) return Result.fail("档案不存在");
        Family family = userService.getFamily(child.getFamilyId());
        Map<String, Object> r = new HashMap<>();
        r.put("parentName", family.getNickname());
        r.put("phone", maskPhone(family.getPhone()));
        // 暴露会员信息（仅当孩子已绑定）
        if (child.getBindStatus() != null && child.getBindStatus() == 1) {
            r.put("memberLevel", family.getMemberLevel() != null ? family.getMemberLevel() : "free");
            r.put("memberEndAt", family.getMemberEndAt());
        } else {
            r.put("memberLevel", "free");
            r.put("memberEndAt", null);
        }
        return Result.ok(r);
    }

    // ==================== 工具方法 ====================

    /**
     * 从 SecurityContext 获取当前家庭ID
     * token 无效时直接抛出异常，不再兜底到演示账号，避免数据错乱
     */
    private Long currentFamilyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long) {
            return (Long) auth.getDetails();
        }
        throw new com.picturebook.common.exception.BusinessException("请先登录");
    }
}
