package com.picturebook.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.user.domain.HabitCheckin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 阅读打卡 Mapper
 *
 * @author Phase2
 */
@Mapper
public interface HabitCheckinMapper extends BaseMapper<HabitCheckin> {
}
