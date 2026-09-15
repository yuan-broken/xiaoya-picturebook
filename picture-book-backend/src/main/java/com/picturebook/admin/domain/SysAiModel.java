package com.picturebook.admin.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import java.math.BigDecimal;

/**
 * AI 模型配置实体（M14）
 * 对应表 sys_ai_model
 */
@TableName("sys_ai_model")
public class SysAiModel extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 能力类型 llm/tts/asr/vlm/emotion */
    private String capabilityType;

    /** 服务商 */
    private String provider;

    /** 模型名 */
    private String modelName;

    /** API 地址 */
    private String apiUrl;

    /** API Key（AES加密存储） */
    private String apiKey;

    /** 温度 */
    private BigDecimal temperature;

    /** 最大 Token */
    private Integer maxToken;

    /** 是否流式 */
    private Integer streamEnabled;

    /** 默认音色 */
    private String defaultVoice;

    /** 首字延迟（毫秒） */
    private Integer firstTokenLatencyMs;

    /** 方言列表 */
    private String dialectList;

    /** 是否启用 */
    private Integer enabled;

    /** 排序 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCapabilityType() { return capabilityType; }
    public void setCapabilityType(String capabilityType) { this.capabilityType = capabilityType; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public Integer getMaxToken() { return maxToken; }
    public void setMaxToken(Integer maxToken) { this.maxToken = maxToken; }
    public Integer getStreamEnabled() { return streamEnabled; }
    public void setStreamEnabled(Integer streamEnabled) { this.streamEnabled = streamEnabled; }
    public String getDefaultVoice() { return defaultVoice; }
    public void setDefaultVoice(String defaultVoice) { this.defaultVoice = defaultVoice; }
    public Integer getFirstTokenLatencyMs() { return firstTokenLatencyMs; }
    public void setFirstTokenLatencyMs(Integer firstTokenLatencyMs) { this.firstTokenLatencyMs = firstTokenLatencyMs; }
    public String getDialectList() { return dialectList; }
    public void setDialectList(String dialectList) { this.dialectList = dialectList; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
