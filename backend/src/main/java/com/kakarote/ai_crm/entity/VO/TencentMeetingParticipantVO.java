package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingParticipantVO {

    private Long id;

    private String userId;

    private String userName;

    private String role;

    private Date joinTime;

    private Date leaveTime;

    private Long durationSeconds;
}
