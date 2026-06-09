package com.kakarote.ai_crm.entity.PO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("crm_wecom_corp_config")
public class WecomCorpConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private String corpId;

    private String corpName;

    private String agentId;

    private String suiteId;

    private String permanentCodeEncrypted;

    private String authInfoJson;

    private String authCorpInfoJson;

    private String authStatus;

    private Date authorizedAt;

    private Date unauthorizedAt;

    private String authUserId;

    private String authUserName;

    private String archiveSecretEncrypted;

    private String archivePrivateKeyEncrypted;

    private String archivePublicKeyVersion;

    private String archiveCorpId;

    private Boolean archiveEnabled;

    private Boolean customerContactEnabled;

    private Boolean syncEnabled;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
