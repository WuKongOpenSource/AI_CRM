package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class MailDraftVO {

    private Long draftId;

    private Long accountId;

    private Long customerId;

    private Long contactId;

    private Long sourceMessageId;

    private String toAddresses;

    private String ccAddresses;

    private String bccAddresses;

    private String subject;

    private String accountEmail;

    private String bodyText;

    private String attachmentRefs;

    private String status;

    private String riskStatus;

    private String riskReasons;

    private Date createTime;

    private Date updateTime;
}
