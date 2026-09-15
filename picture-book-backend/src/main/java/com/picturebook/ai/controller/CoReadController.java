package com.picturebook.ai.controller;

import com.picturebook.ai.domain.CoReadEvent;
import com.picturebook.ai.domain.CoReadRoom;
import com.picturebook.ai.service.CoReadService;
import com.picturebook.common.core.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 远程共读 Controller（M07/M34）
 * - 房间创建/加入/退出
 * - HTTP 轮询状态同步
 * - 家长语音插入
 * - 模式切换
 *
 * @author Phase2
 */
@RestController
@RequestMapping("/api/coread")
public class CoReadController {

    @Autowired
    private CoReadService coReadService;

    /**
     * 创建房间
     * POST /api/coread/create
     * body: {childId, bookId, taskId, mode}
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long familyId = currentFamilyId();
        Long childId = toLong(body.get("childId"));
        Long bookId = toLong(body.get("bookId"));
        Long taskId = toLong(body.get("taskId"));
        String mode = (String) body.get("mode");
        return Result.ok(coReadService.createRoom(familyId, childId, bookId, taskId, mode));
    }

    /**
     * 加入房间
     * POST /api/coread/join
     * body: {roomCode, role}
     */
    @PostMapping("/join")
    public Result<Map<String, Object>> join(@RequestBody Map<String, String> body) {
        String code = body.get("roomCode");
        String role = body.get("role");
        if (role == null) role = "child";
        return Result.ok(coReadService.joinRoom(code, role));
    }

    /**
     * 退出房间
     * POST /api/coread/{roomCode}/exit
     * body: {role}
     */
    @PostMapping("/{roomCode}/exit")
    public Result<Map<String, Object>> exit(@PathVariable String roomCode,
                                            @RequestBody(required = false) Map<String, String> body) {
        String role = body == null ? "child" : body.getOrDefault("role", "child");
        return Result.ok(coReadService.exitRoom(roomCode, role));
    }

    /**
     * 关闭房间
     * POST /api/coread/{roomCode}/close
     */
    @PostMapping("/{roomCode}/close")
    public Result<Map<String, Object>> close(@PathVariable String roomCode) {
        return Result.ok(coReadService.closeRoom(roomCode));
    }

    /**
     * 查询房间状态
     * GET /api/coread/{roomCode}
     */
    @GetMapping("/{roomCode}")
    public Result<Map<String, Object>> getRoom(@PathVariable String roomCode) {
        CoReadRoom r = coReadService.getRoom(roomCode);
        if (r == null) return Result.fail(404, "房间不存在或已关闭");
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", r.getId());
        m.put("roomCode", r.getRoomCode());
        m.put("familyId", r.getFamilyId());
        m.put("childId", r.getChildId());
        m.put("bookId", r.getBookId());
        m.put("bookTitle", r.getBookTitle());
        m.put("taskId", r.getTaskId());
        m.put("hostRole", r.getHostRole());
        m.put("mode", r.getMode());
        m.put("state", r.getState());
        m.put("currentPage", r.getCurrentPage());
        m.put("totalPages", r.getTotalPages());
        m.put("hostJoined", r.getHostJoined());
        m.put("guestJoined", r.getGuestJoined());
        m.put("lastSyncAt", r.getLastSyncAt());
        m.put("expireAt", r.getExpireAt());
        return Result.ok(m);
    }

    /**
     * 同步状态（翻页/暂停/继续）
     * POST /api/coread/{roomCode}/sync
     * body: {eventType, eventData, senderRole}
     */
    @PostMapping("/{roomCode}/sync")
    public Result<Map<String, Object>> sync(@PathVariable String roomCode,
                                           @RequestBody Map<String, Object> body) {
        String eventType = (String) body.get("eventType");
        @SuppressWarnings("unchecked")
        Map<String, Object> eventData = (Map<String, Object>) body.get("eventData");
        String senderRole = (String) body.get("senderRole");
        if (senderRole == null) senderRole = "child";
        return Result.ok(coReadService.syncState(roomCode, eventType, eventData, senderRole));
    }

    /**
     * 拉取事件（HTTP 轮询入口）
     * GET /api/coread/{roomCode}/events?sinceId=
     */
    @GetMapping("/{roomCode}/events")
    public Result<List<CoReadEvent>> pollEvents(@PathVariable String roomCode,
                                                @RequestParam(required = false) Long sinceId) {
        return Result.ok(coReadService.pollEvents(roomCode, sinceId));
    }

    /**
     * 家长语音插入
     * POST /api/coread/{roomCode}/voice-insert
     * body: {audioUrl, text}
     */
    @PostMapping("/{roomCode}/voice-insert")
    public Result<Map<String, Object>> voiceInsert(@PathVariable String roomCode,
                                                   @RequestBody Map<String, String> body) {
        return Result.ok(coReadService.voiceInsert(roomCode, body.get("audioUrl"), body.get("text")));
    }

    /**
     * 切换参与模式
     * POST /api/coread/{roomCode}/switch-mode
     * body: {mode}
     */
    @PostMapping("/{roomCode}/switch-mode")
    public Result<Map<String, Object>> switchMode(@PathVariable String roomCode,
                                                  @RequestBody Map<String, String> body) {
        return Result.ok(coReadService.switchMode(roomCode, body.get("mode")));
    }

    /**
     * 我发起的房间
     * GET /api/coread/my
     */
    @GetMapping("/my")
    public Result<List<CoReadRoom>> myRooms() {
        return Result.ok(coReadService.listMyRooms(currentFamilyId()));
    }

    /**
     * 手动触发清理过期房间（也可由定时任务调用）
     * POST /api/coread/clean-expired
     */
    @PostMapping("/clean-expired")
    public Result<Map<String, Object>> cleanExpired() {
        int n = coReadService.cleanExpiredRooms();
        return Result.ok(Map.of("cleaned", n));
    }

    // ==================== 工具 ====================

    private Long currentFamilyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long) {
            return (Long) auth.getDetails();
        }
        return 1L;
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
}
