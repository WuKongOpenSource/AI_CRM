package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

@Data
public class ImSendMessageBO {
    private Long conversationId;
    private String contentType;   // text/image/file; default text
    private String content;
    private String attachmentName;
    private String attachmentPath;
    private Long attachmentSize;
    private String attachmentMime;
    private java.util.List<Long> mentionedUserIds;
    private Boolean mentionAll;
    private Long parentId;       // set when sending a thread reply
}
