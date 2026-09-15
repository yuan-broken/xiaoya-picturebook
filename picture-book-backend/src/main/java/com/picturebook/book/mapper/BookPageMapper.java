package com.picturebook.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.book.domain.BookPage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookPageMapper extends BaseMapper<BookPage> {
}
