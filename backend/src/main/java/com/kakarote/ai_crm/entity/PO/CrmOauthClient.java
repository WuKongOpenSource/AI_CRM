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
 * 插件 OAuth 应用注册表（实例全局，无 tenant_id）。
 */
@Data
@TableName("crm_oauth_client")
@Schema(name = "CrmOauthClient", description = "Plugin OAuth client registry")
public class CrmOauthClient implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** OAuth client_id（全局唯一） */
    private String clientId;

    /** 所属插件标识 */
    private String pluginId;

    private String displayName;

    /** client_secret 的 SHA-256 哈希，绝不存明文 */
    private String clientSecretHash;

    /** 允许的授权类型，CSV：client_credentials,authorization_code */
    private String grantTypes;

    /** 默认申请 scope（CSV，module:action 词表） */
    private String defaultScopes;

    private String redirectUris;

    private String signingKeyId;

    /** 0=停用 1=启用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
