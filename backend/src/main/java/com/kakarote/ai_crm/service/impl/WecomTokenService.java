package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WecomTokenService {

    @Autowired
    private WecomOpenPlatformService openPlatformService;

    @Autowired
    private WecomAgencyDevService agencyDevService;

    public String fetchAppAccessToken(WecomCorpConfig config) {
        return resolveCorpToken(config);
    }

    public String fetchContactAccessToken(WecomCorpConfig config) {
        return resolveCorpToken(config);
    }

    /**
     * 按授权来源取企业 access_token：代开发授权的企业走自建式 gettoken，
     * 其余（第三方应用授权）走 get_corp_token。
     */
    private String resolveCorpToken(WecomCorpConfig config) {
        if (agencyDevService.owns(config)) {
            return agencyDevService.fetchCorpAccessToken(config);
        }
        return openPlatformService.fetchCorpAccessToken(config);
    }
}
