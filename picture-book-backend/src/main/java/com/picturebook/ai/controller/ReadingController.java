package com.picturebook.ai.controller;

import com.picturebook.ai.dto.InteractionResponseRequest;
import com.picturebook.ai.dto.ReadingSession;
import com.picturebook.ai.dto.ReadingTaskCreateRequest;
import com.picturebook.ai.service.ReadingOrchestratorService;
import com.picturebook.book.domain.InteractionPoint;
import com.picturebook.common.core.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户端 - AI讲读 API
 */
@Tag(name = "用户端-AI讲读", description = "讲读任务、状态机、翻页、互动")
@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
public class ReadingController {

    private final ReadingOrchestratorService orchestrator;

    @Operation(summary = "创建讲读任务")
    @PostMapping("/task")
    public Result<ReadingSession> createTask(@Valid @RequestBody ReadingTaskCreateRequest request) {
        return Result.ok(orchestrator.createTask(request));
    }

    @Operation(summary = "启动讲读（加载第一页）")
    @PostMapping("/task/{taskId}/start")
    public Result<ReadingSession> start(@PathVariable Long taskId) {
        return Result.ok(orchestrator.startReading(taskId));
    }

    @Operation(summary = "暂停讲读")
    @PostMapping("/task/{taskId}/pause")
    public Result<ReadingSession> pause(@PathVariable Long taskId) {
        return Result.ok(orchestrator.pause(taskId));
    }

    @Operation(summary = "继续讲读")
    @PostMapping("/task/{taskId}/resume")
    public Result<ReadingSession> resume(@PathVariable Long taskId) {
        return Result.ok(orchestrator.resume(taskId));
    }

    @Operation(summary = "下一页")
    @PostMapping("/task/{taskId}/next")
    public Result<ReadingSession> next(@PathVariable Long taskId) {
        return Result.ok(orchestrator.nextPage(taskId));
    }

    @Operation(summary = "上一页")
    @PostMapping("/task/{taskId}/prev")
    public Result<ReadingSession> prev(@PathVariable Long taskId) {
        return Result.ok(orchestrator.prevPage(taskId));
    }

    @Operation(summary = "跳转到指定页")
    @PostMapping("/task/{taskId}/goto/{pageNum}")
    public Result<ReadingSession> gotoPage(@PathVariable Long taskId, @PathVariable Integer pageNum) {
        return Result.ok(orchestrator.gotoPage(taskId, pageNum));
    }

    @Operation(summary = "获取讲读会话状态")
    @GetMapping("/task/{taskId}/session")
    public Result<ReadingSession> getSession(@PathVariable Long taskId) {
        return Result.ok(orchestrator.getSession(taskId));
    }

    @Operation(summary = "触发当前页互动点")
    @GetMapping("/task/{taskId}/interactions")
    public Result<List<InteractionPoint>> triggerInteractions(@PathVariable Long taskId) {
        return Result.ok(orchestrator.triggerInteractions(taskId));
    }

    @Operation(summary = "提交互动回应")
    @PostMapping("/task/interaction")
    public Result<Map<String, Object>> submitInteraction(@RequestBody InteractionResponseRequest request) {
        return Result.ok(orchestrator.submitInteraction(request));
    }
}
