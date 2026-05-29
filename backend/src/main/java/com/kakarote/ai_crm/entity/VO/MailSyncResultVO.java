package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class MailSyncResultVO {

    private Long accountId;

    private Long logId;

    private int fetchedCount;

    private int savedCount;

    private int skippedCount;

    private int failedCount;

    private String status;

    private String errorMessage;
}
