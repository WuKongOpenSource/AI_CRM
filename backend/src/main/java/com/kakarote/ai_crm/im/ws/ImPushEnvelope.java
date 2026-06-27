package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Typed payload pushed to /user/{userId}/queue/im. type = message | unread. (presence is broadcast separately on /topic/im.presence) */
@Data
@AllArgsConstructor
public class ImPushEnvelope {
    private String type;
    private String conversationId;
    private ImMessageVO message; // for type=message (new or recalled)
    private Long unread;         // recipient's unread count for the conversation
}
