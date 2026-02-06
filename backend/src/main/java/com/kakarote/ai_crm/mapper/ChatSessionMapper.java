package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kakarote.ai_crm.entity.PO.ChatSession;
import com.kakarote.ai_crm.entity.VO.ChatSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会话Mapper
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 分页查询用户会话
     */
    IPage<ChatSessionVO> queryPageList(IPage<ChatSessionVO> page, @Param("userId") Long userId);

    /**
     * 查询会话详情
     */
    ChatSessionVO getSessionById(@Param("sessionId") Long sessionId);
}
