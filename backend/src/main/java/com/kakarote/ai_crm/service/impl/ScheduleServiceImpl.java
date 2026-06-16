package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.BO.ScheduleAiParseBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.BO.ScheduleUpdateBO;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Schedule;
import com.kakarote.ai_crm.entity.VO.ScheduleAiParseVO;
import com.kakarote.ai_crm.entity.VO.ScheduleParticipantUserVO;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import com.kakarote.ai_crm.mapper.ScheduleMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.IScheduleService;
import com.kakarote.ai_crm.service.ManageUserService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日程服务实现
 */
@Slf4j
@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements IScheduleService {

    @Autowired
    @Lazy
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private DataPermissionService dataPermissionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AI_SCHEDULE_PARSE_PROMPT = """
        你是一名专业的 CRM 日程助理。请分析以下自然语言日程描述，提取结构化信息并以 JSON 格式返回。
        当前时间: %s

        用户输入:
        %s

        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "title": "简洁明确的日程标题",
          "startTime": "开始时间，格式 yyyy-MM-dd HH:mm",
          "endTime": "结束时间，格式 yyyy-MM-dd HH:mm；若无法判断则返回空字符串",
          "type": "日程类型，只能是: meeting, call, visit, other",
          "customerName": "关联客户名称，如果无法识别则为空字符串",
          "participantNames": "参与人名称，多人用逗号分隔；如果无法识别则为空字符串",
          "location": "地点，如果无法识别则为空字符串",
          "description": "补充说明、会议主题或备注"
        }
        """;

    @Override
    public Long addSchedule(ScheduleAddBO scheduleAddBO) {
        Schedule schedule = new Schedule();
        schedule.setTitle(StrUtil.trim(scheduleAddBO.getTitle()));
        schedule.setDescription(StrUtil.emptyToNull(StrUtil.trim(scheduleAddBO.getDescription())));
        schedule.setStartTime(scheduleAddBO.getStartTime());
        schedule.setEndTime(scheduleAddBO.getEndTime());
        schedule.setType(normalizeType(scheduleAddBO.getType()));
        schedule.setCustomerId(scheduleAddBO.getCustomerId());
        schedule.setRelationId(scheduleAddBO.getRelationId());
        schedule.setContactId(scheduleAddBO.getContactId());
        schedule.setLocation(StrUtil.emptyToNull(StrUtil.trim(scheduleAddBO.getLocation())));
        schedule.setParticipantUserIds(joinParticipantUserIds(scheduleAddBO.getParticipantUserIds()));

        if (schedule.getEndTime() != null && schedule.getStartTime() != null && schedule.getEndTime().before(schedule.getStartTime())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "结束时间不能早于开始时间");
        }

        save(schedule);
        return schedule.getScheduleId();
    }

    @Override
    public void updateSchedule(ScheduleUpdateBO scheduleUpdateBO) {
        Schedule schedule = getById(scheduleUpdateBO.getScheduleId());
        if (ObjectUtil.isNull(schedule)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "日程不存在");
        }

        schedule.setTitle(StrUtil.trim(scheduleUpdateBO.getTitle()));
        schedule.setDescription(StrUtil.emptyToNull(StrUtil.trim(scheduleUpdateBO.getDescription())));
        schedule.setStartTime(scheduleUpdateBO.getStartTime());
        schedule.setEndTime(scheduleUpdateBO.getEndTime());
        schedule.setType(normalizeType(scheduleUpdateBO.getType()));
        schedule.setCustomerId(scheduleUpdateBO.getCustomerId());
        schedule.setRelationId(scheduleUpdateBO.getRelationId());
        schedule.setContactId(scheduleUpdateBO.getContactId());
        schedule.setLocation(StrUtil.emptyToNull(StrUtil.trim(scheduleUpdateBO.getLocation())));
        schedule.setParticipantUserIds(joinParticipantUserIds(scheduleUpdateBO.getParticipantUserIds()));

        if (schedule.getEndTime() != null && schedule.getStartTime() != null && schedule.getEndTime().before(schedule.getStartTime())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "结束时间不能早于开始时间");
        }

        updateById(schedule);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = getById(scheduleId);
        if (ObjectUtil.isNull(schedule)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "日程不存在");
        }
        removeById(scheduleId);
    }

    @Override
    public List<ScheduleVO> getMySchedules(String filter) {
        Long userId = UserUtil.getUserId();
        Date today = DateUtil.beginOfDay(new Date());
        Date weekEnd = DateUtil.endOfWeek(new Date());
        DataPermissionContext context = dataPermissionService.createContextByPermission("schedule:view");

        List<ScheduleVO> schedules = baseMapper.getMySchedules(
                userId,
                filter,
                today,
                weekEnd,
                context.isAllData(),
                context.getUserIds()
        );
        enrichSchedules(schedules);
        return schedules;
    }

    @Override
    public BasePage<ScheduleVO> queryPageList(ScheduleQueryBO queryBO) {
        queryBO.setCurrentUserId(UserUtil.getUserId());
        DataPermissionContext context = dataPermissionService.createContextByPermission("schedule:view");
        queryBO.setScheduleAllData(context.isAllData());
        queryBO.setScheduleUserIds(context.getUserIds());
        BasePage<ScheduleVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        enrichSchedules(page.getList());
        return page;
    }

    @Override
    public ScheduleAiParseVO aiParseSchedule(ScheduleAiParseBO parseBO) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String prompt = String.format(AI_SCHEDULE_PARSE_PROMPT, now, parseBO.getContent());

        try {
            String response = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("AI 日程解析原始响应: {}", response);
            return parseScheduleAiResponse(response, parseBO.getContent());
        } catch (Exception e) {
            log.error("AI 日程解析失败，返回默认结果", e);
            return buildFallbackScheduleResult(parseBO.getContent());
        }
    }

    private void enrichSchedules(List<ScheduleVO> schedules) {
        fillTypeName(schedules);
        fillParticipantUsers(schedules);
    }

    private void fillTypeName(List<ScheduleVO> schedules) {
        if (schedules == null) {
            return;
        }
        schedules.forEach(schedule -> schedule.setTypeName(getTypeName(schedule.getType())));
    }

    private void fillParticipantUsers(List<ScheduleVO> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        Set<Long> allUserIds = new LinkedHashSet<>();
        Map<ScheduleVO, List<Long>> scheduleUserIds = new LinkedHashMap<>();

        for (ScheduleVO schedule : schedules) {
            List<Long> userIds = parseParticipantUserIds(schedule.getParticipantUserIdsText());
            schedule.setParticipantUserIds(userIds);
            scheduleUserIds.put(schedule, userIds);
            allUserIds.addAll(userIds);
        }

        Map<Long, ManagerUser> userMap = allUserIds.isEmpty()
                ? Map.of()
                : manageUserService.listByIds(allUserIds).stream()
                    .collect(Collectors.toMap(ManagerUser::getUserId, user -> user, (left, right) -> left));

        for (Map.Entry<ScheduleVO, List<Long>> entry : scheduleUserIds.entrySet()) {
            ScheduleVO schedule = entry.getKey();
            List<Long> userIds = entry.getValue();
            if (userIds.isEmpty()) {
                continue;
            }

            List<ScheduleParticipantUserVO> participantUsers = userIds.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .map(this::toParticipantUser)
                    .toList();

            schedule.setParticipantUsers(participantUsers);
            if (!participantUsers.isEmpty()) {
                schedule.setParticipantNames(participantUsers.stream()
                        .map(ScheduleParticipantUserVO::getRealname)
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    private String getTypeName(String type) {
        if (type == null) return "会议";
        return switch (type.toLowerCase()) {
            case "meeting" -> "会议";
            case "call" -> "电话";
            case "visit" -> "拜访";
            case "other" -> "其他";
            default -> type;
        };
    }

    private ScheduleAiParseVO parseScheduleAiResponse(String response, String originalContent) {
        try {
            String json = extractJsonObject(response);

            JsonNode root = objectMapper.readTree(json);
            ScheduleAiParseVO vo = new ScheduleAiParseVO();
            vo.setTitle(getTextOrDefault(root, "title", ""));
            vo.setStartTime(getTextOrDefault(root, "startTime", ""));
            vo.setEndTime(getTextOrDefault(root, "endTime", ""));
            vo.setType(normalizeType(getTextOrDefault(root, "type", "meeting")));
            vo.setCustomerName(getTextOrDefault(root, "customerName", ""));
            vo.setParticipantNames(getTextOrDefault(root, "participantNames", ""));
            vo.setLocation(getTextOrDefault(root, "location", ""));
            vo.setDescription(getTextOrDefault(root, "description", ""));
            applyParticipantMatches(vo);
            if (!hasMeaningfulAiParseResult(vo)) {
                log.warn("AI 日程解析响应缺少有效字段，使用兜底结果。response={}", response);
                return buildFallbackScheduleResult(originalContent);
            }
            return vo;
        } catch (Exception e) {
            log.warn("AI 日程解析响应 JSON 解析失败: {}", e.getMessage());
            return buildFallbackScheduleResult(originalContent);
        }
    }

    private void applyParticipantMatches(ScheduleAiParseVO vo) {
        ParticipantMatchResult matchResult = resolveParticipantUsersByNames(vo.getParticipantNames());
        vo.setParticipantUserIds(matchResult.participantUsers().stream()
                .map(ScheduleParticipantUserVO::getUserId)
                .toList());
        vo.setParticipantUsers(matchResult.participantUsers());
        vo.setUnmatchedParticipantNames(String.join(", ", matchResult.unmatchedNames()));
    }

    private boolean hasMeaningfulAiParseResult(ScheduleAiParseVO vo) {
        return StrUtil.isNotBlank(vo.getTitle())
                || StrUtil.isNotBlank(vo.getStartTime())
                || StrUtil.isNotBlank(vo.getEndTime())
                || StrUtil.isNotBlank(vo.getCustomerName())
                || StrUtil.isNotBlank(vo.getParticipantNames())
                || StrUtil.isNotBlank(vo.getLocation())
                || StrUtil.isNotBlank(vo.getDescription())
                || (vo.getParticipantUsers() != null && !vo.getParticipantUsers().isEmpty())
                || StrUtil.isNotBlank(vo.getUnmatchedParticipantNames());
    }

    private ParticipantMatchResult resolveParticipantUsersByNames(String participantNames) {
        List<String> names = splitParticipantNames(participantNames);
        if (names.isEmpty()) {
            return new ParticipantMatchResult(List.of(), List.of());
        }

        List<ManagerUser> activeUsers = manageUserService.lambdaQuery()
                .eq(ManagerUser::getStatus, 1)
                .list();

        Map<Long, ScheduleParticipantUserVO> matchedUsers = new LinkedHashMap<>();
        List<String> unmatchedNames = new ArrayList<>();

        for (String name : names) {
            ManagerUser matched = findBestUserMatch(name, activeUsers);
            if (matched == null) {
                unmatchedNames.add(name);
                continue;
            }
            matchedUsers.putIfAbsent(matched.getUserId(), toParticipantUser(matched));
        }

        return new ParticipantMatchResult(new ArrayList<>(matchedUsers.values()), unmatchedNames);
    }

    private ManagerUser findBestUserMatch(String name, List<ManagerUser> users) {
        if (StrUtil.isBlank(name) || users == null || users.isEmpty()) {
            return null;
        }

        List<ManagerUser> exactUsernameMatches = users.stream()
                .filter(user -> StrUtil.equalsIgnoreCase(name, user.getUsername()))
                .toList();
        if (exactUsernameMatches.size() == 1) {
            return exactUsernameMatches.get(0);
        }

        List<ManagerUser> exactRealnameMatches = users.stream()
                .filter(user -> StrUtil.equals(name, user.getRealname()))
                .toList();
        if (exactRealnameMatches.size() == 1) {
            return exactRealnameMatches.get(0);
        }

        List<ManagerUser> fuzzyMatches = users.stream()
                .filter(user -> StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(user.getRealname()), name)
                        || StrUtil.containsIgnoreCase(StrUtil.nullToEmpty(user.getUsername()), name))
                .limit(2)
                .toList();
        if (fuzzyMatches.size() == 1) {
            return fuzzyMatches.get(0);
        }

        return null;
    }

    private ScheduleParticipantUserVO toParticipantUser(ManagerUser user) {
        ScheduleParticipantUserVO vo = new ScheduleParticipantUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealname(getUserDisplayName(user));
        return vo;
    }

    private String getUserDisplayName(ManagerUser user) {
        if (user == null) {
            return "";
        }
        return StrUtil.blankToDefault(StrUtil.trim(user.getRealname()), user.getUsername());
    }

    private List<String> splitParticipantNames(String participantNames) {
        if (StrUtil.isBlank(participantNames)) {
            return List.of();
        }
        return Arrays.stream(participantNames.split("[,，]"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    private List<Long> parseParticipantUserIds(String participantUserIdsText) {
        if (StrUtil.isBlank(participantUserIdsText)) {
            return List.of();
        }

        List<Long> userIds = new ArrayList<>();
        for (String part : participantUserIdsText.split(",")) {
            String value = StrUtil.trim(part);
            if (StrUtil.isBlank(value)) {
                continue;
            }
            try {
                userIds.add(Long.parseLong(value));
            } catch (NumberFormatException e) {
                log.warn("忽略非法参与人员工ID: {}", value);
            }
        }
        return userIds;
    }

    private String joinParticipantUserIds(List<Long> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            return null;
        }
        return participantUserIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private String extractJsonObject(String response) {
        String normalized = StrUtil.nullToEmpty(response).trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("```(?:json)?\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }

        int thinkEnd = normalized.lastIndexOf("</think>");
        if (thinkEnd >= 0) {
            normalized = normalized.substring(thinkEnd + "</think>".length()).trim();
        }

        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return normalized.substring(start, end + 1).trim();
        }
        return StrUtil.isBlank(normalized) ? "{}" : normalized;
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        if (root.has(field) && !root.get(field).isNull()) {
            String value = root.get(field).asText();
            return StrUtil.isNotBlank(value) ? value : defaultValue;
        }
        return defaultValue;
    }

    private String normalizeType(String type) {
        if (StrUtil.isBlank(type)) {
            return "meeting";
        }
        return switch (type.trim().toLowerCase()) {
            case "meeting", "会议" -> "meeting";
            case "call", "电话" -> "call";
            case "visit", "拜访" -> "visit";
            case "other", "其他" -> "other";
            default -> "meeting";
        };
    }

    private ScheduleAiParseVO buildFallbackScheduleResult(String content) {
        ScheduleAiParseVO vo = new ScheduleAiParseVO();
        vo.setTitle(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        vo.setStartTime("");
        vo.setEndTime("");
        vo.setType("meeting");
        vo.setCustomerName("");
        vo.setParticipantNames("");
        vo.setParticipantUserIds(List.of());
        vo.setParticipantUsers(List.of());
        vo.setUnmatchedParticipantNames("");
        vo.setLocation("");
        vo.setDescription(content);
        return vo;
    }

    private record ParticipantMatchResult(
            List<ScheduleParticipantUserVO> participantUsers,
            List<String> unmatchedNames
    ) {
    }
}
