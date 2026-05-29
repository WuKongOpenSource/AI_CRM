package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class ExternalTenantRegisterBO {

    private String email;

    private String verificationCode;

    private String companyName;

    private String realname;

    private Boolean emailVerificationRequired = Boolean.TRUE;
}
