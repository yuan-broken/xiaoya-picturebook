package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI对话会话表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_chat_session")
public class ChatSession extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long sessionId;

    private String sessionNo;
    private Long familyId;
    private Long roleId;
    private String roleName;
    private Integer messageCount;
    private String lastMessage;
    /** 状态（0=正常 1=已结束） */
    private String status;
}
