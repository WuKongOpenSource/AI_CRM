package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.FollowUpAiParseBO;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.BO.FollowUpUpdateBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.FollowUp;
import com.kakarote.ai_crm.entity.PO.FollowUpAttachment;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.FollowUpAiParseVO;
import com.kakarote.ai_crm.entity.VO.FollowUpAttachmentVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.FollowUpAttachmentMapper;
import com.kakarote.ai_crm.mapper.FollowUpMapper;
import com.kakarote.ai_crm.mapper.RelationMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IFollowUpService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 跟进记录服务实现
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

    private static final String AI_PARSE_PROMPT_TEMPLATE = """
        你是一个专业的 CRM 助手。请分析以下跟进记录，提取关键信息并以 JSON 格式返回。
        客户名称: %s
        当前时间: %s

        跟进内容:
        %s

        请严格按照以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "summary": "简明扼要的摘要，1-2句话",
          "type": "跟进类型，只能是以下之一: call, meeting, email, visit, other",
          "followTime": "跟进发生的时间，格式 yyyy-MM-dd HH:mm:ss（如未提及则用当前时间）",
          "nextFollowTime": "建议的下次跟进时间，格式 yyyy-MM-dd HH:mm:ss（根据内容合理推断，通常3-7天后）",
          "keyPoints": ["要点1", "要点2"],
          "todos": ["待办事项1", "待办事项2"]
        }
        """;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private RelationMapper relationMapper;

    @Autowired
    private FollowUpAttachmentMapper followUpAttachmentMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFollowUp(FollowUpAddBO followUpAddBO) {
        Relation relation = validateRelation(followUpAddBO.getRelationId());
        if (followUpAddBO.getCustomerId() == null && relation != null) {
            followUpAddBO.setCustomerId(relation.getCustomerId());
        }
        if (followUpAddBO.getCustomerId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer or relation is required");
        }

        Customer customer = customerMapper.selectById(followUpAddBO.getCustomerId());
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
        }

        FollowUp followUp = BeanUtil.copyProperties(followUpAddBO, FollowUp.class);
        if (followUp.getFollowTime() == null) {
            followUp.setFollowTime(new Date());
        }
        if (followUp.getAiGenerated() == null) {
            followUp.setAiGenerated(0);
        }
        save(followUp);
        saveAttachments(followUp.getFollowUpId(), followUpAddBO.getAttachments());

        customer.setLastContactTime(new Date());
        if (followUpAddBO.getNextFollowTime() != null) {
            customer.setNextFollowTime(followUpAddBO.getNextFollowTime());
        }
        customerMapper.updateById(customer);

        return followUp.getFollowUpId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFollowUp(FollowUpUpdateBO followUpUpdateBO) {
        FollowUp followUp = getById(followUpUpdateBO.getFollowUpId());
        if (ObjectUtil.isNull(followUp)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Follow-up record does not exist");
        }
        validateRelation(followUpUpdateBO.getRelationId());

        followUp.setRelationId(followUpUpdateBO.getRelationId());
        followUp.setContactId(followUpUpdateBO.getContactId());
        followUp.setType(followUpUpdateBO.getType());
        followUp.setContent(followUpUpdateBO.getContent());
        followUp.setSummary(followUpUpdateBO.getSummary());
        followUp.setSceneType(followUpUpdateBO.getSceneType());
        followUp.setAiGenerated(followUpUpdateBO.getAiGenerated());
        followUp.setFollowTime(followUpUpdateBO.getFollowTime());
        followUp.setNextFollowTime(followUpUpdateBO.getNextFollowTime());
        updateById(followUp);

        if (followUp.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(followUp.getCustomerId());
            if (customer != null) {
                customer.setLastContactTime(followUp.getFollowTime());
                customer.setNextFollowTime(followUp.getNextFollowTime());
                customerMapper.updateById(customer);
            }
        }
    }

    @Override
    public void deleteFollowUp(Long followUpId) {
        FollowUp followUp = getById(followUpId);
        if (ObjectUtil.isNull(followUp)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "跟进记录不存在");
        }
        List<FollowUpAttachment> attachments = followUpAttachmentMapper.selectList(
            Wrappers.<FollowUpAttachment>lambdaQuery().eq(FollowUpAttachment::getFollowUpId, followUpId)
        );
        for (FollowUpAttachment attachment : attachments) {
            safeDeleteFile(attachment.getFilePath());
        }
        if (CollUtil.isNotEmpty(attachments)) {
            followUpAttachmentMapper.deleteBatchIds(attachments.stream().map(FollowUpAttachment::getAttachmentId).toList());
        }
        removeById(followUpId);
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
    public FollowUpAttachmentVO getAttachment(Long attachmentId) {
        FollowUpAttachment attachment = followUpAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Attachment does not exist");
        }
        return toAttachmentVO(attachment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAttachment(Long attachmentId) {
        FollowUpAttachment attachment = followUpAttachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            return;
        }
        safeDeleteFile(attachment.getFilePath());
        followUpAttachmentMapper.deleteById(attachmentId);
    }

    @Override
    public FollowUpAiParseVO aiParseFollowUp(FollowUpAiParseBO parseBO) {
        String customerName = StrUtil.blankToDefault(parseBO.getCustomerName(), "未知客户");
        String now = LocalDateTime.now().format(AI_TIME_FORMATTER);
        String prompt = String.format(AI_PARSE_PROMPT_TEMPLATE, customerName, now, parseBO.getContent());

        try {
            String response = chatClientProvider.getChatClient()
                .prompt()
                .user(prompt)
                .call()
                .content();

            log.info("AI 跟进解析原始响应: {}", response);
            return parseAiResponse(response, parseBO.getContent(), now);
        } catch (Exception e) {
            log.error("AI 跟进解析失败，返回默认值", e);
            return buildFallbackResult(parseBO.getContent(), now);
        }
    }

    private Relation validateRelation(Long relationId) {
        if (relationId == null) {
            return null;
        }
        Relation relation = relationMapper.selectById(relationId);
        if (relation == null || Objects.equals(relation.getStatus(), 0)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Relation does not exist");
        }
        Long currentUserId = UserUtil.getUserIdOrNull();
        if (currentUserId != null && relation.getCreateUserId() != null
            && !Objects.equals(currentUserId, relation.getCreateUserId())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Relation does not exist");
        }
        return relation;
    }

    private void saveAttachments(Long followUpId, List<com.kakarote.ai_crm.entity.BO.ChatSendBO.AttachmentDTO> attachments) {
        if (followUpId == null || CollUtil.isEmpty(attachments)) {
            return;
        }
        int sort = 0;
        for (com.kakarote.ai_crm.entity.BO.ChatSendBO.AttachmentDTO dto : attachments) {
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
        for (FollowUpVO followUp : followUps) {
            followUp.setAttachments(attachmentMap.getOrDefault(followUp.getFollowUpId(), List.of()));
        }
    }

    private FollowUpAttachmentVO toAttachmentVO(FollowUpAttachment attachment) {
        return BeanUtil.copyProperties(attachment, FollowUpAttachmentVO.class);
    }

    private void safeDeleteFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return;
        }
        try {
            fileStorageService.delete(filePath);
        } catch (Exception e) {
            log.warn("Failed to delete follow-up attachment file: {}", filePath, e);
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
            vo.setSummary(getTextOrDefault(root, "summary",
                originalContent.length() > 100 ? originalContent.substring(0, 100) + "..." : originalContent));
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

            return vo;
        } catch (Exception e) {
            log.warn("AI 响应 JSON 解析失败: {}", e.getMessage());
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

    private String normalizeRequiredDateTime(String value, String fallback) {
        LocalDateTime fallbackTime = parseDateTime(fallback, LocalTime.now());
        LocalDateTime parsed = parseDateTime(StrUtil.blankToDefault(value, fallback), fallbackTime != null ? fallbackTime.toLocalTime() : LocalTime.now());
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
                // try next format
            }
        }

        try {
            return LocalDate.parse(trimmed, DATE_ONLY_FORMATTER).atTime(defaultTime);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
