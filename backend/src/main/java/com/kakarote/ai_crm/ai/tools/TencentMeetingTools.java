package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.entity.BO.TencentMeetingBindBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingCandidateQueryBO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingCandidateVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingDetailVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingTranscriptSegmentVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingVO;
import com.kakarote.ai_crm.service.impl.TencentMeetingServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
@Component
public class TencentMeetingTools {

    @Autowired
    private TencentMeetingServiceImpl meetingService;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Tool(description = "查询指定客户已关联的腾讯会议。用户询问客户会议、会议纪要、会议转写、会后待办时调用。")
    @AiToolPermission(value = "tencentMeeting:view", action = "查看腾讯会议")
    public String listCustomerMeetings(
            @ToolParam(description = "CRM客户ID，当前客户对话中优先使用绑定客户ID") String customerIdStr) {
        Long customerId = parseLong(customerIdStr);
        if (customerId == null) {
            return "查询腾讯会议失败: 缺少客户ID";
        }
        List<TencentMeetingVO> meetings = meetingService.listCustomerMeetings(customerId);
        if (meetings.isEmpty()) {
            return "当前客户暂无已关联的腾讯会议记录。";
        }
        StringBuilder result = new StringBuilder("## 客户腾讯会议记录\n\n");
        for (TencentMeetingVO meeting : meetings) {
            result.append("- meetingId=").append(meeting.getId())
                    .append("，").append(StrUtil.blankToDefault(meeting.getSubject(), "腾讯会议"));
            if (meeting.getStartTime() != null) {
                result.append("，时间: ").append(dateTimeFormat.format(meeting.getStartTime()));
            }
            if (StrUtil.isNotBlank(meeting.getSummary())) {
                result.append("，摘要: ").append(meeting.getSummary());
            }
            result.append("\n");
        }
        return result.toString();
    }

    @Tool(description = "读取腾讯会议详情、摘要和文字转写。用户要求基于会议生成任务、日程、纪要、需求清单、风险点或下一步建议时调用。")
    @AiToolPermission(value = "tencentMeeting:detail", action = "查看腾讯会议详情")
    public String getMeetingTranscript(
            @ToolParam(description = "系统内部腾讯会议ID，即 listCustomerMeetings 或页面候选返回的 id") String meetingIdStr) {
        Long meetingId = parseLong(meetingIdStr);
        if (meetingId == null) {
            return "读取腾讯会议失败: 会议ID格式无效";
        }
        TencentMeetingDetailVO detail = meetingService.getDetail(meetingId);
        StringBuilder result = new StringBuilder();
        result.append("## 腾讯会议: ").append(StrUtil.blankToDefault(detail.getSubject(), "腾讯会议")).append("\n");
        if (detail.getStartTime() != null) {
            result.append("- 会议时间: ").append(dateTimeFormat.format(detail.getStartTime())).append("\n");
        }
        if (StrUtil.isNotBlank(detail.getSummary())) {
            result.append("- 会议摘要: ").append(detail.getSummary()).append("\n");
        }
        if (StrUtil.isNotBlank(detail.getTodoText())) {
            result.append("- 会议待办: ").append(detail.getTodoText()).append("\n");
        }
        result.append("\n### 会议转写\n");
        if (detail.getTranscriptSegments() == null || detail.getTranscriptSegments().isEmpty()) {
            result.append(StrUtil.blankToDefault(detail.getTranscriptText(), "暂无会议转写。"));
        } else {
            for (TencentMeetingTranscriptSegmentVO segment : detail.getTranscriptSegments()) {
                result.append(StrUtil.blankToDefault(segment.getSpeakerName(), "未知发言人"))
                        .append("：")
                        .append(StrUtil.blankToDefault(segment.getText(), ""))
                        .append("\n");
            }
        }
        return result.toString();
    }

    @Tool(description = "查找可关联到当前客户的腾讯会议候选。用户提到开会、腾讯会议、会议纪要、录音或参会但尚未明确会议ID时调用。")
    @AiToolPermission(value = "tencentMeeting:view", action = "查找腾讯会议候选")
    public String findMeetingCandidates(
            @ToolParam(description = "CRM客户ID") String customerIdStr,
            @ToolParam(description = "用户原始输入或会议关键词", required = false) String inputText) {
        TencentMeetingCandidateQueryBO queryBO = new TencentMeetingCandidateQueryBO();
        queryBO.setCustomerId(parseLong(customerIdStr));
        queryBO.setInputText(inputText);
        queryBO.setLimit(10);
        List<TencentMeetingCandidateVO> candidates = meetingService.queryCandidates(queryBO);
        if (candidates.isEmpty()) {
            return "未找到可关联的腾讯会议候选。";
        }
        StringBuilder result = new StringBuilder("## 腾讯会议候选\n\n");
        for (TencentMeetingCandidateVO candidate : candidates) {
            result.append("- id=").append(candidate.getId())
                    .append("，").append(StrUtil.blankToDefault(candidate.getSubject(), "腾讯会议"))
                    .append("，匹配: ").append(candidate.getMatchReason())
                    .append("，分数: ").append(candidate.getScore())
                    .append("\n");
        }
        return result.toString();
    }

    @Tool(description = "将腾讯会议关联到CRM客户。只有用户明确确认关联某个会议时调用。")
    @AiToolPermission(value = "tencentMeeting:bind", action = "关联腾讯会议客户")
    public String bindMeetingToCustomer(
            @ToolParam(description = "系统内部腾讯会议ID") String meetingIdStr,
            @ToolParam(description = "CRM客户ID") String customerIdStr) {
        Long meetingId = parseLong(meetingIdStr);
        Long customerId = parseLong(customerIdStr);
        if (meetingId == null || customerId == null) {
            return "关联腾讯会议失败: 缺少会议ID或客户ID";
        }
        TencentMeetingBindBO bindBO = new TencentMeetingBindBO();
        bindBO.setMeetingId(meetingId);
        bindBO.setCustomerId(customerId);
        meetingService.bind(bindBO);
        return "腾讯会议已关联到客户。";
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value) || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
