package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class WecomConfigSaveBO {

    private String corpId;

    private String corpName;

    private String agentId;

    private String appSecret;

    private String contactSecret;

    private String archiveSecret;

    private String archivePrivateKey;

    private String archivePublicKeyVersion;

    private Boolean archiveEnabled;

    private Boolean customerContactEnabled;

    private Boolean syncEnabled;
}
