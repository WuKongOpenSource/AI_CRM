package com.kakarote.ai_crm.entity.BO;

import com.kakarote.ai_crm.common.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MailTemplateQueryBO extends PageEntity {

    private String category;

    private String keyword;

    private Boolean commonOnly;
}
