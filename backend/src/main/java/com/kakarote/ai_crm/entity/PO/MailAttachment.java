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
@TableName("crm_mail_attachment")
public class MailAttachment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long attachmentId;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    private Long messageId;

    private String providerAttachmentId;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String filePath;

    private String contentText;

    private Long knowledgeId;

    private String downloadStatus;

    private String scanStatus;

    private String syncMode;

    private String downloadError;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
