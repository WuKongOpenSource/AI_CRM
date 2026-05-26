package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.service.ICrmTenantService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Supplier;

/**
 * Unified AI credit validation and token usage billing service.
 */
@Slf4j
@Service
public class AiQuotaService {

    private static final String DEFAULT_ACTION_NAME = "AI操作";
    private static final int MIN_REQUIRED_TOKENS = 32;
    private static final int RESPONSE_BUFFER_TOKENS = 128;
    private static final BigDecimal DEFAULT_CREDIT_MULTIPLIER = BigDecimal.ONE;

    @Autowired
    private ICrmTenantService tenantService;

    @Autowired
    private AiBillingConfigService aiBillingConfigService;

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(String actionName) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, MIN_REQUIRED_TOKENS);
    }

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(String actionName, int requiredTokens) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, requiredTokens);
    }

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(String actionName, int requiredTokens, BigDecimal creditMultiplier) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, requiredTokens, creditMultiplier);
    }

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(String actionName, String systemPrompt,
                                     List<Message> history, String currentContent) {
        ensureQuotaAvailable(resolveCurrentTenantId(), actionName, systemPrompt, history, currentContent);
    }

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(Long tenantId, String actionName, String systemPrompt,
                                     List<Message> history, String currentContent) {
        ensureQuotaAvailable(tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent));
    }

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(Long tenantId, String actionName, String systemPrompt,
                                     List<Message> history, String currentContent, BigDecimal creditMultiplier) {
        ensureQuotaAvailable(tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent), creditMultiplier);
    }

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(Long tenantId, String actionName, int requiredTokens) {
        ensureQuotaAvailable(tenantId, actionName, requiredTokens, DEFAULT_CREDIT_MULTIPLIER);
    }

    /**
     * 确保额度可用。
     */
    public void ensureQuotaAvailable(Long tenantId, String actionName, int requiredTokens, BigDecimal creditMultiplier) {
        String failureMessage = resolveQuotaFailureMessage(tenantId, actionName, requiredTokens, creditMultiplier);
        if (failureMessage != null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, failureMessage);
        }
    }

    /**
     * 解析额度Failure消息。
     */
    public String resolveQuotaFailureMessage(String actionName) {
        return resolveQuotaFailureMessage(resolveCurrentTenantId(), actionName, MIN_REQUIRED_TOKENS);
    }

    /**
     * 解析额度Failure消息。
     */
    public String resolveQuotaFailureMessage(String actionName, String systemPrompt,
                                             List<Message> history, String currentContent) {
        return resolveQuotaFailureMessage(resolveCurrentTenantId(), actionName, systemPrompt, history, currentContent);
    }

    /**
     * 解析额度Failure消息。
     */
    public String resolveQuotaFailureMessage(Long tenantId, String actionName, String systemPrompt,
                                             List<Message> history, String currentContent) {
        return resolveQuotaFailureMessage(tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent));
    }

    /**
     * 解析额度Failure消息。
     */
    public String resolveQuotaFailureMessage(Long tenantId, String actionName, String systemPrompt,
                                             List<Message> history, String currentContent, BigDecimal creditMultiplier) {
        return resolveQuotaFailureMessage(
            tenantId, actionName, estimateRequiredTokens(systemPrompt, history, currentContent), creditMultiplier);
    }

    /**
     * 解析额度Failure消息。
     */
    public String resolveQuotaFailureMessage(Long tenantId, String actionName, int requiredTokens) {
        return resolveQuotaFailureMessage(tenantId, actionName, requiredTokens, DEFAULT_CREDIT_MULTIPLIER);
    }

    /**
     * 解析额度Failure消息。
     */
    public String resolveQuotaFailureMessage(Long tenantId, String actionName, int requiredTokens, BigDecimal creditMultiplier) {
        String normalizedActionName = normalizeActionName(actionName);
        if (tenantId == null) {
            return normalizedActionName + "失败：当前租户信息缺失，无法校验AI积分";
        }

        long requiredCredits = resolveCredits(requiredTokens, creditMultiplier);
        long remainingCredits = tenantService.getTotalCreditRemaining(tenantId);
        if (remainingCredits >= Math.max(requiredCredits, 1L)) {
            return null;
        }
        return "AI积分不足，本次" + normalizedActionName + "失败，请充值后重试";
    }

    /**
     * 解析TokenUsage。
     */
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

    /**
     * Resolve TokenUsage directly from Spring AI usage metadata.
     */
    public TokenUsageSnapshot resolveTokenUsage(Usage usage,
                                                String systemPrompt,
                                                List<Message> history,
                                                String currentContent,
                                                String responseContent) {
        return resolveTokenUsage(readRawTokenUsage(usage), systemPrompt, history, currentContent, responseContent);
    }

    /**
     * Resolve TokenUsage from normalized raw values.
     */
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

    /**
     * Read positive token counts returned by provider metadata.
     */
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

    /**
     * 解析TokenUsage。
     */
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

    /**
     * 消耗ResolvedTokens。
     */
    public void consumeResolvedTokens(String actionName, TokenUsageSnapshot usageSnapshot) {
        consumeResolvedTokens(resolveCurrentTenantId(), actionName, usageSnapshot);
    }

    /**
     * 消耗ResolvedTokens。
     */
    public void consumeResolvedTokens(Long tenantId, String actionName, TokenUsageSnapshot usageSnapshot) {
        consumeResolvedTokens(tenantId, actionName, usageSnapshot, DEFAULT_CREDIT_MULTIPLIER);
    }

    /**
     * 消耗ResolvedTokens对应的积分。
     */
    public long consumeResolvedTokens(Long tenantId, String actionName, TokenUsageSnapshot usageSnapshot,
                                      BigDecimal creditMultiplier) {
        if (usageSnapshot == null) {
            return 0L;
        }
        long creditsUsed = resolveCredits(usageSnapshot.totalTokens(), creditMultiplier);
        consumeCredits(tenantId, actionName, creditsUsed, usageSnapshot.totalTokens(), creditMultiplier);
        return creditsUsed;
    }

    /**
     * 消耗EstimatedTokens。
     */
    public void consumeEstimatedTokens(String actionName, String promptContent, String responseContent) {
        consumeEstimatedTokens(resolveCurrentTenantId(), actionName, promptContent, responseContent);
    }

    /**
     * 消耗EstimatedTokens。
     */
    public void consumeEstimatedTokens(Long tenantId, String actionName, String promptContent, String responseContent) {
        TokenUsageSnapshot usageSnapshot = resolveTokenUsage(null, null, null, null, null, promptContent, responseContent);
        consumeResolvedTokens(tenantId, actionName, usageSnapshot);
    }

    /**
     * 消耗积分。
     */
    public void consumeCredits(Long tenantId, String actionName, long creditsUsed,
                               Integer totalTokens, BigDecimal creditMultiplier) {
        if (tenantId == null || creditsUsed <= 0) {
            return;
        }
        tenantService.consumeCredits(tenantId, creditsUsed);
        log.info("AI积分扣减完成: tenantId={}, action={}, totalTokens={}, multiplier={}, tokensPerCredit={}, creditsUsed={}",
            tenantId, normalizeActionName(actionName), totalTokens,
            normalizeCreditMultiplier(creditMultiplier), aiBillingConfigService.getTokensPerCredit(), creditsUsed);
    }

    /**
     * 根据总 token 和倍率解析积分消耗。
     */
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

    /**
     * 标准化积分倍率。
     */
    public BigDecimal normalizeCreditMultiplier(BigDecimal creditMultiplier) {
        if (creditMultiplier == null || creditMultiplier.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_CREDIT_MULTIPLIER;
        }
        return creditMultiplier;
    }

    /**
     * 处理estimatePromptTokens方法逻辑。
     */
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

    /**
     * 处理estimateRequiredTokens方法逻辑。
     */
    public int estimateRequiredTokens(String systemPrompt, List<Message> history, String currentContent) {
        return estimatePromptTokens(systemPrompt, history, currentContent) + RESPONSE_BUFFER_TOKENS;
    }

    /**
     * 处理estimateTokens方法逻辑。
     */
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

    /**
     * 标准化Action名称。
     */
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

    /**
     * 解析当前租户ID。
     */
    private Long resolveCurrentTenantId() {
        Long tenantId = UserUtil.getTenantId();
        return tenantId != null ? tenantId : AiContextHolder.getCurrentTenantId();
    }

    /**
     * 处理extractMessageContent方法逻辑。
     */
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

    /**
     * 判断是否CJKCharacter。
     */
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
}
