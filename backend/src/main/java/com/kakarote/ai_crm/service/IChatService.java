package com.kakarote.ai_crm.service;

import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.SessionCreateBO;
import com.kakarote.ai_crm.entity.VO.ChatMessageVO;
import com.kakarote.ai_crm.entity.VO.ChatSessionVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI聊天服务接口
 */
public interface IChatService {

    /**
     * 创建会话
     */
    Long createSession(SessionCreateBO sessionCreateBO);

    /**
     * 获取会话列表
     */
    List<ChatSessionVO> getSessionList();

    /**
     * 删除会话
     */
    void deleteSession(Long sessionId);

    /**
     * 获取会话消息历史
     */
    List<ChatMessageVO> getMessageList(Long sessionId);

    /**
     * 发送消息（流式响应）
     */
    Flux<String> streamChat(Long sessionId, String content);

    /**
     * 发送消息（非流式响应）
     */
    String chat(Long sessionId, String content);
}
