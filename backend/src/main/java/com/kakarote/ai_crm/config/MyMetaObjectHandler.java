package com.kakarote.ai_crm.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.utils.UserUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis Plus 字段自动填充处理器
 * 用于自动填充 createTime, updateTime, createUserId 等字段
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入Fill。
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        // 填充更新时间
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        // 填充创建人ID；系统任务和异步日志线程允许没有用户上下文。
        Long userId = UserUtil.getUserIdOrNull();
        if (userId != null) {
            this.strictInsertFill(metaObject, "createUserId", Long.class, userId);
        }
        // 填充租户ID
        if (metaObject.hasSetter("tenantId")) {
            Object tenantId = getFieldValByName("tenantId", metaObject);
            if (tenantId == null) {
                Long currentTenantId = TenantContextHolder.getTenantId();
                if (currentTenantId != null) {
                    this.strictInsertFill(metaObject, "tenantId", Long.class, currentTenantId);
                }
            }
        }
    }

    /**
     * 更新Fill。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
