package com.kakarote.ai_crm.entity.BO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MailTemplateSaveBO {

    private Long templateId;

    @NotBlank(message = "Template name cannot be empty")
    private String name;

    private String category;

    @NotBlank(message = "Subject cannot be empty")
    private String subject;

    @NotBlank(message = "Body cannot be empty")
    private String bodyText;

    private String variables;

    private Boolean isCommon;
}
