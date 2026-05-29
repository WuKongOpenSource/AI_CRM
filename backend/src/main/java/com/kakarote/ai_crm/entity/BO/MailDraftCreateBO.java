package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MailDraftCreateBO {

    private Long accountId;

    private Long customerId;

    private Long contactId;

    private Long sourceMessageId;

    @NotBlank(message = "Recipients cannot be empty")
    private String toAddresses;

    private String ccAddresses;

    private String bccAddresses;

    @NotBlank(message = "Subject cannot be empty")
    private String subject;

    @NotBlank(message = "Body cannot be empty")
    private String bodyText;

    private String attachmentRefs;
}
