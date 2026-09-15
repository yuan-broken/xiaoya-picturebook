package com.picturebook.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.picturebook.admin.domain.SysConfig;
import com.picturebook.admin.service.StatsService;
import com.picturebook.admin.service.SysConfigService;
import com.picturebook.common.core.Result;
import com.picturebook.common.core.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端系统控制器（M08/M19/M20/M18）
 *
 * - 统计接口（M08/M19）
 * - 系统配置接口（M20）
 * - 内容审核查询接口（M18）
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemController {

    private final StatsService statsService;
    private final SysConfigService sysConfigService;

    public AdminSystemController(StatsService statsService, SysConfigService sysConfigService) {
        this.statsService = statsService;
        this.sysConfigService = sysConfigService;
    }

    // ============================================================
    // M08 / M19 统计接口
    // ============================================================

    /**
     * 数据概览（M08 首页）
     * GET /api/admin/stats/overview
     */
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> statsOverview() {
        return Result.ok(statsService.overview());
    }

    /**
     * 完整统计（M19 数据统计）
     * GET /api/admin/stats/full
     */
    @GetMapping("/stats/full")
    public Result<Map<String, Object>> statsFull() {
        return Result.ok(statsService.fullStats());
    }

    /**
     * 热门绘本排行
     * GET /api/admin/stats/hot-books
     */
    @GetMapping("/stats/hot-books")
    public Result<List<Map<String, Object>>> hotBooks(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(statsService.hotBooks(limit));
    }

    /**
     * 热门角色排行
     * GET /api/admin/stats/hot-roles
     */
    @GetMapping("/stats/hot-roles")
    public Result<List<Map<String, Object>>> hotRoles(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(statsService.hotRoles(limit));
    }

    // ============================================================
    // M20 系统配置接口
    // ============================================================

    /**
     * 配置列表
     * GET /api/admin/config/list
     */
    @GetMapping("/config/list")
    public Result<List<SysConfig>> configList(@RequestParam(required = false) String prefix) {
        return Result.ok(sysConfigService.list());
    }

    /**
     * 获取单个配置
     * GET /api/admin/config/{key}
     */
    @GetMapping("/config/{key}")
    public Result<String> getConfig(@PathVariable String key) {
        return Result.ok(sysConfigService.getConfigValue(key));
    }

    /**
     * 批量获取配置（按前缀）
     * GET /api/admin/config/map?prefix=product.
     */
    @GetMapping("/config/map")
    public Result<Map<String, String>> configMap(@RequestParam(required = false) String prefix) {
        return Result.ok(sysConfigService.getConfigMap(prefix));
    }

    /**
     * 保存配置
     * PUT /api/admin/config
     */
    @PutMapping("/config")
    public Result<Void> saveConfig(@RequestBody SysConfig config) {
        boolean ok = sysConfigService.setConfigValue(config.getConfigKey(), config.getConfigValue());
        return ok ? Result.ok() : Result.fail("保存失败");
    }

    /**
     * 防沉迷配置
     * GET /api/admin/config/anti-addiction
     */
    @GetMapping("/config/anti-addiction")
    public Result<Map<String, Object>> antiAddictionConfig() {
        return Result.ok(sysConfigService.getAntiAddictionConfig());
    }

    /**
     * 产品基础配置
     * GET /api/admin/config/product
     */
    @GetMapping("/config/product")
    public Result<Map<String, String>> productConfig() {
        return Result.ok(sysConfigService.getProductConfig());
    }
}
