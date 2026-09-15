package com.picturebook.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picturebook.admin.domain.SysAdmin;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员账号 Mapper
 *
 * @author Agent-4
 */
@Mapper
public interface SysAdminMapper extends BaseMapper<SysAdmin> {
}
