package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.redis.Redis;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.WecomAgencyDevProperties;
import com.kakarote.ai_crm.config.WecomDevCallbackProperties;
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.PO.ExternalTenantBinding;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomSuiteTicket;
import com.kakarote.ai_crm.mapper.ExternalTenantBindingMapper;
import com.kakarote.ai_crm.mapper.WecomCorpConfigMapper;
import com.kakarote.ai_crm.mapper.WecomSuiteTicketMapper;
import com.kakarote.ai_crm.utils.SecretTextCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企业微信「代开发自建应用」集成。
 *
 * <p>处理代开发模板回调（{@code /wecom/dev/callback} 的 POST 事件）：保存 suite_ticket、收到授权事件时
 * 调 get_permanent_code 捕获企业 permanent_code 并写入 corp 配置；并以自建式
 * {@code gettoken(corpid, permanent_code)} 取企业 access_token。</p>
 *
 * <p>与第三方应用（{@link WecomOpenPlatformService}，get_corp_token）并存，互不影响：corp 配置按
 * {@code suite_id} 区分由谁取 token（见 {@link #owns(WecomCorpConfig)}）。</p>
 */
@Service
public class WecomAgencyDevService {

    private static final Logger log = LoggerFactory.getLogger(WecomAgencyDevService.class);

    private static final String SUITE_TOKEN_KEY = "wecom:agencydev:suite-token:";
    private static final String CORP_TOKEN_KEY = "wecom:agencydev:corp-token:";
    private static final String AUTHORIZED = "AUTHORIZED";
    private static final String UNAUTHORIZED = "UNAUTHORIZED";

    @Autowired
    private WecomAgencyDevProperties properties;

    /** 回调验签/解密复用代开发模板回调的 Token/EncodingAESKey。 */
    @Autowired
    private WecomDevCallbackProperties callbackProperties;

    @Autowired
    private WecomCallbackCryptoService cryptoService;

    @Autowired
    private WecomOpenApiClient apiClient;

    @Autowired
    private WecomSuiteTicketMapper suiteTicketMapper;

    @Autowired
    private WecomCorpConfigMapper configMapper;

    @Autowired
    private ExternalTenantBindingMapper tenantBindingMapper;

    @Autowired
    private SecretTextCipher secretTextCipher;

    @Autowired
    private Redis redis;

    public boolean isUsable() {
        return properties.isUsable();
    }

    /** 该 corp 是否由代开发负责取 token（按 corp 配置记录的 suite_id 与代开发 suite_id 匹配）。 */
    public boolean owns(WecomCorpConfig config) {
        return properties.isUsable()
                && config != null
                && StrUtil.isNotBlank(config.getSuiteId())
                && properties.getSuiteId().equals(config.getSuiteId());
    }

    /** 处理代开发模板回调事件（POST）。本服务关闭时直接忽略（端点仍只应答 success）。 */
    public void handleEvent(String body, String msgSignature, String timestamp, String nonce) {
        if (!properties.isUsable()) {
            return;
        }
        try {
            String encrypted = extractTag(body, "Encrypt");
            if (StrUtil.isBlank(encrypted)) {
                return;
            }
            String xml = cryptoService.decrypt(callbackProperties.getToken(), callbackProperties.getEncodingAesKey(),
                    msgSignature, timestamp, nonce, encrypted);
            String infoType = extractTag(xml, "InfoType");
            if (StrUtil.isBlank(infoType)) {
                return;
            }
            switch (infoType) {
                case "suite_ticket" -> saveSuiteTicket(xml);
                case "create_auth", "change_auth" -> handleAuthEvent(xml);
                case "cancel_auth" -> markUnauthorized(extractTag(xml, "AuthCorpId"));
                default -> log.debug("Ignored WeCom agency-dev event: {}", infoType);
            }
        } catch (Exception e) {
            log.warn("WeCom agency-dev event handling failed: {}", e.getMessage());
        }
    }

    private void saveSuiteTicket(String xml) {
        String suiteId = StrUtil.blankToDefault(extractTag(xml, "SuiteId"), properties.getSuiteId());
        String ticket = extractTag(xml, "SuiteTicket");
        if (StrUtil.isBlank(suiteId) || StrUtil.isBlank(ticket)) {
            return;
        }
        WecomSuiteTicket suiteTicket = new WecomSuiteTicket();
        suiteTicket.setSuiteId(suiteId);
        suiteTicket.setSuiteTicketEncrypted(secretTextCipher.encrypt(ticket));
        suiteTicket.setReceivedAt(new Date());
        suiteTicket.setRawEventXml(xml);
        suiteTicketMapper.insert(suiteTicket);
        redis.del(SUITE_TOKEN_KEY + suiteId);
    }

    private void handleAuthEvent(String xml) {
        String authCode = extractTag(xml, "AuthCode");
        if (StrUtil.isBlank(authCode)) {
            return;
        }
        JSONObject permanentData = apiClient.fetchPermanentCode(fetchSuiteAccessToken(), authCode);
        String corpId = resolveCorpId(permanentData);
        String permanentCode = permanentData.getString("permanent_code");
        if (StrUtil.isBlank(corpId) || StrUtil.isBlank(permanentCode)) {
            return;
        }
        upsertAuthorizedConfig(corpId, permanentCode, permanentData);
    }

    private void upsertAuthorizedConfig(String corpId, String permanentCode, JSONObject permanentData) {
        // 按 corpId 匹配现有行（含手工占位行/原第三方行）并覆盖为代开发授权：写入真实 permanent_code、
        // 把 suite_id 改为代开发 suite，从而后续取 token 改走 gettoken。corpId 在租户内唯一，不按 suite_id 过滤即可完成迁移。
        List<WecomCorpConfig> existing = configMapper.selectThirdPartyByCorpIdIgnoreTenant(corpId);
        Long tenantId;
        WecomCorpConfig config;
        if (existing != null && !existing.isEmpty()) {
            config = existing.get(0);
            tenantId = config.getTenantId();
        } else {
            ExternalTenantBinding binding = tenantBindingMapper.selectOne(Wrappers.<ExternalTenantBinding>lambdaQuery()
                    .eq(ExternalTenantBinding::getProvider, "wecom")
                    .eq(ExternalTenantBinding::getExternalTenantKey, corpId)
                    .last("LIMIT 1"));
            if (binding == null || binding.getTenantId() == null) {
                log.warn("WeCom 代开发授权: corpId={} 无对应租户绑定，未自动建行。请先在 CRM 关联该企业（或手工建一条 corp 配置）。", corpId);
                return;
            }
            tenantId = binding.getTenantId();
            config = new WecomCorpConfig();
            config.setArchiveEnabled(Boolean.FALSE);
            config.setCustomerContactEnabled(Boolean.TRUE);
            config.setSyncEnabled(Boolean.TRUE);
        }
        Long previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            JSONObject corpInfo = permanentData.getJSONObject("auth_corp_info");
            config.setTenantId(tenantId);
            config.setCorpId(corpId);
            config.setSuiteId(properties.getSuiteId());
            config.setPermanentCodeEncrypted(secretTextCipher.encrypt(permanentCode));
            config.setAuthStatus(AUTHORIZED);
            config.setAuthorizedAt(new Date());
            config.setUnauthorizedAt(null);
            if (corpInfo != null) {
                config.setCorpName(StrUtil.blankToDefault(corpInfo.getString("corp_name"), corpId));
            }
            if (config.getId() == null) {
                configMapper.insert(config);
            } else {
                configMapper.updateById(config);
            }
            redis.del(CORP_TOKEN_KEY + corpId);
            log.info("WeCom 代开发授权已写入: corpId={}, tenantId={}", corpId, tenantId);
        } finally {
            restoreTenant(previous);
        }
    }

    private void markUnauthorized(String corpId) {
        if (StrUtil.isBlank(corpId)) {
            return;
        }
        List<WecomCorpConfig> configs = configMapper.selectThirdPartyByCorpIdIgnoreTenant(corpId);
        for (WecomCorpConfig config : configs) {
            if (!properties.getSuiteId().equals(config.getSuiteId())) {
                continue;
            }
            Long previous = TenantContextHolder.getTenantId();
            TenantContextHolder.setTenantId(config.getTenantId());
            try {
                config.setAuthStatus(UNAUTHORIZED);
                config.setUnauthorizedAt(new Date());
                configMapper.updateById(config);
            } finally {
                restoreTenant(previous);
            }
        }
        redis.del(CORP_TOKEN_KEY + corpId);
    }

    /** 代开发取企业 access_token：gettoken(corpid, permanent_code)（自建式），按 corpId 缓存。 */
    public String fetchCorpAccessToken(WecomCorpConfig config) {
        if (config == null || StrUtil.isBlank(config.getCorpId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom 代开发企业未配置");
        }
        String cacheKey = CORP_TOKEN_KEY + config.getCorpId();
        String cached = redis.get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        String permanentCode = decryptRequired(config.getPermanentCodeEncrypted());
        JSONObject json = apiClient.fetchSelfBuiltCorpToken(config.getCorpId(), permanentCode);
        String token = json.getString("access_token");
        redis.setex(cacheKey, tokenTtl(json.getInteger("expires_in")), token);
        return token;
    }

    private String fetchSuiteAccessToken() {
        String cacheKey = SUITE_TOKEN_KEY + properties.getSuiteId();
        String cached = redis.get(cacheKey);
        if (StrUtil.isNotBlank(cached)) {
            return cached;
        }
        WecomSuiteTicket ticket = suiteTicketMapper.selectLatestBySuiteIdIgnoreTenant(properties.getSuiteId());
        if (ticket == null || StrUtil.isBlank(ticket.getSuiteTicketEncrypted())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "WeCom 代开发 suite_ticket 尚未收到（确认 /wecom/dev/callback 已通并等待企微推送，约10分钟一次）");
        }
        JSONObject json = apiClient.fetchSuiteAccessToken(properties.getSuiteId(),
                properties.getSuiteSecret(),
                decryptRequired(ticket.getSuiteTicketEncrypted()));
        String token = json.getString("suite_access_token");
        redis.setex(cacheKey, tokenTtl(json.getInteger("expires_in")), token);
        return token;
    }

    private String resolveCorpId(JSONObject permanentData) {
        if (permanentData == null) {
            return null;
        }
        String corpId = permanentData.getString("auth_corpid");
        if (StrUtil.isBlank(corpId)) {
            JSONObject corpInfo = permanentData.getJSONObject("auth_corp_info");
            corpId = corpInfo == null ? null : corpInfo.getString("corpid");
        }
        return StrUtil.trim(corpId);
    }

    private String decryptRequired(String encrypted) {
        if (StrUtil.isBlank(encrypted)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom 代开发凭证未配置");
        }
        String value = secretTextCipher.decrypt(encrypted);
        if (StrUtil.isBlank(value)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeCom 代开发凭证解密失败");
        }
        return value;
    }

    private int tokenTtl(Integer expiresIn) {
        int expires = expiresIn == null || expiresIn <= 0 ? 7200 : expiresIn;
        return Math.max(60, expires - 300);
    }

    private void restoreTenant(Long previous) {
        if (previous == null) {
            TenantContextHolder.clear();
        } else {
            TenantContextHolder.setTenantId(previous);
        }
    }

    private static String extractTag(String xml, String tag) {
        if (StrUtil.isBlank(xml)) {
            return null;
        }
        Matcher m = Pattern.compile("<" + tag + ">\\s*(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?\\s*</" + tag + ">", Pattern.DOTALL)
                .matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }
}
