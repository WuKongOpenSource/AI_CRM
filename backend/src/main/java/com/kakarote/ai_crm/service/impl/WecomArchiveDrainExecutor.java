package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.mapper.WecomCorpConfigMapper;
import com.kakarote.ai_crm.service.support.SyncTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会话存档增量拉取的异步执行器。
 *
 * <p>独立成 Bean 是为了集中处理会话存档事件触发的后台拉取，并在无请求上下文的同步线程中
 * 手动设置 TenantContextHolder 并在 finally 清理。</p>
 */
@Component
public class WecomArchiveDrainExecutor {

    private static final Logger log = LoggerFactory.getLogger(WecomArchiveDrainExecutor.class);

    @Autowired
    private WecomCorpConfigMapper configMapper;

    @Autowired
    private WecomSyncServiceImpl syncService;

    @Autowired
    private SyncTaskExecutor syncTaskExecutor;

    /**
     * 按 corpId 异步增量拉取会话存档。corpId 可能是真实 corpid 或第三方回写的 corp_id，
     * 故按 (corp_id OR archive_corp_id) 匹配所有已启用存档的授权企业（跨租户）。
     */
    public void drainByCorpId(String corpId, int maxPages) {
        log.debug("WeCom archive drain task accepted: corpId={}, maxPages={}", corpId, maxPages);
        syncTaskExecutor.submitWithTenant("wecom-archive-drain-" + corpId, null,
                () -> drainByCorpIdInternal(corpId, maxPages));
    }

    private void drainByCorpIdInternal(String corpId, int maxPages) {
        log.debug("WeCom archive drain task started: corpId={}, maxPages={}", corpId, maxPages);
        List<WecomCorpConfig> targets = configMapper.selectArchiveTargetsByCorpIdIgnoreTenant(corpId);
        if (targets == null || targets.isEmpty()) {
            log.debug("WeCom archive event: no archive-enabled corp matched corpId={}", corpId);
            return;
        }
        log.debug("WeCom archive drain targets resolved: corpId={}, targetCount={}", corpId, targets.size());
        for (WecomCorpConfig config : targets) {
            if (config.getTenantId() == null) {
                log.warn("WeCom archive target has null tenantId, skipped: corpId={}", corpId);
                continue;
            }
            try {
                TenantContextHolder.setTenantId(config.getTenantId());
                log.debug("WeCom archive drain tenant start: corpId={}, tenantId={}, maxPages={}",
                        corpId, config.getTenantId(), maxPages);
                int saved = syncService.drainArchive(config, maxPages);
                log.info("WeCom archive drained by event: corpId={}, tenantId={}, saved={}",
                        corpId, config.getTenantId(), saved);
                log.debug("WeCom archive drain tenant done: corpId={}, tenantId={}, saved={}",
                        corpId, config.getTenantId(), saved);
            } catch (Exception e) {
                log.warn("WeCom archive drain failed: corpId={}, tenantId={}, err={}",
                        corpId, config.getTenantId(), e.getMessage());
            } finally {
                TenantContextHolder.clear();
            }
        }
    }
}
