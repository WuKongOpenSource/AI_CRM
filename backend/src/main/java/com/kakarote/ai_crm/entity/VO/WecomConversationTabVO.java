package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomConversationTabVO {

    private Long conversationId;

    private String tabKey;

    private String title;

    private String employeeUserId;

    private String employeeName;

    private String conversationType;

    private Date lastMsgTime;

    private Integer messageCount;
}
