package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.tools.support.AiCustomerMatcher;
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
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private IGlobalSearchIndexService globalSearchIndexService;

    @Autowired
    private DataPermissionService dataPermissionService;

    @Autowired
    private AiCustomerMatcher aiCustomerMatcher;

    @Autowired
    private AiQuotaService aiQuotaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AI_SCHEDULE_PARSE_PROMPT = """
        你是一名专业的 CRM 日程助理。请分析以下自然语言日程描述，提取结构化信息并以 JSON 格式返回。
        当前时间: %s

        用户输入:
        %s

        额外规则：
        1. 如果描述中出现“某公司某联系人开会/沟通/讨论”，`customerName` 只填写公司名，不要把客户联系人姓名并入公司名。
        2. `participantNames` 只填写明确提到的内部参与同事/系统员工；客户联系人、外部人员、会议对象不要放进 `participantNames`。
        3. 如果客户联系人是本次会议/沟通对象，标题里应尽量保留该联系人姓名，避免标题信息丢失。

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

    private static final Pattern COMPANY_CONTACT_PATTERN = Pattern.compile(
            "([\\u4E00-\\u9FA5A-Za-z0-9()（）·&._-]{2,80}?(?:有限责任公司|股份有限公司|集团有限公司|有限公司|集团公司|公司|集团|中心|事务所|工作室|工厂|厂))\\s*([\\u4E00-\\u9FA5]{2,4}(?:总|经理|主任|老师|先生|女士)?)\\s*(?=(?:开会|会面|见面|沟通|讨论|交流|拜访|对接|电话|聊|商量|洽谈|汇报|约见))"
    );

    private static final Pattern COMPANY_NAME_PATTERN = Pattern.compile(
            "([\\u4E00-\\u9FA5A-Za-z0-9()（）·&._-]{2,80}?(?:有限责任公司|股份有限公司|集团有限公司|有限公司|集团公司|公司|集团|中心|事务所|工作室|工厂|厂))"
    );

    private static final Pattern EXPLICIT_PARTICIPANT_PATTERN = Pattern.compile(
            "(?:参与人(?:员)?|参与者|参会人(?:员)?|出席人(?:员)?|同行(?:人员)?|与会人员)\\s*(?:有|为|是|包括|包含|[:：])\\s*([^。；;]+)"
    );

    /**
     * 新增日程。
     */
    @Override
    public Long addSchedule(ScheduleAddBO scheduleAddBO) {
        Schedule schedule = new Schedule();
        schedule.setTitle(StrUtil.trim(scheduleAddBO.getTitle()));
        schedule.setDescription(StrUtil.emptyToNull(StrUtil.trim(scheduleAddBO.getDescription())));
        schedule.setStartTime(scheduleAddBO.getStartTime());
        schedule.setEndTime(scheduleAddBO.getEndTime());
        schedule.setType(normalizeType(scheduleAddBO.getType()));
        schedule.setCustomerId(scheduleAddBO.getCustomerId());
        schedule.setContactId(scheduleAddBO.getContactId());
        schedule.setLocation(StrUtil.emptyToNull(StrUtil.trim(scheduleAddBO.getLocation())));
        schedule.setParticipantUserIds(joinParticipantUserIds(scheduleAddBO.getParticipantUserIds()));

        if (schedule.getEndTime() != null && schedule.getStartTime() != null && schedule.getEndTime().before(schedule.getStartTime())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "结束时间不能早于开始时间");
        }

        save(schedule);
        globalSearchIndexService.refreshScheduleIndex(schedule.getScheduleId());
        return schedule.getScheduleId();
    }

    /**
     * 更新日程。
     */
    @Override
    public void updateSchedule(ScheduleUpdateBO scheduleUpdateBO) {
        Schedule schedule = getById(scheduleUpdateBO.getScheduleId());
        if (ObjectUtil.isNull(schedule)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "日程不存在");
        }
        dataPermissionService.assertUserDataAccessByPermission("schedule:edit", schedule.getCreateUserId());

        schedule.setTitle(StrUtil.trim(scheduleUpdateBO.getTitle()));
        schedule.setDescription(StrUtil.emptyToNull(StrUtil.trim(scheduleUpdateBO.getDescription())));
        schedule.setStartTime(scheduleUpdateBO.getStartTime());
        schedule.setEndTime(scheduleUpdateBO.getEndTime());
        schedule.setType(normalizeType(scheduleUpdateBO.getType()));
        schedule.setCustomerId(scheduleUpdateBO.getCustomerId());
        schedule.setContactId(scheduleUpdateBO.getContactId());
        schedule.setLocation(StrUtil.emptyToNull(StrUtil.trim(scheduleUpdateBO.getLocation())));
        schedule.setParticipantUserIds(joinParticipantUserIds(scheduleUpdateBO.getParticipantUserIds()));

        if (schedule.getEndTime() != null && schedule.getStartTime() != null && schedule.getEndTime().before(schedule.getStartTime())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "结束时间不能早于开始时间");
        }

        updateById(schedule);
        globalSearchIndexService.refreshScheduleIndex(schedule.getScheduleId());
    }

    /**
     * 删除日程。
     */
    @Override
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = getById(scheduleId);
        if (ObjectUtil.isNull(schedule)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "日程不存在");
        }
        dataPermissionService.assertUserDataAccessByPermission("schedule:delete", schedule.getCreateUserId());
        removeById(scheduleId);
        globalSearchIndexService.deleteByEntity("schedule", scheduleId);
    }

    /**
     * 获取MY日程。
     */
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

    /**
     * 分页查询日程列表。
     */
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

    /**
     * 使用 AI 解析日程。
     */
    @Override
    public ScheduleAiParseVO aiParseSchedule(ScheduleAiParseBO parseBO) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String prompt = String.format(AI_SCHEDULE_PARSE_PROMPT, now, parseBO.getContent());

        try {
            aiQuotaService.ensureQuotaAvailable("schedule_parse", null, null, prompt);
            var chatResponse = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();
            String response = chatResponse.getResult().getOutput().getText();
            aiQuotaService.consumeResolvedTokens(
                "schedule_parse",
                aiQuotaService.resolveTokenUsage(chatResponse, null, null, prompt, response)
            );

            log.info("AI 日程解析原始响应: {}", response);
            return parseScheduleAiResponse(response, parseBO.getContent());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 日程解析失败，返回默认结果", e);
            return buildFallbackScheduleResult(parseBO.getContent());
        }
    }

    /**
     * 处理enrichSchedules方法逻辑。
     */
    private void enrichSchedules(List<ScheduleVO> schedules) {
        fillTypeName(schedules);
        fillParticipantUsers(schedules);
    }

    /**
     * 填充类型名称。
     */
    private void fillTypeName(List<ScheduleVO> schedules) {
        if (schedules == null) {
            return;
        }
        schedules.forEach(schedule -> schedule.setTypeName(getTypeName(schedule.getType())));
    }

    /**
     * 填充参与人用户。
     */
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

    /**
     * 获取类型名称。
     */
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

    /**
     * 解析日程AI响应。
     */
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
            applyCustomerMatch(vo, originalContent);
            applyParticipantMatches(vo, originalContent);
            refineTitleWithCompanyContact(vo, originalContent);
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

    /**
     * 处理applyCustomerMatch方法逻辑。
     */
    private void applyCustomerMatch(ScheduleAiParseVO vo, String originalContent) {
        LinkedHashSet<String> candidateNames = new LinkedHashSet<>();
        extractCompanyContactMentions(originalContent).stream()
                .map(CompanyContactMention::companyName)
                .forEach(candidateNames::add);
        extractStandaloneCompanyNames(originalContent).forEach(candidateNames::add);

        String parsedCustomerName = normalizeCompanyCandidate(vo.getCustomerName());
        if (StrUtil.isNotBlank(parsedCustomerName)) {
            candidateNames.add(parsedCustomerName);
        }

        for (String candidateName : candidateNames) {
            AiCustomerMatcher.CustomerMatchResult matchResult = aiCustomerMatcher.match(candidateName);
            if (matchResult.isMatched()) {
                vo.setCustomerId(matchResult.getCustomer().getCustomerId());
                vo.setCustomerName(matchResult.getCustomer().getCompanyName());
                return;
            }
        }

        if (StrUtil.isBlank(vo.getCustomerName()) && !candidateNames.isEmpty()) {
            vo.setCustomerName(candidateNames.iterator().next());
        } else {
            vo.setCustomerName(parsedCustomerName);
        }
    }

    /**
     * 处理applyParticipantMatches方法逻辑。
     */
    private void applyParticipantMatches(ScheduleAiParseVO vo, String originalContent) {
        List<String> participantNames = extractExplicitParticipantNames(originalContent);
        if (participantNames.isEmpty()) {
            participantNames = splitParticipantNames(vo.getParticipantNames()).stream()
                    .filter(name -> !isExternalCompanyContactName(name, originalContent))
                    .toList();
        }

        vo.setParticipantNames(String.join(", ", participantNames));

        ParticipantMatchResult matchResult = resolveParticipantUsersByNames(participantNames);
        vo.setParticipantUserIds(matchResult.participantUsers().stream()
                .map(ScheduleParticipantUserVO::getUserId)
                .toList());
        vo.setParticipantUsers(matchResult.participantUsers());
        vo.setUnmatchedParticipantNames(String.join(", ", matchResult.unmatchedNames()));
    }

    /**
     * 处理refineTitle包含CompanyContact方法逻辑。
     */
    private void refineTitleWithCompanyContact(ScheduleAiParseVO vo, String originalContent) {
        if (StrUtil.isBlank(vo.getTitle())) {
            return;
        }

        List<CompanyContactMention> mentions = extractCompanyContactMentions(originalContent);
        if (mentions.size() != 1) {
            return;
        }

        CompanyContactMention mention = mentions.get(0);
        String contactName = mention.contactName();
        if (StrUtil.isBlank(contactName) || StrUtil.contains(vo.getTitle(), contactName)) {
            return;
        }

        String fullCompanyName = StrUtil.blankToDefault(vo.getCustomerName(), mention.companyName());
        String shortCompanyName = shortenCompanyName(fullCompanyName);
        if (StrUtil.isNotBlank(fullCompanyName) && StrUtil.contains(vo.getTitle(), fullCompanyName)) {
            vo.setTitle(vo.getTitle().replaceFirst(Pattern.quote(fullCompanyName), Matcher.quoteReplacement(fullCompanyName + contactName)));
            return;
        }
        if (StrUtil.isNotBlank(shortCompanyName) && StrUtil.contains(vo.getTitle(), shortCompanyName)) {
            vo.setTitle(vo.getTitle().replaceFirst(Pattern.quote(shortCompanyName), Matcher.quoteReplacement(shortCompanyName + contactName)));
        }
    }

    /**
     * 判断是否存在有意义AI解析结果。
     */
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

    /**
     * 解析参与人用户按名称。
     */
    private ParticipantMatchResult resolveParticipantUsersByNames(List<String> names) {
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

    /**
     * 查找Best用户Match。
     */
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

    /**
     * 转换为参与人用户。
     */
    private ScheduleParticipantUserVO toParticipantUser(ManagerUser user) {
        ScheduleParticipantUserVO vo = new ScheduleParticipantUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealname(getUserDisplayName(user));
        return vo;
    }

    /**
     * 获取用户Display名称。
     */
    private String getUserDisplayName(ManagerUser user) {
        if (user == null) {
            return "";
        }
        return StrUtil.blankToDefault(StrUtil.trim(user.getRealname()), user.getUsername());
    }

    /**
     * 处理splitParticipantNames方法逻辑。
     */
    private List<String> splitParticipantNames(String participantNames) {
        if (StrUtil.isBlank(participantNames)) {
            return List.of();
        }
        return Arrays.stream(participantNames.split("[,，]"))
                .map(this::normalizeParticipantName)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    /**
     * 处理extractExplicitParticipantNames方法逻辑。
     */
    private List<String> extractExplicitParticipantNames(String content) {
        if (StrUtil.isBlank(content)) {
            return List.of();
        }

        LinkedHashSet<String> names = new LinkedHashSet<>();
        Matcher matcher = EXPLICIT_PARTICIPANT_PATTERN.matcher(content);
        while (matcher.find()) {
            String segment = sanitizeParticipantSegment(matcher.group(1));
            if (StrUtil.isBlank(segment)) {
                continue;
            }

            String normalizedSegment = segment.replaceAll("\\s*(?:和|及|与)\\s*", "、");
            for (String token : normalizedSegment.split("[、,，/]")) {
                String name = normalizeParticipantName(token);
                if (StrUtil.isNotBlank(name)) {
                    names.add(name);
                }
            }
        }
        return new ArrayList<>(names);
    }

    /**
     * 处理sanitizeParticipantSegment方法逻辑。
     */
    private String sanitizeParticipantSegment(String segment) {
        String cleaned = StrUtil.trim(segment);
        if (StrUtil.isBlank(cleaned)) {
            return "";
        }

        String[] tailKeywords = {"地点", "地址", "备注", "说明", "客户", "公司", "主题", "内容", "时间"};
        int cutIndex = cleaned.length();
        for (String keyword : tailKeywords) {
            int index = cleaned.indexOf(keyword);
            if (index > 0) {
                cutIndex = Math.min(cutIndex, index);
            }
        }
        return cleaned.substring(0, cutIndex).trim();
    }

    /**
     * 判断是否External公司联系人名称。
     */
    private boolean isExternalCompanyContactName(String name, String content) {
        String normalizedName = normalizeParticipantName(name);
        if (StrUtil.isBlank(normalizedName) || StrUtil.isBlank(content)) {
            return false;
        }
        return extractCompanyContactMentions(content).stream()
                .map(CompanyContactMention::contactName)
                .anyMatch(contactName -> StrUtil.equals(normalizedName, normalizeParticipantName(contactName)));
    }

    /**
     * 处理extractCompanyContactMentions方法逻辑。
     */
    private List<CompanyContactMention> extractCompanyContactMentions(String content) {
        if (StrUtil.isBlank(content)) {
            return List.of();
        }

        LinkedHashMap<String, CompanyContactMention> mentions = new LinkedHashMap<>();
        Matcher matcher = COMPANY_CONTACT_PATTERN.matcher(content);
        while (matcher.find()) {
            String companyName = normalizeCompanyCandidate(matcher.group(1));
            String contactName = normalizeParticipantName(matcher.group(2));
            if (StrUtil.isBlank(companyName) || StrUtil.isBlank(contactName)) {
                continue;
            }
            mentions.putIfAbsent(companyName + "|" + contactName, new CompanyContactMention(companyName, contactName));
        }
        return new ArrayList<>(mentions.values());
    }

    /**
     * 处理extractStandaloneCompanyNames方法逻辑。
     */
    private List<String> extractStandaloneCompanyNames(String content) {
        if (StrUtil.isBlank(content)) {
            return List.of();
        }

        LinkedHashSet<String> companies = new LinkedHashSet<>();
        Matcher matcher = COMPANY_NAME_PATTERN.matcher(content);
        while (matcher.find()) {
            String companyName = normalizeCompanyCandidate(matcher.group(1));
            if (StrUtil.isNotBlank(companyName)) {
                companies.add(companyName);
            }
        }
        return new ArrayList<>(companies);
    }

    /**
     * 标准化参与人名称。
     */
    private String normalizeParticipantName(String rawName) {
        String result = StrUtil.trim(rawName);
        if (StrUtil.isBlank(result)) {
            return "";
        }

        String[] prefixes = {"我和", "我们和", "我们", "我司同事", "同事", "还有", "以及", "及", "与", "和"};
        boolean changed = true;
        while (changed && StrUtil.isNotBlank(result)) {
            changed = false;
            for (String prefix : prefixes) {
                if (result.startsWith(prefix) && result.length() > prefix.length()) {
                    result = StrUtil.trim(result.substring(prefix.length()));
                    changed = true;
                }
            }
        }

        String strippedTitle = result.replaceFirst("(总监|经理|主任|老师|先生|女士|总)$", "");
        if (strippedTitle.length() >= 2) {
            result = strippedTitle;
        }

        if (result.length() > 20 || containsNonParticipantKeyword(result)) {
            return "";
        }
        return result;
    }

    /**
     * 处理containsNonParticipantKeyword方法逻辑。
     */
    private boolean containsNonParticipantKeyword(String value) {
        return StrUtil.containsAny(value, "地点", "地址", "会议", "公司", "方案", "讨论", "沟通", "开会");
    }

    /**
     * 标准化公司Candidate。
     */
    private String normalizeCompanyCandidate(String rawCompanyName) {
        String value = StrUtil.trim(rawCompanyName);
        if (StrUtil.isBlank(value)) {
            return "";
        }

        String[] prefixes = {"和", "与", "跟", "约", "找", "去", "到"};
        boolean changed = true;
        while (changed && StrUtil.isNotBlank(value)) {
            changed = false;
            for (String prefix : prefixes) {
                if (value.startsWith(prefix) && value.length() > prefix.length() + 1) {
                    value = StrUtil.trim(value.substring(prefix.length()));
                    changed = true;
                }
            }
        }

        Matcher matcher = COMPANY_NAME_PATTERN.matcher(value);
        if (matcher.find()) {
            value = matcher.group(1);
        }
        return StrUtil.trim(value);
    }

    /**
     * 处理shortenCompanyName方法逻辑。
     */
    private String shortenCompanyName(String companyName) {
        String result = StrUtil.trim(companyName);
        if (StrUtil.isBlank(result)) {
            return "";
        }

        String[] suffixes = {"有限责任公司", "股份有限公司", "集团有限公司", "有限公司", "集团公司", "公司", "集团"};
        for (String suffix : suffixes) {
            if (result.endsWith(suffix) && result.length() > suffix.length()) {
                return StrUtil.removeSuffix(result, suffix);
            }
        }
        return result;
    }

    /**
     * 解析参与人用户ID。
     */
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

    /**
     * 处理joinParticipantUserIds方法逻辑。
     */
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

    /**
     * 处理extractJsonObject方法逻辑。
     */
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

    /**
     * 获取文本OR默认。
     */
    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        if (root.has(field) && !root.get(field).isNull()) {
            String value = root.get(field).asText();
            return StrUtil.isNotBlank(value) ? value : defaultValue;
        }
        return defaultValue;
    }

    /**
     * 标准化类型。
     */
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

    /**
     * 构建兜底日程结果。
     */
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

    private record CompanyContactMention(
            String companyName,
            String contactName
    ) {
    }
}
