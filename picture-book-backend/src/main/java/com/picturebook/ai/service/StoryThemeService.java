package com.picturebook.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picturebook.ai.domain.StoryTheme;
import com.picturebook.ai.mapper.StoryThemeMapper;
import com.picturebook.common.constant.CommonConstants;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 故事主题服务
 */
@Service
public class StoryThemeService extends ServiceImpl<StoryThemeMapper, StoryTheme> {

    /**
     * 用户端：获取已启用主题列表
     */
    public List<StoryTheme> listEnabledThemes() {
        return this.list(new LambdaQueryWrapper<StoryTheme>()
                .eq(StoryTheme::getStatus, CommonConstants.STATUS_ENABLED)
                .orderByAsc(StoryTheme::getSortOrder));
    }

    /**
     * 管理端：新增/编辑主题
     */
    public void saveTheme(StoryTheme theme) {
        if (theme.getStatus() == null) {
            theme.setStatus(CommonConstants.STATUS_ENABLED);
        }
        this.saveOrUpdate(theme);
    }
}
