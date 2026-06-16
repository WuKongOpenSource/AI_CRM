package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class ExternalAuthAuthorizeVO {

    private String provider;

    private String authorizeUrl;

    public ExternalAuthAuthorizeVO() {
    }

    public ExternalAuthAuthorizeVO(String provider, String authorizeUrl) {
        this.provider = provider;
        this.authorizeUrl = authorizeUrl;
    }
}
