package com.kakarote.ai_crm.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.kakarote.ai_crm.utils.UserUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis Plus 字段自动填充处理器
 * 用于自动填充 createTime, updateTime, createUserId 等字段
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(MyMetaObjectHandler.class);

    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        // 填充更新时间
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        // 填充创建人ID（捕获异常避免未登录时报错）
        try {
            Long userId = UserUtil.getUserId();
            if (userId != null) {
                this.strictInsertFill(metaObject, "createUserId", Long.class, userId);
            }
        } catch (Exception e) {
            log.debug("无法获取当前用户ID进行自动填充: {}", e.getMessage());
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
