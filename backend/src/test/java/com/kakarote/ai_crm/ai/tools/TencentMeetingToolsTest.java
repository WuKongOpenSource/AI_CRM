package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.entity.VO.TencentMeetingDetailVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingTranscriptSegmentVO;
import com.kakarote.ai_crm.service.impl.TencentMeetingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TencentMeetingToolsTest {

    @Test
    void getMeetingTranscriptShouldReturnSummaryAndTranscriptText() {
        TencentMeetingServiceImpl meetingService = mock(TencentMeetingServiceImpl.class);
        TencentMeetingTools tools = new TencentMeetingTools();
        ReflectionTestUtils.setField(tools, "meetingService", meetingService);

        TencentMeetingDetailVO detail = new TencentMeetingDetailVO();
        detail.setId(10L);
        detail.setSubject("报价方案会");
        detail.setStartTime(new Date(1710000000000L));
        detail.setSummary("客户关注报价有效期");
        TencentMeetingTranscriptSegmentVO segment = new TencentMeetingTranscriptSegmentVO();
        segment.setSpeakerName("张三");
        segment.setText("下周三之前把报价方案发给客户");
        detail.setTranscriptSegments(List.of(segment));
        when(meetingService.getDetail(10L)).thenReturn(detail);

        String result = tools.getMeetingTranscript("10");

        assertThat(result).contains("报价方案会");
        assertThat(result).contains("客户关注报价有效期");
        assertThat(result).contains("张三：下周三之前把报价方案发给客户");
    }
}
