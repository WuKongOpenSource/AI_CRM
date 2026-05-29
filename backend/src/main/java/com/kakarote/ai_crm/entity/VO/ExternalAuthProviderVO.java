package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class ExternalAuthProviderVO {

    private String provider;

    private String name;

    private Boolean enabled;
}
