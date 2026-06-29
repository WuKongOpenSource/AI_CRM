package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.CandidateAddBO;
import com.kakarote.ai_crm.entity.BO.CandidateQueryBO;
import com.kakarote.ai_crm.entity.BO.CandidateUpdateBO;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.PO.Candidate;
import com.kakarote.ai_crm.entity.VO.CandidateDetailVO;
import com.kakarote.ai_crm.entity.VO.CandidateListVO;
import com.kakarote.ai_crm.service.ICandidateService;
import com.kakarote.ai_crm.service.IScheduleService;
import com.kakarote.ai_crm.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

@Slf4j
@Component
public class CandidateTools {

    @Autowired
    private ICandidateService candidateService;

    @Autowired
    private IScheduleService scheduleService;

    @Autowired
    private PermissionService permissionService;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @Tool(description = "查询候选人。可按姓名、手机号、邮箱、当前公司、应聘岗位、技能标签关键词查询，也可按候选人阶段筛选。")
    @AiToolPermission(value = "candidate:view", action = "查询候选人")
    public String queryCandidates(
            @ToolParam(description = "搜索关键词", required = false) String keyword,
            @ToolParam(description = "候选人阶段: new/screening/interview_scheduled/interview_passed/offer/hired/rejected", required = false) String stage,
            @ToolParam(description = "返回数量，默认10，最多20", required = false) String limitStr) {
        CandidateQueryBO queryBO = new CandidateQueryBO();
        queryBO.setKeyword(clean(keyword));
        queryBO.setStage(clean(stage));
        queryBO.setPage(1);
        queryBO.setLimit(Math.min(parseInt(limitStr, 10), 20));
        BasePage<CandidateListVO> page = candidateService.queryPageList(queryBO);
        if (page.getRecords().isEmpty()) {
            return "没有找到匹配的候选人。";
        }
        StringBuilder sb = new StringBuilder("候选人查询结果:\n");
        for (CandidateListVO candidate : page.getRecords()) {
            sb.append("- candidateId=").append(candidate.getCandidateId())
                    .append("，姓名=").append(nullToDash(candidate.getName()))
                    .append("，阶段=").append(nullToDash(candidate.getStageName()))
                    .append("，应聘岗位=").append(nullToDash(candidate.getAppliedPosition()))
                    .append("，当前=").append(nullToDash(candidate.getCurrentCompany()))
                    .append(" ").append(nullToDash(candidate.getCurrentPosition()))
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "查看当前候选人或指定候选人的完整资料摘要。candidateId 留空时使用当前候选人会话绑定的 candidateId。")
    @AiToolPermission(value = "candidate:view", action = "查看候选人")
    public String getCandidateProfile(
            @ToolParam(description = "候选人ID，留空使用当前候选人", required = false) String candidateIdStr) {
        Long candidateId = resolveCandidateId(candidateIdStr);
        if (candidateId == null) {
            return "查看候选人失败: 当前会话没有绑定候选人，请提供 candidateId。";
        }
        CandidateDetailVO detail = candidateService.getCandidateDetail(candidateId);
        return formatCandidateDetail(detail);
    }

    @Tool(description = "创建候选人。用户明确要新增候选人或录入候选人基础信息时调用。")
    @AiToolPermission(value = "candidate:create", action = "创建候选人")
    public String createCandidate(
            @ToolParam(description = "候选人姓名，必填") String name,
            @ToolParam(description = "手机号", required = false) String phone,
            @ToolParam(description = "邮箱", required = false) String email,
            @ToolParam(description = "应聘岗位", required = false) String appliedPosition,
            @ToolParam(description = "岗位要求或招聘要求", required = false) String jobRequirements,
            @ToolParam(description = "当前公司", required = false) String currentCompany,
            @ToolParam(description = "当前职位", required = false) String currentPosition,
            @ToolParam(description = "来源", required = false) String source,
            @ToolParam(description = "技能标签，逗号分隔", required = false) String skillTags,
            @ToolParam(description = "备注", required = false) String remark) {
        CandidateAddBO bo = new CandidateAddBO();
        bo.setName(name);
        bo.setPhone(clean(phone));
        bo.setEmail(clean(email));
        bo.setAppliedPosition(clean(appliedPosition));
        bo.setJobRequirements(clean(jobRequirements));
        bo.setCurrentCompany(clean(currentCompany));
        bo.setCurrentPosition(clean(currentPosition));
        bo.setSource(clean(source));
        bo.setSkillTags(clean(skillTags));
        bo.setRemark(clean(remark));
        Long candidateId = candidateService.addCandidate(bo);
        return "候选人创建成功。\n- candidateId: " + candidateId + "\n- 姓名: " + name;
    }

    @Tool(description = "更新候选人资料。candidateId 留空时使用当前候选人；只填写需要更新的字段。")
    @AiToolPermission(value = "candidate:edit", action = "更新候选人")
    public String updateCandidate(
            @ToolParam(description = "候选人ID，留空使用当前候选人", required = false) String candidateIdStr,
            @ToolParam(description = "候选人阶段: new/screening/interview_scheduled/interview_passed/offer/hired/rejected", required = false) String stage,
            @ToolParam(description = "应聘岗位", required = false) String appliedPosition,
            @ToolParam(description = "岗位要求或招聘要求", required = false) String jobRequirements,
            @ToolParam(description = "当前公司", required = false) String currentCompany,
            @ToolParam(description = "当前职位", required = false) String currentPosition,
            @ToolParam(description = "期望城市", required = false) String expectedCity,
            @ToolParam(description = "期望薪资", required = false) String expectedSalary,
            @ToolParam(description = "技能标签，逗号分隔", required = false) String skillTags,
            @ToolParam(description = "备注", required = false) String remark) {
        Long candidateId = resolveCandidateId(candidateIdStr);
        if (candidateId == null) {
            return "更新候选人失败: 当前会话没有绑定候选人，请提供 candidateId。";
        }
        CandidateUpdateBO bo = new CandidateUpdateBO();
        bo.setCandidateId(candidateId);
        String normalizedStage = normalizeCandidateStageAlias(stage);
        if (StrUtil.isNotBlank(normalizedStage) && !permissionService.hasPermission("candidate:change_stage")) {
            return "更新候选人失败: 你暂无变更候选人阶段的权限。";
        }
        bo.setStage(normalizedStage);
        bo.setAppliedPosition(clean(appliedPosition));
        bo.setJobRequirements(clean(jobRequirements));
        bo.setCurrentCompany(clean(currentCompany));
        bo.setCurrentPosition(clean(currentPosition));
        bo.setExpectedCity(clean(expectedCity));
        bo.setExpectedSalary(clean(expectedSalary));
        bo.setSkillTags(clean(skillTags));
        bo.setRemark(clean(remark));
        candidateService.updateCandidate(bo);
        return "候选人更新成功。\n- candidateId: " + candidateId;
    }

    @Tool(description = "变更候选人阶段。candidateId 留空时使用当前候选人；支持中文阶段：新候选人、初筛、安排面试、面试通过、Offer、已入职、已淘汰。")
    @AiToolPermission(value = "candidate:change_stage", action = "变更候选人阶段")
    public String changeCandidateStage(
            @ToolParam(description = "候选人ID，留空使用当前候选人", required = false) String candidateIdStr,
            @ToolParam(description = "候选人阶段: new/screening/interview_scheduled/interview_passed/offer/hired/rejected，或中文阶段名", required = true) String stage) {
        Long candidateId = resolveCandidateId(candidateIdStr);
        if (candidateId == null) {
            return "变更候选人阶段失败: 当前会话没有绑定候选人，请提供 candidateId。";
        }
        String normalizedStage = normalizeCandidateStageAlias(stage);
        if (StrUtil.isBlank(normalizedStage)) {
            return "变更候选人阶段失败: 阶段不能为空。";
        }
        candidateService.updateStage(candidateId, normalizedStage);
        Candidate candidate = candidateService.getVisibleCandidate(candidateId);
        return "候选人阶段变更成功。\n- candidateId: " + candidateId
                + "\n- 候选人: " + candidate.getName()
                + "\n- 阶段: " + stageLabel(normalizedStage) + "（" + normalizedStage + "）";
    }

    @Tool(description = "为候选人创建面试日程。仅当用户提到具体面试时间点时调用；location 可填写面试间、会议室或会议链接。candidateId 留空时使用当前候选人。")
    @AiToolPermission(value = "schedule:create", action = "创建候选人面试")
    public String createCandidateInterviewSchedule(
            @ToolParam(description = "候选人ID，留空使用当前候选人", required = false) String candidateIdStr,
            @ToolParam(description = "面试标题，必填") String title,
            @ToolParam(description = "开始时间，格式 yyyy-MM-dd HH:mm，必填") String startTime,
            @ToolParam(description = "结束时间，格式 yyyy-MM-dd HH:mm", required = false) String endTime,
            @ToolParam(description = "地点、面试间、会议室或会议链接", required = false) String location,
            @ToolParam(description = "描述", required = false) String description) {
        try {
            Long candidateId = resolveCandidateId(candidateIdStr);
            if (candidateId == null) {
                return "创建面试失败: 当前会话没有绑定候选人，请提供 candidateId。";
            }
            Candidate candidate = candidateService.getVisibleCandidate(candidateId);
            ScheduleAddBO bo = new ScheduleAddBO();
            bo.setCandidateId(candidateId);
            bo.setTitle(StrUtil.blankToDefault(clean(title), "候选人面试 - " + candidate.getName()));
            bo.setType("interview");
            bo.setStartTime(dateTimeFormat.parse(startTime));
            if (StrUtil.isNotBlank(clean(endTime))) {
                bo.setEndTime(dateTimeFormat.parse(endTime));
            }
            bo.setLocation(clean(location));
            bo.setDescription(clean(description));
            Long scheduleId = scheduleService.addSchedule(bo);
            return "面试日程创建成功。\n- scheduleId: " + scheduleId
                    + "\n- candidateId: " + candidateId
                    + "\n- 候选人: " + candidate.getName()
                    + "\n- 开始时间: " + startTime
                    + (StrUtil.isNotBlank(clean(location)) ? "\n- 地点: " + clean(location) : "");
        } catch (Exception exception) {
            log.warn("创建候选人面试失败", exception);
            return "创建面试失败: " + exception.getMessage();
        }
    }

    private Long resolveCandidateId(String candidateIdStr) {
        if (StrUtil.isNotBlank(clean(candidateIdStr))) {
            return Long.parseLong(candidateIdStr.trim());
        }
        return AiContextHolder.getCurrentCandidateId();
    }

    private String formatCandidateDetail(CandidateDetailVO detail) {
        StringBuilder sb = new StringBuilder("候选人资料:\n");
        sb.append("- candidateId: ").append(detail.getCandidateId()).append("\n");
        sb.append("- 姓名: ").append(nullToDash(detail.getName())).append("\n");
        sb.append("- 阶段: ").append(nullToDash(detail.getStageName())).append("\n");
        sb.append("- 应聘岗位: ").append(nullToDash(detail.getAppliedPosition())).append("\n");
        if (StrUtil.isNotBlank(detail.getJobRequirements())) {
            sb.append("- 岗位要求: ").append(detail.getJobRequirements()).append("\n");
        }
        sb.append("- 当前: ").append(nullToDash(detail.getCurrentCompany())).append(" ").append(nullToDash(detail.getCurrentPosition())).append("\n");
        sb.append("- 联系方式: ").append(nullToDash(detail.getPhone())).append(" / ").append(nullToDash(detail.getEmail())).append("\n");
        sb.append("- 学历: ").append(nullToDash(detail.getEducation())).append(" ").append(nullToDash(detail.getSchool())).append("\n");
        sb.append("- 期望: ").append(nullToDash(detail.getExpectedCity())).append(" ").append(nullToDash(detail.getExpectedSalary())).append("\n");
        if (StrUtil.isNotBlank(detail.getSkillTags())) {
            sb.append("- 技能: ").append(detail.getSkillTags()).append("\n");
        }
        if (StrUtil.isNotBlank(detail.getResumeSummary())) {
            sb.append("- 简历摘要: ").append(detail.getResumeSummary()).append("\n");
        }
        if (StrUtil.isNotBlank(detail.getAiEvaluation())) {
            sb.append("- AI评估: ").append(detail.getAiEvaluation()).append("\n");
        }
        return sb.toString();
    }

    private String clean(String value) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) || "null".equalsIgnoreCase(normalized) ? null : normalized;
    }

    private String normalizeCandidateStageAlias(String stage) {
        String normalized = clean(stage);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        String compact = normalized.replaceAll("[\\s,，。.!！;；:：\"“”'‘’、]", "").toLowerCase();
        if (StrUtil.equalsAny(compact, "new", "新候选人", "新建候选人")) {
            return "new";
        }
        if (StrUtil.equalsAny(compact, "screening", "初筛", "筛选")) {
            return "screening";
        }
        if (StrUtil.equalsAny(compact, "interview_scheduled", "安排面试", "待面试", "约面试", "面试中", "进入面试")) {
            return "interview_scheduled";
        }
        if (StrUtil.equalsAny(compact, "interview_passed", "面试通过", "通过面试")) {
            return "interview_passed";
        }
        if (StrUtil.equalsAny(compact, "offer", "发offer", "给offer")) {
            return "offer";
        }
        if (StrUtil.equalsAny(compact, "hired", "已入职", "入职", "入职了", "录用")) {
            return "hired";
        }
        if (StrUtil.equalsAny(compact, "rejected", "已淘汰", "淘汰", "淘汰了", "拒绝", "不合适", "面试不通过", "未通过", "pass掉")) {
            return "rejected";
        }
        return normalized;
    }

    private String stageLabel(String stage) {
        return switch (StrUtil.blankToDefault(stage, "")) {
            case "new" -> "新候选人";
            case "screening" -> "初筛";
            case "interview_scheduled" -> "安排面试";
            case "interview_passed" -> "面试通过";
            case "offer" -> "Offer";
            case "hired" -> "已入职";
            case "rejected" -> "已淘汰";
            default -> stage;
        };
    }

    private String nullToDash(String value) {
        return StrUtil.blankToDefault(value, "-");
    }

    private int parseInt(String value, int defaultValue) {
        if (StrUtil.isBlank(clean(value))) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
