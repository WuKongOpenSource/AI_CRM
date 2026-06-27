package com.kakarote.ai_crm.service;

/**
 * AI 用量计量抽象 hook（P0-B）。
 *
 * <p>本变更只提供 no-op 默认实现（{@code NoOpPluginAiUsageMeter}）——本代码库不存在
 * AiQuotaService/credit/计费系统，真正的计量/扣费是未来变更的非目标。网关在 AI 消耗型
 * 调用前后分别调用 {@link #ensure} 与 {@link #record}；未来替换为真实实现即可计量，
 * 无需改动网关调用点。</p>
 */
public interface PluginAiUsageMeter {

    /** 调用前预检；真实实现可在额度不足时抛异常以阻断调用。no-op 默认放行。 */
    void ensure(String pluginId, long estimatedTokens);

    /** 调用后按真实用量记账。no-op 默认不记账。 */
    void record(String pluginId, long actualTokens);
}
