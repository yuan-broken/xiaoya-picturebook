package com.picturebook.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.book.domain.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
