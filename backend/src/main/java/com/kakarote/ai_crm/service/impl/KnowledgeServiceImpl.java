package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.KnowledgeAskBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeAiSearchBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeTargetedScriptBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.PO.KnowledgeTag;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiAnalyzeVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeAiSearchVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.entity.VO.WeKnoraKnowledge;
import com.kakarote.ai_crm.mapper.FollowUpMapper;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.mapper.KnowledgeTagMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.service.AiAudioTranscriptionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ICustomerService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import com.kakarote.ai_crm.utils.AiMediaUtil;
import com.kakarote.ai_crm.utils.DocumentTextExtractor;
import com.kakarote.ai_crm.utils.KnowledgeAnswerLocalizationUtil;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements IKnowledgeService {

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private FollowUpMapper followUpMapper;

    @Autowired
    private ICustomerService customerService;

    @Autowired
    private WeKnoraClient weKnoraClient;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AiAudioTranscriptionService aiAudioTranscriptionService;

    @Autowired
    private VideoMediaExtractionService videoMediaExtractionService;

    @Lazy
    @Autowired
    private KnowledgeServiceImpl self;

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_AI_SEARCH_LIMIT = 5;
    private static final int MAX_AI_SEARCH_LIMIT = 8;
    private static final int MAX_AI_SEARCH_CONTEXT_LENGTH = 1200;
    private static final int MAX_TARGETED_SCRIPT_DOC_COUNT = 4;
    private static final int MAX_TARGETED_SCRIPT_DOC_CONTENT_LENGTH = 1000;
    private static final int MAX_TARGETED_SCRIPT_FOLLOWUP_COUNT = 3;
    private static final int MAX_SEARCHABLE_CONTENT_LENGTH = 20000;
    private static final int MAX_AI_ANALYZE_SOURCE_LENGTH = 4000;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadFile(MultipartFile file, String type, Long customerId, String summary) {
        if (customerId != null) {
            Customer customer = customerMapper.selectById(customerId);
            if (ObjectUtil.isNull(customer)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
            }
        }
        // Generate file path
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = IdUtil.fastSimpleUUID() + "." + FileUtil.extName(file.getOriginalFilename());
        String relativePath = datePath + "/" + fileName;

        // Use FileStorageService to upload file
        fileStorageService.upload(file, relativePath);

        // Create knowledge record
        Knowledge knowledge = new Knowledge();
        knowledge.setName(file.getOriginalFilename());
        knowledge.setType(StrUtil.isEmpty(type) ? "document" : type);
        knowledge.setCustomerId(customerId);
        knowledge.setFilePath(relativePath);
        knowledge.setFileSize(file.getSize());
        knowledge.setMimeType(file.getContentType());
        knowledge.setSummary(summary);
        knowledge.setContentText(extractSearchableContent(file));
        knowledge.setUploadUserId(UserUtil.getUserId());

        // 检查文件类型是否被 WeKnora 支持
        boolean weKnoraSupported = weKnoraClient.isSupportedFileType(file.getOriginalFilename());
        if (weKnoraSupported) {
            knowledge.setWeKnoraParseStatus("pending");
        } else {
            knowledge.setWeKnoraParseStatus("unsupported");
            log.info("文件类型不被 WeKnora 支持，跳过 RAG 处理: {}", file.getOriginalFilename());
        }

        save(knowledge);

        // Async upload to WeKnora (only for supported file types)
        if (weKnoraClient.isEnabled() && weKnoraSupported) {
            self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), relativePath, file.getOriginalFilename());
        }

        return knowledge.getKnowledgeId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long archiveExistingStandaloneFile(String fileName, String filePath, Long fileSize, String mimeType, Long customerId, String summary) {
        if (customerId != null) {
            Customer customer = customerMapper.selectById(customerId);
            if (ObjectUtil.isNull(customer)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
            }
        }
        if (StrUtil.isBlank(filePath)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文件路径不能为空");
        }

        String normalizedFileName = StrUtil.blankToDefault(StrUtil.trim(fileName), FileUtil.getName(filePath));
        Knowledge knowledge = new Knowledge();
        knowledge.setName(normalizedFileName);
        knowledge.setType("document");
        knowledge.setCustomerId(customerId);
        knowledge.setFilePath(filePath);
        knowledge.setFileSize(fileSize);
        knowledge.setMimeType(mimeType);
        knowledge.setSummary(summary);
        knowledge.setContentText(extractSearchableContent(filePath, mimeType, normalizedFileName));
        knowledge.setUploadUserId(UserUtil.getUserId());

        boolean weKnoraSupported = weKnoraClient.isSupportedFileType(normalizedFileName);
        if (weKnoraSupported) {
            knowledge.setWeKnoraParseStatus("pending");
        } else {
            knowledge.setWeKnoraParseStatus("unsupported");
            log.info("任务附件类型不被 WeKnora 支持，跳过 RAG 处理: {}", normalizedFileName);
        }

        save(knowledge);

        if (weKnoraClient.isEnabled() && weKnoraSupported) {
            self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), filePath, normalizedFileName);
        }

        return knowledge.getKnowledgeId();
    }

    /**
     * 异步上传文件到 WeKnora。
     */
    @Async
    public void asyncUploadToWeKnora(Long knowledgeId, String relativePath, String originalFilename) {
        File tempFile = null;
        try {
            // 在文件名前加上 knowledgeId 前缀，防止不同用户上传同名文件冲突
            String uniqueFilename = knowledgeId + "_" + originalFilename;
            log.info("开始上传文件到 WeKnora: knowledgeId={}, file={}", knowledgeId, uniqueFilename);

            String localPath = fileStorageService.getLocalPath(relativePath);
            String uploadFilePath;

            if (localPath != null) {
                uploadFilePath = localPath;
            } else {
                tempFile = File.createTempFile("weknora_", "_" + originalFilename);
                try (InputStream is = fileStorageService.getFileStream(relativePath)) {
                    Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                uploadFilePath = tempFile.getAbsolutePath();
            }

            WeKnoraKnowledge result = weKnoraClient.uploadDocument(uploadFilePath, uniqueFilename);

            // Update knowledge record
            Knowledge knowledge = baseMapper.selectByIdIgnoreDataPermission(knowledgeId);
            if (knowledge != null) {
                if (result == null) {
                    baseMapper.updateParseStatusIgnoreDataPermission(knowledgeId, "failed");
                    log.error("WeKnora 上传未返回有效结果: knowledgeId={}", knowledgeId);
                    return;
                }
                baseMapper.updateWeKnoraInfoIgnoreDataPermission(
                    knowledgeId, result.getId(), result.getParseStatus()
                );
                log.info("WeKnora 上传成功: knowledgeId={}, weKnoraId={}, status={}",
                    knowledgeId, result.getId(), result.getParseStatus());

                // 轮询等待 WeKnora 解析完成
                String currentStatus = result.getParseStatus();
                if ("pending".equals(currentStatus) || "processing".equals(currentStatus)) {
                    pollWeKnoraParseStatus(knowledgeId, result.getId());
                }
            }
        } catch (Exception e) {
            log.error("WeKnora 上传失败: knowledgeId={}, error={}", knowledgeId, e.getMessage(), e);
            Knowledge knowledge = baseMapper.selectByIdIgnoreDataPermission(knowledgeId);
            if (knowledge != null) {
                baseMapper.updateParseStatusIgnoreDataPermission(knowledgeId, "failed");
            }
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledge(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        // Delete from WeKnora
        if (weKnoraClient.isEnabled() && StrUtil.isNotEmpty(knowledge.getWeKnoraKnowledgeId())) {
            try {
                weKnoraClient.deleteKnowledge(knowledge.getWeKnoraKnowledgeId());
                log.info("WeKnora 删除成功: weKnoraId={}", knowledge.getWeKnoraKnowledgeId());
            } catch (Exception e) {
                log.warn("WeKnora 删除失败: weKnoraId={}, error={}",
                    knowledge.getWeKnoraKnowledgeId(), e.getMessage());
            }
        }

        // Delete file from storage
        if (StrUtil.isNotEmpty(knowledge.getFilePath())) {
            fileStorageService.delete(knowledge.getFilePath());
        }

        // Delete record
        removeById(knowledgeId);

        // Delete tags
        knowledgeTagMapper.delete(
            new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getKnowledgeId, knowledgeId)
        );
    }

    @Override
    public BasePage<KnowledgeVO> queryPageList(KnowledgeQueryBO queryBO) {
        if (StrUtil.isNotBlank(queryBO.getFileType())) {
            queryBO.setFileType(queryBO.getFileType().trim().toLowerCase(Locale.ROOT));
        }
        BasePage<KnowledgeVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        return page;
    }

    @Override
    public KnowledgeAiSearchVO aiSearch(KnowledgeAiSearchBO searchBO) {
        long startedAt = System.currentTimeMillis();
        String keyword = searchBO == null ? null : StrUtil.trim(searchBO.getKeyword());
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Search keyword is required");
        }

        String type = searchBO == null ? null : StrUtil.trimToNull(searchBO.getType());
        int limit = normalizeAiSearchLimit(searchBO == null ? null : searchBO.getLimit());

        List<KnowledgeAiSearchVO.ReferenceItem> references = buildSemanticReferences(keyword, type, limit);
        if (references.isEmpty()) {
            references = buildLocalReferences(keyword, type, limit);
        }

        KnowledgeAiSearchVO result = new KnowledgeAiSearchVO();
        result.setKeyword(keyword);
        result.setReferences(references);
        result.setTotalHits(references.size());
        result.setMatchPercent(calculateOverallMatchPercent(references));
        result.setAnswer(buildAiSearchAnswer(keyword, references));
        result.setTookMs(System.currentTimeMillis() - startedAt);
        return result;
    }

    private int normalizeAiSearchLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_AI_SEARCH_LIMIT;
        }
        return Math.min(limit, MAX_AI_SEARCH_LIMIT);
    }

    @Override
    public Flux<String> streamTargetedScript(KnowledgeTargetedScriptBO scriptBO) {
        if (scriptBO == null || scriptBO.getCustomerId() == null
                || scriptBO.getKnowledgeIds() == null || scriptBO.getKnowledgeIds().isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Generation parameters are incomplete");
        }

        List<Long> requestedIds = scriptBO.getKnowledgeIds().stream()
                .filter(ObjectUtil::isNotNull)
                .distinct()
                .limit(MAX_TARGETED_SCRIPT_DOC_COUNT)
                .toList();
        if (requestedIds.isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Please select at least one reference document");
        }

        List<Knowledge> fetchedKnowledges = lambdaQuery()
                .in(Knowledge::getKnowledgeId, requestedIds)
                .ne(Knowledge::getStatus, 2)
                .list();
        Map<Long, Knowledge> knowledgeMap = new LinkedHashMap<>();
        for (Knowledge knowledge : fetchedKnowledges) {
            knowledgeMap.put(knowledge.getKnowledgeId(), knowledge);
        }

        List<Knowledge> knowledges = new ArrayList<>();
        for (Long knowledgeId : requestedIds) {
            Knowledge knowledge = knowledgeMap.get(knowledgeId);
            if (knowledge != null) {
                knowledges.add(knowledge);
            }
        }
        if (knowledges.size() != requestedIds.size()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Some reference documents do not exist");
        }

        CustomerDetailVO customerDetail = customerService.getCustomerDetail(scriptBO.getCustomerId());
        List<FollowUpVO> recentFollowUps = followUpMapper.getRecentByCustomerId(
                scriptBO.getCustomerId(), MAX_TARGETED_SCRIPT_FOLLOWUP_COUNT
        );
        String fallback = buildTargetedScriptFallback(customerDetail, recentFollowUps, knowledges);

        if (!chatClientProvider.isApiKeyConfigured()) {
            return Flux.just(fallback);
        }

        String prompt = buildTargetedScriptPrompt(customerDetail, recentFollowUps, knowledges);
        return chatClientProvider.getChatClient()
                .prompt()
                .user(prompt)
                .stream()
                .chatResponse()
                .mapNotNull(chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        return chatResponse.getResult().getOutput().getText();
                    }
                    return null;
                })
                .onErrorResume(error -> {
                    log.warn("Targeted sales script generation failed: customerId={}, knowledgeIds={}, error={}",
                            scriptBO.getCustomerId(), requestedIds, error.getMessage());
                    return Flux.just(fallback);
                });
    }

    private List<KnowledgeAiSearchVO.ReferenceItem> buildSemanticReferences(String keyword, String type, int limit) {
        if (!weKnoraClient.isEnabled()) {
            return List.of();
        }

        try {
            List<WeKnoraChunk> chunks = weKnoraClient.searchKnowledge(keyword);
            if (chunks == null || chunks.isEmpty()) {
                return List.of();
            }

            Map<String, SemanticSearchHit> hits = new LinkedHashMap<>();
            for (WeKnoraChunk chunk : chunks) {
                if (chunk == null || StrUtil.isBlank(chunk.getKnowledgeId())) {
                    continue;
                }
                SemanticSearchHit hit = hits.computeIfAbsent(chunk.getKnowledgeId(), ignored -> new SemanticSearchHit());
                if (StrUtil.isBlank(hit.excerpt) && StrUtil.isNotBlank(chunk.getContent())) {
                    hit.excerpt = chunk.getContent();
                }
                if (chunk.getScore() != null && (hit.score == null || chunk.getScore() > hit.score)) {
                    hit.score = chunk.getScore();
                }
            }

            if (hits.isEmpty()) {
                return List.of();
            }

            LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<Knowledge>()
                    .in(Knowledge::getWeKnoraKnowledgeId, hits.keySet())
                    .ne(Knowledge::getStatus, 2);
            if (StrUtil.isNotBlank(type)) {
                wrapper.eq(Knowledge::getType, type);
            }

            List<Knowledge> knowledges = list(wrapper);
            if (knowledges.isEmpty()) {
                return List.of();
            }

            Map<String, Knowledge> knowledgeByWeKnoraId = new LinkedHashMap<>();
            for (Knowledge knowledge : knowledges) {
                if (StrUtil.isNotBlank(knowledge.getWeKnoraKnowledgeId())) {
                    knowledgeByWeKnoraId.put(knowledge.getWeKnoraKnowledgeId(), knowledge);
                }
            }

            Map<Long, String> customerNames = loadCustomerNameMap(knowledges);
            List<KnowledgeAiSearchVO.ReferenceItem> references = new ArrayList<>();
            for (Map.Entry<String, SemanticSearchHit> entry : hits.entrySet()) {
                Knowledge knowledge = knowledgeByWeKnoraId.get(entry.getKey());
                if (knowledge == null) {
                    continue;
                }

                KnowledgeAiSearchVO.ReferenceItem reference = toReferenceItem(
                        knowledge,
                        customerNames.get(knowledge.getCustomerId())
                );
                SemanticSearchHit hit = entry.getValue();
                reference.setExcerpt(StrUtil.blankToDefault(abbreviate(hit.excerpt, 360),
                        extractSnippet(knowledge, keyword)));
                reference.setMatchPercent(toMatchPercent(hit.score));
                references.add(reference);
                if (references.size() >= limit) {
                    break;
                }
            }
            return references;
        } catch (Exception e) {
            log.warn("Knowledge semantic search failed, falling back to local search: {}", e.getMessage());
            return List.of();
        }
    }

    private List<KnowledgeAiSearchVO.ReferenceItem> buildLocalReferences(String keyword, String type, int limit) {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<Knowledge>()
                .ne(Knowledge::getStatus, 2)
                .and(query -> query.like(Knowledge::getName, keyword)
                        .or()
                        .like(Knowledge::getSummary, keyword)
                        .or()
                        .like(Knowledge::getContentText, keyword))
                .orderByDesc(Knowledge::getCreateTime)
                .last("LIMIT " + limit);
        if (StrUtil.isNotBlank(type)) {
            wrapper.eq(Knowledge::getType, type);
        }

        List<Knowledge> knowledges = list(wrapper);
        if (knowledges.isEmpty()) {
            return List.of();
        }

        Map<Long, String> customerNames = loadCustomerNameMap(knowledges);
        List<KnowledgeAiSearchVO.ReferenceItem> references = new ArrayList<>();
        for (Knowledge knowledge : knowledges) {
            KnowledgeAiSearchVO.ReferenceItem reference = toReferenceItem(
                    knowledge,
                    customerNames.get(knowledge.getCustomerId())
            );
            reference.setExcerpt(extractSnippet(knowledge, keyword));
            reference.setMatchPercent(calculateLocalMatchPercent(knowledge, keyword));
            references.add(reference);
        }
        return references;
    }

    private Map<Long, String> loadCustomerNameMap(List<Knowledge> knowledges) {
        Set<Long> customerIds = new LinkedHashSet<>();
        for (Knowledge knowledge : knowledges) {
            if (knowledge.getCustomerId() != null) {
                customerIds.add(knowledge.getCustomerId());
            }
        }
        if (customerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> names = new LinkedHashMap<>();
        List<Customer> customers = customerMapper.selectBatchIds(customerIds);
        for (Customer customer : customers) {
            names.put(customer.getCustomerId(), customer.getCompanyName());
        }
        return names;
    }

    private KnowledgeAiSearchVO.ReferenceItem toReferenceItem(Knowledge knowledge, String customerName) {
        KnowledgeAiSearchVO.ReferenceItem reference = new KnowledgeAiSearchVO.ReferenceItem();
        reference.setKnowledgeId(knowledge.getKnowledgeId());
        reference.setName(knowledge.getName());
        reference.setType(knowledge.getType());
        reference.setCustomerName(customerName);
        reference.setSummary(StrUtil.trimToNull(knowledge.getSummary()));
        reference.setFileSize(knowledge.getFileSize());
        reference.setCreateTime(knowledge.getCreateTime());
        return reference;
    }

    private String buildAiSearchAnswer(String keyword, List<KnowledgeAiSearchVO.ReferenceItem> references) {
        if (references.isEmpty()) {
            return "未找到与「" + keyword + "」相关的知识库内容。请尝试更换关键词，或先上传并解析相关文档。";
        }
        if (!chatClientProvider.isApiKeyConfigured()) {
            return buildFallbackAiSearchAnswer(keyword, references);
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 CRM 知识库助手，请使用简体中文回答。\n");
        prompt.append("只能基于下方参考资料作答；如果证据不足，请明确说明信息不足。\n\n");
        prompt.append("用户问题：").append(keyword).append("\n\n");
        prompt.append("参考资料：\n");
        for (int i = 0; i < references.size(); i++) {
            KnowledgeAiSearchVO.ReferenceItem reference = references.get(i);
            prompt.append(i + 1)
                    .append(". 标题：").append(StrUtil.blankToDefault(reference.getName(), "-"))
                    .append("\n   类型：").append(StrUtil.blankToDefault(reference.getType(), "-"))
                    .append("\n   客户：").append(StrUtil.blankToDefault(reference.getCustomerName(), "-"))
                    .append("\n   摘要：").append(StrUtil.blankToDefault(reference.getSummary(), "-"))
                    .append("\n   命中片段：").append(StrUtil.blankToDefault(
                            abbreviate(reference.getExcerpt(), MAX_AI_SEARCH_CONTEXT_LENGTH), "-"))
                    .append("\n");
        }
        prompt.append("\n请输出简洁答案，包含关键信息，并提及最相关的参考资料。所有标题、分节名和标签都必须使用中文。");

        try {
            String answer = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt.toString())
                    .call()
                    .content();
            if (StrUtil.isNotBlank(answer)) {
                return KnowledgeAnswerLocalizationUtil.localizeToChinese(answer);
            }
        } catch (Exception e) {
            log.warn("Knowledge AI search answer generation failed: {}", e.getMessage());
        }
        return buildFallbackAiSearchAnswer(keyword, references);
    }

    private String buildFallbackAiSearchAnswer(String keyword, List<KnowledgeAiSearchVO.ReferenceItem> references) {
        StringBuilder answer = new StringBuilder();
        answer.append("已找到 ").append(references.size()).append(" 份与「").append(keyword).append("」相关的知识资料。\n\n");
        for (KnowledgeAiSearchVO.ReferenceItem reference : references) {
            answer.append("- **").append(StrUtil.blankToDefault(reference.getName(), "未命名文档")).append("**");
            if (reference.getMatchPercent() != null) {
                answer.append("，匹配度 ").append(reference.getMatchPercent()).append("%");
            }
            String detail = StrUtil.firstNonBlank(reference.getSummary(), reference.getExcerpt());
            if (StrUtil.isNotBlank(detail)) {
                answer.append("：").append(abbreviate(detail, 160));
            }
            answer.append("\n");
        }
        answer.append("\n配置自建 AI Key 后，可以获得更完整的归纳和建议。");
        return answer.toString();
    }

    private String buildTargetedScriptPrompt(CustomerDetailVO customerDetail,
                                             List<FollowUpVO> recentFollowUps,
                                             List<Knowledge> knowledges) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是资深 B2B CRM 销售顾问。请基于客户上下文和参考资料，生成一份可直接执行的销售话术和跟进 SOP。\n");
        prompt.append("要求：用简体中文输出；不要编造资料中不存在的事实；话术要贴近客户当前阶段；包含开场、需求探查、价值呈现、异议处理、推进收口和 SOP。\n\n");
        prompt.append("【客户上下文】\n").append(buildTargetedCustomerContext(customerDetail, recentFollowUps)).append("\n\n");
        prompt.append("【参考资料】\n").append(buildTargetedKnowledgeContext(knowledges)).append("\n\n");
        prompt.append("请用 Markdown 输出，结构清晰，方便销售直接复制使用。");
        return prompt.toString();
    }

    private String buildTargetedCustomerContext(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps) {
        StringBuilder builder = new StringBuilder();
        builder.append("公司名称: ").append(StrUtil.blankToDefault(detail.getCompanyName(), "未提供")).append('\n');
        builder.append("行业: ").append(StrUtil.blankToDefault(detail.getIndustry(), "未提供")).append('\n');
        builder.append("商机阶段: ").append(StrUtil.blankToDefault(detail.getStageName(), detail.getStage())).append('\n');
        builder.append("客户级别: ").append(StrUtil.blankToDefault(detail.getLevel(), "未提供")).append('\n');
        builder.append("客户来源: ").append(StrUtil.blankToDefault(detail.getSource(), "未提供")).append('\n');
        builder.append("最后联系时间: ").append(formatDateTime(detail.getLastContactTime())).append('\n');
        builder.append("下次跟进时间: ").append(formatDateTime(detail.getNextFollowTime())).append('\n');
        builder.append("备注: ").append(StrUtil.blankToDefault(detail.getRemark(), "未提供")).append('\n');
        builder.append("最近跟进记录:\n").append(formatFollowUpSummary(recentFollowUps));
        return builder.toString().trim();
    }

    private String buildTargetedKnowledgeContext(List<Knowledge> knowledges) {
        StringBuilder builder = new StringBuilder();
        Map<Long, String> customerNameMap = loadCustomerNameMap(knowledges);
        for (int i = 0; i < knowledges.size(); i++) {
            Knowledge knowledge = knowledges.get(i);
            builder.append("资料").append(i + 1).append('\n');
            builder.append("名称: ").append(StrUtil.blankToDefault(knowledge.getName(), "未命名文档")).append('\n');
            builder.append("类型: ").append(StrUtil.blankToDefault(knowledge.getType(), "document")).append('\n');
            builder.append("关联客户: ")
                    .append(StrUtil.blankToDefault(customerNameMap.get(knowledge.getCustomerId()), "未关联"))
                    .append('\n');
            builder.append("摘要: ").append(StrUtil.blankToDefault(knowledge.getSummary(), "未提供")).append('\n');
            builder.append("核心片段: ").append(extractTargetedScriptSnippet(knowledge)).append("\n\n");
        }
        return builder.toString().trim();
    }

    private String extractTargetedScriptSnippet(Knowledge knowledge) {
        String content = normalizeSearchableContent(knowledge.getContentText());
        if (StrUtil.isBlank(content)) {
            return StrUtil.blankToDefault(knowledge.getSummary(), "暂无可用正文片段");
        }
        return abbreviate(content, MAX_TARGETED_SCRIPT_DOC_CONTENT_LENGTH);
    }

    private String buildTargetedScriptFallback(CustomerDetailVO detail,
                                               List<FollowUpVO> recentFollowUps,
                                               List<Knowledge> knowledges) {
        String customerName = StrUtil.blankToDefault(detail.getCompanyName(), "该客户");
        String industry = StrUtil.blankToDefault(detail.getIndustry(), "所在行业");
        String stage = StrUtil.blankToDefault(detail.getStageName(), StrUtil.blankToDefault(detail.getStage(), "当前阶段"));
        String openingHook = StrUtil.firstNonBlank(
                detail.getRemark(),
                recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.get(0).getContent()
        );

        StringBuilder builder = new StringBuilder();
        builder.append("## 客户判断\n\n");
        builder.append(customerName).append("当前处于**").append(stage).append("**阶段，所属行业为**").append(industry)
                .append("**。建议围绕客户业务目标、当前推进节点和参考资料中的关键价值点展开沟通，避免直接堆砌产品能力。\n\n");

        builder.append("## 参考依据\n\n");
        for (Knowledge knowledge : knowledges) {
            builder.append("- **").append(StrUtil.blankToDefault(knowledge.getName(), "未命名文档")).append("**：")
                    .append(StrUtil.blankToDefault(
                            StrUtil.firstNonBlank(knowledge.getSummary(), extractTargetedScriptSnippet(knowledge)),
                            "暂无摘要"))
                    .append('\n');
        }
        builder.append('\n');

        builder.append("### 1. 开场白（建立连接与专业度）\n\n");
        builder.append("> 您好，我这边结合贵司目前的业务推进情况和我们整理的参考资料，特别关注到")
                .append(StrUtil.blankToDefault(openingHook, "当前项目推进效率与服务协同的提升空间"))
                .append("。今天想先和您确认一下，现阶段最希望优先解决的是流程效率、协同响应，还是业务增长相关的问题？\n\n");

        builder.append("### 2. 需求探查（识别真实诉求）\n\n");
        builder.append("> 从目前信息看，贵司已经进入").append(stage)
                .append("阶段。为了让后续方案更贴合实际，我想先确认三个点：第一，当前内部最需要优化的业务环节是什么；第二，项目推进时最担心的风险点是什么；第三，决策时更看重上线速度、落地效果还是整体投入产出？\n\n");

        builder.append("### 3. 价值呈现（把方案与客户场景绑定）\n\n");
        builder.append("> 我们这次不是单纯介绍产品功能，而是希望结合您当前的业务目标，把资料里提到的关键策略和落地经验，转成更适合贵司现阶段的推进方案。这样既能缩短内部沟通成本，也能让后续决策更聚焦在实际收益上。\n\n");

        builder.append("### 4. 异议处理（提前化解顾虑）\n\n");
        builder.append("> 如果您担心实施复杂度、投入产出或内部协同成本，我们建议先从最核心、最容易验证价值的环节切入，先跑通一个小范围场景，再逐步扩展。这样既能控制风险，也更方便内部形成共识。\n\n");

        builder.append("### 5. 收口推进（推动下一步）\n\n");
        builder.append("> 如果方向上您认可，我们可以下一步一起把关键需求、当前流程痛点和预期目标梳理清楚，再输出一版更贴合贵司实际情况的推进建议，帮助您内部更快评估和决策。\n\n");

        builder.append("## SOP 建议\n\n");
        builder.append("1. 沟通前先统一内部口径，明确本次要验证的 2-3 个核心问题。\n");
        builder.append("2. 结合参考资料中的重点信息，准备与客户场景直接相关的案例、政策或产品价值表达。\n");
        builder.append("3. 会中优先确认客户当前阶段、推进阻力、关键决策人和时间节点，再展开方案说明。\n");
        builder.append("4. 若客户表达顾虑，先复述问题并给出可落地的分阶段方案，不急于一次性覆盖全部诉求。\n");
        builder.append("5. 会后 24 小时内发送沟通纪要，明确下一步动作、责任人和时间安排。\n\n");

        builder.append("## 风险提醒\n\n");
        builder.append("- 若客户内部目标和决策链尚不清晰，建议先补齐关键信息，再推进正式方案。\n");
        builder.append("- 当前参考资料更多提供方向性支撑，具体预算、周期和交付边界仍需进一步确认。\n");
        builder.append("- 如果最近跟进已中断较久，建议先用低压沟通方式重新建立互动，再推进深入交流。\n");
        builder.append("\n配置自建 AI Key 后，可以获得更贴近上下文的定制话术。");

        return builder.toString().trim();
    }

    private String formatFollowUpSummary(List<FollowUpVO> recentFollowUps) {
        if (recentFollowUps == null || recentFollowUps.isEmpty()) {
            return "暂无最近跟进记录";
        }
        StringBuilder builder = new StringBuilder();
        for (FollowUpVO followUp : recentFollowUps) {
            builder.append("- ")
                    .append(formatDateTime(followUp.getFollowTime()))
                    .append(" / ")
                    .append(StrUtil.blankToDefault(followUp.getTypeName(), followUp.getType()))
                    .append(" / ")
                    .append(abbreviate(followUp.getContent(), 220))
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String formatDateTime(Date date) {
        if (date == null) {
            return "未提供";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
    }

    private String extractSnippet(Knowledge knowledge, String keyword) {
        String source = StrUtil.firstNonBlank(knowledge.getContentText(), knowledge.getSummary(), knowledge.getName());
        String normalized = normalizeSearchableContent(source);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }

        String lowerSource = normalized.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int index = lowerSource.indexOf(lowerKeyword);
        if (index < 0) {
            return abbreviate(normalized, 260);
        }

        int start = Math.max(0, index - 80);
        int end = Math.min(normalized.length(), index + lowerKeyword.length() + 180);
        String snippet = normalized.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < normalized.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }

    private int calculateLocalMatchPercent(Knowledge knowledge, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        int score = 40;
        if (StrUtil.blankToDefault(knowledge.getName(), "").toLowerCase().contains(lowerKeyword)) {
            score += 35;
        }
        if (StrUtil.blankToDefault(knowledge.getSummary(), "").toLowerCase().contains(lowerKeyword)) {
            score += 20;
        }
        if (StrUtil.blankToDefault(knowledge.getContentText(), "").toLowerCase().contains(lowerKeyword)) {
            score += 15;
        }
        return Math.min(score, 98);
    }

    private int calculateOverallMatchPercent(List<KnowledgeAiSearchVO.ReferenceItem> references) {
        if (references.isEmpty()) {
            return 0;
        }
        int sum = 0;
        int count = 0;
        for (KnowledgeAiSearchVO.ReferenceItem reference : references) {
            if (reference.getMatchPercent() != null) {
                sum += reference.getMatchPercent();
                count += 1;
            }
        }
        return count == 0 ? 0 : Math.round((float) sum / count);
    }

    private int toMatchPercent(Double score) {
        if (score == null) {
            return 86;
        }
        double normalized = score <= 1 ? score * 100 : score;
        return (int) Math.max(30, Math.min(99, Math.round(normalized)));
    }

    private String normalizeSearchableContent(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private String abbreviate(String text, int maxLength) {
        String normalized = normalizeSearchableContent(text);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String resolveAnalyzeContentText(Knowledge knowledge) {
        String contentText = normalizeSearchableContent(knowledge.getContentText());
        if (StrUtil.isBlank(contentText) && StrUtil.isNotBlank(knowledge.getFilePath())) {
            contentText = extractSearchableContent(knowledge.getFilePath(), knowledge.getMimeType(), knowledge.getName());
            if (StrUtil.isNotBlank(contentText)) {
                persistKnowledgeContentText(knowledge, contentText);
            }
        }
        if (StrUtil.isBlank(contentText)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文档内容为空或无法提取，请重新上传可解析的文件");
        }
        return contentText;
    }

    private String transcribeStoredMedia(Knowledge knowledge) {
        if (StrUtil.isBlank(knowledge.getFilePath())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文件路径为空，无法读取媒体内容");
        }
        try (InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath())) {
            if (inputStream == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "媒体文件无法读取，请重新上传后重试");
            }
            byte[] audioBytes = inputStream.readAllBytes();
            return aiAudioTranscriptionService.transcribe(
                    audioBytes,
                    StrUtil.blankToDefault(knowledge.getName(), FileUtil.getName(knowledge.getFilePath())),
                    resolveAudioMimeType(knowledge.getMimeType(), knowledge.getName())
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to read stored audio media: knowledgeId={}, fileName={}",
                    knowledge.getKnowledgeId(), knowledge.getName(), ex);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "媒体文件读取失败，请重新上传后重试");
        }
    }

    private void persistKnowledgeContentText(Knowledge knowledge, String contentText) {
        String normalized = normalizeSearchableContent(contentText);
        if (StrUtil.isBlank(normalized)) {
            return;
        }
        knowledge.setContentText(abbreviate(normalized, MAX_SEARCHABLE_CONTENT_LENGTH));
        lambdaUpdate()
                .eq(Knowledge::getKnowledgeId, knowledge.getKnowledgeId())
                .set(Knowledge::getContentText, knowledge.getContentText())
                .update();
    }

    private String buildVideoSearchableContent(String transcript, String audioNote, KnowledgeAiAnalyzeVO result) {
        StringBuilder builder = new StringBuilder();
        builder.append("视频音轨内容\n")
                .append(StrUtil.isNotBlank(transcript) ? transcript : StrUtil.blankToDefault(audioNote, "视频未提取到可用音轨"))
                .append("\n\n视频画面分析\n")
                .append(buildAnalyzeResultContent(result));
        return builder.toString();
    }

    private String buildAnalyzeResultContent(KnowledgeAiAnalyzeVO result) {
        if (result == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        if (StrUtil.isNotBlank(result.getCoreHighlights())) {
            builder.append(result.getCoreHighlights()).append('\n');
        }
        if (result.getTalkingPoints() != null && !result.getTalkingPoints().isEmpty()) {
            builder.append("话术建议: ").append(String.join("；", result.getTalkingPoints())).append('\n');
        }
        if (result.getRelatedEntities() != null && !result.getRelatedEntities().isEmpty()) {
            builder.append("关联实体: ");
            for (KnowledgeAiAnalyzeVO.RelatedEntity entity : result.getRelatedEntities()) {
                if (entity == null || StrUtil.isBlank(entity.getName())) {
                    continue;
                }
                builder.append(entity.getName());
                if (StrUtil.isNotBlank(entity.getType())) {
                    builder.append(" (").append(entity.getType()).append(')');
                }
                builder.append(' ');
            }
        }
        return builder.toString().trim();
    }

    private String extractSearchableContent(MultipartFile file) {
        if (file == null || file.isEmpty() || isAudioFile(file.getContentType(), file.getOriginalFilename())
                || isVideoFile(file.getContentType(), file.getOriginalFilename())) {
            return null;
        }
        try (InputStream inputStream = file.getInputStream()) {
            return extractSearchableContent(inputStream, file.getContentType(), file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("文件文本提取失败: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    private String extractSearchableContent(String filePath, String mimeType, String fileName) {
        if (StrUtil.isBlank(filePath) || isAudioFile(mimeType, fileName) || isVideoFile(mimeType, fileName)) {
            return null;
        }
        try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
            return extractSearchableContent(inputStream, mimeType, fileName);
        } catch (Exception e) {
            log.warn("文件文本提取失败: {}", fileName, e);
            return null;
        }
    }

    private String extractSearchableContent(InputStream inputStream, String mimeType, String fileName) throws Exception {
        if (inputStream == null) {
            return null;
        }
        String text;
        if (isPlainTextFile(mimeType, fileName)) {
            byte[] bytes = inputStream.readNBytes(MAX_SEARCHABLE_CONTENT_LENGTH * 3);
            text = new String(bytes, StandardCharsets.UTF_8);
        } else if (isDocumentFile(mimeType, fileName)) {
            text = DocumentTextExtractor.parseToString(inputStream, mimeType, fileName);
        } else {
            return null;
        }
        return abbreviate(text, MAX_SEARCHABLE_CONTENT_LENGTH);
    }

    private boolean isPlainTextFile(String mimeType, String fileName) {
        String normalizedMime = normalizeMimeType(mimeType);
        if (StrUtil.isNotBlank(normalizedMime)
                && (normalizedMime.startsWith("text/") || "application/json".equals(normalizedMime))) {
            return true;
        }
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt")
                || lower.endsWith(".md")
                || lower.endsWith(".csv")
                || lower.endsWith(".json")
                || lower.endsWith(".xml")
                || lower.endsWith(".log");
    }

    private boolean isDocumentFile(String mimeType, String fileName) {
        String normalizedMime = normalizeMimeType(mimeType);
        if (StrUtil.isNotBlank(normalizedMime)
                && (normalizedMime.equals("application/pdf")
                || normalizedMime.equals("application/msword")
                || normalizedMime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || normalizedMime.equals("application/vnd.ms-excel")
                || normalizedMime.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || normalizedMime.equals("application/vnd.ms-powerpoint")
                || normalizedMime.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation"))) {
            return true;
        }
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        return lower.endsWith(".pdf")
                || lower.endsWith(".doc")
                || lower.endsWith(".docx")
                || lower.endsWith(".xls")
                || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt")
                || lower.endsWith(".pptx");
    }

    private boolean isAudioFile(String mimeType, String fileName) {
        String normalizedMime = normalizeMimeType(mimeType);
        if (StrUtil.isNotBlank(normalizedMime) && normalizedMime.startsWith("audio/")) {
            return true;
        }
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3")
                || lower.endsWith(".wav")
                || lower.endsWith(".m4a")
                || lower.endsWith(".aac")
                || lower.endsWith(".ogg")
                || lower.endsWith(".oga")
                || lower.endsWith(".opus")
                || lower.endsWith(".flac")
                || lower.endsWith(".weba")
                || lower.endsWith(".amr");
    }

    private boolean isVideoFile(String mimeType, String fileName) {
        String normalizedMime = normalizeMimeType(mimeType);
        if (StrUtil.isNotBlank(normalizedMime) && normalizedMime.startsWith("video/")) {
            return true;
        }
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4")
                || lower.endsWith(".webm")
                || lower.endsWith(".mov")
                || lower.endsWith(".m4v")
                || lower.endsWith(".avi")
                || lower.endsWith(".mkv")
                || lower.endsWith(".ogv")
                || lower.endsWith(".3gp");
    }

    private String resolveAudioMimeType(String mimeType, String fileName) {
        String normalizedMime = normalizeMimeType(mimeType);
        if (StrUtil.isNotBlank(normalizedMime) && normalizedMime.startsWith("audio/")) {
            return normalizedMime;
        }
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        if (lower.endsWith(".m4a")) {
            return "audio/mp4";
        }
        if (lower.endsWith(".aac")) {
            return "audio/aac";
        }
        if (lower.endsWith(".ogg") || lower.endsWith(".oga") || lower.endsWith(".opus")) {
            return "audio/ogg";
        }
        if (lower.endsWith(".flac")) {
            return "audio/flac";
        }
        if (lower.endsWith(".weba")) {
            return "audio/webm";
        }
        if (lower.endsWith(".amr")) {
            return "audio/amr";
        }
        return "audio/mpeg";
    }

    private String normalizeMimeType(String mimeType) {
        if (StrUtil.isBlank(mimeType)) {
            return null;
        }
        return mimeType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public KnowledgeVO getKnowledgeDetail(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }
        return BeanUtil.copyProperties(knowledge, KnowledgeVO.class);
    }

    /**
     * 轮询 WeKnora 解析状态，直到完成或失败
     */
    private void pollWeKnoraParseStatus(Long knowledgeId, String weKnoraKnowledgeId) {
        int maxAttempts = 60;
        long intervalMs = 20000; // 20秒

        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                WeKnoraKnowledge detail = weKnoraClient.getKnowledgeDetail(weKnoraKnowledgeId);
                if (detail == null) {
                    log.warn("WeKnora 状态查询返回 null: weKnoraId={}", weKnoraKnowledgeId);
                    continue;
                }

                String status = detail.getParseStatus();
                log.debug("WeKnora 解析状态轮询: knowledgeId={}, attempt={}, status={}",
                    knowledgeId, i + 1, status);

                if ("completed".equals(status) || "success".equals(status) || "failed".equals(status)) {
                    Knowledge knowledge = baseMapper.selectByIdIgnoreDataPermission(knowledgeId);
                    if (knowledge != null) {
                        baseMapper.updateParseStatusIgnoreDataPermission(
                            knowledgeId, "success".equals(status) ? "completed" : status
                        );
                        log.info("WeKnora 解析完成: knowledgeId={}, status={}", knowledgeId, status);
                    }
                    return;
                }
            } catch (Exception e) {
                log.warn("WeKnora 状态查询异常: knowledgeId={}, error={}", knowledgeId, e.getMessage());
            }
        }

        log.warn("WeKnora 解析状态轮询超时(20min): knowledgeId={}, weKnoraId={}", knowledgeId, weKnoraKnowledgeId);
    }

    @Override
    public void reparseKnowledge(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }
        if (!weKnoraClient.isEnabled()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "WeKnora 未启用");
        }
        if (!weKnoraClient.isSupportedFileType(knowledge.getName())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "该文件类型不支持 RAG 解析");
        }

        // 删除 WeKnora 中的旧记录（如果存在）
        if (StrUtil.isNotEmpty(knowledge.getWeKnoraKnowledgeId())) {
            try {
                weKnoraClient.deleteKnowledge(knowledge.getWeKnoraKnowledgeId());
                log.info("WeKnora 旧记录已删除: weKnoraId={}", knowledge.getWeKnoraKnowledgeId());
            } catch (Exception e) {
                log.warn("删除 WeKnora 旧记录失败: {}", e.getMessage());
            }
        }

        // 重置状态
        knowledge.setWeKnoraParseStatus("pending");
        knowledge.setWeKnoraKnowledgeId(null);
        knowledge.setContentText(extractSearchableContent(knowledge.getFilePath(), knowledge.getMimeType(), knowledge.getName()));
        updateById(knowledge);

        // 重新异步上传（含轮询）
        self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), knowledge.getFilePath(), knowledge.getName());
    }

    @Override
    public void addTag(Long knowledgeId, String tagName) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        // Check if tag already exists
        Long count = knowledgeTagMapper.selectCount(
            new LambdaQueryWrapper<KnowledgeTag>()
                .eq(KnowledgeTag::getKnowledgeId, knowledgeId)
                .eq(KnowledgeTag::getTagName, tagName)
        );
        if (count > 0) {
            return;
        }

        KnowledgeTag tag = new KnowledgeTag();
        tag.setKnowledgeId(knowledgeId);
        tag.setTagName(tagName);
        knowledgeTagMapper.insert(tag);
    }

    // ==================== AI 文档分析 ====================

    private static final String AI_ANALYZE_PROMPT_TEMPLATE = """
        你是一个专业的 CRM 助手。请分析以下知识库文档，提取关键信息并以 JSON 格式返回。

        文档名称: %s
        文档类型: %s
        文档摘要: %s

        文档内容:
        %s

        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "coreHighlights": "文档核心内容的精炼总结（2-3句话，突出关键信息和价值点）",
          "talkingPoints": ["基于文档内容的销售话术建议1", "话术建议2", "话术建议3"],
          "relatedEntities": [{"name": "相关的客户或商机名称", "type": "customer 或 opportunity"}]
        }

        注意：
        - coreHighlights 应该是对文档最重要内容的高度概括
        - talkingPoints 应该是3-5条可以在销售场景中使用的话术建议
        - relatedEntities 从文档中提取提到的客户名称、公司名称或商机/项目名称
        """;

    private static final String DOC_QA_SYSTEM_PROMPT_TEMPLATE = """
        你是一个专业的文档问答助手。请基于以下文档内容回答用户的问题。

        文档名称: %s
        文档类型: %s

        文档内容:
        %s

        请用中文回答，回答要准确、简洁。如果文档中没有相关信息，请如实说明。
        """;

    @Override
    public KnowledgeAiAnalyzeVO aiAnalyzeDocument(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        KnowledgeAiAnalyzeVO cachedResult = readCachedAnalyzeResult(knowledge);
        if (cachedResult != null) {
            persistKnowledgeSummaryIfNeeded(knowledge, cachedResult.getCoreHighlights());
            return cachedResult;
        }

        try {
            if (isAudioFile(knowledge.getMimeType(), knowledge.getName())) {
                return analyzeAudioKnowledge(knowledge);
            }
            if (isVideoFile(knowledge.getMimeType(), knowledge.getName())) {
                return analyzeVideoKnowledge(knowledge);
            }
            return analyzeTextKnowledge(knowledge);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 文档分析失败，返回默认值", e);
            return buildFallbackAnalyzeResult(knowledge);
        }
    }

    private KnowledgeAiAnalyzeVO analyzeTextKnowledge(Knowledge knowledge) {
        String prompt = buildAnalyzePrompt(knowledge, resolveAnalyzeContentText(knowledge));
        String response = chatClientProvider.getChatClient()
                .prompt()
                .user(prompt)
                .call()
                .content();

        log.info("AI 文档分析原始响应: {}", response);
        KnowledgeAiAnalyzeVO result = parseAnalyzeResponse(response, knowledge);
        persistAnalyzeResult(knowledge, result);
        return result;
    }

    private KnowledgeAiAnalyzeVO analyzeAudioKnowledge(Knowledge knowledge) {
        String transcript = transcribeStoredMedia(knowledge);
        if (StrUtil.isBlank(transcript)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "未能识别音频内容，请确认文件可播放后重试");
        }
        String contentText = "音频转写内容\n" + transcript;
        persistKnowledgeContentText(knowledge, contentText);
        return analyzeTextLikeMediaKnowledge(knowledge, contentText);
    }

    private KnowledgeAiAnalyzeVO analyzeVideoKnowledge(Knowledge knowledge) {
        ensureVisionSupported("视频");
        VideoMediaExtractionService.VideoExtractionResult extraction =
                videoMediaExtractionService.extract(knowledge.getFilePath(), knowledge.getName());

        String transcript = null;
        String audioNote = "视频未提取到可用音轨";
        if (extraction.hasAudio()) {
            transcript = aiAudioTranscriptionService.transcribe(
                    extraction.audioBytes(),
                    extraction.audioFileName(),
                    extraction.audioContentType()
            );
            if (StrUtil.isBlank(transcript)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "未能识别视频音轨内容，请确认文件可播放后重试");
            }
            audioNote = transcript;
        }

        List<Media> frameMedia = extraction.frames().stream()
                .map(frame -> AiMediaUtil.buildMedia(frame.bytes(), frame.fileName(), MimeType.valueOf(frame.mimeType())))
                .toList();
        String prompt = String.format("""
                你是一个专业的 CRM 助手。请结合视频关键帧和音轨转写，分析以下知识库视频，并以 JSON 格式返回。

                视频名称: %s
                视频类型: %s
                视频摘要: %s
                关键帧数量: %s

                音轨转写或说明:
                %s

                请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
                {
                  "coreHighlights": "视频核心内容的精炼总结，2-3句话，突出关键信息和价值点",
                  "talkingPoints": ["基于视频内容的销售话术建议", "话术建议2", "话术建议3"],
                  "relatedEntities": [{"name": "相关的客户或商机名称", "type": "customer 或 opportunity"}]
                }
                """,
                knowledge.getName(),
                StrUtil.blankToDefault(knowledge.getMimeType(), "video"),
                StrUtil.blankToDefault(knowledge.getSummary(), "无"),
                extraction.frames().size(),
                abbreviate(audioNote, MAX_AI_ANALYZE_SOURCE_LENGTH));

        String response = chatClientProvider.getChatClient()
                .prompt()
                .user(userSpec -> userSpec.text(prompt).media(frameMedia.toArray(Media[]::new)))
                .call()
                .content();
        KnowledgeAiAnalyzeVO result = parseAnalyzeResponse(response, knowledge);
        persistAnalyzeResult(knowledge, result);
        persistKnowledgeContentText(knowledge, buildVideoSearchableContent(transcript, audioNote, result));
        return result;
    }

    private KnowledgeAiAnalyzeVO analyzeTextLikeMediaKnowledge(Knowledge knowledge, String contentText) {
        String response = chatClientProvider.getChatClient()
                .prompt()
                .user(buildAnalyzePrompt(knowledge, contentText))
                .call()
                .content();
        KnowledgeAiAnalyzeVO result = parseAnalyzeResponse(response, knowledge);
        persistAnalyzeResult(knowledge, result);
        return result;
    }

    private String buildAnalyzePrompt(Knowledge knowledge, String contentText) {
        return String.format(AI_ANALYZE_PROMPT_TEMPLATE,
                knowledge.getName(),
                StrUtil.blankToDefault(knowledge.getType(), "document"),
                StrUtil.blankToDefault(knowledge.getSummary(), "无"),
                abbreviate(contentText, MAX_AI_ANALYZE_SOURCE_LENGTH));
    }

    private void ensureVisionSupported(String mediaLabel) {
        AiModelCapabilities capabilities = chatClientProvider.getCurrentCapabilities();
        if (capabilities == null || !capabilities.isSupportsVision()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "当前模型不支持" + mediaLabel + "画面分析，请切换到支持视觉的模型后重试");
        }
    }

    private KnowledgeAiAnalyzeVO readCachedAnalyzeResult(Knowledge knowledge) {
        String snapshot = StrUtil.trimToNull(knowledge.getAiAnalysisSnapshot());
        if (snapshot == null) {
            return null;
        }

        try {
            KnowledgeAiAnalyzeVO cached = objectMapper.readValue(snapshot, KnowledgeAiAnalyzeVO.class);
            return normalizeAnalyzeResult(cached, knowledge);
        } catch (Exception e) {
            log.warn("解析知识库缓存分析结果失败: knowledgeId={}, error={}",
                    knowledge.getKnowledgeId(), e.getMessage());
            return null;
        }
    }

    private void persistAnalyzeResult(Knowledge knowledge, KnowledgeAiAnalyzeVO result) {
        try {
            KnowledgeAiAnalyzeVO normalized = normalizeAnalyzeResult(result, knowledge);
            knowledge.setAiAnalysisSnapshot(objectMapper.writeValueAsString(normalized));
            knowledge.setAiAnalysisTime(new Date());
            applyKnowledgeSummaryIfNeeded(knowledge, normalized.getCoreHighlights());
            updateById(knowledge);
        } catch (Exception e) {
            log.warn("保存知识库 AI 分析缓存失败: knowledgeId={}, error={}",
                    knowledge.getKnowledgeId(), e.getMessage());
        }
    }

    private void persistKnowledgeSummaryIfNeeded(Knowledge knowledge, String summary) {
        if (!applyKnowledgeSummaryIfNeeded(knowledge, summary)) {
            return;
        }
        try {
            lambdaUpdate()
                    .eq(Knowledge::getKnowledgeId, knowledge.getKnowledgeId())
                    .set(Knowledge::getSummary, knowledge.getSummary())
                    .update();
        } catch (Exception e) {
            log.warn("同步知识库 AI 摘要失败: knowledgeId={}, error={}",
                    knowledge.getKnowledgeId(), e.getMessage());
        }
    }

    private boolean applyKnowledgeSummaryIfNeeded(Knowledge knowledge, String summary) {
        String normalizedSummary = StrUtil.trim(summary);
        if (StrUtil.isBlank(normalizedSummary)
                || StrUtil.equals(normalizedSummary, StrUtil.trim(knowledge.getSummary()))) {
            return false;
        }
        knowledge.setSummary(normalizedSummary);
        return true;
    }

    private KnowledgeAiAnalyzeVO normalizeAnalyzeResult(KnowledgeAiAnalyzeVO source, Knowledge knowledge) {
        KnowledgeAiAnalyzeVO normalized = new KnowledgeAiAnalyzeVO();
        if (source != null) {
            normalized.setCoreHighlights(source.getCoreHighlights());
            normalized.setTalkingPoints(source.getTalkingPoints());
            normalized.setRelatedEntities(source.getRelatedEntities());
        }
        if (StrUtil.isBlank(normalized.getCoreHighlights())) {
            normalized.setCoreHighlights(StrUtil.blankToDefault(knowledge.getSummary(), "暂无摘要"));
        }
        if (normalized.getTalkingPoints() == null) {
            normalized.setTalkingPoints(List.of());
        }
        if (normalized.getRelatedEntities() == null) {
            normalized.setRelatedEntities(List.of());
        }
        return normalized;
    }

    private KnowledgeAiAnalyzeVO parseAnalyzeResponse(String response, Knowledge knowledge) {
        try {
            // Strip markdown code block markers if present
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            KnowledgeAiAnalyzeVO vo = new KnowledgeAiAnalyzeVO();

            // 核心提炼
            if (root.has("coreHighlights") && !root.get("coreHighlights").isNull()) {
                vo.setCoreHighlights(root.get("coreHighlights").asText());
            } else {
                vo.setCoreHighlights(StrUtil.blankToDefault(knowledge.getSummary(), "暂无摘要"));
            }

            // 推荐话术
            List<String> talkingPoints = new ArrayList<>();
            if (root.has("talkingPoints") && root.get("talkingPoints").isArray()) {
                root.get("talkingPoints").forEach(n -> talkingPoints.add(n.asText()));
            }
            vo.setTalkingPoints(talkingPoints);

            // 关联实体
            List<KnowledgeAiAnalyzeVO.RelatedEntity> entities = new ArrayList<>();
            if (root.has("relatedEntities") && root.get("relatedEntities").isArray()) {
                root.get("relatedEntities").forEach(n -> {
                    KnowledgeAiAnalyzeVO.RelatedEntity entity = new KnowledgeAiAnalyzeVO.RelatedEntity();
                    entity.setName(n.has("name") ? n.get("name").asText() : "");
                    entity.setType(n.has("type") ? n.get("type").asText() : "");
                    if (StrUtil.isNotBlank(entity.getName())) {
                        entities.add(entity);
                    }
                });
            }
            vo.setRelatedEntities(entities);

            return vo;
        } catch (Exception e) {
            log.warn("AI 文档分析 JSON 解析失败: {}", e.getMessage());
            return buildFallbackAnalyzeResult(knowledge);
        }
    }

    private KnowledgeAiAnalyzeVO buildFallbackAnalyzeResult(Knowledge knowledge) {
        KnowledgeAiAnalyzeVO vo = new KnowledgeAiAnalyzeVO();
        vo.setCoreHighlights(StrUtil.blankToDefault(knowledge.getSummary(), "暂无摘要"));
        vo.setTalkingPoints(List.of());
        vo.setRelatedEntities(List.of());
        return vo;
    }

    @Override
    public Flux<String> askDocumentQuestion(Long knowledgeId, KnowledgeAskBO askBO) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        // 设置 AI 上下文
        Long currentUserId = UserUtil.getUserIdOrNull();
        if (currentUserId != null) {
            AiContextHolder.setContext(knowledgeId, currentUserId);
        }

        // 构建文档内容上下文
        String contentText = StrUtil.blankToDefault(knowledge.getContentText(), "");
        if (contentText.length() > 4000) {
            contentText = contentText.substring(0, 4000) + "...";
        }

        String systemPrompt = String.format(DOC_QA_SYSTEM_PROMPT_TEMPLATE,
                knowledge.getName(),
                StrUtil.blankToDefault(knowledge.getType(), "document"),
                contentText);

        // 构建对话历史
        List<Message> history = new ArrayList<>();
        if (askBO.getHistory() != null) {
            for (KnowledgeAskBO.ChatHistoryItem item : askBO.getHistory()) {
                if ("user".equals(item.getRole())) {
                    history.add(new UserMessage(item.getContent()));
                } else if ("assistant".equals(item.getRole())) {
                    history.add(new AssistantMessage(item.getContent()));
                }
            }
        }

        // 流式调用
        return chatClientProvider.getChatClient()
                .prompt()
                .system(systemPrompt)
                .messages(history)
                .user(askBO.getQuestion())
                .stream()
                .chatResponse()
                .mapNotNull(chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        return chatResponse.getResult().getOutput().getText();
                    }
                    return null;
                })
                .doOnComplete(AiContextHolder::clear)
                .doOnError(error -> {
                    log.error("文档问答错误: {}", error.getMessage(), error);
                    AiContextHolder.clear();
                });
    }

    private static final class SemanticSearchHit {
        private String excerpt;
        private Double score;
    }
}
