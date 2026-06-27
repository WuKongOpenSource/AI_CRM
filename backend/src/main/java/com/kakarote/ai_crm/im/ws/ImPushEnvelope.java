package com.kakarote.ai_crm.im.ws;

import com.kakarote.ai_crm.entity.VO.ImMessageVO;
import com.kakarote.ai_crm.entity.VO.ImReactionVO;
import lombok.Data;
import java.util.List;

/** Typed payload pushed to /user/{userId}/queue/im. type = message | unread | reaction. */
@Data
public class ImPushEnvelope {
    private String type;
    private String conversationId;
    private ImMessageVO message;      // type=message
    private Long unread;              // type=message|unread
    private String messageId;         // type=reaction
    private List<ImReactionVO> reactions; // type=reaction

    public static ImPushEnvelope message(String conversationId, ImMessageVO message, Long unread) {
        ImPushEnvelope e = new ImPushEnvelope();
        e.type = "message"; e.conversationId = conversationId; e.message = message; e.unread = unread;
        return e;
    }

    public static ImPushEnvelope unread(String conversationId, Long unread) {
        ImPushEnvelope e = new ImPushEnvelope();
        e.type = "unread"; e.conversationId = conversationId; e.unread = unread;
        return e;
    }

    public static ImPushEnvelope reaction(String conversationId, String messageId, List<ImReactionVO> reactions) {
        ImPushEnvelope e = new ImPushEnvelope();
        e.type = "reaction"; e.conversationId = conversationId; e.messageId = messageId; e.reactions = reactions;
        return e;
    }
}
