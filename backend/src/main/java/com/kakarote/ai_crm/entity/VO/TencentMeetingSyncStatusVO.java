package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingSyncStatusVO {

    private String appId;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;

    private Integer fetchedCount;

    private Integer savedCount;

    private Integer failedCount;
}
