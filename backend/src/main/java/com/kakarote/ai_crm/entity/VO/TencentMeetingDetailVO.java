package com.kakarote.ai_crm.entity.VO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TencentMeetingDetailVO extends TencentMeetingVO {

    private String transcriptText;

    private List<TencentMeetingParticipantVO> participants = new ArrayList<>();

    private List<TencentMeetingRecordingVO> recordings = new ArrayList<>();

    private List<TencentMeetingTranscriptSegmentVO> transcriptSegments = new ArrayList<>();
}
