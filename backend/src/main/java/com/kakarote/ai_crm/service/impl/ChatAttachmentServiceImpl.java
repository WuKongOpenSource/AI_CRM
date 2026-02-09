package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.PO.ChatAttachment;
import com.kakarote.ai_crm.mapper.ChatAttachmentMapper;
import com.kakarote.ai_crm.service.IChatAttachmentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天附件服务实现
 */
@Service
public class ChatAttachmentServiceImpl extends ServiceImpl<ChatAttachmentMapper, ChatAttachment>
        implements IChatAttachmentService {

    @Override
    public void saveBatchAttachments(Long messageId, List<ChatSendBO.AttachmentDTO> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        List<ChatAttachment> entities = new ArrayList<>();
        for (ChatSendBO.AttachmentDTO dto : attachments) {
            ChatAttachment attachment = new ChatAttachment();
            attachment.setMessageId(messageId);
            attachment.setFileName(dto.getFileName());
            attachment.setFilePath(dto.getFilePath());
            attachment.setFileSize(dto.getFileSize());
            attachment.setMimeType(dto.getMimeType());
            entities.add(attachment);
        }
        saveBatch(entities);
    }

    @Override
    public List<ChatAttachment> getByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<ChatAttachment>()
                .in(ChatAttachment::getMessageId, messageIds));
    }
}
