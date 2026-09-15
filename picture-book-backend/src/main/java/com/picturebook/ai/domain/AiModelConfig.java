package com.picturebook.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import java.math.BigDecimal;

/**
 * AI 模型配置 Domain（M14）
 * 与 SysAiModel 对应，提供 AI 业务域访问的视图对象
 */
@TableName("sys_ai_model")
public class AiModelConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String capabilityType;
    private String provider;
    private String modelName;
    private String apiUrl;
    private String apiKey;
    private BigDecimal temperature;
    private Integer maxToken;
    private Integer streamEnabled;
    private String defaultVoice;
    private Integer firstTokenLatencyMs;
    private String dialectList;
    private Integer enabled;
    private Integer sortOrder;
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
