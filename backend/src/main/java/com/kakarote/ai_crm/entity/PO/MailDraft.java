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
@TableName("crm_mail_draft")
public class MailDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long draftId;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long userId;

    private Long accountId;

    private Long customerId;

    private Long contactId;

    private Long sourceMessageId;

    private String toAddresses;

    private String ccAddresses;

    private String bccAddresses;

    private String subject;

    private String bodyText;

    private String attachmentRefs;

    private String status;

    private String riskStatus;

    private String riskReasons;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
