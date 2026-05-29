package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

import java.util.List;

@Data
public class MailSyncPolicyBO {

    private Long accountId;

    private List<String> folders;

    private Integer syncDays;

    private Integer syncLimit;

    private String bodySyncMode;

    private String attachmentSyncMode;

    private Long maxAutoAttachmentSize;

    private Integer retentionDays;

    private Boolean extractActions;
}
