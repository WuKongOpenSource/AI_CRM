package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomMessageVO {

    private Long id;

    private Long conversationId;

    private String msgId;

    private Long seq;

    private String action;

    private String msgType;

    private String senderId;

    private String senderType;

    private String receiverList;

    private Date msgTime;

    private String contentText;

    private String contentJson;

    private String sdkFileId;

    private String fileName;

    private Long fileSize;

    private String fileUrl;

    private Boolean recalled;
}
