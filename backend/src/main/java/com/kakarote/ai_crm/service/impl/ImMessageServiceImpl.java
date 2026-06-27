package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ImSendMessageBO;
import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.PO.ImConversationMember;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.im.event.ImConversationReadEvent;
import com.kakarote.ai_crm.im.event.ImMessageRecalledEvent;
import com.kakarote.ai_crm.im.event.ImMessageSentEvent;
import com.kakarote.ai_crm.mapper.ImConversationMapper;
import com.kakarote.ai_crm.mapper.ImConversationMemberMapper;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ImConversationService;
import com.kakarote.ai_crm.service.ImMessageService;
import com.kakarote.ai_crm.service.ImReactionService;
import com.kakarote.ai_crm.service.ManageUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class ImMessageServiceImpl extends ServiceImpl<ImMessageMapper, ImMessage>
        implements ImMessageService {

    @Autowired
    private ImConversationService conversationService;

    @Autowired
    private ImConversationMapper conversationMapper;

    @Autowired
    private ImConversationMemberMapper memberMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private FileStorageService fileStorageService;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private ImReactionService reactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImMessageVO send(Long senderId, ImSendMessageBO bo) {
        if (bo == null || bo.getConversationId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "缺少会话ID");
        }
        conversationService.assertMember(bo.getConversationId(), senderId);
        String type = StrUtil.blankToDefault(bo.getContentType(), "text");
        if ("text".equals(type) && StrUtil.isBlank(bo.getContent())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "消息内容不能为空");
        }

        ImMessage m = new ImMessage();
        m.setConversationId(bo.getConversationId());
        m.setSenderId(senderId);
        m.setContentType(type);
        m.setContent(bo.getContent());
        m.setAttachmentName(bo.getAttachmentName());
        m.setAttachmentPath(bo.getAttachmentPath());
        m.setAttachmentSize(bo.getAttachmentSize());
        m.setAttachmentMime(bo.getAttachmentMime());
        m.setStatus("normal");
        m.setParentId(bo.getParentId());
        if (bo.getMentionedUserIds() != null && !bo.getMentionedUserIds().isEmpty()) {
            m.setMentionedUserIds(cn.hutool.core.util.StrUtil.join(",", bo.getMentionedUserIds()));
        }
        m.setMentionAll(Boolean.TRUE.equals(bo.getMentionAll()));
        Date now = new Date();
        m.setCreateTime(now);
        m.setUpdateTime(now);
        save(m);

        ImConversation conv = new ImConversation();
        conv.setId(bo.getConversationId());
        conv.setLastMessageId(m.getId());
        conv.setUpdateTime(now);
        conversationMapper.updateById(conv);

        // bump root reply count when this is a thread reply (atomic: avoids lost-update race)
        if (bo.getParentId() != null) {
            update(new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ImMessage>()
                    .eq("id", bo.getParentId())
                    .setSql("reply_count = COALESCE(reply_count, 0) + 1, last_reply_time = NOW()"));
        }

        // sender has implicitly read their own message
        bumpReadIfNewer(bo.getConversationId(), senderId, m.getId());

        // NOTE: published inside the transaction — any consumer (e.g. the WebSocket push layer)
        // MUST use @TransactionalEventListener(phase = AFTER_COMMIT) to avoid reading uncommitted rows.
        eventPublisher.publishEvent(new ImMessageSentEvent(
                bo.getConversationId(), m.getId(), conversationService.memberUserIds(bo.getConversationId())));
        return toVO(m);
    }

    @Override
    public List<ImMessageVO> history(Long conversationId, Long viewerId, Long beforeId, int limit) {
        int size = Math.min(Math.max(limit, 1), 50);
        LambdaQueryWrapper<ImMessage> w = new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .isNull(ImMessage::getParentId)
                .orderByDesc(ImMessage::getId)
                .last("LIMIT " + size);
        if (beforeId != null && beforeId > 0) {
            w.lt(ImMessage::getId, beforeId);
        }
        List<ImMessage> rows = list(w);
        rows.sort(Comparator.comparing(ImMessage::getId));
        List<ImMessageVO> out = new ArrayList<>();
        for (ImMessage msg : rows) {
            out.add(toVO(msg, viewerId));
        }
        return out;
    }

    @Override
    public List<ImMessageVO> getThread(Long rootId, Long viewerId) {
        ImMessage root = getById(rootId);
        if (root == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "话题不存在");
        }
        conversationService.assertMember(root.getConversationId(), viewerId);
        List<ImMessageVO> out = new ArrayList<>();
        out.add(toVO(root, viewerId));
        List<ImMessage> replies = list(new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getParentId, rootId)
                .orderByAsc(ImMessage::getId));
        for (ImMessage r : replies) {
            out.add(toVO(r, viewerId));
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImMessageVO recall(Long messageId, Long userId) {
        ImMessage m = getById(messageId);
        if (m == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "消息不存在");
        }
        if (!m.getSenderId().equals(userId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "只能撤回自己发送的消息");
        }
        if ("recalled".equals(m.getStatus())) {
            return toVO(m); // idempotent: already recalled, skip the window check
        }
        long ageMs = System.currentTimeMillis() - m.getCreateTime().getTime();
        if (ageMs > RECALL_WINDOW_MINUTES * 60_000L) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "超过 " + RECALL_WINDOW_MINUTES + " 分钟，无法撤回");
        }
        m.setStatus("recalled");
        m.setContent(null);
        m.setAttachmentName(null);
        m.setAttachmentPath(null);
        m.setAttachmentSize(null);
        m.setAttachmentMime(null);
        m.setUpdateTime(new Date());
        updateById(m);
        // NOTE: published inside the transaction — any consumer (e.g. the WebSocket push layer)
        // MUST use @TransactionalEventListener(phase = AFTER_COMMIT) to avoid reading uncommitted rows.
        eventPublisher.publishEvent(new ImMessageRecalledEvent(
                m.getConversationId(), m.getId(), conversationService.memberUserIds(m.getConversationId())));
        return toVO(m);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long conversationId, Long userId) {
        conversationService.assertMember(conversationId, userId);
        ImMessage latest = getOne(new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .orderByDesc(ImMessage::getId)
                .last("LIMIT 1"), false);
        long latestId = latest == null ? 0L : latest.getId();
        bumpReadIfNewer(conversationId, userId, latestId);
        // NOTE: published inside the transaction — any consumer (e.g. the WebSocket push layer)
        // MUST use @TransactionalEventListener(phase = AFTER_COMMIT) to avoid reading uncommitted rows.
        eventPublisher.publishEvent(new ImConversationReadEvent(conversationId, userId));
    }

    private void bumpReadIfNewer(Long conversationId, Long userId, long messageId) {
        ImConversationMember member = memberMapper.selectOne(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, userId));
        if (member == null) {
            return;
        }
        long current = member.getLastReadMessageId() == null ? 0L : member.getLastReadMessageId();
        if (messageId > current) {
            member.setLastReadMessageId(messageId);
            member.setUpdateTime(new Date());
            memberMapper.updateById(member);
        }
    }

    @Override
    public ImMessageVO getMessageVO(Long messageId) {
        ImMessage m = getById(messageId);
        return m == null ? null : toVO(m);
    }

    private ImMessageVO toVO(ImMessage m) {
        return toVO(m, null);
    }

    private ImMessageVO toVO(ImMessage m, Long viewerId) {
        ImMessageVO vo = new ImMessageVO();
        vo.setId(String.valueOf(m.getId()));
        vo.setConversationId(String.valueOf(m.getConversationId()));
        vo.setSenderId(String.valueOf(m.getSenderId()));
        vo.setContentType(m.getContentType());
        vo.setContent(m.getContent());
        vo.setAttachmentName(m.getAttachmentName());
        vo.setAttachmentSize(m.getAttachmentSize());
        vo.setAttachmentMime(m.getAttachmentMime());
        vo.setStatus(m.getStatus());
        vo.setCreateTime(m.getCreateTime());
        vo.setParentId(m.getParentId() == null ? null : String.valueOf(m.getParentId()));
        vo.setReplyCount(m.getReplyCount() == null ? 0 : m.getReplyCount());
        vo.setMentionAll(Boolean.TRUE.equals(m.getMentionAll()));
        if (StrUtil.isNotBlank(m.getMentionedUserIds())) {
            vo.setMentionedUserIds(Arrays.asList(m.getMentionedUserIds().split(",")));
        }
        vo.setReactions(reactionService.aggregate(m.getId(), viewerId));
        if (m.getSenderId() != null) {
            try {
                ManagerUser u = manageUserService.getById(m.getSenderId());
                if (u != null) {
                    vo.setSenderName(StrUtil.blankToDefault(u.getRealname(), u.getUsername()));
                }
            } catch (Exception ignored) {
                // leave senderName null if user lookup fails
            }
        }
        if (StrUtil.isNotBlank(m.getAttachmentPath()) && fileStorageService != null) {
            try {
                vo.setAttachmentUrl(fileStorageService.getUrl(m.getAttachmentPath()));
            } catch (Exception ignored) {
                // leave url null if storage lookup fails
            }
        }
        return vo;
    }
}
