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
 * 插件事件订阅（领域事件 -> 插件 webhook 端点）。
 */
@Data
@TableName("crm_plugin_subscription")
@Schema(name = "CrmPluginSubscription", description = "Plugin event subscription")
public class CrmPluginSubscription implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long subscriptionId;

    private String pluginId;

    private String eventType;

    private String endpointUrl;

    /** HMAC 签名密钥,SecretTextCipher 加密 */
    private String signingSecretEncrypted;

    /** 0=停用 1=启用 */
    private Integer active;

    /** 预留：NULL=单实例；未来多租户用 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
