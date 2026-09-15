package com.picturebook.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.user.domain.Badge;
import org.apache.ibatis.annotations.Mapper;

/**
 * 勋章定义 Mapper
 *
 * @author Phase2
 */
@Mapper
public interface BadgeMapper extends BaseMapper<Badge> {
}
