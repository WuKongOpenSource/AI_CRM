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
 * 已签发的插件令牌（仅存哈希）。
 */
@Data
@TableName("crm_plugin_token")
@Schema(name = "CrmPluginToken", description = "Issued plugin token (hashed)")
public class CrmPluginToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long tokenId;

    private String pluginId;

    private String clientId;

    /** 令牌的 SHA-256 哈希，绝不存明文 */
    private String tokenHash;

    /** 有效 scope（CSV）= 安装者权限 ∩ 已授权 scope */
    private String scopes;

    private Date expiresAt;

    /** 0=有效 1=已吊销 */
    private Integer revoked;

    /** 预留：NULL=单实例；未来多租户用 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
