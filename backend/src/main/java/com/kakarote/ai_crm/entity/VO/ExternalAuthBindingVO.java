package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class ExternalAuthBindingVO {

    private String provider;

    private String providerName;

    private Boolean bound;

    private String displayName;

    private String email;

    private Date bindTime;
}
