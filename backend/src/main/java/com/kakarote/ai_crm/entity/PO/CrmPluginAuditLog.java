package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 插件网关调用与生命周期事件的不可变审计。
 */
@Data
@TableName("crm_plugin_audit_log")
@Schema(name = "CrmPluginAuditLog", description = "Plugin gateway/lifecycle audit log")
public class CrmPluginAuditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long logId;

    private String pluginId;

    private String actor;

    /** api_call / install / uninstall / enable / disable / webhook_delivery ... */
    private String action;

    private String scope;

    /** success / denied / throttled / error */
    private String result;

    private String requestMeta;

    /** 预留：NULL=单实例；未来多租户用 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
