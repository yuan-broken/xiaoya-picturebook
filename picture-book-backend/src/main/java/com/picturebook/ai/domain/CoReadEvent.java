package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 远程共读事件流水 biz_coread_event
 * 用于 HTTP 轮询获取最新事件
 *
 * @author Phase2
 */
@Data
@TableName("biz_coread_event")
public class CoReadEvent {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("room_code")
    private String roomCode;

    @TableField("family_id")
    private Long familyId;

    @TableField("child_id")
    private Long childId;

    /** 事件类型: join/leave/page_next/page_prev/pause/resume/voice_insert/state_sync/mode_change */
    @TableField("event_type")
    private String eventType;

    /** 事件 JSON */
    @TableField("event_data")
    private String eventData;

    /** 发送者角色: parent/child/system */
    @TableField("sender_role")
    private String senderRole;

    @TableField("create_time")
    private Date createTime;
}
