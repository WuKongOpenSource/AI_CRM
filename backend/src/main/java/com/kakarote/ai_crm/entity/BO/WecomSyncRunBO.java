package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class WecomSyncRunBO {

    private Boolean syncEmployees = Boolean.TRUE;

    private Boolean syncCustomers = Boolean.TRUE;

    private Boolean syncConversations = Boolean.TRUE;

    private Integer archiveLimit = 100;
}
