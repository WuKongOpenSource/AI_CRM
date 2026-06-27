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
 * 插件授权/同意记录：安装时管理员批准的 module:action scope（已按管理员权限下放）。
 */
@Data
@TableName("crm_plugin_grant")
@Schema(name = "CrmPluginGrant", description = "Plugin consent grant")
public class CrmPluginGrant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long grantId;

    private String pluginId;

    private String clientId;

    /** 已批准 scope（CSV，module:action） */
    private String grantedScopes;

    private Long grantedByUserId;

    @TableField(fill = FieldFill.INSERT)
    private Long createUserId;

    /** 预留：NULL=单实例；未来多租户用 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
