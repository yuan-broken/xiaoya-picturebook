package com.picturebook.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建故事创作请求
 */
@Data
public class StoryCreateRequest {

    /** 角色ID（可选） */
    private Long roleId;

    /** 角色名称 */
    private String roleName;

    /** 主题ID（可选） */
    private Long themeId;

    /** 故事灵感输入 */
    @NotBlank(message = "故事灵感不能为空")
    private String storyInput;
}
