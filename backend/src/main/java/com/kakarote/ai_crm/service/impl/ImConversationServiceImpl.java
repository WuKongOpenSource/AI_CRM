package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.PO.ImConversation;
import com.kakarote.ai_crm.entity.PO.ImConversationMember;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.mapper.ImConversationMapper;
import com.kakarote.ai_crm.mapper.ImConversationMemberMapper;
import com.kakarote.ai_crm.mapper.ImMessageMapper;
import com.kakarote.ai_crm.service.ImConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class ImConversationServiceImpl extends ServiceImpl<ImConversationMapper, ImConversation>
        implements ImConversationService {

    @Autowired
    private ImConversationMemberMapper memberMapper;

    @Autowired
    private ImMessageMapper messageMapper;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private com.kakarote.ai_crm.service.ImMessageService imMessageService;

    @Autowired
    private com.kakarote.ai_crm.service.ManageUserService manageUserService;

    @Autowired(required = false)
    private com.kakarote.ai_crm.service.FileStorageService fileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImConversation getOrCreateDirect(Long currentUserId, Long peerUserId) {
        if (currentUserId == null || peerUserId == null || currentUserId.equals(peerUserId)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "无效的会话对象");
        }
        String key = ImConversationService.directMemberKey(currentUserId, peerUserId);
        ImConversation existing = getOne(new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getMemberKey, key), false);
        if (existing != null) {
            return existing;
        }
        ImConversation conv = new ImConversation();
        conv.setType("direct");
        conv.setMemberKey(key);
        Date now = new Date();
        conv.setCreateTime(now);
        conv.setUpdateTime(now);
        try {
            save(conv);
        } catch (DuplicateKeyException race) {
            // Concurrent create — return the winner.
            ImConversation winner = getOne(new LambdaQueryWrapper<ImConversation>()
                    .eq(ImConversation::getMemberKey, key), false);
            if (winner == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "会话并发创建失败，请重试");
            }
            return winner;
        }
        for (Long uid : List.of(currentUserId, peerUserId)) {
            ImConversationMember m = new ImConversationMember();
            m.setConversationId(conv.getId());
            m.setUserId(uid);
            m.setLastReadMessageId(0L);
            m.setCreateTime(now);
            m.setUpdateTime(now);
            memberMapper.insert(m);
        }
        return conv;
    }

    @Override
    public List<Long> memberUserIds(Long conversationId) {
        return memberMapper.selectList(new LambdaQueryWrapper<ImConversationMember>()
                        .eq(ImConversationMember::getConversationId, conversationId))
                .stream().map(ImConversationMember::getUserId).toList();
    }

    @Override
    public void assertMember(Long conversationId, Long userId) {
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, userId));
        if (count == null || count == 0) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "你不在该会话中");
        }
    }

    @Override
    public long unreadCount(Long conversationId, Long userId) {
        ImConversationMember member = memberMapper.selectOne(new LambdaQueryWrapper<ImConversationMember>()
                .eq(ImConversationMember::getConversationId, conversationId)
                .eq(ImConversationMember::getUserId, userId));
        long lastRead = (member == null || member.getLastReadMessageId() == null) ? 0L : member.getLastReadMessageId();
        return messageMapper.selectCount(new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .gt(ImMessage::getId, lastRead)
                .ne(ImMessage::getSenderId, userId)
                .eq(ImMessage::getStatus, "normal"));
    }

    @Override
    public java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> listMyConversations(Long userId) {
        java.util.List<ImConversationMember> myMemberships = memberMapper.selectList(
                new LambdaQueryWrapper<ImConversationMember>()
                        .eq(ImConversationMember::getUserId, userId));
        java.util.List<com.kakarote.ai_crm.entity.VO.ImConversationVO> out = new java.util.ArrayList<>();
        for (ImConversationMember mine : myMemberships) {
            ImConversation conv = getById(mine.getConversationId());
            if (conv == null) {
                continue;
            }
            // peer = the other member of this direct conversation
            ImConversationMember peerMember = memberMapper.selectOne(
                    new LambdaQueryWrapper<ImConversationMember>()
                            .eq(ImConversationMember::getConversationId, conv.getId())
                            .ne(ImConversationMember::getUserId, userId)
                            .last("LIMIT 1"));
            com.kakarote.ai_crm.entity.VO.ImConversationVO vo = new com.kakarote.ai_crm.entity.VO.ImConversationVO();
            vo.setId(String.valueOf(conv.getId()));
            vo.setUpdateTime(conv.getUpdateTime());
            vo.setUnreadCount(unreadCount(conv.getId(), userId));
            if (peerMember != null) {
                vo.setPeerUserId(String.valueOf(peerMember.getUserId()));
                com.kakarote.ai_crm.entity.PO.ManagerUser peer = manageUserService.getById(peerMember.getUserId());
                if (peer != null) {
                    vo.setPeerName(cn.hutool.core.util.StrUtil.blankToDefault(peer.getRealname(), peer.getUsername()));
                    if (cn.hutool.core.util.StrUtil.isNotBlank(peer.getImg()) && fileStorageService != null) {
                        try {
                            vo.setPeerAvatarUrl(fileStorageService.getUrl(peer.getImg()));
                        } catch (Exception ignored) {
                            // leave avatar null
                        }
                    }
                }
            }
            if (conv.getLastMessageId() != null) {
                vo.setLastMessage(imMessageService.getMessageVO(conv.getLastMessageId()));
            }
            out.add(vo);
        }
        out.sort(java.util.Comparator.comparing(
                com.kakarote.ai_crm.entity.VO.ImConversationVO::getUpdateTime,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        return out;
    }
}
