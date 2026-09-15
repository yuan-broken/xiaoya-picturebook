package com.picturebook.user.service;

import com.picturebook.user.domain.ChildProfile;
import com.picturebook.user.domain.Family;

import java.util.List;
import java.util.Map;

/**
 * 用户与家长服务接口
 * 对应 PRD M33：家庭账号、孩子档案、成长档案、内容筛选、时长管控、防沉迷。
 *
 * @author Agent-4
 */
public interface UserService {

    // ========== 认证相关 ==========

    /**
     * 家长登录
     *
     * @param username 账号（手机号或邮箱）
     * @param password 明文密码
     * @return 登录结果，含 token 与 familyId
     */
    Map<String, Object> login(String username, String password);

    /**
     * 家长注册
     *
     * @param family 家庭信息（至少含 username、password、parentName、phone）
     * @return 新建的家庭ID
     */
    Long register(Family family);

    /**
     * 修改家长密码
     *
     * @param familyId    家庭ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long familyId, String oldPassword, String newPassword);

    /**
     * 家长退出登录
     * JWT 无状态，后端仅返回成功并可选记录退出时间。
     * 前端负责清理本地 token。
     *
     * @param familyId 当前家庭ID（来自 SecurityContext）
     */
    void logout(Long familyId);

    // ========== 首页聚合 ==========

    /**
     * 首页聚合数据：成长概览 + 最近伴读 + 今日推荐
     * 对应 GET /api/user/home
     *
     * @param familyId 家庭ID
     * @return 聚合数据
     */
    Map<String, Object> getHomeData(Long familyId);

    // ========== 家庭与孩子档案 ==========

    /**
     * 查询家庭信息
     */
    Family getFamily(Long familyId);

    /**
     * 更新家庭基础信息（家长昵称、头像等）
     */
    void updateFamily(Family family);

    /**
     * 查询家庭下的所有孩子档案
     */
    List<ChildProfile> listChildren(Long familyId);

    /**
     * 新增孩子档案（家庭版最多3个，对应 PRD 会员权益）
     * 含 username/password 独立账号创建
     */
    Long addChild(ChildProfile child);

    /**
     * 孩子独立登录（用孩子账号密码）
     *
     * @param username 孩子账号
     * @param password 明文密码
     * @return 登录结果，含 token 与 childId
     */
    Map<String, Object> childLogin(String username, String password);

    /**
     * 更新孩子档案（兴趣、阅读能力等级等）
     */
    void updateChild(ChildProfile child);

    /**
     * 删除孩子档案（软删除）
     */
    void removeChild(Long childId);

    // ========== 内容筛选与时长管控 ==========

    /**
     * 获取家长设置（白/黑名单、时长限制、防沉迷）
     */
    Map<String, Object> getSettings(Long familyId);

    /**
     * 更新家长设置
     */
    void updateSettings(Long familyId, String whitelistThemes, String blacklistThemes,
                        Integer dailyLimitMin, Integer singleLimitMin, String antiAddictionFlag);

    /**
     * 根据孩子画像过滤绘本主题白/黑名单（供 M24/M35 调用）
     *
     * @param familyId 家庭ID
     * @param themeIds 候选主题ID列表
     * @return 过滤后可用的主题ID列表
     */
    List<String> filterThemesByFamilySetting(Long familyId, List<String> themeIds);

    /**
     * 校验单次/每日阅读时长是否超出限制（供 M23 讲读编排调用）
     *
     * @param familyId      家庭ID
     * @param childId       孩子ID
     * @param requestMinutes 本次预计阅读时长
     * @return 校验结果，含 allowed=false 与 reason
     */
    Map<String, Object> checkReadingLimit(Long familyId, Long childId, Integer requestMinutes);

    /**
     * 根据孩子出生日期计算年龄段
     *
     * @param birthday 生日
     * @return toddler/preschool/lowerPrimary/upperPrimary
     */
    default String calcAgeGroup(java.util.Date birthday) {
        if (birthday == null) {
            return "preschool";
        }
        long days = (System.currentTimeMillis() - birthday.getTime()) / (1000L * 60 * 60 * 24);
        long years = days / 365;
        if (years <= 3) return "toddler";
        if (years <= 6) return "preschool";
        if (years <= 9) return "lowerPrimary";
        return "upperPrimary";
    }
}
