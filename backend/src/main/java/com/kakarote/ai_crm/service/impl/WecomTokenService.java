package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WecomTokenService {

    @Autowired
    private WecomApiClient apiClient;

    @Autowired
    private SecretTextCipher secretTextCipher;

    public String fetchAppAccessToken(WecomCorpConfig config) {
        return apiClient.fetchAccessToken(config.getCorpId(), decryptRequired(config.getAppSecretEncrypted(), "app secret"));
    }

    public String fetchContactAccessToken(WecomCorpConfig config) {
        String secret = StrUtil.blankToDefault(config.getContactSecretEncrypted(), config.getAppSecretEncrypted());
        return apiClient.fetchAccessToken(config.getCorpId(), decryptRequired(secret, "customer contact secret"));
    }

    private String decryptRequired(String encrypted, String label) {
        if (StrUtil.isBlank(encrypted)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom " + label + " is not configured");
        }
        try {
            return secretTextCipher.decrypt(encrypted);
        } catch (Exception e) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom " + label + " decrypt failed");
        }
    }
}
