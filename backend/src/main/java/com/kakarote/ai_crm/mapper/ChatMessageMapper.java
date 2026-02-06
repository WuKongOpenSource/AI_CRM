package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ChatMessage;
import com.kakarote.ai_crm.entity.VO.ChatMessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 查询会话消息列表
     */
    List<ChatMessageVO> getMessagesBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 查询会话最后一条消息
     */
    ChatMessage getLastMessage(@Param("sessionId") Long sessionId);

    /**
     * 统计会话消息数
     */
    Integer countBySessionId(@Param("sessionId") Long sessionId);
}
