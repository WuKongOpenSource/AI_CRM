package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class TencentMeetingTranscriptSegmentVO {

    private Long id;

    private String pid;

    private String speakerUserId;

    private String speakerName;

    private Long startTimeMs;

    private Long endTimeMs;

    private String text;
}
