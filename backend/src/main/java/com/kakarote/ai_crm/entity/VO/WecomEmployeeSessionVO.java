package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class WecomEmployeeSessionVO {

    private Long id;

    private String userId;

    private Long crmUserId;

    private String name;

    private String avatar;

    private String position;

    private Integer status;

    private Long conversationCount;

    private Date lastMsgTime;
}
