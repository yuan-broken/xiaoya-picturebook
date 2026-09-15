package com.picturebook.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.user.domain.UserBadge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户勋章 Mapper
 *
 * @author Phase2
 */
@Mapper
public interface UserBadgeMapper extends BaseMapper<UserBadge> {
}
