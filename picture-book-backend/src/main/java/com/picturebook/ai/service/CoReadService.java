package com.picturebook.ai.service;

import com.picturebook.ai.domain.CoReadEvent;
import com.picturebook.ai.domain.CoReadRoom;

import java.util.List;
import java.util.Map;

/**
 * 远程共读服务（M34）
 * - 房间创建/加入/退出
 * - HTTP 轮询状态同步（替代 WebSocket）
 * - 家长语音插入
 * - 单向观看/双向参与模式
 *
 * @author Phase2
 */
public interface CoReadService {

    /**
     * 创建房间，返回房间码
     * @param familyId 发起家长家庭ID
     * @param childId 孩子ID（可空）
     * @param bookId 绘本ID
     * @param taskId 讲读任务ID（可空，加入时关联）
     * @param mode one_way / two_way
     */
    Map<String, Object> createRoom(Long familyId, Long childId, Long bookId, Long taskId, String mode);

    /**
     * 加入房间
     * @param roomCode 房间码
     * @param role parent/child
     */
    Map<String, Object> joinRoom(String roomCode, String role);

    /**
     * 退出房间
     */
    Map<String, Object> exitRoom(String roomCode, String role);

    /**
     * 关闭房间（家长主动结束）
     */
    Map<String, Object> closeRoom(String roomCode);

    /**
     * 获取房间状态
     */
    CoReadRoom getRoom(String roomCode);

    /**
     * 同步状态变更（翻页/暂停/继续/家长语音插入）
     * @param roomCode 房间码
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @param senderRole parent/child/system
     */
    Map<String, Object> syncState(String roomCode, String eventType, Map<String, Object> eventData, String senderRole);

    /**
     * 拉取最近事件（HTTP 轮询入口，2秒一次）
     * @param sinceId 上次拉取的最大事件ID
     */
    List<CoReadEvent> pollEvents(String roomCode, Long sinceId);

    /**
     * 家长语音插入
     * @param roomCode 房间码
     * @param audioUrl 音频URL（前端先上传获得）
     * @param text 文字内容（前端可选调ASR获得）
     */
    Map<String, Object> voiceInsert(String roomCode, String audioUrl, String text);

    /**
     * 切换参与模式 one_way/two_way
     */
    Map<String, Object> switchMode(String roomCode, String newMode);

    /**
     * 列出我（家长）发起的房间
     */
    List<CoReadRoom> listMyRooms(Long familyId);

    /**
     * 清理过期房间（定时任务）
     */
    int cleanExpiredRooms();
}
