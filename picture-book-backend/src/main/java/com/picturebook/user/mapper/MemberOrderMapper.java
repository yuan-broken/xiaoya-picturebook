package com.picturebook.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.user.domain.MemberOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员订单 Mapper
 *
 * @author Agent-4
 */
@Mapper
public interface MemberOrderMapper extends BaseMapper<MemberOrder> {
}
