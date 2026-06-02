package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingVO {

    private Long id;

    private String meetingId;

    private String meetingCode;

    private String subject;

    private String status;

    private String creatorUserId;

    private String creatorName;

    private Long crmCreatorUserId;

    private String participantNames;

    private Integer participantCount;

    private Date startTime;

    private Date endTime;

    private Long durationSeconds;

    private String bindStatus;

    private Long customerId;

    private String customerName;

    private String summary;

    private String todoText;
}
