package com.picturebook.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.user.domain.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏夹 Mapper
 *
 * @author Phase2
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
