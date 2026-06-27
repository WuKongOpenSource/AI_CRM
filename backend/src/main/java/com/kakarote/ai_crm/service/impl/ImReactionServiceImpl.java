package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import com.kakarote.ai_crm.im.event.ImReactionChangedEvent;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.mapper.ImMessageReactionMapper;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImReactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ImReactionServiceImpl extends ServiceImpl<ImMessageReactionMapper, ImMessageReaction> implements ImReactionService {

    @Autowired
    private ImMessageMapper messageMapper;

    @Autowired
    private ImConversationService conversationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private String canonicalEmoji(String e) {
        if (e == null) return null;
        String stripped = e.replace("️", "");
        for (String allowed : ALLOWED_EMOJI) {
            if (allowed.replace("️", "").equals(stripped)) return allowed;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ImReactionVO> toggle(Long userId, Long messageId, String emoji) {
        String canonical = canonicalEmoji(emoji);
        if (canonical == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "不支持的表情");
        }
        ImMessage msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "消息不存在");
        }
        conversationService.assertMember(msg.getConversationId(), userId);

        ImMessageReaction existing = getOne(new LambdaQueryWrapper<ImMessageReaction>()
                .eq(ImMessageReaction::getMessageId, messageId)
                .eq(ImMessageReaction::getUserId, userId)
                .eq(ImMessageReaction::getEmoji, canonical), false);
        if (existing != null) {
            removeById(existing.getId());
        } else {
            ImMessageReaction r = new ImMessageReaction();
            r.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId());
            r.setMessageId(messageId);
            r.setConversationId(msg.getConversationId());
            r.setUserId(userId);
            r.setEmoji(canonical);
            r.setCreateTime(new Date());
            baseMapper.insertIgnoreDuplicate(r);
        }
        // notify members (each gets their own `mine`) after commit
        eventPublisher.publishEvent(new ImReactionChangedEvent(
                msg.getConversationId(), messageId, conversationService.memberUserIds(msg.getConversationId())));
        return aggregate(messageId, userId);
    }

    @Override
    public List<ImReactionVO> aggregate(Long messageId, Long viewerId) {
        List<ImMessageReaction> rows = list(new LambdaQueryWrapper<ImMessageReaction>()
                .eq(ImMessageReaction::getMessageId, messageId)
                .orderByAsc(ImMessageReaction::getId));
        // preserve first-seen emoji order
        LinkedHashMap<String, ImReactionVO> byEmoji = new LinkedHashMap<>();
        for (ImMessageReaction r : rows) {
            ImReactionVO vo = byEmoji.computeIfAbsent(r.getEmoji(), e -> {
                ImReactionVO v = new ImReactionVO();
                v.setEmoji(e);
                v.setCount(0);
                v.setMine(false);
                return v;
            });
            vo.setCount(vo.getCount() + 1);
            if (viewerId != null && viewerId.equals(r.getUserId())) {
                vo.setMine(true);
            }
        }
        return new ArrayList<>(byEmoji.values());
    }
}
