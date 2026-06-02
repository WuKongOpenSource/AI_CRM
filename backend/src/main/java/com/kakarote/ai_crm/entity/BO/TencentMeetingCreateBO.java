package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TencentMeetingCreateBO {

    private String subject;

    private Date startTime;

    private Date endTime;

    private String creatorUserId;

    private String password;

    private List<String> inviteeUserIds;
}
