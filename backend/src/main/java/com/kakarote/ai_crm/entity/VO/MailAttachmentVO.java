package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class MailAttachmentVO {

    private Long attachmentId;

    private Long messageId;

    private String providerAttachmentId;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String filePath;

    private Long knowledgeId;

    private String downloadStatus;

    private String scanStatus;

    private String syncMode;

    private String downloadError;
}
