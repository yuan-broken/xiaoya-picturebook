package com.picturebook.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.ai.domain.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
