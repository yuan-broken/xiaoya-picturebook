package com.picturebook.ai.controller;

import com.picturebook.ai.domain.ChatMessage;
import com.picturebook.ai.domain.ChatSession;
import com.picturebook.ai.domain.StoryCreation;
import com.picturebook.ai.dto.SendMessageRequest;
import com.picturebook.ai.dto.StoryCreateRequest;
import com.picturebook.ai.service.AiChatService;
import com.picturebook.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户端 - AI故事创作与角色对话 API
 */
@Tag(name = "用户端-AI故事与对话", description = "故事创作、角色聊天")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    // ============ 故事创作 ============

    @Operation(summary = "创建AI故事")
    @PostMapping("/story/create")
    public Result<StoryCreation> createStory(@Valid @RequestBody StoryCreateRequest request) {
        return Result.ok(aiChatService.createStory(request));
    }

    @Operation(summary = "查询故事创作状态")
    @GetMapping("/story/{creationId}")
    public Result<StoryCreation> getStoryStatus(@PathVariable Long creationId) {
        return Result.ok(aiChatService.getStoryStatus(creationId));
    }

    @Operation(summary = "我的故事创作列表")
    @GetMapping("/story/my")
    public Result<List<StoryCreation>> myStories() {
        return Result.ok(aiChatService.myStories());
    }

    // ============ 角色对话 ============

    @Operation(summary = "创建或恢复对话会话")
    @PostMapping("/chat/session/{roleId}")
    public Result<ChatSession> createOrResumeSession(@PathVariable Long roleId) {
        return Result.ok(aiChatService.createOrResumeSession(roleId));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/chat/send")
    public Result<Map<String, Object>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        return Result.ok(aiChatService.sendMessage(request));
    }

    @Operation(summary = "获取会话消息列表")
    @GetMapping("/chat/{sessionId}/messages")
    public Result<List<ChatMessage>> getMessages(@PathVariable Long sessionId) {
        return Result.ok(aiChatService.getMessages(sessionId));
    }

    @Operation(summary = "我的会话列表")
    @GetMapping("/chat/sessions")
    public Result<List<ChatSession>> mySessions() {
        return Result.ok(aiChatService.mySessions());
    }
}
