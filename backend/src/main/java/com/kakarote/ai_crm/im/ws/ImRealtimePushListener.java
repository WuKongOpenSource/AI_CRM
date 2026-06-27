package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.im.event.ImConversationReadEvent;
import com.kakarote.ai_crm.im.event.ImMessageRecalledEvent;
import com.kakarote.ai_crm.im.event.ImMessageSentEvent;
import com.kakarote.ai_crm.im.event.ImReactionChangedEvent;
import com.kakarote.ai_crm.service.ImMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ImRealtimePushListener {

    @Autowired
    private ImMessageService messageService;

    @Autowired
    private ImPushService pushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSent(ImMessageSentEvent event) {
        try {
            ImMessageVO vo = messageService.getMessageVO(event.messageId());
            if (vo != null) {
                pushService.pushMessage(event.conversationId(), vo, event.memberUserIds());
            }
        } catch (Exception e) {
            log.error("IM 实时推送失败(onMessageSent) conversationId={}, messageId={}: {}",
                    event.conversationId(), event.messageId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageRecalled(ImMessageRecalledEvent event) {
        try {
            ImMessageVO vo = messageService.getMessageVO(event.messageId());
            if (vo != null) {
                pushService.pushMessage(event.conversationId(), vo, event.memberUserIds());
            }
        } catch (Exception e) {
            log.error("IM 实时推送失败(onMessageRecalled) conversationId={}, messageId={}: {}",
                    event.conversationId(), event.messageId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConversationRead(ImConversationReadEvent event) {
        try {
            pushService.pushUnread(event.conversationId(), event.userId());
        } catch (Exception e) {
            log.error("IM 实时推送失败(onConversationRead) conversationId={}, userId={}: {}",
                    event.conversationId(), event.userId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReactionChanged(ImReactionChangedEvent event) {
        try {
            pushService.pushReaction(event.conversationId(), event.messageId(), event.memberUserIds());
        } catch (Exception e) {
            log.error("IM 实时推送失败(onReactionChanged) conversationId={}, messageId={}: {}",
                    event.conversationId(), event.messageId(), e.getMessage(), e);
        }
    }
}
