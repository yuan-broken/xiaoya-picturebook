package com.picturebook.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.ai.domain.ReadingTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReadingTaskMapper extends BaseMapper<ReadingTask> {
}
