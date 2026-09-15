package com.picturebook.ai.controller;

import com.picturebook.ai.domain.VoiceClone;
import com.picturebook.ai.service.VoiceCloneService;
import com.picturebook.common.core.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 声音克隆 Controller
 *
 * 接口（均需登录）：
 * - POST /api/ai/voice/clone      提交克隆任务 {voiceName, sampleAudioUrl}
 * - GET  /api/ai/voice/clone/list 列出当前家庭克隆音色
 * - GET  /api/ai/voice/clone/{id} 查询单条状态
 * - DELETE /api/ai/voice/clone/{id} 软删除
 *
 * @author Agent-AI
 */
@RestController
@RequestMapping("/api/ai/voice/clone")
public class VoiceCloneController {

    private final VoiceCloneService voiceCloneService;

    public VoiceCloneController(VoiceCloneService voiceCloneService) {
        this.voiceCloneService = voiceCloneService;
    }

    @PostMapping
    public Result<VoiceClone> submit(@RequestBody Map<String, String> body) {
        String voiceName = body.get("voiceName");
        String sampleAudioUrl = body.get("sampleAudioUrl");
        if (voiceName == null || voiceName.trim().isEmpty()) {
            return Result.fail("音色名称不能为空");
        }
        if (sampleAudioUrl == null || sampleAudioUrl.trim().isEmpty()) {
            return Result.fail("录音样本不能为空");
        }
        Long familyId = currentFamilyId();
        VoiceClone vc = voiceCloneService.submitClone(familyId, voiceName.trim(), sampleAudioUrl.trim());
        return Result.ok(vc);
    }

    @GetMapping("/list")
    public Result<List<VoiceClone>> list() {
        Long familyId = currentFamilyId();
        return Result.ok(voiceCloneService.listByFamily(familyId));
    }

    @GetMapping("/{id}")
    public Result<VoiceClone> get(@PathVariable Long id) {
        VoiceClone vc = voiceCloneService.getById(id);
        if (vc == null) {
            return Result.fail("记录不存在");
        }
        return Result.ok(vc);
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        Long familyId = currentFamilyId();
        boolean ok = voiceCloneService.removeById(id, familyId);
        return ok ? Result.ok() : Result.fail("删除失败");
    }

    private Long currentFamilyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long) {
            return (Long) auth.getDetails();
        }
        return 1L;
    }
}
