package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.kakarote.ai_crm.entity.BO.FollowUpUpdateBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.FollowUp;
import com.kakarote.ai_crm.entity.VO.FollowUpAiParseVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.FollowUpMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IFollowUpService;
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
import java.util.List;

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
    private static final int MAX_EXTRACTED_TEXT_LENGTH = 3000;

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

        Keep summary, keyPoints, and todos in the same language as the user's content.
        Return strict JSON only with this exact shape:
        {
          "summary": "short summary in 1-2 sentences",
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
        save(followUp);
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
        removeById(followUpId);
        syncCustomerFollowUpSummary(customerId);
    }

    @Override
    public List<FollowUpVO> queryByCustomer(Long customerId) {
        List<FollowUp> followUps = lambdaQuery()
            .eq(FollowUp::getCustomerId, customerId)
            .orderByDesc(FollowUp::getFollowTime)
            .list();
        return BeanUtil.copyToList(followUps, FollowUpVO.class);
    }

    @Override
    public BasePage<FollowUpVO> queryPageList(FollowUpQueryBO queryBO) {
        BasePage<FollowUpVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
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

    private FollowUpAiParseVO parseAiResponse(String response, String originalContent, String now) {
        try {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            FollowUpAiParseVO vo = new FollowUpAiParseVO();
            vo.setSummary(getTextOrDefault(
                root,
                "summary",
                originalContent.length() > 100 ? originalContent.substring(0, 100) + "..." : originalContent
            ));
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

            return normalizeParsedTimes(vo, now);
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
        vo.setSummary(content.length() > 100 ? content.substring(0, 100) + "..." : content);
        vo.setType("other");
        vo.setFollowTime(now);
        vo.setNextFollowTime("");
        vo.setKeyPoints(List.of());
        vo.setTodos(List.of());
        return vo;
    }

    private FollowUpAiParseVO normalizeParsedTimes(FollowUpAiParseVO vo, String now) {
        LocalDateTime nowTime = parseDateTime(now, LocalTime.now());
        if (nowTime == null) {
            return vo;
        }

        LocalDateTime followTime = parseDateTime(vo.getFollowTime(), nowTime.toLocalTime());
        LocalDateTime nextFollowTime = parseDateTime(vo.getNextFollowTime(), nowTime.toLocalTime());

        if (followTime != null
            && followTime.isAfter(nowTime.plusMinutes(5))
            && nextFollowTime == null) {
            vo.setNextFollowTime(AI_TIME_FORMATTER.format(followTime));
            vo.setFollowTime(now);
        }

        return vo;
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
    }
}
