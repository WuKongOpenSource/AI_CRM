package com.kakarote.ai_crm.entity.VO;

import lombok.Data;

@Data
public class TencentMeetingRecordingVO {

    private Long id;

    private String recordFileId;

    private String fileName;

    private String downloadUrl;

    private String playUrl;

    private Long fileSize;

    private Long durationSeconds;

    private String transcriptStatus;

    private String summary;

    private String todoText;
}
