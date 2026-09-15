package com.picturebook.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picturebook.admin.domain.SysConfig;
import com.picturebook.admin.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务（M20）
 *
 * 配置项：
 *   product.*          产品基础（名称/Logo/首页文案）
 *   upload.*            上传限制
 *   ai.*                默认AI模型/TTS/Prompt
 *   privacy.*           隐私提示
 *   antiAddiction.*     防沉迷
 */
@Service
public class SysConfigService extends ServiceImpl<SysConfigMapper, SysConfig> {

    /**
     * 本地缓存（减少数据库查询）
     */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 获取配置值
     */
    public String getConfigValue(String key) {
        return cache.computeIfAbsent(key, k -> {
            SysConfig config = findByKey(k);
            return config != null ? config.getConfigValue() : null;
        });
    }

    /**
     * 获取配置值（带默认值）
     */
    public String getConfigValue(String key, String defaultValue) {
        String val = getConfigValue(key);
        return StringUtils.hasText(val) ? val : defaultValue;
    }

    /**
     * 获取 int 类型配置
     */
    public int getConfigInt(String key, int defaultValue) {
        String val = getConfigValue(key);
        if (!StringUtils.hasText(val)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 设置配置值
     */
    public boolean setConfigValue(String key, String value) {
        SysConfig config = findByKey(key);
        if (config != null) {
            config.setConfigValue(value);
            boolean ok = this.updateById(config);
            if (ok) {
                cache.put(key, value);
            }
            return ok;
        } else {
            // 不存在则新增
            SysConfig newConfig = new SysConfig();
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(value);
            newConfig.setConfigType("string");
            boolean ok = this.save(newConfig);
            if (ok) {
                cache.put(key, value);
            }
            return ok;
        }
    }

    /**
     * 批量获取配置（以 Map 返回）
     */
    public Map<String, String> getConfigMap(String prefix) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(prefix)) {
            wrapper.likeRight(SysConfig::getConfigKey, prefix);
        }
        List<SysConfig> list = this.list(wrapper);
        Map<String, String> result = new HashMap<>();
        list.forEach(c -> result.put(c.getConfigKey(), c.getConfigValue()));
        return result;
    }

    /**
     * 获取防沉迷配置
     */
    public Map<String, Object> getAntiAddictionConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("singleSessionMinutes", getConfigInt("antiAddiction.single", 30));
        result.put("dailyMinutes", getConfigInt("antiAddiction.daily", 60));
        result.put("nightModeStart", getConfigValue("antiAddiction.nightStart", "21:00"));
        result.put("nightModeBrightness", getConfigInt("antiAddiction.nightBrightness", 30));
        return result;
    }

    /**
     * 获取产品基础配置
     */
    public Map<String, String> getProductConfig() {
        return getConfigMap("product.");
    }

    /**
     * 清除缓存（配置变更后调用）
     */
    public void clearCache() {
        cache.clear();
    }

    private SysConfig findByKey(String key) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        return this.getOne(wrapper);
    }
}
