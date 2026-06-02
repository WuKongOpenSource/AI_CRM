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
@TableName("crm_tencent_meeting_corp_config")
public class TencentMeetingCorpConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private String appId;

    private String sdkId;

    private String corpName;

    private String secretIdEncrypted;

    private String secretKeyEncrypted;

    private String webhookSecretEncrypted;

    private String stsTokenEncrypted;

    private Date stsTokenExpireTime;

    private String operatorUserId;

    private Boolean syncEnabled;

    private Boolean transcriptEnabled;

    private Boolean archiveToKnowledge;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
