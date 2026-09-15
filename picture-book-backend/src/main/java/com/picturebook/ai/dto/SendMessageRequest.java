package com.picturebook.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class SendMessageRequest {

    /** 会话ID */
    private Long sessionId;

    /** 角色ID */
    private Long roleId;

    /** 消息类型：text/voice/image */
    private String messageType;

    /** 消息内容（文字或语音转写文本） */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /** 媒体文件URL（语音/图片） */
    private String mediaUrl;
}
