package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomConversationVO {

    private Long id;

    private String conversationType;

    private Long employeeId;

    private String employeeUserId;

    private Long externalCustomerId;

    private String externalUserId;

    private Long groupChatId;

    private String chatId;

    private String title;

    private String peerName;

    private String peerAvatar;

    private Long customerId;

    private Long ownerUserId;

    private String lastMsgId;

    private Date lastMsgTime;

    private String lastMsgPreview;

    private Integer messageCount;
}
