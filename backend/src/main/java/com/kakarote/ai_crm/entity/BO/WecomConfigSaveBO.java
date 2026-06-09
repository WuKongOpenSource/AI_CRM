package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class WecomConfigSaveBO {

    private String archiveSecret;

    private String archivePrivateKey;

    private String archivePublicKeyVersion;

    private String archiveCorpId;

    private Boolean archiveEnabled;

    private Boolean customerContactEnabled;

    private Boolean syncEnabled;
}
