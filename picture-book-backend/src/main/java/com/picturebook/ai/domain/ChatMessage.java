package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI对话消息表
 */
@Data
@TableName("biz_chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long messageId;

    private Long sessionId;
    /** 发送者类型（user/ai） */
    private String senderType;
    /** 消息类型（text/voice/image） */
    private String messageType;
    private String content;
    private String mediaUrl;
    private Integer durationMs;
    private String modelName;
    /** 异常状态 */
    private String exceptionStatus;
    private Date createTime;
}
