package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class MailTemplateVO {

    private Long templateId;

    private String name;

    private String category;

    private String subject;

    private String bodyText;

    private String variables;

    private Boolean isCommon;

    private Date createTime;

    private Date updateTime;
}
