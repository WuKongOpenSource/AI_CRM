package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.service.PluginAiUsageMeter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认 no-op 计量实现：放行所有 AI 消耗型调用,不记账。
 * 计量/扣费系统是未来变更的非目标(本代码库无 AiQuotaService/credit)。
 */
@Slf4j
@Component
public class NoOpPluginAiUsageMeter implements PluginAiUsageMeter {

    @Override
    public void ensure(String pluginId, long estimatedTokens) {
        log.debug("[no-op meter] ensure plugin={} estTokens={} -> 放行", pluginId, estimatedTokens);
    }

    @Override
    public void record(String pluginId, long actualTokens) {
        log.debug("[no-op meter] record plugin={} actualTokens={} -> 不记账", pluginId, actualTokens);
    }
}
