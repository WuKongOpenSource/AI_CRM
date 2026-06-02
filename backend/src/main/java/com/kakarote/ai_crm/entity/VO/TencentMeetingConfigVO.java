package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingConfigVO {

    private Long id;

    private String appId;

    private String sdkId;

    private String corpName;

    private String secretIdMasked;

    private String operatorUserId;

    private Boolean syncEnabled;

    private Boolean transcriptEnabled;

    private Boolean archiveToKnowledge;

    private Date stsTokenExpireTime;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;
}
