package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class ExternalTenantMemberRegisterBO {

    private Long tenantId;

    private String email;

    private String realname;
}
