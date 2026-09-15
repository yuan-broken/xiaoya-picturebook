package com.picturebook.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picturebook.ai.domain.CoReadEvent;
import com.picturebook.ai.domain.CoReadRoom;
import com.picturebook.ai.mapper.CoReadEventMapper;
import com.picturebook.ai.mapper.CoReadRoomMapper;
import com.picturebook.ai.service.CoReadService;
import com.picturebook.book.domain.Book;
import com.picturebook.book.service.BookService;
import com.picturebook.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 远程共读服务 Mock 实现
 *
 * - 房间码：6位大写字母+数字（如 A3B9XK）
 * - 房间状态：等待/进行中/暂停/结束/关闭
 * - 同步机制：HTTP 轮询事件流（替代 WebSocket，符合用户决策）
 * - 过期：30分钟自动关闭
 *
 * @author Phase2
 */
@Service
public class CoReadServiceImpl implements CoReadService {

    private static final Logger log = LoggerFactory.getLogger(CoReadServiceImpl.class);

    private static final String ROOM_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去除易混字符
    private static final int ROOM_CODE_LEN = 6;
    private static final int ROOM_TTL_MINUTES = 30;
    private static final String REDIS_ROOM_KEY = "picturebook:coread:room:";

    @Autowired
    private CoReadRoomMapper roomMapper;
    @Autowired
    private CoReadEventMapper eventMapper;
    @Autowired
    private BookService bookService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Object> createRoom(Long familyId, Long childId, Long bookId, Long taskId, String mode) {
        if (familyId == null || bookId == null) {
            throw new BusinessException("familyId/bookId 不能为空");
        }
        if (!StringUtils.hasText(mode)) {
            mode = "two_way";
        }
        // 生成房间码（确保唯一）
        String code;
        int retry = 0;
        do {
            code = genRoomCode();
            retry++;
            if (retry > 10) break;
        } while (roomMapper.selectOne(new LambdaQueryWrapper<CoReadRoom>()
                .eq(CoReadRoom::getRoomCode, code)
                .eq(CoReadRoom::getDelFlag, "0")) != null);

        // 查绘本信息
        String bookTitle = null;
        int totalPages = 0;
        try {
            Book b = bookService.getBookDetail(bookId);
            if (b != null) {
                bookTitle = b.getTitle();
                totalPages = b.getTotalPages() == null ? 0 : b.getTotalPages();
            }
        } catch (Exception ignore) { /* 绘本可能未上架 */ }

        // 房间写入DB
        CoReadRoom room = new CoReadRoom();
        room.setRoomCode(code);
        room.setFamilyId(familyId);
        room.setChildId(childId);
        room.setBookId(bookId);
        room.setBookTitle(bookTitle);
        room.setTaskId(taskId);
        room.setHostRole("parent");
        room.setMode(mode);
        room.setState("waiting");
        room.setCurrentPage(1);
        room.setTotalPages(totalPages);
        room.setHostJoined(1); // 家长发起即加入
        room.setGuestJoined(0);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, ROOM_TTL_MINUTES);
        room.setExpireAt(cal.getTime());
        room.setLastSyncAt(new Date());
        room.setDelFlag("0");
        room.setCreateBy(String.valueOf(familyId));
        room.setCreateTime(new Date());
        roomMapper.insert(room);

        // Redis 缓存房间状态
        cacheRoom(room);

        // 写入事件
        writeEvent(room.getRoomCode(), familyId, childId, "create", room2Map(room), "parent");

        log.info("[CoRead] room created: code={} familyId={} bookId={}", code, familyId, bookId);

        Map<String, Object> r = room2Map(room);
        r.put("message", "房间已创建，请将房间码发送给孩子");
        r.put("expireSeconds", ROOM_TTL_MINUTES * 60L);
        return r;
    }

    @Override
    public Map<String, Object> joinRoom(String roomCode, String role) {
        CoReadRoom room = loadRoomOrFail(roomCode);
        if ("parent".equals(role)) {
            room.setHostJoined(1);
        } else if ("child".equals(role)) {
            room.setGuestJoined(1);
            if ("waiting".equals(room.getState())) {
                room.setState("active");
            }
        }
        room.setLastSyncAt(new Date());
        room.setUpdateTime(new Date());
        roomMapper.updateById(room);
        cacheRoom(room);

        writeEvent(roomCode, room.getFamilyId(), room.getChildId(), "join",
                Map.of("role", role, "joinedAt", System.currentTimeMillis()), role);

        Map<String, Object> r = room2Map(room);
        r.put("message", "已加入房间");
        return r;
    }

    @Override
    public Map<String, Object> exitRoom(String roomCode, String role) {
        CoReadRoom room = loadRoomOrFail(roomCode);
        if ("parent".equals(role)) {
            room.setHostJoined(0);
        } else if ("child".equals(role)) {
            room.setGuestJoined(0);
        }
        // 任一方退出房间进入关闭
        if (room.getHostJoined() == 0 || room.getGuestJoined() == 0) {
            room.setState("closed");
            // 移除缓存
            try { redisTemplate.delete(REDIS_ROOM_KEY + roomCode); } catch (Exception ignore) {}
        }
        room.setLastSyncAt(new Date());
        room.setUpdateTime(new Date());
        roomMapper.updateById(room);

        writeEvent(roomCode, room.getFamilyId(), room.getChildId(), "leave",
                Map.of("role", role), role);

        Map<String, Object> r = room2Map(room);
        r.put("message", "已退出房间");
        return r;
    }

    @Override
    public Map<String, Object> closeRoom(String roomCode) {
        CoReadRoom room = loadRoomOrFail(roomCode);
        room.setState("closed");
        room.setHostJoined(0);
        room.setGuestJoined(0);
        room.setLastSyncAt(new Date());
        room.setUpdateTime(new Date());
        roomMapper.updateById(room);
        try { redisTemplate.delete(REDIS_ROOM_KEY + roomCode); } catch (Exception ignore) {}

        writeEvent(roomCode, room.getFamilyId(), room.getChildId(), "close", Map.of(), "parent");
        Map<String, Object> r = room2Map(room);
        r.put("message", "房间已关闭");
        return r;
    }

    @Override
    public CoReadRoom getRoom(String roomCode) {
        return loadRoomOrNull(roomCode);
    }

    @Override
    public Map<String, Object> syncState(String roomCode, String eventType, Map<String, Object> eventData, String senderRole) {
        CoReadRoom room = loadRoomOrFail(roomCode);
        // 权限校验：one_way 模式下，child 不能控制翻页/暂停
        if ("one_way".equals(room.getMode()) && "child".equals(senderRole)
                && ("page_next".equals(eventType) || "page_prev".equals(eventType)
                    || "pause".equals(eventType) || "resume".equals(eventType))) {
            throw new BusinessException("单向观看模式不允许孩子端控制");
        }
        // 应用状态变更
        switch (eventType) {
            case "page_next":
                int cur = room.getCurrentPage() == null ? 1 : room.getCurrentPage();
                int total = room.getTotalPages() == null ? 0 : room.getTotalPages();
                if (cur < total) room.setCurrentPage(cur + 1);
                if ("active".equals(room.getState()) == false && "paused".equals(room.getState()) == false) {
                    room.setState("active");
                }
                break;
            case "page_prev":
                int c2 = room.getCurrentPage() == null ? 1 : room.getCurrentPage();
                if (c2 > 1) room.setCurrentPage(c2 - 1);
                break;
            case "pause":
                room.setState("paused");
                break;
            case "resume":
                room.setState("active");
                break;
            case "voice_insert":
                // 不变更房间状态，只发事件
                break;
            case "mode_change":
                String newMode = eventData == null ? null : (String) eventData.get("mode");
                if (StringUtils.hasText(newMode)) room.setMode(newMode);
                break;
            default:
                break;
        }
        room.setLastSyncAt(new Date());
        room.setUpdateTime(new Date());
        roomMapper.updateById(room);
        cacheRoom(room);

        // 写事件流
        Map<String, Object> evData = eventData == null ? new HashMap<>() : new HashMap<>(eventData);
        evData.put("currentPage", room.getCurrentPage());
        evData.put("state", room.getState());
        evData.put("timestamp", System.currentTimeMillis());
        writeEvent(roomCode, room.getFamilyId(), room.getChildId(), eventType, evData, senderRole);

        return room2Map(room);
    }

    @Override
    public List<CoReadEvent> pollEvents(String roomCode, Long sinceId) {
        CoReadRoom room = loadRoomOrNull(roomCode);
        if (room == null) return Collections.emptyList();
        LambdaQueryWrapper<CoReadEvent> w = new LambdaQueryWrapper<CoReadEvent>()
                .eq(CoReadEvent::getRoomCode, roomCode);
        if (sinceId != null && sinceId > 0) {
            w.gt(CoReadEvent::getId, sinceId);
        }
        w.orderByAsc(CoReadEvent::getId);
        // 限制每次最多50条
        w.last("LIMIT 50");
        return eventMapper.selectList(w);
    }

    @Override
    public Map<String, Object> voiceInsert(String roomCode, String audioUrl, String text) {
        CoReadRoom room = loadRoomOrFail(roomCode);
        Map<String, Object> evData = new HashMap<>();
        evData.put("audioUrl", audioUrl);
        evData.put("text", text);
        evData.put("timestamp", System.currentTimeMillis());
        writeEvent(roomCode, room.getFamilyId(), room.getChildId(), "voice_insert", evData, "parent");
        // 房间状态不变，但记录最后同步时间
        room.setLastSyncAt(new Date());
        room.setUpdateTime(new Date());
        roomMapper.updateById(room);
        Map<String, Object> r = new HashMap<>();
        r.put("ok", true);
        r.put("message", "家长语音已插入");
        return r;
    }

    @Override
    public Map<String, Object> switchMode(String roomCode, String newMode) {
        if (!"one_way".equals(newMode) && !"two_way".equals(newMode)) {
            throw new BusinessException("模式仅支持 one_way / two_way");
        }
        return syncState(roomCode, "mode_change", Map.of("mode", newMode), "parent");
    }

    @Override
    public List<CoReadRoom> listMyRooms(Long familyId) {
        return roomMapper.selectList(new LambdaQueryWrapper<CoReadRoom>()
                .eq(CoReadRoom::getFamilyId, familyId)
                .eq(CoReadRoom::getDelFlag, "0")
                .orderByDesc(CoReadRoom::getCreateTime));
    }

    @Override
    public int cleanExpiredRooms() {
        // 关闭所有过期的房间
        Date now = new Date();
        List<CoReadRoom> expired = roomMapper.selectList(new LambdaQueryWrapper<CoReadRoom>()
                .lt(CoReadRoom::getExpireAt, now)
                .in(CoReadRoom::getState, "waiting", "active", "paused")
                .eq(CoReadRoom::getDelFlag, "0"));
        int cnt = 0;
        for (CoReadRoom r : expired) {
            r.setState("closed");
            r.setUpdateTime(now);
            roomMapper.updateById(r);
            try { redisTemplate.delete(REDIS_ROOM_KEY + r.getRoomCode()); } catch (Exception ignore) {}
            writeEvent(r.getRoomCode(), r.getFamilyId(), r.getChildId(), "close",
                    Map.of("reason", "expired"), "system");
            cnt++;
        }
        log.info("[CoRead] cleaned {} expired rooms", cnt);
        return cnt;
    }

    // ==================== 工具 ====================

    private String genRoomCode() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(ROOM_CODE_LEN);
        for (int i = 0; i < ROOM_CODE_LEN; i++) {
            sb.append(ROOM_CODE_ALPHABET.charAt(rnd.nextInt(ROOM_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private CoReadRoom loadRoomOrFail(String roomCode) {
        if (!StringUtils.hasText(roomCode)) throw new BusinessException("roomCode 不能为空");
        // 先查 Redis
        Object cached = null;
        try {
            cached = redisTemplate.opsForValue().get(REDIS_ROOM_KEY + roomCode);
        } catch (Exception ignore) { /* Redis 不可用降级 */ }
        if (cached instanceof CoReadRoom) {
            // 验证未过期
            CoReadRoom r = (CoReadRoom) cached;
            if (r.getExpireAt() == null || r.getExpireAt().after(new Date())) {
                return r;
            }
        }
        // 查 DB
        CoReadRoom room = roomMapper.selectOne(new LambdaQueryWrapper<CoReadRoom>()
                .eq(CoReadRoom::getRoomCode, roomCode)
                .eq(CoReadRoom::getDelFlag, "0"));
        if (room == null) {
            throw new BusinessException("房间不存在或已关闭: " + roomCode);
        }
        // 检查过期
        if (room.getExpireAt() != null && room.getExpireAt().before(new Date())
                && !"closed".equals(room.getState()) && !"finished".equals(room.getState())) {
            room.setState("closed");
            room.setUpdateTime(new Date());
            roomMapper.updateById(room);
            throw new BusinessException("房间码已过期，请重新创建房间");
        }
        // 检查关闭
        if ("closed".equals(room.getState()) || "finished".equals(room.getState())) {
            throw new BusinessException("房间已" + ("closed".equals(room.getState()) ? "关闭" : "结束"));
        }
        cacheRoom(room);
        return room;
    }

    private CoReadRoom loadRoomOrNull(String roomCode) {
        try {
            return loadRoomOrFail(roomCode);
        } catch (BusinessException e) {
            return null;
        }
    }

    private void cacheRoom(CoReadRoom room) {
        try {
            redisTemplate.opsForValue().set(REDIS_ROOM_KEY + room.getRoomCode(), room,
                    ROOM_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[CoRead] redis cache failed: {}", e.getMessage());
        }
    }

    private void writeEvent(String roomCode, Long familyId, Long childId, String type,
                            Map<String, Object> data, String senderRole) {
        CoReadEvent ev = new CoReadEvent();
        ev.setRoomCode(roomCode);
        ev.setFamilyId(familyId);
        ev.setChildId(childId);
        ev.setEventType(type);
        ev.setEventData(toJsonString(data));
        ev.setSenderRole(senderRole);
        ev.setCreateTime(new Date());
        eventMapper.insert(ev);
    }

    private String toJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        // 简化 JSON 序列化（无外部库依赖）
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number) {
                sb.append(v);
            } else if (v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append("\"").append(escape(String.valueOf(v))).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private Map<String, Object> room2Map(CoReadRoom room) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", room.getId());
        m.put("roomCode", room.getRoomCode());
        m.put("familyId", room.getFamilyId());
        m.put("childId", room.getChildId());
        m.put("bookId", room.getBookId());
        m.put("bookTitle", room.getBookTitle());
        m.put("taskId", room.getTaskId());
        m.put("hostRole", room.getHostRole());
        m.put("mode", room.getMode());
        m.put("state", room.getState());
        m.put("currentPage", room.getCurrentPage());
        m.put("totalPages", room.getTotalPages());
        m.put("hostJoined", room.getHostJoined());
        m.put("guestJoined", room.getGuestJoined());
        m.put("lastSyncAt", room.getLastSyncAt());
        m.put("expireAt", room.getExpireAt());
        m.put("expireSeconds", room.getExpireAt() == null ? 0 :
                Math.max(0, (room.getExpireAt().getTime() - System.currentTimeMillis()) / 1000));
        return m;
    }
}
