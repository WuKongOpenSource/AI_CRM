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
@TableName("crm_mail_message")
@Schema(name = "MailMessage", description = "已同步邮件")
public class MailMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long messageId;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long accountId;

    private Long userId;

    private String provider;

    private String providerMessageId;

    private String internetMessageId;

    private String threadId;

    private String folder;

    private String direction;

    private String subject;

    private String fromName;

    private String fromAddress;

    private String toAddresses;

    private String ccAddresses;

    private String bccAddresses;

    private Date sentTime;

    private Date receivedTime;

    private String bodySyncMode;

    private String bodySyncStatus;

    private String summary;

    private String keywords;

    private String intent;

    private String actionItemsJson;

    private Date replyDeadlineTime;

    private String extractionStatus;

    private String extractionError;

    private String bodyText;

    private String bodyHtml;

    private String rawFilePath;

    private Long rawFileSize;

    private Boolean hasAttachments;

    private String readStatus;

    private Boolean starred;

    private Boolean deleted;

    private Long customerId;

    private Long contactId;

    private Long knowledgeId;

    private String syncStatus;

    private String syncError;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
