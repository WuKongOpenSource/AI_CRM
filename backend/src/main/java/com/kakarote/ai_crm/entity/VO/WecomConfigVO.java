package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomConfigVO {

    private Long id;

    private String corpId;

    private String corpName;

    private String agentId;

    private Boolean appSecretConfigured;

    private Boolean contactSecretConfigured;

    private Boolean archiveSecretConfigured;

    private Boolean archivePrivateKeyConfigured;

    private String archivePublicKeyVersion;

    private Boolean archiveEnabled;

    private Boolean customerContactEnabled;

    private Boolean syncEnabled;

    private Date lastSyncTime;

    private String lastSyncStatus;

    private String lastSyncError;
}
