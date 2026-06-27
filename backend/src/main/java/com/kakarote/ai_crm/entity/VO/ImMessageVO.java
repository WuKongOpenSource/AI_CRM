package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class ImMessageVO {
    private String id;            // serialized as String (Snowflake precision)
    private String conversationId;
    private String senderId;
    private String contentType;
    private String content;
    private String attachmentName;
    private String attachmentUrl; // browser-reachable URL resolved from attachmentPath
    private Long attachmentSize;
    private String attachmentMime;
    private String status;
    private Date createTime;
    private String senderName;
    private java.util.List<ImReactionVO> reactions;
    private java.util.List<String> mentionedUserIds;
    private Boolean mentionAll;
    private String parentId;     // root message id if this is a thread reply
    private Integer replyCount;  // for root messages
}
