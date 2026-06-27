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
 * 插件安装记录（按实例,预留 tenant_id）。config_encrypted 既存插件配置,
 * 也用于记录声明式扩展的已应用引用(供卸载反向)。
 */
@Data
@TableName("crm_plugin_install")
@Schema(name = "CrmPluginInstall", description = "Plugin install record")
public class CrmPluginInstall implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long installId;

    private String pluginId;

    private String version;

    /** 0=停用 1=启用 */
    private Integer enabled;

    /** 加密存储：插件配置 + 已应用声明式扩展引用 */
    private String configEncrypted;

    /** marketplace / local_manifest */
    private String installSource;

    /** 预留：NULL=单实例；未来多租户用 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
