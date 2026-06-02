package com.kakarote.ai_crm.entity.BO;

import lombok.Data;

import java.util.Date;

@Data
public class TencentMeetingCandidateQueryBO {

    private Long customerId;

    private String keyword;

    private String inputText;

    private Date startTimeFrom;

    private Date startTimeTo;

    private Integer limit;
}
