package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.auth.RequirePermission;
import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.TencentMeetingCandidateQueryBO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingCandidateVO;
import com.kakarote.ai_crm.entity.VO.TencentMeetingVO;
import com.kakarote.ai_crm.service.impl.TencentMeetingServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/customer/{id}/tencent-meetings")
@Tag(name = "客户腾讯会议")
public class CustomerTencentMeetingController {

    @Autowired
    private TencentMeetingServiceImpl meetingService;

    @GetMapping
    @Operation(summary = "客户腾讯会议列表")
    @RequirePermission("tencentMeeting:view")
    public Result<List<TencentMeetingVO>> listCustomerMeetings(@PathVariable("id") Long customerId) {
        return Result.ok(meetingService.listCustomerMeetings(customerId));
    }

    @PostMapping("/candidates")
    @Operation(summary = "客户腾讯会议候选")
    @RequirePermission("tencentMeeting:view")
    public Result<List<TencentMeetingCandidateVO>> candidates(
            @PathVariable("id") Long customerId,
            @RequestBody(required = false) TencentMeetingCandidateQueryBO queryBO) {
        TencentMeetingCandidateQueryBO actual = queryBO == null ? new TencentMeetingCandidateQueryBO() : queryBO;
        actual.setCustomerId(customerId);
        return Result.ok(meetingService.queryCandidates(actual));
    }
}
