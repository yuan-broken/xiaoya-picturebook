package com.picturebook.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picturebook.admin.domain.SysAiModel;
import com.picturebook.admin.mapper.SysAiModelMapper;
import com.picturebook.common.utils.AesUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * AI 模型配置服务（M14）
 * - 增删改查
 * - API Key 加密存储
 * - 列表查询脱敏
 * - 同能力类型同时只允许一个启用
 */
@Service
public class SysAiModelService extends ServiceImpl<SysAiModelMapper, SysAiModel> {

    @Value("${picturebook.ai.review.aes-key}")
    private String aesKey;

    /**
     * 分页查询
     */
    public IPage<SysAiModel> queryPage(int pageNum, int pageSize, String capabilityType, String provider) {
        Page<SysAiModel> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysAiModel> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(capabilityType)) {
            wrapper.eq(SysAiModel::getCapabilityType, capabilityType);
        }
        if (StringUtils.hasText(provider)) {
            wrapper.like(SysAiModel::getProvider, provider);
        }
        wrapper.orderByAsc(SysAiModel::getSortOrder).orderByDesc(SysAiModel::getCreateTime);
        IPage<SysAiModel> result = this.page(page, wrapper);
        // 脱敏 api_key
        result.getRecords().forEach(this::maskApiKey);
        return result;
    }

    /**
     * 查询全部启用配置
     */
    public List<SysAiModel> listEnabled() {
        LambdaQueryWrapper<SysAiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAiModel::getEnabled, 1)
                .orderByAsc(SysAiModel::getCapabilityType);
        return this.list(wrapper);
    }

    /**
     * 新增配置
     */
    public boolean addModel(SysAiModel model) {
        encryptApiKey(model);
        return this.save(model);
    }

    /**
     * 编辑配置
     */
    public boolean updateModel(SysAiModel model) {
        // 若传入的 apiKey 已是脱敏格式（****），则保留原值
        if (model.getApiKey() != null && model.getApiKey().contains("****")) {
            SysAiModel existing = this.getById(model.getId());
            if (existing != null) {
                model.setApiKey(existing.getApiKey());
            }
        } else {
            encryptApiKey(model);
        }
        return this.updateById(model);
    }

    /**
     * 切换启用状态（同类型其他自动禁用）
     */
    public boolean toggleEnabled(Long id) {
        SysAiModel model = this.getById(id);
        if (model == null) {
            return false;
        }
        int newEnabled = model.getEnabled() == 1 ? 0 : 1;
        if (newEnabled == 1) {
            // 同类型其他记录禁用
            LambdaQueryWrapper<SysAiModel> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysAiModel::getCapabilityType, model.getCapabilityType())
                    .eq(SysAiModel::getEnabled, 1)
                    .ne(SysAiModel::getId, id);
            List<SysAiModel> others = this.list(wrapper);
            others.forEach(o -> {
                o.setEnabled(0);
                this.updateById(o);
            });
        }
        model.setEnabled(newEnabled);
        return this.updateById(model);
    }

    /**
     * 解密 API Key（内部调用用，不对外返回明文）
     */
    public String decryptApiKey(SysAiModel model) {
        if (model == null || !StringUtils.hasText(model.getApiKey())) {
            return null;
        }
        try {
            return AesUtil.decrypt(model.getApiKey(), aesKey);
        } catch (Exception e) {
            // 可能是未加密的占位数据
            return model.getApiKey();
        }
    }

    private void encryptApiKey(SysAiModel model) {
        if (StringUtils.hasText(model.getApiKey()) && !model.getApiKey().contains("****")) {
            model.setApiKey(AesUtil.encrypt(model.getApiKey(), aesKey));
        }
    }

    private void maskApiKey(SysAiModel model) {
        if (StringUtils.hasText(model.getApiKey())) {
            try {
                String plain = AesUtil.decrypt(model.getApiKey(), aesKey);
                model.setApiKey(AesUtil.mask(plain));
            } catch (Exception e) {
                model.setApiKey(AesUtil.mask(model.getApiKey()));
            }
        }
    }
}
