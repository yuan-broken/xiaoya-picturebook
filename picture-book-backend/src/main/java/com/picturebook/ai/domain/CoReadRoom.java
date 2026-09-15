package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 远程共读房间 biz_coread_room
 *
 * @author Phase2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_coread_room")
public class CoReadRoom extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("room_code")
    private String roomCode;

    @TableField("family_id")
    private Long familyId;

    @TableField("child_id")
    private Long childId;

    @TableField("book_id")
    private Long bookId;

    @TableField("book_title")
    private String bookTitle;

    @TableField("task_id")
    private Long taskId;

    /** 发起者角色 parent/child/both */
    @TableField("host_role")
    private String hostRole;

    /** 模式 one_way/two_way */
    private String mode;

    /** 状态 waiting/active/paused/finished/closed */
    private String state;

    @TableField("current_page")
    private Integer currentPage;

    @TableField("total_pages")
    private Integer totalPages;

    @TableField("host_joined")
    private Integer hostJoined;

    @TableField("guest_joined")
    private Integer guestJoined;

    @TableField("last_sync_at")
    private Date lastSyncAt;

    @TableField("expire_at")
    private Date expireAt;
}
