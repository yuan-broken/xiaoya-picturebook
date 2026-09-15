package com.picturebook.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.ai.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
