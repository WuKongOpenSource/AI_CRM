package com.kakarote.ai_crm.plugin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.entity.PO.CrmPluginSubscription;
import com.kakarote.ai_crm.mapper.CrmPluginSubscriptionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 领域事件 -> 订阅插件 webhook 投递。事务提交后触发(无事务时也回退执行),
 * 实际 HTTP 投递交给 PluginWebhookDeliverer 异步完成。
 */
@Slf4j
@Component
public class PluginWebhookDispatcher {

    @Autowired
    private CrmPluginSubscriptionMapper subscriptionMapper;

    @Autowired
    private PluginWebhookDeliverer deliverer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDomainEvent(PluginDomainEvent event) {
        try {
            List<CrmPluginSubscription> subs = subscriptionMapper.selectList(
                    new LambdaQueryWrapper<CrmPluginSubscription>()
                            .eq(CrmPluginSubscription::getEventType, event.getEventType())
                            .eq(CrmPluginSubscription::getActive, 1));
            for (CrmPluginSubscription sub : subs) {
                deliverer.deliver(sub, event.getEventType(), event.getPayload());
            }
        } catch (Exception e) {
            log.warn("插件 webhook 分发失败 event={} err={}", event.getEventType(), e.getMessage());
        }
    }
}
