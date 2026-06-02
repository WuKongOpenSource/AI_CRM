package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class TencentMeetingConfigSaveBO {

    private String appId;

    private String sdkId;

    private String corpName;

    private String secretId;

    private String secretKey;

    private String webhookSecret;

    private String operatorUserId;

    private Boolean syncEnabled;

    private Boolean transcriptEnabled;

    private Boolean archiveToKnowledge;
}
