package com.picturebook.ai.controller;

import com.picturebook.ai.domain.AiRole;
import com.picturebook.ai.domain.StoryTheme;
import com.picturebook.ai.service.AiRoleService;
import com.picturebook.ai.service.StoryThemeService;
import com.picturebook.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 - AI角色与故事主题 API
 */
@Tag(name = "用户端-AI角色与主题", description = "角色列表、故事主题列表")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiResourceController {

    private final AiRoleService aiRoleService;
    private final StoryThemeService storyThemeService;

    @Operation(summary = "AI角色列表")
    @GetMapping("/role/list")
    public Result<List<AiRole>> roleList() {
        return Result.ok(aiRoleService.listEnabledRoles());
    }

    @Operation(summary = "故事主题列表")
    @GetMapping("/theme/list")
    public Result<List<StoryTheme>> themeList() {
        return Result.ok(storyThemeService.listEnabledThemes());
    }
}
