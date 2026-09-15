package com.picturebook.framework.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.picturebook.common.utils.SecurityUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis Plus 自动填充处理器
 * 自动填充 createBy/createTime/updateBy/updateTime
 */
@Component
public class MybatisMetaHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        String username = SecurityUtil.getCurrentUsername();
        this.strictInsertFill(metaObject, "createBy", String.class, username);
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateBy", String.class, username);
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "delFlag", String.class, "0");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateBy", String.class, SecurityUtil.getCurrentUsername());
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
