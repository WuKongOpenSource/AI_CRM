package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingCandidateVO {

    private Long id;

    private String meetingId;

    private String subject;

    private String creatorName;

    private String participantNames;

    private Date startTime;

    private Long durationSeconds;

    private String summary;

    private Integer score;

    private String matchReason;
}
