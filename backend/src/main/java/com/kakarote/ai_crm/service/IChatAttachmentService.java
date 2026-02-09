package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.PO.ChatAttachment;

import java.util.List;

/**
 * 聊天附件服务接口
 */
public interface IChatAttachmentService extends IService<ChatAttachment> {

    /**
     * 批量保存附件
     */
    void saveBatchAttachments(Long messageId, List<ChatSendBO.AttachmentDTO> attachments);

    /**
     * 根据消息ID列表批量查询附件
     */
    List<ChatAttachment> getByMessageIds(List<Long> messageIds);
}
