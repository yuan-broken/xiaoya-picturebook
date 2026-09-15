package com.picturebook.ai.service;

import com.picturebook.ai.domain.VoiceClone;

import java.util.List;

/**
 * 声音克隆服务
 * - 提交克隆任务（录音样本 → Mock 训练）
 * - 列出家庭已克隆音色
 * - 查询训练状态
 * - 软删除
 *
 * 当前为 Mock 实现：提交后异步 3 秒变 success。
 * 未来接入真实 TTS 服务商（如火山引擎声音复刻）时，替换 trainInternal 即可。
 *
 * @author Agent-AI
 */
public interface VoiceCloneService {

    /**
     * 提交声音克隆任务
     * @param familyId 家庭ID
     * @param voiceName 自定义音色名
     * @param sampleAudioUrl 录音样本URL（由 /api/file/upload 上传得到）
     * @return 创建的克隆记录（含 id、status=pending）
     */
    VoiceClone submitClone(Long familyId, String voiceName, String sampleAudioUrl);

    /**
     * 列出当前家庭未删除的克隆音色
     */
    List<VoiceClone> listByFamily(Long familyId);

    /**
     * 查询单条克隆记录（含训练状态）
     */
    VoiceClone getById(Long id);

    /**
     * 软删除克隆音色
     */
    boolean removeById(Long id, Long familyId);
}
