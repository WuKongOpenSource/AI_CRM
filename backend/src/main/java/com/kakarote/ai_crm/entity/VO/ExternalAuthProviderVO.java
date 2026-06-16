package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class ExternalAuthProviderVO {

    public ExternalAuthProviderVO() {
    }

    public ExternalAuthProviderVO(String provider, String name, Boolean enabled) {
        this.provider = provider;
        this.name = name;
        this.enabled = enabled;
    }

    private String provider;

    private String name;

    private Boolean enabled;
}
