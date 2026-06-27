package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.ImSendMessageBO;
import com.kakarote.ai_crm.entity.PO.ImMessage;
import com.kakarote.ai_crm.entity.VO.ImMessageVO;

import java.util.List;

public interface ImMessageService extends IService<ImMessage> {

    /** Recall window in minutes. */
    int RECALL_WINDOW_MINUTES = 2;

    ImMessageVO send(Long senderId, ImSendMessageBO bo);

    /** History newest-first; beforeId null = latest page. Excludes thread replies. */
    List<ImMessageVO> history(Long conversationId, Long viewerId, Long beforeId, int limit);

    /** Return root message + replies for a thread, ascending. */
    List<ImMessageVO> getThread(Long rootId, Long viewerId);

    /** Recall own message within the window; throws otherwise. */
    ImMessageVO recall(Long messageId, Long userId);

    /** Mark conversation read up to the latest message for userId. */
    void markRead(Long conversationId, Long userId);

    /** Build the VO for a single message id (used by the realtime push layer). Returns null if missing. */
    ImMessageVO getMessageVO(Long messageId);
}
