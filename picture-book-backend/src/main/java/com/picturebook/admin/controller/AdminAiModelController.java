package com.picturebook.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.picturebook.admin.domain.SysAiModel;
import com.picturebook.admin.service.SysAiModelService;
import com.picturebook.common.core.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 模型配置管理端接口（M14）
 */
@RestController
@RequestMapping("/api/admin/ai-model")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiModelController {

    private final SysAiModelService sysAiModelService;

    public AdminAiModelController(SysAiModelService sysAiModelService) {
        this.sysAiModelService = sysAiModelService;
    }

    /**
     * 分页查询
     */
    @GetMapping("/list")
    public Result<IPage<SysAiModel>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String capabilityType,
            @RequestParam(required = false) String provider) {
        return Result.ok(sysAiModelService.queryPage(pageNum, pageSize, capabilityType, provider));
    }

    /**
     * 查询启用的配置列表
     */
    @GetMapping("/enabled")
    public Result<?> listEnabled() {
        return Result.ok(sysAiModelService.listEnabled());
    }

    /**
     * 新增
     */
    @PostMapping
    public Result<Void> add(@RequestBody SysAiModel model) {
        return sysAiModelService.addModel(model) ? Result.ok() : Result.fail("新增失败");
    }

    /**
     * 编辑
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysAiModel model) {
        return sysAiModelService.updateModel(model) ? Result.ok() : Result.fail("编辑失败");
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return sysAiModelService.removeById(id) ? Result.ok() : Result.fail("删除失败");
    }

    /**
     * 切换启用
     */
    @PutMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        return sysAiModelService.toggleEnabled(id) ? Result.ok() : Result.fail("切换失败");
    }
}
