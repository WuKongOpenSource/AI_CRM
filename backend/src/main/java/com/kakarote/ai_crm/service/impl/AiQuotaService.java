package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.AiModelSource;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.AiCreditRecord;
import com.kakarote.ai_crm.mapper.SystemConfigMapper;
import com.kakarote.ai_crm.service.ICrmTenantService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Supplier;

/**
 * Unified AI credit validation, billing, and ledger service.
 */
@Slf4j
@Service
public class AiQuotaService {

    private static final String DEFAULT_ACTION_NAME = "AI操作";
    private static final String AI_MODE_KEY = "ai_mode";
    private static final String AI_PROVIDER_KEY = "ai_provider";
    private static final String AI_MODEL_KEY = "ai_model";
    private static final int MIN_REQUIRED_TOKENS = 32;
    private static final int RESPONSE_BUFFER_TOKENS = 128;
    private static final BigDecimal DEFAULT_CREDIT_MULTIPLIER = BigDecimal.ONE;

    @Autowired
    private ICrmTenantService tenantService;

    @Autowired
    private AiBillingConfigService aiBillingConfigService;

    @Autowired
    private AiCreditRecordService aiCreditRecordService;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    public void ensureQuotaAvailable(String actionName) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, MIN_REQUIRED_TOKENS);
    }

    public void ensureQuotaAvailable(String actionName, int requiredTokens) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, requiredTokens);
    }

    public void ensureQuotaAvailable(String actionName, int requiredTokens, BigDecimal creditMultiplier) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, requiredTokens, creditMultiplier);
    }

    public void ensureQuotaAvailable(String actionName, String systemPrompt,
                                     List<Message> history, String currentContent) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, systemPrompt, history, currentContent);
    }

    public void ensureQuotaAvailable(Long tenantId, String actionName, String systemPrompt,
                                     List<Message> history, String currentContent) {
        ensureQuotaAvailable(tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent));
    }

    public void ensureQuotaAvailable(Long tenantId, String actionName, String systemPrompt,
                                     List<Message> history, String currentContent, BigDecimal creditMultiplier) {
        ensureQuotaAvailable(tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent), creditMultiplier);
    }

    public void ensureQuotaAvailable(Long tenantId, String actionName, String systemPrompt,
                                     List<Message> history, String currentContent,
                                     BigDecimal creditMultiplier, String modelSource) {
        ensureQuotaAvailable(
            tenantId,
            actionName,
            estimateRequiredTokens(systemPrompt, history, currentContent),
            creditMultiplier,
            modelSource
        );
    }

    public void ensureQuotaAvailable(Long tenantId, String actionName, int requiredTokens) {
        ensureQuotaAvailable(tenantId, actionName, requiredTokens, DEFAULT_CREDIT_MULTIPLIER);
    }

    public void ensureQuotaAvailable(Long tenantId, String actionName, int requiredTokens, BigDecimal creditMultiplier) {
        ensureQuotaAvailable(tenantId, actionName, requiredTokens, creditMultiplier, resolveDefaultModelSource(tenantId));
    }

    public void ensureQuotaAvailable(Long tenantId, String actionName, int requiredTokens,
                                     BigDecimal creditMultiplier, String modelSource) {
        String failureMessage = resolveQuotaFailureMessage(tenantId, actionName, requiredTokens, creditMultiplier, modelSource);
        if (failureMessage != null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, failureMessage);
        }
    }

    public String resolveQuotaFailureMessage(String actionName) {
        return resolveQuotaFailureMessage(resolveCurrentTenantId(), actionName, MIN_REQUIRED_TOKENS);
    }

    public String resolveQuotaFailureMessage(String actionName, String systemPrompt,
                                             List<Message> history, String currentContent) {
        return resolveQuotaFailureMessage(resolveCurrentTenantId(), actionName, systemPrompt, history, currentContent);
    }

    public String resolveQuotaFailureMessage(Long tenantId, String actionName, String systemPrompt,
                                             List<Message> history, String currentContent) {
        return resolveQuotaFailureMessage(tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent));
    }

    public String resolveQuotaFailureMessage(Long tenantId, String actionName, String systemPrompt,
                                             List<Message> history, String currentContent, BigDecimal creditMultiplier) {
        return resolveQuotaFailureMessage(
            tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent), creditMultiplier);
    }

    public String resolveQuotaFailureMessage(Long tenantId, String actionName, String systemPrompt,
                                             List<Message> history, String currentContent,
                                             BigDecimal creditMultiplier, String modelSource) {
        return resolveQuotaFailureMessage(
            tenantId,
            actionName,
            estimateRequiredTokens(systemPrompt, history, currentContent),
            creditMultiplier,
            modelSource
        );
    }

    public String resolveQuotaFailureMessage(Long tenantId, String actionName, int requiredTokens) {
        return resolveQuotaFailureMessage(tenantId, actionName, requiredTokens, DEFAULT_CREDIT_MULTIPLIER);
    }

    public String resolveQuotaFailureMessage(Long tenantId, String actionName, int requiredTokens, BigDecimal creditMultiplier) {
        return resolveQuotaFailureMessage(tenantId, actionName, requiredTokens, creditMultiplier, resolveDefaultModelSource(tenantId));
    }

    public String resolveQuotaFailureMessage(Long tenantId, String actionName, int requiredTokens,
                                             BigDecimal creditMultiplier, String modelSource) {
        String normalizedActionName = normalizeActionName(actionName);
        if (tenantId == null) {
            return normalizedActionName + "失败：当前租户信息缺失，无法校验AI积分";
        }
        if (!isChargeable(modelSource)) {
            return null;
        }

        long requiredCredits = resolveCredits(requiredTokens, creditMultiplier);
        long remainingCredits = tenantService.getTotalCreditRemaining(tenantId);
        if (remainingCredits >= Math.max(requiredCredits, 1L)) {
            return null;
        }
        return "AI积分不足，本次" + normalizedActionName + "失败，请充值后重试";
    }

    public TokenUsageSnapshot resolveTokenUsage(ChatResponse chatResponse,
                                                String systemPrompt,
                                                List<Message> history,
                                                String currentContent,
                                                String responseContent) {
        RawTokenUsage rawUsage = null;
        if (chatResponse != null && chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
            rawUsage = readRawTokenUsage(chatResponse.getMetadata().getUsage());
        }
        return resolveTokenUsage(rawUsage, systemPrompt, history, currentContent, responseContent);
    }

    public TokenUsageSnapshot resolveTokenUsage(Usage usage,
                                                String systemPrompt,
                                                List<Message> history,
                                                String currentContent,
                                                String responseContent) {
        return resolveTokenUsage(readRawTokenUsage(usage), systemPrompt, history, currentContent, responseContent);
    }

    public TokenUsageSnapshot resolveTokenUsage(RawTokenUsage rawUsage,
                                                String systemPrompt,
                                                List<Message> history,
                                                String currentContent,
                                                String responseContent) {
        return resolveTokenUsage(
            rawUsage != null ? rawUsage.promptTokens() : null,
            rawUsage != null ? rawUsage.completionTokens() : null,
            rawUsage != null ? rawUsage.totalTokens() : null,
            systemPrompt,
            history,
            currentContent,
            responseContent
        );
    }

    public RawTokenUsage readRawTokenUsage(Usage usage) {
        if (usage == null) {
            return RawTokenUsage.empty();
        }
        return new RawTokenUsage(
            readPositiveUsageToken(usage::getPromptTokens),
            readPositiveUsageToken(usage::getCompletionTokens),
            readPositiveUsageToken(usage::getTotalTokens)
        );
    }

    public TokenUsageSnapshot resolveTokenUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens,
                                                String systemPrompt, List<Message> history,
                                                String currentContent, String responseContent) {
        int resolvedPromptTokens = promptTokens != null && promptTokens > 0
            ? promptTokens
            : estimatePromptTokens(systemPrompt, history, currentContent);
        int resolvedCompletionTokens = completionTokens != null && completionTokens > 0
            ? completionTokens
            : estimateTokens(responseContent);
        int resolvedTotalTokens = totalTokens != null && totalTokens > 0
            ? totalTokens
            : resolvedPromptTokens + resolvedCompletionTokens;

        return new TokenUsageSnapshot(resolvedPromptTokens, resolvedCompletionTokens, resolvedTotalTokens);
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeResolvedTokens(String actionName, TokenUsageSnapshot usageSnapshot) {
        consumeResolvedTokens(resolveCurrentTenantId(), actionName, usageSnapshot);
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeResolvedTokens(Long tenantId, String actionName, TokenUsageSnapshot usageSnapshot) {
        consumeResolvedTokens(tenantId, actionName, usageSnapshot, DEFAULT_CREDIT_MULTIPLIER);
    }

    @Transactional(rollbackFor = Exception.class)
    public long consumeResolvedTokens(Long tenantId, String actionName, TokenUsageSnapshot usageSnapshot,
                                      BigDecimal creditMultiplier) {
        return consumeResolvedTokens(
            tenantId,
            actionName,
            usageSnapshot,
            creditMultiplier,
            resolveDefaultModelSource(tenantId),
            resolveDefaultBillingModelProvider(tenantId),
            resolveDefaultBillingModelName(tenantId),
            null,
            null
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public long consumeResolvedTokens(Long tenantId, String actionName, TokenUsageSnapshot usageSnapshot,
                                      BigDecimal creditMultiplier, String modelSource,
                                      String billingModelProvider, String billingModelName,
                                      String referenceType, Long referenceId) {
        if (usageSnapshot == null) {
            return 0L;
        }
        long resolvedCredits = isChargeable(modelSource)
            ? resolveCredits(usageSnapshot.totalTokens(), creditMultiplier)
            : 0L;
        return consumeCredits(
            tenantId,
            actionName,
            resolvedCredits,
            usageSnapshot,
            creditMultiplier,
            modelSource,
            billingModelProvider,
            billingModelName,
            referenceType,
            referenceId
        ).creditsUsed();
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeEstimatedTokens(String actionName, String promptContent, String responseContent) {
        consumeEstimatedTokens(resolveCurrentTenantId(), actionName, promptContent, responseContent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void consumeEstimatedTokens(Long tenantId, String actionName, String promptContent, String responseContent) {
        TokenUsageSnapshot usageSnapshot = resolveTokenUsage(null, null, null, null, null, promptContent, responseContent);
        consumeResolvedTokens(tenantId, actionName, usageSnapshot);
    }

    @Transactional(rollbackFor = Exception.class)
    public CreditRecordResult consumeCredits(Long tenantId, String actionName, long creditsUsed,
                                             Integer totalTokens, BigDecimal creditMultiplier) {
        TokenUsageSnapshot usageSnapshot = new TokenUsageSnapshot(0, 0, Math.max(totalTokens != null ? totalTokens : 0, 0));
        return consumeCredits(
            tenantId,
            actionName,
            creditsUsed,
            usageSnapshot,
            creditMultiplier,
            resolveDefaultModelSource(tenantId),
            resolveDefaultBillingModelProvider(tenantId),
            resolveDefaultBillingModelName(tenantId),
            null,
            null
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public CreditRecordResult consumeCredits(Long tenantId, String actionName, long creditsUsed,
                                             TokenUsageSnapshot usageSnapshot, BigDecimal creditMultiplier,
                                             String modelSource, String billingModelProvider, String billingModelName,
                                             String referenceType, Long referenceId) {
        if (tenantId == null || usageSnapshot == null) {
            return CreditRecordResult.empty();
        }

        String normalizedSource = normalizeModelSource(modelSource, tenantId);
        boolean chargeable = isChargeable(normalizedSource);
        BigDecimal multiplier = normalizeCreditMultiplier(creditMultiplier);
        int tokensPerCredit = aiBillingConfigService.getTokensPerCredit();
        long currentBalance = tenantService.getTotalCreditRemaining(tenantId);
        ICrmTenantService.CreditConsumeResult consumeResult = chargeable && creditsUsed > 0
            ? tenantService.consumeCredits(tenantId, creditsUsed)
            : ICrmTenantService.CreditConsumeResult.zero(currentBalance);
        long balanceBefore = consumeResult.balanceBefore();
        long balanceAfter = consumeResult.balanceAfter();

        AiCreditRecord record = new AiCreditRecord();
        record.setTenantId(tenantId);
        record.setUserId(resolveCurrentUserId());
        record.setActionName(StrUtil.blankToDefault(StrUtil.trim(actionName), DEFAULT_ACTION_NAME));
        record.setModelSource(normalizedSource);
        record.setBillingModelProvider(blankToNull(billingModelProvider));
        record.setBillingModelName(blankToNull(billingModelName));
        record.setPromptTokens(Math.max(usageSnapshot.promptTokens(), 0));
        record.setCompletionTokens(Math.max(usageSnapshot.completionTokens(), 0));
        record.setTotalTokens(Math.max(usageSnapshot.totalTokens(), 0));
        record.setTokensPerCredit(tokensPerCredit);
        record.setCreditMultiplier(multiplier);
        record.setChargeable(chargeable);
        record.setCreditsUsed(chargeable ? consumeResult.creditsUsed() : 0L);
        record.setGiftCreditsUsed(chargeable ? consumeResult.giftCreditsUsed() : 0L);
        record.setPurchasedCreditsUsed(chargeable ? consumeResult.purchasedCreditsUsed() : 0L);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setReferenceType(blankToNull(referenceType));
        record.setReferenceId(referenceId);
        aiCreditRecordService.save(record);

        log.info("AI积分流水已记录: tenantId={}, action={}, source={}, totalTokens={}, multiplier={}, tokensPerCredit={}, chargeable={}, creditsUsed={}",
            tenantId, normalizeActionName(actionName), normalizedSource, usageSnapshot.totalTokens(),
            multiplier, tokensPerCredit, chargeable, record.getCreditsUsed());

        return new CreditRecordResult(
            record.getCreditsUsed(),
            record.getGiftCreditsUsed(),
            record.getPurchasedCreditsUsed(),
            record.getBalanceBefore(),
            record.getBalanceAfter(),
            record.getRecordId()
        );
    }

    public long resolveCredits(Integer totalTokens, BigDecimal creditMultiplier) {
        if (totalTokens == null || totalTokens <= 0) {
            return 0L;
        }
        BigDecimal multiplier = normalizeCreditMultiplier(creditMultiplier);
        BigDecimal tokensPerCredit = BigDecimal.valueOf(aiBillingConfigService.getTokensPerCredit());
        return BigDecimal.valueOf(totalTokens.longValue())
            .multiply(multiplier)
            .divide(tokensPerCredit, 0, RoundingMode.CEILING)
            .setScale(0, RoundingMode.CEILING)
            .max(BigDecimal.ONE)
            .longValue();
    }

    public BigDecimal normalizeCreditMultiplier(BigDecimal creditMultiplier) {
        if (creditMultiplier == null || creditMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_CREDIT_MULTIPLIER;
        }
        return creditMultiplier;
    }

    public int estimatePromptTokens(String systemPrompt, List<Message> history, String currentContent) {
        int total = estimateTokens(systemPrompt);
        if (history != null) {
            for (Message message : history) {
                total += estimateTokens(extractMessageContent(message)) + 4;
            }
        }
        total += estimateTokens(currentContent);
        return Math.max(total, 1);
    }

    public int estimateRequiredTokens(String systemPrompt, List<Message> history, String currentContent) {
        return estimatePromptTokens(systemPrompt, history, currentContent) + RESPONSE_BUFFER_TOKENS;
    }

    public int estimateTokens(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }

        int cjkChars = 0;
        int otherChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                continue;
            }
            if (isCjkCharacter(current)) {
                cjkChars++;
            } else {
                otherChars++;
            }
        }

        int estimated = cjkChars + (int) Math.ceil(otherChars / 4.0);
        return Math.max(estimated, 1);
    }

    private boolean isChargeable(String modelSource) {
        return !AiModelSource.isCustom(modelSource);
    }

    private String blankToNull(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private String normalizeModelSource(String modelSource, Long tenantId) {
        String normalized = AiModelSource.normalize(modelSource);
        if (StrUtil.isBlank(normalized)) {
            return resolveDefaultModelSource(tenantId);
        }
        return AiModelSource.isCustom(normalized) ? AiModelSource.CUSTOM : AiModelSource.SYSTEM;
    }

    private String resolveDefaultModelSource(Long tenantId) {
        String mode = readTenantConfigValue(tenantId, AI_MODE_KEY);
        return AiModelSource.CUSTOM.equalsIgnoreCase(StrUtil.nullToEmpty(mode).trim())
            ? AiModelSource.CUSTOM
            : AiModelSource.SYSTEM;
    }

    private String resolveDefaultBillingModelProvider(Long tenantId) {
        return readTenantConfigValue(tenantId, AI_PROVIDER_KEY);
    }

    private String resolveDefaultBillingModelName(Long tenantId) {
        return readTenantConfigValue(tenantId, AI_MODEL_KEY);
    }

    private String readTenantConfigValue(Long tenantId, String configKey) {
        if (tenantId == null || StrUtil.isBlank(configKey)) {
            return null;
        }
        try {
            return systemConfigMapper.selectValueByTenantId(tenantId, configKey);
        } catch (RuntimeException exception) {
            log.debug("Failed to read tenant AI config value: tenantId={}, key={}, error={}",
                tenantId, configKey, exception.getMessage());
            return null;
        }
    }

    private String normalizeActionName(String actionName) {
        String normalized = StrUtil.blankToDefault(StrUtil.trim(actionName), DEFAULT_ACTION_NAME);
        return switch (normalized) {
            case "chat" -> "AI对话";
            case "customer_report" -> "客户AI报告生成";
            case "customer_parse" -> "客户AI解析";
            case "customer_search_parse" -> "客户AI搜索解析";
            case "knowledge_search_answer" -> "知识库AI搜索回答生成";
            case "knowledge_tool_search" -> "知识库内容检索";
            case "knowledge_targeted_script" -> "知识库定向话术生成";
            case "knowledge_analyze" -> "知识库文档AI分析";
            case "knowledge_ask" -> "知识库文档问答";
            case "followup_parse" -> "跟进AI解析";
            case "followup_attachment" -> "跟进附件AI分析";
            case "audio_transcription" -> "音频AI转写";
            case "task_parse" -> "任务AI解析";
            case "schedule_parse" -> "日程AI解析";
            case "system_ai_test" -> "AI连接测试";
            case "knowledge_tool_ask" -> "知识库问答";
            default -> normalized;
        };
    }

    private Long resolveCurrentTenantId() {
        Long tenantId = UserUtil.getTenantId();
        return tenantId != null ? tenantId : AiContextHolder.getCurrentTenantId();
    }

    private Long resolveCurrentUserId() {
        Long userId = UserUtil.getUserIdOrNull();
        return userId != null ? userId : AiContextHolder.getCurrentUserId();
    }

    private String extractMessageContent(Message message) {
        if (message == null) {
            return null;
        }
        try {
            Object value = message.getClass().getMethod("getContent").invoke(message);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception ignored) {
        }
        try {
            Object value = message.getClass().getMethod("getText").invoke(message);
            return value != null ? String.valueOf(value) : null;
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isCjkCharacter(char value) {
        Character.UnicodeScript script = Character.UnicodeScript.of(value);
        return script == Character.UnicodeScript.HAN
            || script == Character.UnicodeScript.HIRAGANA
            || script == Character.UnicodeScript.KATAKANA
            || script == Character.UnicodeScript.HANGUL;
    }

    private Integer readPositiveUsageToken(Supplier<Integer> supplier) {
        try {
            Integer value = supplier.get();
            return value != null && value > 0 ? value : null;
        } catch (RuntimeException exception) {
            log.debug("Failed to read AI usage token value: {}", exception.getMessage());
            return null;
        }
    }

    public record RawTokenUsage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {

        public static RawTokenUsage empty() {
            return new RawTokenUsage(null, null, null);
        }

        public boolean hasAnyToken() {
            return promptTokens != null || completionTokens != null || totalTokens != null;
        }
    }

    public record TokenUsageSnapshot(int promptTokens, int completionTokens, int totalTokens) {
    }

    public record CreditRecordResult(long creditsUsed,
                                     long giftCreditsUsed,
                                     long purchasedCreditsUsed,
                                     long balanceBefore,
                                     long balanceAfter,
                                     Long recordId) {

        public static CreditRecordResult empty() {
            return new CreditRecordResult(0L, 0L, 0L, 0L, 0L, null);
        }
    }
}
