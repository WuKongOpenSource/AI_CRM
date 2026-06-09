package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomConfigVO {

    private Long id;

    private String corpId;

    private String corpName;

    private String agentId;

    private String authStatus;

    private Boolean thirdPartyEnabled;

    private Boolean thirdPartyAuthorized;

    private String authorizationUrl;

    private Date authorizedAt;

    private Date unauthorizedAt;

    private Boolean archiveSecretConfigured;

    private Boolean archivePrivateKeyConfigured;

    private String archivePublicKeyVersion;

    private String archiveCorpId;

    private Boolean archiveEnabled;

    private Boolean customerContactEnabled;

    private Boolean syncEnabled;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;
}
