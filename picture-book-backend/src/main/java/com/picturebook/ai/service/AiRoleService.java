package com.picturebook.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picturebook.ai.domain.AiRole;
import com.picturebook.ai.mapper.AiRoleMapper;
import com.picturebook.common.constant.CommonConstants;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI角色服务
 */
@Service
public class AiRoleService extends ServiceImpl<AiRoleMapper, AiRole> {

    /**
     * 用户端：获取已启用角色列表
     */
    public List<AiRole> listEnabledRoles() {
        return this.list(new LambdaQueryWrapper<AiRole>()
                .eq(AiRole::getStatus, CommonConstants.STATUS_ENABLED)
                .orderByAsc(AiRole::getSortOrder));
    }

    /**
     * 管理端：新增/编辑角色
     */
    public void saveRole(AiRole role) {
        if (role.getStatus() == null) {
            role.setStatus(CommonConstants.STATUS_ENABLED);
        }
        this.saveOrUpdate(role);
    }
}
