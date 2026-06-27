package com.kakarote.ai_crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kakarote.ai_crm.entity.PO.ImMessageReaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImMessageReactionMapper extends BaseMapper<ImMessageReaction> {

    @org.apache.ibatis.annotations.Insert("INSERT INTO crm_im_message_reaction(id, message_id, conversation_id, user_id, emoji, create_time) " +
            "VALUES(#{id}, #{messageId}, #{conversationId}, #{userId}, #{emoji}, #{createTime}) " +
            "ON CONFLICT (message_id, user_id, emoji) DO NOTHING")
    int insertIgnoreDuplicate(ImMessageReaction r);
}
