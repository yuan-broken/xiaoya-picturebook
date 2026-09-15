package com.picturebook.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picturebook.ai.domain.VoiceClone;
import com.picturebook.ai.mapper.VoiceCloneMapper;
import com.picturebook.ai.service.VoiceCloneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 声音克隆服务实现（Mock 版本）
 *
 * 训练流程：
 * 1. submitClone 插入 pending 记录
 * 2. 新线程 sleep 3 秒 → status=training → sleep 1 秒 → status=success，生成 voiceId
 *
 * 未来接入真实服务商时，只需替换 triggerTrain 方法内的 HTTP 调用即可。
 *
 * @author Agent-AI
 */
@Service
public class VoiceCloneServiceImpl implements VoiceCloneService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCloneServiceImpl.class);

    private final VoiceCloneMapper voiceCloneMapper;

    public VoiceCloneServiceImpl(VoiceCloneMapper voiceCloneMapper) {
        this.voiceCloneMapper = voiceCloneMapper;
    }

    @Override
    public VoiceClone submitClone(Long familyId, String voiceName, String sampleAudioUrl) {
        VoiceClone vc = new VoiceClone();
        vc.setFamilyId(familyId);
        vc.setVoiceName(voiceName);
        vc.setSampleAudioUrl(sampleAudioUrl);
        vc.setStatus("pending");
        vc.setCreateBy("family_" + familyId);
        voiceCloneMapper.insert(vc);

        // 异步触发 Mock 训练
        triggerMockTrain(vc.getId());

        return vc;
    }

    @Override
    public List<VoiceClone> listByFamily(Long familyId) {
        return voiceCloneMapper.selectList(new LambdaQueryWrapper<VoiceClone>()
                .eq(VoiceClone::getFamilyId, familyId)
                .orderByDesc(VoiceClone::getCreateTime));
    }

    @Override
    public VoiceClone getById(Long id) {
        return voiceCloneMapper.selectById(id);
    }

    @Override
    public boolean removeById(Long id, Long familyId) {
        // 仅允许本人家庭删除
        VoiceClone vc = voiceCloneMapper.selectById(id);
        if (vc == null || !vc.getFamilyId().equals(familyId)) {
            return false;
        }
        int rows = voiceCloneMapper.deleteById(id);
        return rows > 0;
    }

    /**
     * Mock 训练：pending → training(1s) → success(再 1s)
     * 真实场景应替换为调用 TTS 服务商的声音复刻 API
     */
    private void triggerMockTrain(Long id) {
        new Thread(() -> {
            try {
                Thread.sleep(1000L);
                VoiceClone training = new VoiceClone();
                training.setId(id);
                training.setStatus("training");
                training.setUpdateTime(new Date());
                voiceCloneMapper.updateById(training);

                Thread.sleep(2000L);
                VoiceClone success = new VoiceClone();
                success.setId(id);
                success.setStatus("success");
                success.setVoiceId("clone_" + id + "_" + System.currentTimeMillis());
                success.setUpdateTime(new Date());
                voiceCloneMapper.updateById(success);

                log.info("Voice clone {} training success", id);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                VoiceClone failed = new VoiceClone();
                failed.setId(id);
                failed.setStatus("failed");
                failed.setFailReason("训练被中断");
                failed.setUpdateTime(new Date());
                voiceCloneMapper.updateById(failed);
            }
        }, "voice-clone-train-" + id).start();
    }
}
