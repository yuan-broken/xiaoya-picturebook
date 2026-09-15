package com.picturebook.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.ai.domain.InteractionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InteractionRecordMapper extends BaseMapper<InteractionRecord> {
}
