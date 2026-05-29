package com.kakarote.ai_crm.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MailOAuthStartVO {
    private String provider;
    private String authorizeUrl;
    private String state;
}
