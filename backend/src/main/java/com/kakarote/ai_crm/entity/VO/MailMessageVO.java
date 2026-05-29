package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MailMessageVO {

    private Long messageId;

    private Long accountId;

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

    private String bodyText;

    private String bodyHtml;

    private Boolean hasAttachments;

    private String readStatus;

    private Boolean starred;

    private Boolean deleted;

    private Long customerId;

    private String customerName;

    private Long contactId;

    private String contactName;

    private Long knowledgeId;

    private Date createTime;

    private List<MailAttachmentVO> attachments;
}
