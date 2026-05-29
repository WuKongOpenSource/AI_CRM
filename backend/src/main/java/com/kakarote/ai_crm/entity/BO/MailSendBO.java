package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class MailSendBO {

    private Long draftId;

    private MailDraftCreateBO draft;
}
