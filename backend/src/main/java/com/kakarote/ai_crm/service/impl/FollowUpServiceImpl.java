package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.FollowUpAiParseBO;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.BO.FollowUpSuggestedTaskBO;
import com.kakarote.ai_crm.entity.BO.FollowUpUpdateBO;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.FollowUp;
import com.kakarote.ai_crm.entity.PO.FollowUpAttachment;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.FollowUpAttachmentVO;
import com.kakarote.ai_crm.entity.VO.FollowUpAiParseVO;
import com.kakarote.ai_crm.entity.VO.FollowUpLinkedTaskVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.FollowUpAttachmentMapper;
import com.kakarote.ai_crm.mapper.FollowUpMapper;
import com.kakarote.ai_crm.service.AiAudioTranscriptionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IFollowUpService;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.utils.AiMediaUtil;
import com.kakarote.ai_crm.utils.DocumentTextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Follow-up service implementation.
 */
@Slf4j
@Service
public class FollowUpServiceImpl extends ServiceImpl<FollowUpMapper, FollowUp> implements IFollowUpService {

    private static final DateTimeFormatter AI_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    private static final Pattern CHINESE_RELATIVE_TIME_PATTERN = Pattern.compile(
        "(明天|后天|明早|明晚|今天)\\s*(凌晨|早上|早晨|上午|中午|下午|傍晚|晚上|今晚)?\\s*(\\d{1,2})(?:\\s*[:点时]\\s*(\\d{1,2}))?(半)?\\s*(?:分)?"
    );
    private static final Pattern ENGLISH_RELATIVE_TIME_PATTERN = Pattern.compile(
        "(?i)\\b(today|tomorrow|day after tomorrow)\\b(?:\\s+at)?(?:\\s+(morning|afternoon|evening))?\\s*(\\d{1,2})(?::(\\d{1,2}))?\\s*(am|pm)?"
    );
    private static final int MAX_EXTRACTED_TEXT_LENGTH = 3000;
    private static final int MAX_ATTACHMENT_ANALYSIS_LENGTH = 4000;
    private static final int MAX_FOLLOW_UP_SUMMARY_LENGTH = 22;

    private static final String AI_PARSE_PROMPT_TEMPLATE = """
        You are a professional CRM assistant.
        Analyze the follow-up note and attachment context below, then return JSON only.

        Customer: %s
        Current time: %s

        User content:
        %s

        Attachment context:
        %s

        Time rules:
        1. `followTime` means the time this follow-up record is created or the actual completed follow-up time.
        2. If the content is describing a future plan or scheduled contact such as "tomorrow at 10am call the customer", keep `followTime` as Current time.
        3. Put planned future contact times into `nextFollowTime`.
        4. Do not put a future planned time into `followTime` when the content has not happened yet.
        5. Resolve relative time phrases like "明天上午10点" or "tomorrow at 10am" against Current time and output concrete timestamps.
        6. Example: if Current time is "2026-04-13 15:00:00" and the content says "明天上午10点电话沟通", then `followTime` must be "2026-04-13 15:00:00" and `nextFollowTime` must be "2026-04-14 10:00:00".

        Keep summary, sceneType, keyPoints, and todos in the same language as the user's content.
        The `summary` must be very concise and title-like, suitable for a card heading.
        Prefer 8-18 Chinese characters or within 30 characters in other languages.
        Do not write a paragraph, explanation, or multiple sentences.
        Return strict JSON only with this exact shape:
        {
          "summary": "very short heading-style summary",
          "sceneType": "brief scene label in the same language, or empty string",
          "type": "one of: call, meeting, email, visit, other",
          "followTime": "yyyy-MM-dd HH:mm:ss",
          "nextFollowTime": "yyyy-MM-dd HH:mm:ss or empty string",
          "keyPoints": ["point 1", "point 2"],
          "todos": ["todo 1", "todo 2"]
        }
        """;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FollowUpAttachmentMapper followUpAttachmentMapper;

    @Autowired
    private ITaskService taskService;

    @Lazy
    @Autowired
    private AiAudioTranscriptionService aiAudioTranscriptionService;

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFollowUp(FollowUpAddBO followUpAddBO) {
        Customer customer = customerMapper.selectById(followUpAddBO.getCustomerId());
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer does not exist or is not accessible");
        }

        FollowUp followUp = BeanUtil.copyProperties(followUpAddBO, FollowUp.class);
        if (followUp.getFollowTime() == null) {
            followUp.setFollowTime(new Date());
        }
        if (followUp.getAiGenerated() == null) {
            followUp.setAiGenerated(0);
        }
        followUp.setSummary(normalizeStoredSummary(followUp.getSummary()));
        save(followUp);
        saveAttachments(followUp.getFollowUpId(), followUpAddBO.getAttachments());
        createSuggestedTasks(followUp, followUpAddBO.getSuggestedTasks());
        syncCustomerFollowUpSummary(followUp.getCustomerId());

        return followUp.getFollowUpId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowUp(FollowUpUpdateBO followUpUpdateBO) {
        FollowUp followUp = getById(followUpUpdateBO.getFollowUpId());
        if (ObjectUtil.isNull(followUp)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Follow-up record does not exist");
        }

        BeanUtil.copyProperties(
            followUpUpdateBO,
            followUp,
            "followUpId",
            "customerId",
            "createUserId",
            "createTime",
            "tenantId"
        );
        updateById(followUp);
        syncCustomerFollowUpSummary(followUp.getCustomerId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFollowUp(Long followUpId) {
        FollowUp followUp = getById(followUpId);
        if (ObjectUtil.isNull(followUp)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Follow-up record does not exist");
        }
        Long customerId = followUp.getCustomerId();
        followUpAttachmentMapper.delete(
            Wrappers.<FollowUpAttachment>lambdaQuery().eq(FollowUpAttachment::getFollowUpId, followUpId)
        );
        removeById(followUpId);
        syncCustomerFollowUpSummary(customerId);
    }

    @Override
    public List<FollowUpVO> queryByCustomer(Long customerId) {
        List<FollowUp> followUps = lambdaQuery()
            .eq(FollowUp::getCustomerId, customerId)
            .orderByDesc(FollowUp::getFollowTime)
            .list();
        List<FollowUpVO> result = BeanUtil.copyToList(followUps, FollowUpVO.class);
        enrichFollowUps(result);
        return result;
    }

    @Override
    public BasePage<FollowUpVO> queryPageList(FollowUpQueryBO queryBO) {
        BasePage<FollowUpVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        enrichFollowUps(page.getList());
        return page;
    }

    @Override
    public FollowUpAiParseVO aiParseFollowUp(FollowUpAiParseBO parseBO) {
        String customerName = StrUtil.blankToDefault(parseBO.getCustomerName(), "Unknown customer");
        String now = LocalDateTime.now().format(AI_TIME_FORMATTER);
        AiModelCapabilities capabilities = chatClientProvider.getCurrentCapabilities();
        String attachmentContext = buildAttachmentContext(parseBO.getAttachments(), capabilities);
        String prompt = String.format(
            AI_PARSE_PROMPT_TEMPLATE,
            customerName,
            now,
            parseBO.getContent(),
            StrUtil.blankToDefault(attachmentContext, "No usable attachment content")
        );

        try {
            List<Media> mediaList = buildMediaList(parseBO.getAttachments(), capabilities);
            var requestSpec = chatClientProvider.getChatClient().prompt();

            if (CollUtil.isNotEmpty(mediaList)) {
                requestSpec.user(user -> user.text(prompt).media(mediaList.toArray(new Media[0])));
            } else {
                requestSpec.user(prompt);
            }

            String response = requestSpec.call().content();
            log.info("AI follow-up parse raw response: {}", response);
            return parseAiResponse(response, parseBO.getContent(), now);
        } catch (Exception e) {
            log.error("AI follow-up parse failed, using fallback result", e);
            return buildFallbackResult(parseBO.getContent(), now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FollowUpAttachmentVO analyzeAttachment(Long attachmentId) {
        FollowUpAttachment attachment = followUpAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment does not exist");
        }

        attachment.setAnalysisStatus("processing");
        followUpAttachmentMapper.updateById(attachment);

        try {
            String analysis = buildAttachmentAnalysis(attachment);
            attachment.setAnalysisStatus("completed");
            attachment.setAnalysisContent(analysis);
            attachment.setAnalysisTime(new Date());
            followUpAttachmentMapper.updateById(attachment);
            return toAttachmentVO(attachment);
        } catch (BusinessException ex) {
            attachment.setAnalysisStatus("failed");
            attachment.setAnalysisContent(ex.getMessage());
            attachment.setAnalysisTime(new Date());
            followUpAttachmentMapper.updateById(attachment);
            return toAttachmentVO(attachment);
        } catch (Exception ex) {
            log.error("Attachment AI analysis failed, attachmentId={}", attachmentId, ex);
            attachment.setAnalysisStatus("failed");
            attachment.setAnalysisContent("AI analysis failed, please try again later.");
            attachment.setAnalysisTime(new Date());
            followUpAttachmentMapper.updateById(attachment);
            return toAttachmentVO(attachment);
        }
    }

    @Override
    public FollowUpAttachmentVO getAttachment(Long attachmentId) {
        FollowUpAttachment attachment = followUpAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment does not exist");
        }
        return toAttachmentVO(attachment);
    }

    private FollowUpAiParseVO parseAiResponse(String response, String originalContent, String now) {
        try {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            FollowUpAiParseVO vo = new FollowUpAiParseVO();
            vo.setSummary(normalizeGeneratedSummary(getTextOrDefault(root, "summary", ""), originalContent));
            vo.setSceneType(getTextOrDefault(root, "sceneType", ""));
            vo.setType(getTextOrDefault(root, "type", "other"));
            vo.setFollowTime(normalizeRequiredDateTime(getTextOrDefault(root, "followTime", now), now));
            vo.setNextFollowTime(normalizeOptionalDateTime(getTextOrDefault(root, "nextFollowTime", ""), now));

            List<String> keyPoints = new ArrayList<>();
            if (root.has("keyPoints") && root.get("keyPoints").isArray()) {
                root.get("keyPoints").forEach(node -> keyPoints.add(node.asText()));
            }
            vo.setKeyPoints(keyPoints);

            List<String> todos = new ArrayList<>();
            if (root.has("todos") && root.get("todos").isArray()) {
                root.get("todos").forEach(node -> todos.add(node.asText()));
            }
            vo.setTodos(todos);

            return normalizeParsedTimes(vo, originalContent, now);
        } catch (Exception e) {
            log.warn("AI response JSON parse failed: {}", e.getMessage());
            return buildFallbackResult(originalContent, now);
        }
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        if (root.has(field) && !root.get(field).isNull()) {
            String value = root.get(field).asText();
            return StrUtil.isNotBlank(value) ? value.trim() : defaultValue;
        }
        return defaultValue;
    }

    private FollowUpAiParseVO buildFallbackResult(String content, String now) {
        FollowUpAiParseVO vo = new FollowUpAiParseVO();
        vo.setSummary(normalizeGeneratedSummary("", content));
        vo.setSceneType("");
        vo.setType("other");
        vo.setFollowTime(now);
        vo.setNextFollowTime("");
        vo.setKeyPoints(List.of());
        vo.setTodos(List.of());
        return normalizeParsedTimes(vo, content, now);
    }

    private FollowUpAiParseVO normalizeParsedTimes(FollowUpAiParseVO vo, String originalContent, String now) {
        LocalDateTime nowTime = parseDateTime(now, LocalTime.now());
        if (nowTime == null) {
            return vo;
        }

        LocalDateTime followTime = parseDateTime(vo.getFollowTime(), nowTime.toLocalTime());
        LocalDateTime nextFollowTime = parseDateTime(vo.getNextFollowTime(), nowTime.toLocalTime());
        LocalDateTime inferredNextFollowTime = inferNextFollowTimeFromContent(originalContent, nowTime);

        if (followTime != null && followTime.isAfter(nowTime.plusMinutes(5))) {
            LocalDateTime normalizedNextFollowTime = chooseFutureNextFollowTime(
                inferredNextFollowTime,
                nextFollowTime,
                followTime,
                nowTime
            );
            if (normalizedNextFollowTime != null) {
                vo.setNextFollowTime(AI_TIME_FORMATTER.format(normalizedNextFollowTime));
                nextFollowTime = normalizedNextFollowTime;
            }
            vo.setFollowTime(now);
            followTime = nowTime;
        }

        if (inferredNextFollowTime != null
            && inferredNextFollowTime.isAfter(nowTime.plusMinutes(5))
            && (nextFollowTime == null || !nextFollowTime.isAfter(nowTime.plusMinutes(5)))) {
            vo.setNextFollowTime(AI_TIME_FORMATTER.format(inferredNextFollowTime));
        }

        return vo;
    }

    private String normalizeGeneratedSummary(String summary, String fallbackContent) {
        String candidate = StrUtil.isNotBlank(summary) ? summary : fallbackContent;
        return compactSummary(candidate);
    }

    private String normalizeStoredSummary(String summary) {
        if (StrUtil.isBlank(summary)) {
            return null;
        }
        return compactSummary(summary);
    }

    private String compactSummary(String text) {
        String normalized = normalizeSingleLineText(text);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }

        String sentence = firstSegment(normalized, "[。！？!?；;\\r\\n]+");
        String clause = firstSegment(sentence, "[，,:：]+");
        String candidate = StrUtil.isNotBlank(clause) ? clause : sentence;
        candidate = candidate.replaceFirst("^[\\s\\-•·*#\\d.、）)]+", "");
        candidate = candidate.replaceFirst("[\\s,，.。!！?？;；:：]+$", "");
        candidate = normalizeSingleLineText(candidate);
        if (StrUtil.isBlank(candidate)) {
            candidate = normalized;
        }

        if (candidate.length() > MAX_FOLLOW_UP_SUMMARY_LENGTH) {
            candidate = candidate.substring(0, MAX_FOLLOW_UP_SUMMARY_LENGTH).trim();
            candidate = candidate.replaceFirst("[\\s,，.。!！?？;；:：]+$", "");
        }

        return candidate;
    }

    private String firstSegment(String text, String regex) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String[] parts = text.split(regex);
        for (String part : parts) {
            if (StrUtil.isNotBlank(part)) {
                return part.trim();
            }
        }
        return text.trim();
    }

    private String normalizeSingleLineText(String text) {
        return StrUtil.blankToDefault(text, "").replaceAll("\\s+", " ").trim();
    }

    private LocalDateTime chooseFutureNextFollowTime(
        LocalDateTime inferredNextFollowTime,
        LocalDateTime nextFollowTime,
        LocalDateTime followTime,
        LocalDateTime nowTime
    ) {
        if (inferredNextFollowTime != null && inferredNextFollowTime.isAfter(nowTime.plusMinutes(5))) {
            return inferredNextFollowTime;
        }
        if (nextFollowTime != null && nextFollowTime.isAfter(nowTime.plusMinutes(5))) {
            return nextFollowTime;
        }
        if (followTime != null && followTime.isAfter(nowTime.plusMinutes(5))) {
            return followTime;
        }
        return null;
    }

    private LocalDateTime inferNextFollowTimeFromContent(String content, LocalDateTime nowTime) {
        if (StrUtil.isBlank(content) || nowTime == null) {
            return null;
        }

        LocalDateTime chineseCandidate = parseChineseRelativeTime(content, nowTime);
        if (chineseCandidate != null) {
            return chineseCandidate;
        }

        return parseEnglishRelativeTime(content, nowTime);
    }

    private LocalDateTime parseChineseRelativeTime(String content, LocalDateTime nowTime) {
        Matcher matcher = CHINESE_RELATIVE_TIME_PATTERN.matcher(content);
        while (matcher.find()) {
            String dayToken = matcher.group(1);
            String periodToken = StrUtil.blankToDefault(matcher.group(2), deriveChinesePeriodToken(dayToken));
            Integer hour = parseHourToken(matcher.group(3));
            Integer minute = parseMinuteToken(matcher.group(4), matcher.group(5));
            Integer normalizedHour = normalizeHour(hour, periodToken);
            if (normalizedHour == null || minute == null) {
                continue;
            }

            LocalDateTime candidate = LocalDateTime.of(
                nowTime.toLocalDate().plusDays(resolveChineseDayOffset(dayToken)),
                LocalTime.of(normalizedHour, minute)
            );
            if (candidate.isAfter(nowTime)) {
                return candidate;
            }
        }
        return null;
    }

    private LocalDateTime parseEnglishRelativeTime(String content, LocalDateTime nowTime) {
        Matcher matcher = ENGLISH_RELATIVE_TIME_PATTERN.matcher(content);
        while (matcher.find()) {
            String dayToken = matcher.group(1);
            String periodToken = matcher.group(5) != null ? matcher.group(5) : matcher.group(2);
            Integer hour = parseHourToken(matcher.group(3));
            Integer minute = parseMinuteToken(matcher.group(4), null);
            Integer normalizedHour = normalizeHour(hour, periodToken);
            if (normalizedHour == null || minute == null) {
                continue;
            }

            LocalDateTime candidate = LocalDateTime.of(
                nowTime.toLocalDate().plusDays(resolveEnglishDayOffset(dayToken)),
                LocalTime.of(normalizedHour, minute)
            );
            if (candidate.isAfter(nowTime)) {
                return candidate;
            }
        }
        return null;
    }

    private long resolveChineseDayOffset(String dayToken) {
        if (StrUtil.startWith(dayToken, "后天")) {
            return 2;
        }
        if (StrUtil.equalsAny(dayToken, "明天", "明早", "明晚")) {
            return 1;
        }
        return 0;
    }

    private long resolveEnglishDayOffset(String dayToken) {
        String normalized = StrUtil.nullToEmpty(dayToken).toLowerCase(Locale.ROOT);
        if (normalized.contains("day after tomorrow")) {
            return 2;
        }
        if (normalized.contains("tomorrow")) {
            return 1;
        }
        return 0;
    }

    private String deriveChinesePeriodToken(String dayToken) {
        if (StrUtil.equals(dayToken, "明早")) {
            return "上午";
        }
        if (StrUtil.equals(dayToken, "明晚")) {
            return "晚上";
        }
        return "";
    }

    private Integer parseHourToken(String value) {
        try {
            int hour = Integer.parseInt(StrUtil.trim(value));
            return hour >= 0 && hour <= 23 ? hour : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseMinuteToken(String value, String halfToken) {
        if (StrUtil.isNotBlank(value)) {
            try {
                int minute = Integer.parseInt(StrUtil.trim(value));
                return minute >= 0 && minute <= 59 ? minute : null;
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return StrUtil.isNotBlank(halfToken) ? 30 : 0;
    }

    private Integer normalizeHour(Integer hour, String periodToken) {
        if (hour == null) {
            return null;
        }

        String normalizedPeriod = StrUtil.nullToEmpty(periodToken).toLowerCase(Locale.ROOT);
        int normalizedHour = hour;

        if (normalizedPeriod.contains("下午") || normalizedPeriod.contains("傍晚") || normalizedPeriod.contains("晚上")
            || normalizedPeriod.contains("今晚") || normalizedPeriod.contains("afternoon")
            || normalizedPeriod.contains("evening") || normalizedPeriod.equals("pm")) {
            if (normalizedHour < 12) {
                normalizedHour += 12;
            }
        } else if (normalizedPeriod.contains("中午")) {
            if (normalizedHour < 11) {
                normalizedHour += 12;
            }
        } else if (normalizedPeriod.contains("凌晨")
            || normalizedPeriod.contains("早上")
            || normalizedPeriod.contains("早晨")
            || normalizedPeriod.contains("上午")
            || normalizedPeriod.contains("morning")
            || normalizedPeriod.equals("am")) {
            if (normalizedHour == 12) {
                normalizedHour = 0;
            }
        }

        return normalizedHour >= 0 && normalizedHour <= 23 ? normalizedHour : null;
    }

    private String normalizeRequiredDateTime(String value, String fallback) {
        LocalDateTime fallbackTime = parseDateTime(fallback, LocalTime.now());
        LocalDateTime parsed = parseDateTime(
            StrUtil.blankToDefault(value, fallback),
            fallbackTime != null ? fallbackTime.toLocalTime() : LocalTime.now()
        );
        if (parsed == null) {
            return fallback;
        }
        return AI_TIME_FORMATTER.format(parsed);
    }

    private String normalizeOptionalDateTime(String value, String fallback) {
        if (StrUtil.isBlank(value)) {
            return "";
        }

        LocalDateTime fallbackTime = parseDateTime(fallback, LocalTime.now());
        LocalDateTime parsed = parseDateTime(value, fallbackTime != null ? fallbackTime.toLocalTime() : LocalTime.now());
        if (parsed == null) {
            return "";
        }
        return AI_TIME_FORMATTER.format(parsed);
    }

    private LocalDateTime parseDateTime(String value, LocalTime defaultTime) {
        String trimmed = StrUtil.trim(value);
        if (StrUtil.isBlank(trimmed)) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }

        try {
            return LocalDate.parse(trimmed, DATE_ONLY_FORMATTER).atTime(defaultTime);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private void saveAttachments(Long followUpId, List<ChatSendBO.AttachmentDTO> attachments) {
        if (followUpId == null || CollUtil.isEmpty(attachments)) {
            return;
        }

        int sort = 0;
        for (ChatSendBO.AttachmentDTO dto : attachments) {
            if (dto == null || StrUtil.isBlank(dto.getFilePath())) {
                continue;
            }

            FollowUpAttachment attachment = new FollowUpAttachment();
            attachment.setFollowUpId(followUpId);
            attachment.setFileName(StrUtil.blankToDefault(dto.getFileName(), "attachment"));
            attachment.setFilePath(dto.getFilePath());
            attachment.setFileSize(dto.getFileSize());
            attachment.setMimeType(dto.getMimeType());
            attachment.setSort(sort++);
            attachment.setAnalysisStatus("idle");
            followUpAttachmentMapper.insert(attachment);
        }
    }

    private void createSuggestedTasks(FollowUp followUp, List<FollowUpSuggestedTaskBO> suggestedTasks) {
        if (followUp == null || followUp.getCustomerId() == null || CollUtil.isEmpty(suggestedTasks)) {
            return;
        }

        for (FollowUpSuggestedTaskBO suggestedTask : suggestedTasks) {
            if (suggestedTask == null || StrUtil.isBlank(suggestedTask.getTitle())) {
                continue;
            }

            TaskAddBO taskAddBO = new TaskAddBO();
            taskAddBO.setTitle(suggestedTask.getTitle().trim());
            taskAddBO.setDescription(StrUtil.blankToDefault(StrUtil.trim(suggestedTask.getDescription()), followUp.getSummary()));
            taskAddBO.setDueDate(suggestedTask.getDueDate() != null ? suggestedTask.getDueDate() : followUp.getNextFollowTime());
            taskAddBO.setPriority("medium");
            taskAddBO.setTaskType(StrUtil.blankToDefault(suggestedTask.getTaskType(), "跟进"));
            taskAddBO.setCustomerId(followUp.getCustomerId());
            taskAddBO.setGeneratedByAi(1);
            taskAddBO.setAiContext("Generated from follow-up " + followUp.getFollowUpId());
            taskAddBO.setSourceFollowUpId(followUp.getFollowUpId());
            taskService.addTask(taskAddBO);
        }
    }

    private void enrichFollowUps(List<FollowUpVO> followUps) {
        if (CollUtil.isEmpty(followUps)) {
            return;
        }

        List<Long> followUpIds = followUps.stream()
            .map(FollowUpVO::getFollowUpId)
            .filter(ObjectUtil::isNotNull)
            .toList();
        if (followUpIds.isEmpty()) {
            return;
        }

        Map<Long, List<FollowUpAttachmentVO>> attachmentMap = followUpAttachmentMapper.selectList(
                Wrappers.<FollowUpAttachment>lambdaQuery()
                    .in(FollowUpAttachment::getFollowUpId, followUpIds)
                    .orderByAsc(FollowUpAttachment::getSort)
                    .orderByAsc(FollowUpAttachment::getAttachmentId)
            ).stream()
            .map(this::toAttachmentVO)
            .collect(Collectors.groupingBy(
                FollowUpAttachmentVO::getFollowUpId,
                LinkedHashMap::new,
                Collectors.toList()
            ));

        Map<Long, List<FollowUpLinkedTaskVO>> taskMap = taskService.lambdaQuery()
            .in(Task::getSourceFollowUpId, followUpIds)
            .orderByAsc(Task::getDueDate)
            .orderByAsc(Task::getCreateTime)
            .list()
            .stream()
            .filter(task -> task.getSourceFollowUpId() != null)
            .collect(Collectors.groupingBy(
                Task::getSourceFollowUpId,
                LinkedHashMap::new,
                Collectors.mapping(this::toLinkedTaskVO, Collectors.toList())
            ));

        for (FollowUpVO followUp : followUps) {
            followUp.setSummary(normalizeStoredSummary(followUp.getSummary()));
            followUp.setAttachments(attachmentMap.getOrDefault(followUp.getFollowUpId(), List.of()));
            followUp.setTasks(taskMap.getOrDefault(followUp.getFollowUpId(), List.of()));
        }
    }

    private FollowUpAttachmentVO toAttachmentVO(FollowUpAttachment attachment) {
        return BeanUtil.copyProperties(attachment, FollowUpAttachmentVO.class);
    }

    private FollowUpLinkedTaskVO toLinkedTaskVO(Task task) {
        FollowUpLinkedTaskVO vo = new FollowUpLinkedTaskVO();
        vo.setTaskId(task.getTaskId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setDueDate(task.getDueDate());
        vo.setStatus(task.getStatus());
        vo.setTaskType(task.getTaskType());
        vo.setGeneratedByAi(task.getGeneratedByAi());
        return vo;
    }

    private String buildAttachmentAnalysis(FollowUpAttachment attachment) {
        String mimeType = StrUtil.blankToDefault(attachment.getMimeType(), "");
        String fileName = StrUtil.blankToDefault(attachment.getFileName(), "");
        if (mimeType.startsWith("image/")) {
            return analyzeImageAttachment(attachment);
        }
        if (isAudioFile(mimeType, fileName)) {
            return analyzeAudioAttachment(attachment);
        }

        String content;
        if (isTextFile(mimeType, fileName)) {
            content = extractFileText(attachment.getFilePath());
        } else if (isDocumentFile(mimeType, fileName)) {
            content = extractDocumentText(attachment.getFilePath());
        } else {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Current attachment type is not supported for AI analysis");
        }

        if (StrUtil.isBlank(content)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "No readable content could be extracted from this attachment");
        }
        return analyzeTextLikeAttachment(fileName, content);
    }

    private String analyzeImageAttachment(FollowUpAttachment attachment) {
        AiModelCapabilities capabilities = chatClientProvider.getCurrentCapabilities();
        if (capabilities == null || !capabilities.isSupportsVision()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "The current model does not support image analysis");
        }

        String prompt = """
            You are a CRM assistant.
            Analyze this follow-up attachment image and reply in concise Simplified Chinese.
            Keep the answer to 2-3 sentences. Mention the most important visible facts first, then at most one practical implication.
            Do not use markdown or bullet points.
            """;

        try {
            Media media = AiMediaUtil.buildMedia(
                fileStorageService,
                attachment.getFilePath(),
                MimeType.valueOf(StrUtil.blankToDefault(attachment.getMimeType(), "image/jpeg"))
            );
            return StrUtil.blankToDefault(
                chatClientProvider.getChatClient()
                    .prompt()
                    .user(user -> user.text(prompt).media(media))
                    .call()
                    .content(),
                "图片内容较模糊，暂时无法形成有效分析。"
            ).trim();
        } catch (Exception ex) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Image analysis failed, please try again later");
        }
    }

    private String analyzeAudioAttachment(FollowUpAttachment attachment) {
        try (InputStream inputStream = fileStorageService.getFileStream(attachment.getFilePath())) {
            if (inputStream == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment audio could not be loaded");
            }
            byte[] bytes = inputStream.readAllBytes();
            String transcript = aiAudioTranscriptionService.transcribe(bytes, attachment.getFileName(), attachment.getMimeType());
            if (StrUtil.isBlank(transcript)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "No valid transcript could be produced from this audio");
            }
            return analyzeTextLikeAttachment(
                attachment.getFileName(),
                "音频转写内容：\n" + limitText(transcript, MAX_ATTACHMENT_ANALYSIS_LENGTH)
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Audio analysis failed, please try again later");
        }
    }

    private String analyzeTextLikeAttachment(String fileName, String content) {
        String prompt = """
            You are a CRM assistant.
            Please analyze the attachment content below and reply in concise Simplified Chinese.
            Requirements:
            1. Keep the answer within 2-3 sentences.
            2. Summarize the most important concrete facts from the attachment.
            3. If helpful, add one practical follow-up implication in the last sentence.
            4. Do not use markdown, bullet points, or quotation marks.

            Attachment: %s
            Content:
            %s
            """.formatted(fileName, limitText(content, MAX_ATTACHMENT_ANALYSIS_LENGTH));

        try {
            return StrUtil.blankToDefault(
                chatClientProvider.getChatClient().prompt().user(prompt).call().content(),
                "附件内容已读取，但暂时无法生成有效分析。"
            ).trim();
        } catch (Exception ex) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "Attachment analysis failed, please try again later");
        }
    }

    private String buildAttachmentContext(List<ChatSendBO.AttachmentDTO> attachments, AiModelCapabilities capabilities) {
        if (CollUtil.isEmpty(attachments)) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("[Uploaded attachments]\n");

        for (ChatSendBO.AttachmentDTO att : attachments) {
            String mimeType = att.getMimeType();
            String fileName = att.getFileName();

            if (mimeType != null && mimeType.startsWith("image/")) {
                if (capabilities != null && capabilities.isSupportsVision()) {
                    context.append(String.format("- Image: %s (available as visual input)\n", fileName));
                } else {
                    context.append(String.format("- Image: %s (visual analysis is not supported by the current model)\n", fileName));
                }
            } else if (isTextFile(mimeType, fileName)) {
                String textContent = extractFileText(att.getFilePath());
                if (StrUtil.isNotBlank(textContent)) {
                    context.append(String.format("- Text file: %s\n```\n%s\n```\n", fileName, textContent));
                } else {
                    context.append(String.format("- Text file: %s (content could not be read)\n", fileName));
                }
            } else if (isDocumentFile(mimeType, fileName)) {
                String textContent = extractDocumentText(att.getFilePath());
                if (StrUtil.isNotBlank(textContent)) {
                    context.append(String.format("- Document: %s\n```\n%s\n```\n", fileName, textContent));
                } else {
                    context.append(String.format("- Document: %s (text could not be extracted)\n", fileName));
                }
            } else if (isAudioFile(mimeType, fileName)) {
                context.append(String.format("- Audio file: %s\n", fileName));
            } else {
                context.append(String.format("- File: %s (type: %s)\n", fileName, mimeType));
            }
        }

        return context.toString();
    }

    private List<Media> buildMediaList(List<ChatSendBO.AttachmentDTO> attachments, AiModelCapabilities capabilities) {
        if (CollUtil.isEmpty(attachments) || capabilities == null || !capabilities.isSupportsVision()) {
            return Collections.emptyList();
        }

        List<Media> mediaList = new ArrayList<>();
        for (ChatSendBO.AttachmentDTO att : attachments) {
            if (att.getMimeType() != null && att.getMimeType().startsWith("image/")) {
                try {
                    MimeType mimeType = MimeType.valueOf(att.getMimeType());
                    Media media = AiMediaUtil.buildMedia(fileStorageService, att.getFilePath(), mimeType);
                    mediaList.add(media);
                } catch (Exception e) {
                    log.warn("Failed to build image media for follow-up parse: {}", att.getFileName(), e);
                }
            }
        }
        return mediaList;
    }

    private boolean isTextFile(String mimeType, String fileName) {
        if (mimeType != null && (mimeType.startsWith("text/") || "application/json".equals(mimeType))) {
            return true;
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv")
                || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".yaml")
                || lower.endsWith(".yml") || lower.endsWith(".log");
        }
        return false;
    }

    private boolean isDocumentFile(String mimeType, String fileName) {
        if (mimeType != null) {
            if (mimeType.equals("application/pdf")
                || mimeType.equals("application/msword")
                || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || mimeType.equals("application/vnd.ms-excel")
                || mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || mimeType.equals("application/vnd.ms-powerpoint")
                || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
                return true;
            }
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx")
                || lower.endsWith(".xls") || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt") || lower.endsWith(".pptx");
        }
        return false;
    }

    private boolean isAudioFile(String mimeType, String fileName) {
        String lowerMimeType = StrUtil.blankToDefault(mimeType, "").toLowerCase(Locale.ROOT);
        if (lowerMimeType.startsWith("audio/")) {
            return true;
        }
        String lowerFileName = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        return lowerFileName.endsWith(".mp3")
            || lowerFileName.endsWith(".wav")
            || lowerFileName.endsWith(".m4a")
            || lowerFileName.endsWith(".aac")
            || lowerFileName.endsWith(".webm")
            || lowerFileName.endsWith(".ogg");
    }

    private String extractFileText(String filePath) {
        try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
            if (inputStream == null) {
                return null;
            }

            byte[] bytes = inputStream.readNBytes(MAX_EXTRACTED_TEXT_LENGTH * 3);
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() > MAX_EXTRACTED_TEXT_LENGTH) {
                text = text.substring(0, MAX_EXTRACTED_TEXT_LENGTH) + "\n...(truncated)";
            }
            return text;
        } catch (Exception e) {
            log.warn("Failed to read text attachment: {}", filePath, e);
            return null;
        }
    }

    private String extractDocumentText(String filePath) {
        try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
            if (inputStream == null) {
                return null;
            }

            String text = DocumentTextExtractor.parseToString(inputStream, null, filePath);
            if (StrUtil.isBlank(text)) {
                return null;
            }
            if (text.length() > MAX_EXTRACTED_TEXT_LENGTH) {
                text = text.substring(0, MAX_EXTRACTED_TEXT_LENGTH) + "\n...(truncated)";
            }
            return text.trim();
        } catch (Exception e) {
            log.warn("Failed to extract document text: {}", filePath, e);
            return null;
        }
    }

    private String limitText(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        String normalized = content.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "\n...(truncated)";
    }

    private void syncCustomerFollowUpSummary(Long customerId) {
        if (customerId == null) {
            return;
        }

        FollowUp latestFollowUp = lambdaQuery()
            .eq(FollowUp::getCustomerId, customerId)
            .orderByDesc(FollowUp::getFollowTime)
            .orderByDesc(FollowUp::getCreateTime)
            .orderByDesc(FollowUp::getFollowUpId)
            .last("LIMIT 1")
            .one();

        LambdaUpdateWrapper<Customer> updateWrapper = new LambdaUpdateWrapper<Customer>()
            .eq(Customer::getCustomerId, customerId)
            .set(Customer::getLastContactTime, latestFollowUp != null ? latestFollowUp.getFollowTime() : null)
            .set(Customer::getNextFollowTime, latestFollowUp != null ? latestFollowUp.getNextFollowTime() : null);
        customerMapper.update(null, updateWrapper);
        taskService.refreshValuePriorityByCustomerId(customerId);
    }
}
