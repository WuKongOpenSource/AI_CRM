package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class MailSyncLogVO {

    private Long logId;

    private Long accountId;

    private Long userId;

    private String syncType;

    private String status;

    private Integer fetchedCount;

    private Integer savedCount;

    private Integer skippedCount;

    private Integer failedCount;

    private Date startedAt;

    private Date finishedAt;

    private String errorMessage;
}
