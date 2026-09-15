package com.picturebook.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.ai.domain.CoReadEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 远程共读事件 Mapper
 *
 * @author Phase2
 */
@Mapper
public interface CoReadEventMapper extends BaseMapper<CoReadEvent> {
}
