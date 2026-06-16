package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class ExternalAuthBindingVO {

    private String provider;

    private String providerName;

    private String name;

    private Boolean enabled;

    private Boolean bound;

    private String subject;

    private String displayName;

    private String email;

    private String avatarUrl;

    private Date bindTime;

    private Date lastLoginTime;
}
