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

@Data
@TableName("crm_mail_account")
@Schema(name = "MailAccount", description = "邮箱授权账号")
public class MailAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long accountId;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long userId;

    private String provider;

    private String authType;

    private String emailAddress;

    private String displayName;

    private String imapHost;

    private Integer imapPort;

    private Boolean imapSsl;

    private String smtpHost;

    private Integer smtpPort;

    private Boolean smtpSsl;

    private String username;

    private String credentialJson;

    private String folders;

    private Integer syncDays;

    private Integer syncLimit;

    private String bodySyncMode;

    private String attachmentSyncMode;

    private Long maxAutoAttachmentSize;

    private Integer retentionDays;

    private Boolean extractActions;

    private Boolean enabled;

    private Boolean isDefault;

    private String connectionStatus;

    private Date lastUsedTime;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
