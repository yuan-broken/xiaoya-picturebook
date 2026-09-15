package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 声音克隆实体 biz_voice_clone
 * 记录家庭自定义的克隆音色任务（录音样本 → Mock 训练 → 可选用朗读）
 *
 * @author Agent-AI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_voice_clone")
public class VoiceClone extends BaseEntity {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属家庭ID */
    private Long familyId;

    /** 自定义音色名 */
    private String voiceName;

    /** 录音样本URL（/uploads/voice_clone/xxx.wav） */
    private String sampleAudioUrl;

    /** 训练状态：pending/training/success/failed */
    private String status;

    /** 训练后返回的音色ID（如 clone_1_xxx） */
    private String voiceId;

    /** 失败原因 */
    private String failReason;
}
