package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
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
import com.kakarote.ai_crm.config.tenant.TenantContextHolder;
import com.kakarote.ai_crm.entity.BO.KnowledgeAskBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeAiSearchBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeTargetedScriptBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.PO.KnowledgeTag;
import com.kakarote.ai_crm.entity.VO.ContactVO;
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
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import com.kakarote.ai_crm.utils.AiMediaUtil;
import com.kakarote.ai_crm.utils.DocumentTextExtractor;
import com.kakarote.ai_crm.utils.KnowledgeAnswerLocalizationUtil;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 知识库服务实现
 */
@Slf4j
@Service
public class KnowledgeServiceImpl extends ServiceImpl<KnowledgeMapper, Knowledge> implements IKnowledgeService {

    private static final int MAX_SEARCHABLE_CONTENT_LENGTH = 20_000;
    private static final int DEFAULT_AI_SEARCH_LIMIT = 5;
    private static final int MAX_AI_SEARCH_LIMIT = 8;
    private static final int MAX_AI_SEARCH_RAG_SCOPE_SIZE = 80;
    private static final int MAX_AI_SEARCH_CONTEXT_LENGTH = 1_200;
    private static final int MAX_TARGETED_SCRIPT_DOC_COUNT = 4;
    private static final int MAX_TARGETED_SCRIPT_DOC_CONTENT_LENGTH = 1_000;
    private static final int MAX_TARGETED_SCRIPT_FOLLOWUP_COUNT = 3;
    private static final int MAX_AI_ANALYZE_CONTENT_LENGTH = 4_000;
    private static final String CHAT_ATTACHMENT_AUTO_ARCHIVE_SUMMARY_PREFIX = "聊天附件自动归档，消息ID:";

    @Value("${file.upload-path:./uploads}")
    private String uploadPath;

    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private FollowUpMapper followUpMapper;

    @Autowired
    private WeKnoraClient weKnoraClient;

    @Autowired
    private FileStorageService fileStorageService;

    @Lazy
    @Autowired
    private KnowledgeServiceImpl self;

    @Lazy
    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private IGlobalSearchIndexService globalSearchIndexService;

    @Lazy
    @Autowired
    private ICustomerService customerService;

    @Autowired
    private AiQuotaService aiQuotaService;

    @Autowired
    private AiAudioTranscriptionService aiAudioTranscriptionService;

    @Autowired
    private VideoMediaExtractionService videoMediaExtractionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传文件。
     */
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
            self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), relativePath, file.getOriginalFilename(), UserUtil.getTenantId());
        }

        globalSearchIndexService.refreshKnowledgeIndex(knowledge.getKnowledgeId());

        return knowledge.getKnowledgeId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long archiveExistingFile(String fileName, String filePath, Long fileSize, String mimeType, Long customerId, String summary) {
        if (customerId == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不能为空");
        }
        Customer customer = customerMapper.selectById(customerId);
        if (ObjectUtil.isNull(customer)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
        }
        if (StrUtil.isBlank(filePath)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "文件路径不能为空");
        }

        String normalizedFileName = StrUtil.blankToDefault(fileName, FileUtil.getName(filePath));
        Knowledge knowledge = new Knowledge();
        knowledge.setName(normalizedFileName);
        knowledge.setType("document");
        knowledge.setCustomerId(customerId);
        knowledge.setFilePath(filePath);
        knowledge.setFileSize(fileSize);
        knowledge.setMimeType(mimeType);
        knowledge.setSummary(normalizeUserFacingSummary(summary));
        knowledge.setContentText(extractSearchableContent(filePath, mimeType, normalizedFileName));
        knowledge.setUploadUserId(UserUtil.getUserId());

        boolean weKnoraSupported = weKnoraClient.isSupportedFileType(normalizedFileName);
        if (weKnoraSupported) {
            knowledge.setWeKnoraParseStatus("pending");
        } else {
            knowledge.setWeKnoraParseStatus("unsupported");
            log.info("聊天附件类型不被 WeKnora 支持，跳过 RAG 解析: {}", normalizedFileName);
        }

        save(knowledge);

        if (weKnoraClient.isEnabled() && weKnoraSupported) {
            self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), filePath, normalizedFileName, UserUtil.getTenantId());
        }

        globalSearchIndexService.refreshKnowledgeIndex(knowledge.getKnowledgeId());
        return knowledge.getKnowledgeId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long archiveText(String fileName, String contentText, String type, Long customerId, String summary) {
        String normalizedText = normalizeSearchableContent(contentText);
        if (StrUtil.isBlank(normalizedText)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识正文不能为空");
        }
        if (customerId != null) {
            Customer customer = customerMapper.selectById(customerId);
            if (ObjectUtil.isNull(customer)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "客户不存在或无权限访问");
            }
        }

        String normalizedFileName = StrUtil.blankToDefault(fileName, "mail-" + IdUtil.fastSimpleUUID() + ".txt");
        if (!normalizedFileName.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            normalizedFileName = normalizedFileName + ".txt";
        }
        byte[] bytes = normalizedText.getBytes(StandardCharsets.UTF_8);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = datePath + "/" + IdUtil.fastSimpleUUID() + ".txt";
        fileStorageService.upload(new ByteArrayInputStream(bytes), bytes.length, relativePath, "text/plain;charset=UTF-8");

        Knowledge knowledge = new Knowledge();
        knowledge.setName(normalizedFileName);
        knowledge.setType(StrUtil.blankToDefault(type, "email"));
        knowledge.setCustomerId(customerId);
        knowledge.setFilePath(relativePath);
        knowledge.setFileSize((long) bytes.length);
        knowledge.setMimeType("text/plain");
        knowledge.setSummary(normalizeUserFacingSummary(summary));
        knowledge.setContentText(normalizedText);
        knowledge.setUploadUserId(UserUtil.getUserId());
        knowledge.setWeKnoraParseStatus("pending");
        save(knowledge);

        if (weKnoraClient.isEnabled()) {
            self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), relativePath, normalizedFileName, UserUtil.getTenantId());
        }
        globalSearchIndexService.refreshKnowledgeIndex(knowledge.getKnowledgeId());
        return knowledge.getKnowledgeId();
    }

    /**
     * 异步上传文件到 WeKnora（传入 tenantId 解决 @Async 线程上下文丢失问题）
     */
    @Async
    public void asyncUploadToWeKnora(Long knowledgeId, String relativePath, String originalFilename, Long tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        File tempFile = null;
        try {
            // 获取租户的 WeKnora 上下文（API Key + 知识库 ID，懒创建）
            WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(tenantId);

            // 在文件名前加上 knowledgeId 前缀，防止不同用户上传同名文件冲突
            String uniqueFilename = knowledgeId + "_" + originalFilename;
            log.info("开始上传文件到 WeKnora: knowledgeId={}, file={}, tenantKbId={}", knowledgeId, uniqueFilename, ctx.getKnowledgeBaseId());

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

            // Upload to WeKnora using tenant-specific API Key and knowledge base
            WeKnoraKnowledge result = weKnoraClient.uploadDocument(uploadFilePath, uniqueFilename, ctx.getKnowledgeBaseId(), ctx.getApiKey());

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
                    pollWeKnoraParseStatus(knowledgeId, result.getId(), ctx.getApiKey());
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
            TenantContextHolder.clear();
        }
    }

    /**
     * 删除知识。
     */
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
                WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(UserUtil.getTenantId());
                weKnoraClient.deleteKnowledge(knowledge.getWeKnoraKnowledgeId(), ctx.getApiKey());
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
        globalSearchIndexService.deleteByEntity("knowledge", knowledgeId);
    }

    /**
     * 分页查询知识列表。
     */
    @Override
    public BasePage<KnowledgeVO> queryPageList(KnowledgeQueryBO queryBO) {
        if (StrUtil.isNotBlank(queryBO.getFileType())) {
            queryBO.setFileType(queryBO.getFileType().trim().toLowerCase(Locale.ROOT));
        }
        BasePage<KnowledgeVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        return page;
    }

    /**
     * 获取知识详情。
     */
    @Override
    public KnowledgeVO getKnowledgeDetail(Long knowledgeId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }
        KnowledgeVO detail = BeanUtil.copyProperties(knowledge, KnowledgeVO.class);
        detail.setAiAnalyzeResult(readCachedAnalyzeResult(knowledge));
        return detail;
    }

    /**
     * 轮询 WeKnora 解析状态，直到完成或失败
     */
    @Override
    public KnowledgeAiSearchVO aiSearch(KnowledgeAiSearchBO searchBO) {
        long startAt = System.currentTimeMillis();
        String keyword = searchBO != null ? StrUtil.trim(searchBO.getKeyword()) : null;
        if (StrUtil.isBlank(keyword)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "搜索内容不能为空");
        }

        String type = searchBO != null ? StrUtil.trim(searchBO.getType()) : null;
        int limit = DEFAULT_AI_SEARCH_LIMIT;
        if (searchBO != null && searchBO.getLimit() != null && searchBO.getLimit() > 0) {
            limit = Math.min(searchBO.getLimit(), MAX_AI_SEARCH_LIMIT);
        }

        String answer = null;
        List<KnowledgeAiSearchVO.ReferenceItem> references = List.of();

        WeKnoraClient.WeKnoraChatResult ragResult = askKnowledgeBaseQuestion(keyword, type);
        if (ragResult != null) {
            if (!isUnavailableRagAnswer(ragResult.getAnswer())) {
                answer = KnowledgeAnswerLocalizationUtil.localizeToChinese(ragResult.getAnswer());
            }
            references = buildReferencesFromChunks(weKnoraClient.filterRelevantChunks(ragResult.getReferences(), keyword), type, limit);
        }

        if (references.isEmpty()) {
            references = buildSemanticReferences(keyword, type, limit);
        }
        if (references.isEmpty()) {
            references = buildLocalReferences(keyword, type, limit);
        }
        if (StrUtil.isBlank(answer)) {
            answer = buildAiSearchAnswer(keyword, references);
        }

        KnowledgeAiSearchVO result = new KnowledgeAiSearchVO();
        result.setKeyword(keyword);
        result.setReferences(references);
        result.setTotalHits(references.size());
        result.setMatchPercent(calculateOverallMatchPercent(references));
        result.setAnswer(answer);
        result.setTookMs(System.currentTimeMillis() - startAt);
        return result;
    }

    /**
     * Stream targeted sales script content.
     */
    @Override
    public Flux<String> streamTargetedScript(KnowledgeTargetedScriptBO scriptBO) {
        if (scriptBO == null || scriptBO.getCustomerId() == null
            || scriptBO.getKnowledgeIds() == null || scriptBO.getKnowledgeIds().isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "生成参数不完整");
        }

        List<Long> requestedIds = scriptBO.getKnowledgeIds().stream()
            .filter(ObjectUtil::isNotNull)
            .distinct()
            .limit(MAX_TARGETED_SCRIPT_DOC_COUNT)
            .toList();
        if (requestedIds.isEmpty()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "请至少选择一份参考文档");
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
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "部分参考文档不存在或不可访问");
        }

        CustomerDetailVO customerDetail = customerService.getCustomerDetail(scriptBO.getCustomerId());
        List<FollowUpVO> recentFollowUps = followUpMapper.getRecentByCustomerId(
            scriptBO.getCustomerId(), MAX_TARGETED_SCRIPT_FOLLOWUP_COUNT
        );
        String fallback = buildTargetedScriptFallback(customerDetail, recentFollowUps, knowledges);

        if (!chatClientProvider.isApiKeyConfigured()) {
            return Flux.just(fallback);
        }

        Long currentTenantId = UserUtil.getTenantId();
        String prompt = buildTargetedScriptPrompt(customerDetail, recentFollowUps, knowledges);
        String quotaTip = aiQuotaService.resolveQuotaFailureMessage(
            currentTenantId, "knowledge_targeted_script", null, null, prompt
        );
        if (quotaTip != null) {
            return Flux.just(quotaTip);
        }

        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<Integer> promptTokensRef = new AtomicReference<>(0);
        AtomicReference<Integer> completionTokensRef = new AtomicReference<>(0);
        AtomicReference<Integer> totalTokensRef = new AtomicReference<>(0);
        AtomicBoolean streamBilled = new AtomicBoolean(false);
        AtomicBoolean streamFailed = new AtomicBoolean(false);
        Runnable consumeTargetedScriptCredits = () -> {
            if (!streamBilled.compareAndSet(false, true)) {
                return;
            }
            aiQuotaService.consumeResolvedTokens(
                currentTenantId,
                "knowledge_targeted_script",
                aiQuotaService.resolveTokenUsage(
                    promptTokensRef.get(),
                    completionTokensRef.get(),
                    totalTokensRef.get(),
                    null,
                    null,
                    prompt,
                    fullResponse.toString()
                )
            );
        };

        return chatClientProvider.getChatClient()
            .prompt()
            .user(prompt)
            .stream()
            .chatResponse()
            .doOnNext(chatResponse -> {
                if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                    String text = chatResponse.getResult().getOutput().getText();
                    if (text != null) {
                        fullResponse.append(text);
                    }
                }
                if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                    captureRawTokenUsage(
                        chatResponse.getMetadata().getUsage(),
                        promptTokensRef,
                        completionTokensRef,
                        totalTokensRef
                    );
                }
            })
            .mapNotNull(chatResponse -> {
                if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                    return chatResponse.getResult().getOutput().getText();
                }
                return null;
            })
            .doOnComplete(consumeTargetedScriptCredits)
            .doOnError(error -> {
                streamFailed.set(true);
                log.warn(
                    "流式生成定向销售话术失败: customerId={}, knowledgeIds={}, error={}",
                    scriptBO.getCustomerId(), requestedIds, error.getMessage()
                );
            })
            .doFinally(signalType -> {
                if (signalType == SignalType.CANCEL && !streamFailed.get() && fullResponse.length() > 0) {
                    consumeTargetedScriptCredits.run();
                }
            });
    }

    private void captureRawTokenUsage(Usage usage,
                                      AtomicReference<Integer> promptTokensRef,
                                      AtomicReference<Integer> completionTokensRef,
                                      AtomicReference<Integer> totalTokensRef) {
        AiQuotaService.RawTokenUsage rawUsage = aiQuotaService.readRawTokenUsage(usage);
        if (!rawUsage.hasAnyToken()) {
            return;
        }
        updatePositiveTokenRef(promptTokensRef, rawUsage.promptTokens());
        updatePositiveTokenRef(completionTokensRef, rawUsage.completionTokens());
        updatePositiveTokenRef(totalTokensRef, rawUsage.totalTokens());
    }

    private void updatePositiveTokenRef(AtomicReference<Integer> tokenRef, Integer value) {
        if (value != null && value > 0) {
            tokenRef.set(value);
        }
    }

    private void pollWeKnoraParseStatus(Long knowledgeId, String weKnoraKnowledgeId, String tenantApiKey) {
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
                WeKnoraKnowledge detail = weKnoraClient.getKnowledgeDetail(weKnoraKnowledgeId, tenantApiKey);
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

    /**
     * 重新解析知识。
     */
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
                WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(UserUtil.getTenantId());
                weKnoraClient.deleteKnowledge(knowledge.getWeKnoraKnowledgeId(), ctx.getApiKey());
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
        globalSearchIndexService.refreshKnowledgeIndex(knowledgeId);

        // 重新异步上传（含轮询）
        self.asyncUploadToWeKnora(knowledge.getKnowledgeId(), knowledge.getFilePath(), knowledge.getName(), UserUtil.getTenantId());
    }

    /**
     * 新增标签。
     */
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
        globalSearchIndexService.refreshKnowledgeIndex(knowledgeId);
    }

    // ==================== AI 文档分析 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomer(Long knowledgeId, Long customerId) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Knowledge file does not exist");
        }

        if (customerId != null) {
            Customer customer = customerMapper.selectById(customerId);
            if (ObjectUtil.isNull(customer)) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer does not exist or is inaccessible");
            }
        }

        if (ObjectUtil.equal(knowledge.getCustomerId(), customerId)) {
            return;
        }

        knowledge.setCustomerId(customerId);
        updateById(knowledge);
        globalSearchIndexService.refreshKnowledgeIndex(knowledgeId);
    }

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

    private static final String IMAGE_AI_ANALYZE_PROMPT_TEMPLATE = """
        你是一个专业的 CRM 助手。请分析随消息附带的图片，并以 JSON 格式返回。

        图片名称: %s
        图片类型: %s
        已有摘要: %s

        请重点识别图片中的文字、表格、产品、合同/票据/现场信息、客户名称、项目名称、时间、金额、风险点或后续动作。
        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "coreHighlights": "图片核心内容的精炼总结（2-3句话，突出可见事实和业务价值）",
          "talkingPoints": ["基于图片内容的销售/跟进建议1", "建议2", "建议3"],
          "relatedEntities": [{"name": "图片中出现的客户、公司、项目或商机名称", "type": "customer 或 opportunity"}]
        }
        """;

    private static final String VIDEO_AI_ANALYZE_PROMPT_TEMPLATE = """
        你是一个专业的 CRM 助手。请综合分析视频关键帧和音轨信息，并以 JSON 格式返回。

        视频名称: %s
        视频类型: %s
        已有摘要: %s
        关键帧数量: %d

        音轨信息:
        %s

        请综合关键帧画面和音轨内容，重点识别客户、场景、产品、会议/拜访过程、项目线索、风险点和下一步动作。
        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "coreHighlights": "视频核心内容的精炼总结（2-3句话，说明主要画面/语音信息和业务价值）",
          "talkingPoints": ["基于视频内容的销售/跟进建议1", "建议2", "建议3"],
          "relatedEntities": [{"name": "视频中出现的客户、公司、项目或商机名称", "type": "customer 或 opportunity"}]
        }
        """;

    private static final String DOC_QA_SYSTEM_PROMPT_TEMPLATE = """
        你是一个专业的文档问答助手。请基于以下文档内容回答用户的问题。

        文档名称: %s
        文档类型: %s

        文档内容:
        %s

        请用中文回答，回答要准确、简洁。如果文档中没有相关信息，请如实说明。
        """;

    private static final String TARGETED_SCRIPT_PROMPT_TEMPLATE = """
        你是一位资深 B2B 销售总监兼售前顾问。请结合客户画像、最近跟进信息和参考文档，为一线销售生成一份“可直接拿来用”的针对性销售话术与 SOP。

        输出要求：
        1. 使用中文 Markdown。
        2. 必须围绕客户当前场景输出，不要写成泛泛的产品介绍。
        3. 不要编造参考资料中不存在的能力或承诺；若信息不足，请明确写“需进一步确认”。
        4. 请严格使用以下结构：
           ## 客户判断
           ## 参考依据
           ### 1. 开场白（建立连接与专业度）
           ### 2. 需求探查（识别真实诉求）
           ### 3. 价值呈现（把方案与客户场景绑定）
           ### 4. 异议处理（提前化解顾虑）
           ### 5. 收口推进（推动下一步）
           ## SOP 建议
           ## 风险提醒
        5. 每个“话术”小节都输出 2-4 句可直接对客户说的话，必要时用引用块 `>` 包裹。
        6. “SOP 建议”写成 4-6 条编号步骤，突出拜访/通话前准备、会中动作、会后跟进。
        7. 语气专业、自然、克制，适合企业服务销售场景。

        目标客户信息：
        %s

        参考文档信息：
        %s
        """;

    /**
     * 使用 AI 分析文档。
     */
    @Override
    public KnowledgeAiAnalyzeVO aiAnalyzeDocument(Long knowledgeId, boolean forceRefresh) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        KnowledgeAiAnalyzeVO cachedResult = readCachedAnalyzeResult(knowledge);
        if (!forceRefresh && cachedResult != null) {
            return cachedResult;
        }

        try {
            if (isImageFile(knowledge.getMimeType(), knowledge.getName())) {
                return analyzeImageKnowledge(knowledge);
            }
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
            log.error("AI 文档分析失败", e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "AI 分析失败，请稍后重试");
        }
    }

    private KnowledgeAiAnalyzeVO analyzeTextKnowledge(Knowledge knowledge) {
        String contentText = resolveAnalyzeContentText(knowledge);

        String summary = StrUtil.blankToDefault(normalizeUserFacingSummary(knowledge.getSummary()), "无");
        String prompt = String.format(AI_ANALYZE_PROMPT_TEMPLATE,
                knowledge.getName(),
                StrUtil.blankToDefault(knowledge.getType(), "document"),
                summary,
                contentText);

        return callStructuredAnalyze(knowledge, prompt, List.of(), null);
    }

    private KnowledgeAiAnalyzeVO analyzeImageKnowledge(Knowledge knowledge) {
        ensureVisionSupported("图片");
        String summary = StrUtil.blankToDefault(normalizeUserFacingSummary(knowledge.getSummary()), "无");
        String prompt = String.format(IMAGE_AI_ANALYZE_PROMPT_TEMPLATE,
            knowledge.getName(),
            StrUtil.blankToDefault(knowledge.getMimeType(), "image"),
            summary);
        Media media = buildStoredImageMedia(knowledge);
        return callStructuredAnalyze(knowledge, prompt, List.of(media), result ->
            "图片内容摘要：\n" + buildAnalyzeResultContent(result));
    }

    private KnowledgeAiAnalyzeVO analyzeAudioKnowledge(Knowledge knowledge) {
        String transcript = transcribeStoredMedia(knowledge);
        if (StrUtil.isBlank(transcript)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "音频未生成有效转写内容，请确认音频清晰后重试");
        }
        String contentText = "音频转写内容：\n" + transcript;
        persistKnowledgeContentText(knowledge, contentText);
        return analyzeTextLikeMediaKnowledge(knowledge, contentText);
    }

    private KnowledgeAiAnalyzeVO analyzeVideoKnowledge(Knowledge knowledge) {
        ensureVisionSupported("视频画面");
        VideoMediaExtractionService.VideoExtractionResult extraction =
            videoMediaExtractionService.extract(knowledge.getFilePath(), knowledge.getName());

        String transcript = null;
        String audioNote = "未检测到可用音轨。";
        if (extraction.hasAudio()) {
            try {
                transcript = aiAudioTranscriptionService.transcribe(
                    extraction.audioBytes(),
                    extraction.audioFileName(),
                    extraction.audioContentType()
                );
                if (StrUtil.isBlank(transcript)) {
                    throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "视频音轨未生成有效转写内容，请确认音频清晰后重试");
                }
                audioNote = transcript;
            } catch (BusinessException ex) {
                log.info("Video audio transcription failed: knowledgeId={}, reason={}",
                    knowledge.getKnowledgeId(), ex.getMsg());
                throw ex;
            }
        }

        List<Media> frameMedia = extraction.frames().stream()
            .map(frame -> AiMediaUtil.buildMedia(frame.bytes(), frame.fileName(), MimeType.valueOf(frame.mimeType())))
            .toList();

        String prompt = String.format(VIDEO_AI_ANALYZE_PROMPT_TEMPLATE,
            knowledge.getName(),
            StrUtil.blankToDefault(knowledge.getMimeType(), "video"),
            StrUtil.blankToDefault(normalizeUserFacingSummary(knowledge.getSummary()), "无"),
            extraction.frames().size(),
            limitAnalyzeSourceText(audioNote));

        String finalTranscript = transcript;
        String finalAudioNote = audioNote;
        return callStructuredAnalyze(knowledge, prompt, frameMedia, result ->
            buildVideoSearchableContent(finalTranscript, finalAudioNote, result));
    }

    private KnowledgeAiAnalyzeVO analyzeTextLikeMediaKnowledge(Knowledge knowledge, String contentText) {
        String summary = StrUtil.blankToDefault(normalizeUserFacingSummary(knowledge.getSummary()), "无");
        String prompt = String.format(AI_ANALYZE_PROMPT_TEMPLATE,
            knowledge.getName(),
            StrUtil.blankToDefault(knowledge.getType(), "document"),
            summary,
            limitAnalyzeSourceText(contentText));
        return callStructuredAnalyze(knowledge, prompt, List.of(), null);
    }

    private KnowledgeAiAnalyzeVO callStructuredAnalyze(Knowledge knowledge, String prompt, List<Media> mediaList,
                                                       java.util.function.Function<KnowledgeAiAnalyzeVO, String> contentTextBuilder) {
        try {
            aiQuotaService.ensureQuotaAvailable("knowledge_analyze", null, null, prompt);
            var promptSpec = chatClientProvider.getChatClient().prompt();
            var callSpec = CollUtil.isNotEmpty(mediaList)
                ? promptSpec.user(user -> user.text(prompt).media(mediaList.toArray(new Media[0])))
                : promptSpec.user(prompt);
            var chatResponse = callSpec.call().chatResponse();
            String response = chatResponse.getResult().getOutput().getText();
            aiQuotaService.consumeResolvedTokens(
                "knowledge_analyze",
                aiQuotaService.resolveTokenUsage(chatResponse, null, null, prompt, response)
            );

            log.info("AI 文档分析原始响应: {}", response);
            KnowledgeAiAnalyzeVO analyzedResult = parseAnalyzeResponse(response, knowledge);
            if (analyzedResult != null
                && StrUtil.isNotBlank(analyzedResult.getCoreHighlights())
                && !isChatAttachmentAutoArchiveSummary(analyzedResult.getCoreHighlights())) {
                persistAnalyzeResult(knowledge, analyzedResult);
                if (contentTextBuilder != null) {
                    persistKnowledgeContentText(knowledge, contentTextBuilder.apply(analyzedResult));
                }
                return analyzedResult;
            }
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "AI 分析结果格式异常，请重新分析");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 文档分析失败", e);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "AI 分析失败，请稍后重试");
        }
    }

    /**
     * 检查当前模型是否支持视觉分析。
     */
    private void ensureVisionSupported(String mediaLabel) {
        AiModelCapabilities capabilities = chatClientProvider.getCurrentCapabilities();
        if (capabilities == null || !capabilities.isSupportsVision()) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                "当前模型不支持" + mediaLabel + "分析，请切换到支持视觉能力的模型后重试");
        }
    }

    private Media buildStoredImageMedia(Knowledge knowledge) {
        try {
            String mimeType = resolveImageMimeType(knowledge.getMimeType(), knowledge.getName());
            return AiMediaUtil.buildMedia(fileStorageService, knowledge.getFilePath(), MimeType.valueOf(mimeType));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to build stored image media: knowledgeId={}, fileName={}",
                knowledge.getKnowledgeId(), knowledge.getName(), ex);
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "图片文件读取失败，请重新上传后重试");
        }
    }

    private String transcribeStoredMedia(Knowledge knowledge) {
        if (StrUtil.isBlank(knowledge.getFilePath())) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "音频文件路径为空，请重新上传后重试");
        }
        try (InputStream inputStream = fileStorageService.getFileStream(knowledge.getFilePath())) {
            if (inputStream == null) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "音频文件无法读取，请重新上传后重试");
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
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "音频文件读取失败，请重新上传后重试");
        }
    }

    private void persistKnowledgeContentText(Knowledge knowledge, String contentText) {
        String normalized = normalizeSearchableContent(contentText);
        if (StrUtil.isBlank(normalized)) {
            return;
        }
        lambdaUpdate()
            .eq(Knowledge::getKnowledgeId, knowledge.getKnowledgeId())
            .set(Knowledge::getContentText, normalized)
            .update();
        knowledge.setContentText(normalized);
        globalSearchIndexService.refreshKnowledgeIndex(knowledge.getKnowledgeId());
    }

    private String limitAnalyzeSourceText(String text) {
        return StrUtil.blankToDefault(abbreviateAnalyzeContent(text), "");
    }

    private String buildAnalyzeResultContent(KnowledgeAiAnalyzeVO result) {
        StringBuilder builder = new StringBuilder();
        if (StrUtil.isNotBlank(result.getCoreHighlights())) {
            builder.append(result.getCoreHighlights()).append('\n');
        }
        if (CollUtil.isNotEmpty(result.getTalkingPoints())) {
            builder.append("跟进建议：");
            for (String point : result.getTalkingPoints()) {
                if (StrUtil.isNotBlank(point)) {
                    builder.append('\n').append("- ").append(point);
                }
            }
            builder.append('\n');
        }
        if (CollUtil.isNotEmpty(result.getRelatedEntities())) {
            builder.append("相关实体：");
            for (KnowledgeAiAnalyzeVO.RelatedEntity entity : result.getRelatedEntities()) {
                if (entity != null && StrUtil.isNotBlank(entity.getName())) {
                    builder.append('\n')
                        .append("- ")
                        .append(entity.getName());
                    if (StrUtil.isNotBlank(entity.getType())) {
                        builder.append(" (").append(entity.getType()).append(')');
                    }
                }
            }
        }
        return builder.toString().trim();
    }

    private String buildVideoSearchableContent(String transcript, String audioNote, KnowledgeAiAnalyzeVO result) {
        StringBuilder builder = new StringBuilder();
        builder.append("视频音轨信息：\n")
            .append(StrUtil.isNotBlank(transcript) ? transcript : StrUtil.blankToDefault(audioNote, "未检测到可用音轨。"))
            .append("\n\n视频画面与综合摘要：\n")
            .append(buildAnalyzeResultContent(result));
        return builder.toString();
    }

    private boolean isImageFile(String mimeType, String fileName) {
        String normalizedMime = normalizeMimeType(mimeType);
        if (StrUtil.isNotBlank(normalizedMime) && normalizedMime.startsWith("image/")) {
            return true;
        }
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
            || lower.endsWith(".jpg")
            || lower.endsWith(".jpeg")
            || lower.endsWith(".gif")
            || lower.endsWith(".webp")
            || lower.endsWith(".bmp")
            || lower.endsWith(".svg")
            || lower.endsWith(".avif");
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
            || lower.endsWith(".3gp")
            || lower.endsWith(".3gpp");
    }

    private String resolveImageMimeType(String mimeType, String fileName) {
        String normalizedMime = normalizeMimeType(mimeType);
        if (StrUtil.isNotBlank(normalizedMime) && normalizedMime.startsWith("image/")) {
            return normalizedMime;
        }
        String lower = StrUtil.blankToDefault(fileName, "").toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".avif")) {
            return "image/avif";
        }
        throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "暂不支持该图片类型分析");
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

    private String resolveAnalyzeContentText(Knowledge knowledge) {
        String contentText = normalizeSearchableContent(knowledge.getContentText());
        if (StrUtil.isBlank(contentText) && StrUtil.isNotBlank(knowledge.getFilePath())) {
            contentText = extractSearchableContent(knowledge.getFilePath(), knowledge.getMimeType(), knowledge.getName());
            if (StrUtil.isNotBlank(contentText)) {
                lambdaUpdate()
                    .eq(Knowledge::getKnowledgeId, knowledge.getKnowledgeId())
                    .set(Knowledge::getContentText, contentText)
                    .update();
                knowledge.setContentText(contentText);
                globalSearchIndexService.refreshKnowledgeIndex(knowledge.getKnowledgeId());
            }
        }

        String abbreviatedContent = abbreviateAnalyzeContent(contentText);
        if (StrUtil.isBlank(abbreviatedContent)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "无法提取可分析的文档正文，请确认附件内容可读取后重试");
        }
        return abbreviatedContent;
    }

    /**
     * 提取 AI 响应中的 JSON 对象。
     */
    private String extractAnalyzeJson(String response) {
        String json = StrUtil.trim(response);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        int fenceStart = json.indexOf("```");
        if (fenceStart >= 0) {
            String fenced = json.substring(fenceStart + 3).trim();
            if (fenced.toLowerCase(Locale.ROOT).startsWith("json")) {
                fenced = fenced.substring(4).trim();
            }
            int fenceEnd = fenced.indexOf("```");
            if (fenceEnd >= 0) {
                json = fenced.substring(0, fenceEnd).trim();
            } else {
                json = fenced;
            }
        }

        int objectStart = json.indexOf('{');
        int objectEnd = json.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return json.substring(objectStart, objectEnd + 1).trim();
        }
        return json;
    }

    /**
     * 解析分析响应。
     */
    private KnowledgeAiAnalyzeVO parseAnalyzeResponse(String response, Knowledge knowledge) {
        try {
            String json = extractAnalyzeJson(response);
            if (StrUtil.isBlank(json)) {
                return null;
            }

            JsonNode root = objectMapper.readTree(json);
            KnowledgeAiAnalyzeVO vo = new KnowledgeAiAnalyzeVO();

            // 核心提炼
            if (root.has("coreHighlights") && !root.get("coreHighlights").isNull()) {
                vo.setCoreHighlights(root.get("coreHighlights").asText());
            } else {
                vo.setCoreHighlights(StrUtil.blankToDefault(normalizeUserFacingSummary(knowledge.getSummary()), ""));
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
            return normalizeAnalyzeResult(vo, knowledge);
        } catch (Exception e) {
            log.warn("AI 文档分析 JSON 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 读取Cached分析结果。
     */
    private KnowledgeAiAnalyzeVO readCachedAnalyzeResult(Knowledge knowledge) {
        String snapshot = StrUtil.trimToNull(knowledge.getAiAnalysisSnapshot());
        if (snapshot == null) {
            return null;
        }

        try {
            KnowledgeAiAnalyzeVO cached = objectMapper.readValue(snapshot, KnowledgeAiAnalyzeVO.class);
            KnowledgeAiAnalyzeVO normalized = normalizeAnalyzeResult(cached, knowledge);
            if (isChatAttachmentAutoArchiveSummary(normalized.getCoreHighlights())
                || (StrUtil.isBlank(normalized.getCoreHighlights())
                    && CollUtil.isEmpty(normalized.getTalkingPoints())
                    && CollUtil.isEmpty(normalized.getRelatedEntities()))) {
                return null;
            }
            return normalized;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析知识库缓存分析结果失败: knowledgeId={}, error={}",
                knowledge.getKnowledgeId(), e.getMessage());
            return null;
        }
    }

    /**
     * 标准化分析结果。
     */
    private KnowledgeAiAnalyzeVO normalizeAnalyzeResult(KnowledgeAiAnalyzeVO source, Knowledge knowledge) {
        KnowledgeAiAnalyzeVO normalized = new KnowledgeAiAnalyzeVO();
        normalized.setCoreHighlights(StrUtil.blankToDefault(normalizeUserFacingSummary(source.getCoreHighlights()),
            StrUtil.blankToDefault(normalizeUserFacingSummary(knowledge.getSummary()), "")));

        List<String> talkingPoints = new ArrayList<>();
        if (source.getTalkingPoints() != null) {
            source.getTalkingPoints().forEach(point -> {
                String normalizedPoint = StrUtil.trim(point);
                if (StrUtil.isNotBlank(normalizedPoint)) {
                    talkingPoints.add(normalizedPoint);
                }
            });
        }
        normalized.setTalkingPoints(talkingPoints);

        List<KnowledgeAiAnalyzeVO.RelatedEntity> entities = new ArrayList<>();
        if (source.getRelatedEntities() != null) {
            source.getRelatedEntities().forEach(item -> {
                if (item == null) {
                    return;
                }
                String name = StrUtil.trim(item.getName());
                if (StrUtil.isBlank(name)) {
                    return;
                }

                KnowledgeAiAnalyzeVO.RelatedEntity entity = new KnowledgeAiAnalyzeVO.RelatedEntity();
                entity.setName(name);
                entity.setType(StrUtil.trim(item.getType()));
                entities.add(entity);
            });
        }
        normalized.setRelatedEntities(entities);
        return normalized;
    }

    /**
     * 处理persistAnalyzeResult方法逻辑。
     */
    private void persistAnalyzeResult(Knowledge knowledge, KnowledgeAiAnalyzeVO analyzeResult) {
        String snapshot;
        try {
            snapshot = objectMapper.writeValueAsString(analyzeResult);
        } catch (Exception e) {
            log.warn("序列化知识库分析结果失败: knowledgeId={}, error={}",
                knowledge.getKnowledgeId(), e.getMessage());
            return;
        }

        String previousSummary = normalizeUserFacingSummary(knowledge.getSummary());
        String normalizedSummary = normalizeUserFacingSummary(analyzeResult.getCoreHighlights());
        Date analysisTime = new Date();

        lambdaUpdate()
            .eq(Knowledge::getKnowledgeId, knowledge.getKnowledgeId())
            .set(Knowledge::getAiAnalysisSnapshot, snapshot)
            .set(Knowledge::getAiAnalysisTime, analysisTime)
            .set(StrUtil.isNotBlank(normalizedSummary), Knowledge::getSummary, normalizedSummary)
            .update();

        knowledge.setAiAnalysisSnapshot(snapshot);
        knowledge.setAiAnalysisTime(analysisTime);
        if (StrUtil.isNotBlank(normalizedSummary)) {
            knowledge.setSummary(normalizedSummary);
        }

        if (StrUtil.isNotBlank(normalizedSummary) && !StrUtil.equals(previousSummary, normalizedSummary)) {
            globalSearchIndexService.refreshKnowledgeIndex(knowledge.getKnowledgeId());
        }
    }

    /**
     * 过滤只供系统内部使用的摘要。
     */
    private String normalizeUserFacingSummary(String summary) {
        String normalized = StrUtil.trim(summary);
        if (isChatAttachmentAutoArchiveSummary(normalized)) {
            return null;
        }
        return normalized;
    }

    /**
     * 判断是否为客户聊天附件自动归档生成的内部说明。
     */
    private boolean isChatAttachmentAutoArchiveSummary(String summary) {
        String normalized = StrUtil.trim(summary);
        return StrUtil.isNotBlank(normalized)
            && normalized.startsWith(CHAT_ATTACHMENT_AUTO_ARCHIVE_SUMMARY_PREFIX);
    }

    /**
     * 截断用于 AI 分析的正文。
     */
    private String abbreviateAnalyzeContent(String contentText) {
        String normalized = normalizeSearchableContent(contentText);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        return normalized.length() > MAX_AI_ANALYZE_CONTENT_LENGTH
            ? normalized.substring(0, MAX_AI_ANALYZE_CONTENT_LENGTH) + "..."
            : normalized;
    }

    /**
     * 发起问答文档Question。
     */
    @Override
    public Flux<String> askDocumentQuestion(Long knowledgeId, KnowledgeAskBO askBO) {
        Knowledge knowledge = getById(knowledgeId);
        if (ObjectUtil.isNull(knowledge)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "知识库文件不存在");
        }

        // 设置 AI 上下文
        Long currentUserId = UserUtil.getUserIdOrNull();
        Long currentTenantId = UserUtil.getTenantId();
        if (currentUserId != null) {
            AiContextHolder.setContext(knowledgeId, currentUserId, currentTenantId);
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
        String quotaTip = aiQuotaService.resolveQuotaFailureMessage(
            currentTenantId, "knowledge_ask", systemPrompt, history, askBO.getQuestion()
        );
        if (quotaTip != null) {
            AiContextHolder.clear();
            return Flux.just(quotaTip);
        }

        StringBuilder fullResponse = new StringBuilder();

        return chatClientProvider.getChatClient()
                .prompt()
                .system(systemPrompt)
                .messages(history)
                .user(askBO.getQuestion())
                .stream()
                .chatResponse()
                .doOnNext(chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        String text = chatResponse.getResult().getOutput().getText();
                        if (text != null) {
                            fullResponse.append(text);
                        }
                    }
                })
                .mapNotNull(chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                        return chatResponse.getResult().getOutput().getText();
                    }
                    return null;
                })
                .doOnComplete(() -> {
                    aiQuotaService.consumeEstimatedTokens(
                        currentTenantId,
                        "knowledge_ask",
                        systemPrompt + "\n\n" + askBO.getQuestion(),
                        fullResponse.toString()
                    );
                    AiContextHolder.clear();
                })
                .doOnError(error -> {
                    log.error("文档问答错误: {}", error.getMessage(), error);
                    AiContextHolder.clear();
                });
    }

    /**
     * 构建SemanticReferences。
     */
    private List<KnowledgeAiSearchVO.ReferenceItem> buildSemanticReferences(String keyword, String type, int limit) {
        if (!weKnoraClient.isEnabled()) {
            return List.of();
        }

        Long tenantId = UserUtil.getTenantId();
        if (tenantId == null) {
            return List.of();
        }

        try {
            WeKnoraClient.TenantWeKnoraContext ctx = weKnoraClient.getOrCreateTenantContext(tenantId);
            List<WeKnoraChunk> chunks = weKnoraClient.searchKnowledge(keyword, ctx.getKnowledgeBaseId(), ctx.getApiKey());
            if (chunks.isEmpty()) {
                return List.of();
            }

            Set<String> weKnoraIds = new LinkedHashSet<>();
            for (WeKnoraChunk chunk : chunks) {
                if (StrUtil.isNotBlank(chunk.getKnowledgeId())) {
                    weKnoraIds.add(chunk.getKnowledgeId());
                }
            }
            if (weKnoraIds.isEmpty()) {
                return List.of();
            }

            LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<Knowledge>()
                .in(Knowledge::getWeKnoraKnowledgeId, weKnoraIds)
                .ne(Knowledge::getStatus, 2);
            if (StrUtil.isNotBlank(type)) {
                wrapper.eq(Knowledge::getType, type);
            }

            List<Knowledge> localKnowledges = list(wrapper);
            if (localKnowledges.isEmpty()) {
                return List.of();
            }

            Map<String, Knowledge> knowledgeByWeKnoraId = new LinkedHashMap<>();
            for (Knowledge knowledge : localKnowledges) {
                if (StrUtil.isNotBlank(knowledge.getWeKnoraKnowledgeId())) {
                    knowledgeByWeKnoraId.put(knowledge.getWeKnoraKnowledgeId(), knowledge);
                }
            }

            Map<Long, String> customerNameMap = loadCustomerNameMap(localKnowledges);
            LinkedHashMap<Long, KnowledgeAiSearchVO.ReferenceItem> deduped = new LinkedHashMap<>();
            for (WeKnoraChunk chunk : chunks) {
                Knowledge knowledge = knowledgeByWeKnoraId.get(chunk.getKnowledgeId());
                if (knowledge == null) {
                    continue;
                }

                KnowledgeAiSearchVO.ReferenceItem item = deduped.computeIfAbsent(
                    knowledge.getKnowledgeId(),
                    ignored -> toReferenceItem(knowledge, customerNameMap.get(knowledge.getCustomerId()))
                );
                item.setMatchPercent(Math.max(item.getMatchPercent(), toMatchPercent(chunk.getScore())));
                if (StrUtil.isBlank(item.getExcerpt())) {
                    item.setExcerpt(extractSnippet(chunk.getContent(), keyword));
                }
            }

            return deduped.values().stream()
                .sorted(Comparator.comparingInt(KnowledgeAiSearchVO.ReferenceItem::getMatchPercent).reversed()
                    .thenComparing(KnowledgeAiSearchVO.ReferenceItem::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
        } catch (Exception e) {
            log.warn("知识库语义检索失败，回退到本地内容搜索: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 发起问答知识基础Question。
     */
    private WeKnoraClient.WeKnoraChatResult askKnowledgeBaseQuestion(String keyword, String type) {
        if (!weKnoraClient.isEnabled()) {
            return null;
        }

        Long tenantId = UserUtil.getTenantId();
        if (tenantId == null) {
            return null;
        }

        List<String> scopedKnowledgeIds = resolveAiSearchScopedKnowledgeIds(type);
        if (StrUtil.isNotBlank(type) && scopedKnowledgeIds == null) {
            return null;
        }

        try {
            aiQuotaService.ensureQuotaAvailable(tenantId, "knowledge_search_answer", null, null, keyword);
            WeKnoraClient.WeKnoraChatResult result = weKnoraClient.askKnowledgeQuestion(
                tenantId,
                null,
                keyword,
                scopedKnowledgeIds == null ? List.of() : scopedKnowledgeIds
            );
            aiQuotaService.consumeEstimatedTokens(
                tenantId,
                "knowledge_search_answer",
                keyword,
                result != null ? result.getAnswer() : null
            );
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("鐭ヨ瘑搴?AI 妫€绱㈢洿鎺ラ棶绛斿け璐ワ紝鍥為€€鍒版绱㈡憳瑕? keyword={}, type={}, error={}",
                keyword, type, e.getMessage());
            return null;
        }
    }

    /**
     * 解析AI搜索范围内知识ID。
     */
    private List<String> resolveAiSearchScopedKnowledgeIds(String type) {
        if (StrUtil.isBlank(type)) {
            return List.of();
        }

        List<String> scopedKnowledgeIds = list(new LambdaQueryWrapper<Knowledge>()
            .select(Knowledge::getWeKnoraKnowledgeId)
            .eq(Knowledge::getType, type)
            .ne(Knowledge::getStatus, 2)
            .isNotNull(Knowledge::getWeKnoraKnowledgeId))
            .stream()
            .map(Knowledge::getWeKnoraKnowledgeId)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .toList();

        if (scopedKnowledgeIds.isEmpty()) {
            return null;
        }
        if (scopedKnowledgeIds.size() > MAX_AI_SEARCH_RAG_SCOPE_SIZE) {
            log.info("鐭ヨ瘑搴?AI 妫€绱㈣烦杩囩被鍨嬮檺瀹氶棶绛旓紝鑼冨洿杩囧ぇ: type={}, count={}",
                type, scopedKnowledgeIds.size());
            return null;
        }
        return scopedKnowledgeIds;
    }

    /**
     * 构建ReferencesChunks。
     */
    private List<KnowledgeAiSearchVO.ReferenceItem> buildReferencesFromChunks(List<WeKnoraChunk> chunks,
                                                                              String type,
                                                                              int limit) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        Set<String> weKnoraIds = new LinkedHashSet<>();
        for (WeKnoraChunk chunk : chunks) {
            if (StrUtil.isNotBlank(chunk.getKnowledgeId())) {
                weKnoraIds.add(chunk.getKnowledgeId());
            }
        }
        if (weKnoraIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<Knowledge>()
            .in(Knowledge::getWeKnoraKnowledgeId, weKnoraIds)
            .ne(Knowledge::getStatus, 2);
        if (StrUtil.isNotBlank(type)) {
            wrapper.eq(Knowledge::getType, type);
        }

        List<Knowledge> localKnowledges = list(wrapper);
        if (localKnowledges.isEmpty()) {
            return List.of();
        }

        Map<String, Knowledge> knowledgeByWeKnoraId = new LinkedHashMap<>();
        for (Knowledge knowledge : localKnowledges) {
            if (StrUtil.isNotBlank(knowledge.getWeKnoraKnowledgeId())) {
                knowledgeByWeKnoraId.put(knowledge.getWeKnoraKnowledgeId(), knowledge);
            }
        }

        Map<Long, String> customerNameMap = loadCustomerNameMap(localKnowledges);
        LinkedHashMap<Long, KnowledgeAiSearchVO.ReferenceItem> deduped = new LinkedHashMap<>();
        for (WeKnoraChunk chunk : chunks) {
            Knowledge knowledge = knowledgeByWeKnoraId.get(chunk.getKnowledgeId());
            if (knowledge == null) {
                continue;
            }

            KnowledgeAiSearchVO.ReferenceItem item = deduped.computeIfAbsent(
                knowledge.getKnowledgeId(),
                ignored -> toReferenceItem(knowledge, customerNameMap.get(knowledge.getCustomerId()))
            );
            item.setMatchPercent(Math.max(item.getMatchPercent(), toMatchPercent(chunk.getScore())));

            String chunkExcerpt = extractSnippet(chunk.getContent(), null);
            if (StrUtil.isNotBlank(chunkExcerpt)) {
                item.setExcerpt(chunkExcerpt);
            }
        }

        return deduped.values().stream()
            .sorted(Comparator.comparingInt(KnowledgeAiSearchVO.ReferenceItem::getMatchPercent).reversed()
                .thenComparing(KnowledgeAiSearchVO.ReferenceItem::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(limit)
            .toList();
    }

    /**
     * 判断是否UnavailableRAG答案。
     */
    private boolean isUnavailableRagAnswer(String answer) {
        return StrUtil.isBlank(answer) || answer.contains("NO_MATCH");
    }

    /**
     * 构建本地References。
     */
    private List<KnowledgeAiSearchVO.ReferenceItem> buildLocalReferences(String keyword, String type, int limit) {
        KnowledgeQueryBO queryBO = new KnowledgeQueryBO();
        queryBO.setKeyword(keyword);
        queryBO.setType(type);
        queryBO.setPage(1);
        queryBO.setLimit(limit);

        BasePage<KnowledgeVO> page = queryPageList(queryBO);
        List<KnowledgeAiSearchVO.ReferenceItem> references = new ArrayList<>();
        for (KnowledgeVO knowledge : page.getList()) {
            KnowledgeAiSearchVO.ReferenceItem item = new KnowledgeAiSearchVO.ReferenceItem();
            item.setKnowledgeId(knowledge.getKnowledgeId());
            item.setName(knowledge.getName());
            item.setType(knowledge.getType());
            item.setCustomerName(knowledge.getCustomerName());
            item.setSummary(knowledge.getSummary());
            item.setExcerpt(extractSnippet(knowledge.getContentText(), keyword));
            item.setMatchPercent(calculateLocalMatchPercent(keyword, knowledge));
            item.setFileSize(knowledge.getFileSize());
            item.setCreateTime(knowledge.getCreateTime());
            references.add(item);
        }
        return references;
    }

    /**
     * 构建AI搜索答案。
     */
    private String buildAiSearchAnswer(String keyword, List<KnowledgeAiSearchVO.ReferenceItem> references) {
        if (references.isEmpty()) {
            return "### 未找到相关内容\n\n当前知识库里还没有与“" + keyword + "”直接相关的资料，可以换个关键词再试试。";
        }

        String fallback = buildFallbackAiAnswer(keyword, references);
        if (!chatClientProvider.isApiKeyConfigured()) {
            return fallback;
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < references.size(); i++) {
            KnowledgeAiSearchVO.ReferenceItem item = references.get(i);
            contextBuilder.append("资料").append(i + 1).append("：").append(item.getName()).append("\n");
            if (StrUtil.isNotBlank(item.getSummary())) {
                contextBuilder.append("摘要：").append(item.getSummary()).append("\n");
            }
            if (StrUtil.isNotBlank(item.getExcerpt())) {
                contextBuilder.append("命中片段：").append(item.getExcerpt()).append("\n");
            }
            contextBuilder.append("\n");
        }

        String prompt = """
            你是企业知识库搜索助手。请基于以下检索到的资料，用中文输出一段简洁、专业、可直接展示给业务用户的答案。

            要求：
            1. 只基于提供的资料回答，不要编造。
            2. 输出使用 Markdown。
            3. 先给结论，再分点列出关键依据。
            4. 如果资料不足以得出确定结论，要明确说明信息不足。
            5. 不要输出“根据资料1/资料2”这类生硬措辞，改成自然表达。
            6. 所有标题、分节名和标签都必须使用中文，不要出现 Summary、Retrieved Information、Key Points、References 等英文标题。

            用户问题：
            %s

            检索资料：
            %s
            """.formatted(keyword, contextBuilder);

        try {
            aiQuotaService.ensureQuotaAvailable("knowledge_search_answer", null, null, prompt);
            var chatResponse = chatClientProvider.getChatClient()
                .prompt()
                .user(prompt)
                .call()
                .chatResponse();
            String answer = chatResponse.getResult().getOutput().getText();
            aiQuotaService.consumeResolvedTokens(
                "knowledge_search_answer",
                aiQuotaService.resolveTokenUsage(chatResponse, null, null, prompt, answer)
            );
            return StrUtil.isNotBlank(answer)
                ? KnowledgeAnswerLocalizationUtil.localizeToChinese(answer)
                : fallback;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("生成知识库 AI 检索答案失败，使用兜底摘要: {}", e.getMessage());
            return fallback;
        }
    }

    /**
     * 构建兜底AI答案。
     */
    private String buildFallbackAiAnswer(String keyword, List<KnowledgeAiSearchVO.ReferenceItem> references) {
        StringBuilder answer = new StringBuilder();
        answer.append("### “").append(keyword).append("”相关知识解答\n\n");
        KnowledgeAiSearchVO.ReferenceItem top = references.getFirst();
        if (StrUtil.isNotBlank(top.getSummary())) {
            answer.append(top.getSummary()).append("\n\n");
        } else if (StrUtil.isNotBlank(top.getExcerpt())) {
            answer.append(top.getExcerpt()).append("\n\n");
        } else {
            answer.append("已找到与该主题相关的知识资料，建议优先查看下方参考文档。\n\n");
        }

        answer.append("#### 关键信息\n");
        for (KnowledgeAiSearchVO.ReferenceItem item : references.stream().limit(3).toList()) {
            answer.append("- **").append(item.getName()).append("**");
            if (StrUtil.isNotBlank(item.getExcerpt())) {
                answer.append("：").append(item.getExcerpt());
            } else if (StrUtil.isNotBlank(item.getSummary())) {
                answer.append("：").append(item.getSummary());
            }
            answer.append("\n");
        }
        return answer.toString().trim();
    }

    /**
     * 构建TargetedScriptPrompt。
     */
    private String buildTargetedScriptPrompt(CustomerDetailVO customerDetail,
                                             List<FollowUpVO> recentFollowUps,
                                             List<Knowledge> knowledges) {
        return TARGETED_SCRIPT_PROMPT_TEMPLATE.formatted(
            buildTargetedCustomerContext(customerDetail, recentFollowUps),
            buildTargetedKnowledgeContext(knowledges)
        );
    }

    /**
     * 构建Targeted客户上下文。
     */
    private String buildTargetedCustomerContext(CustomerDetailVO detail, List<FollowUpVO> recentFollowUps) {
        StringBuilder builder = new StringBuilder();
        builder.append("公司名称: ").append(StrUtil.blankToDefault(detail.getCompanyName(), "未提供")).append('\n');
        builder.append("行业: ").append(StrUtil.blankToDefault(detail.getIndustry(), "未提供")).append('\n');
        builder.append("商机阶段: ").append(StrUtil.blankToDefault(detail.getStageName(), detail.getStage())).append('\n');
        builder.append("客户级别: ").append(StrUtil.blankToDefault(detail.getLevel(), "未提供")).append('\n');
        builder.append("客户来源: ").append(StrUtil.blankToDefault(detail.getSource(), "未提供")).append('\n');
        builder.append("最后联系时间: ").append(formatDateTime(detail.getLastContactTime())).append('\n');
        builder.append("下次跟进时间: ").append(formatDateTime(detail.getNextFollowTime())).append('\n');
        builder.append("AI 状态判断: ").append(StrUtil.blankToDefault(detail.getAiStatusDetection(), "未提供")).append('\n');
        builder.append("AI 洞察: ").append(StrUtil.blankToDefault(detail.getAiInsight(), "未提供")).append('\n');
        builder.append("备注: ").append(StrUtil.blankToDefault(detail.getRemark(), "未提供")).append('\n');
        builder.append("主联系人: ").append(resolvePrimaryContactSummary(detail.getContacts())).append('\n');
        builder.append("最近跟进记录: ").append(formatFollowUpSummary(recentFollowUps)).append('\n');
        builder.append("近期任务: ").append(formatTaskSummary(detail)).append('\n');
        return builder.toString().trim();
    }

    /**
     * 构建Targeted知识上下文。
     */
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

    /**
     * 处理extractTargetedScriptSnippet方法逻辑。
     */
    private String extractTargetedScriptSnippet(Knowledge knowledge) {
        String content = normalizeSearchableContent(knowledge.getContentText());
        if (StrUtil.isBlank(content)) {
            return StrUtil.blankToDefault(knowledge.getSummary(), "暂无可用正文片段");
        }
        return content.length() > MAX_TARGETED_SCRIPT_DOC_CONTENT_LENGTH
            ? content.substring(0, MAX_TARGETED_SCRIPT_DOC_CONTENT_LENGTH) + "..."
            : content;
    }

    /**
     * 构建TargetedScript兜底。
     */
    private String buildTargetedScriptFallback(CustomerDetailVO detail,
                                               List<FollowUpVO> recentFollowUps,
                                               List<Knowledge> knowledges) {
        String customerName = StrUtil.blankToDefault(detail.getCompanyName(), "该客户");
        String industry = StrUtil.blankToDefault(detail.getIndustry(), "所在行业");
        String stage = StrUtil.blankToDefault(detail.getStageName(), StrUtil.blankToDefault(detail.getStage(), "当前阶段"));
        String openingHook = firstNonBlank(
            detail.getAiInsight(),
            detail.getRemark(),
            recentFollowUps == null || recentFollowUps.isEmpty() ? null : recentFollowUps.getFirst().getContent()
        );
        List<String> evidenceLines = new ArrayList<>();
        for (Knowledge knowledge : knowledges) {
            evidenceLines.add("- **%s**：%s".formatted(
                StrUtil.blankToDefault(knowledge.getName(), "未命名文档"),
                StrUtil.blankToDefault(firstNonBlank(knowledge.getSummary(), extractTargetedScriptSnippet(knowledge)), "暂无摘要")
            ));
        }

        StringBuilder builder = new StringBuilder();
        builder.append("## 客户判断\n\n");
        builder.append(customerName).append("当前处于**").append(stage).append("**阶段，所属行业为**").append(industry)
            .append("**。建议围绕客户业务目标、当前推进节点和文档中的关键价值点展开沟通，避免直接堆砌产品能力。\n\n");

        builder.append("## 参考依据\n\n");
        evidenceLines.forEach(line -> builder.append(line).append('\n'));
        builder.append('\n');

        builder.append("### 1. 开场白（建立连接与专业度）\n\n");
        builder.append("> 您好，我这边结合贵司目前的业务推进情况和我们整理的参考资料，特别关注到")
            .append(StrUtil.blankToDefault(openingHook, "当前项目推进效率与服务协同的提升空间"))
            .append("。今天想先和您确认一下，现阶段最希望优先解决的是流程效率、协同响应，还是业务增长相关的问题？\n\n");

        builder.append("### 2. 需求探查（识别真实诉求）\n\n");
        builder.append("> 从目前信息看，贵司已经进入").append(stage)
            .append("阶段。为了让后续方案更贴合实际，我想先确认三个点：第一，当前内部最需要优化的业务环节是什么；第二，项目推进时最担心的风险点是什么；第三，决策时更看重上线速度、落地效果还是整体投入产出？\n\n");

        builder.append("### 3. 价值呈现（把方案与客户场景绑定）\n\n");
        builder.append("> 我们这次不是单纯介绍产品功能，而是希望结合您当前的业务目标，把文档里提到的关键策略和落地经验，转成更适合贵司现阶段的推进方案。这样既能缩短内部沟通成本，也能让后续决策更聚焦在实际收益上。\n\n");

        builder.append("### 4. 异议处理（提前化解顾虑）\n\n");
        builder.append("> 如果您担心实施复杂度、投入产出或内部协同成本，我们建议先从最核心、最容易验证价值的环节切入，先跑通一个小范围场景，再逐步扩展。这样既能控制风险，也更方便内部形成共识。\n\n");

        builder.append("### 5. 收口推进（推动下一步）\n\n");
        builder.append("> 如果方向上您认可，我们可以下一步一起把关键需求、当前流程痛点和预期目标梳理清楚，再输出一版更贴合贵司实际情况的推进建议，帮助您内部更快评估和决策。\n\n");

        builder.append("## SOP 建议\n\n");
        builder.append("1. 通话或拜访前先统一内部口径，明确本次沟通要验证的 2-3 个核心问题。\n");
        builder.append("2. 结合参考文档中的重点信息，准备与客户场景直接相关的案例、政策或产品价值表达。\n");
        builder.append("3. 会中优先确认客户当前阶段、推进阻力、关键决策人和时间节点，再展开方案说明。\n");
        builder.append("4. 若客户表达顾虑，先复述问题并给出可落地的分阶段方案，不急于一次性覆盖全部诉求。\n");
        builder.append("5. 会后 24 小时内发送本次沟通纪要，明确下一步动作、责任人和时间安排。\n\n");

        builder.append("## 风险提醒\n\n");
        builder.append("- 若客户内部目标和决策链尚不清晰，建议先补齐关键信息，再推进正式方案。\n");
        builder.append("- 当前参考资料更多提供方向性支撑，具体预算、周期和交付边界仍需进一步确认。\n");
        builder.append("- 如果最近跟进已中断较久，建议先用低压沟通方式重新建立互动，再推进深入交流。\n");

        return builder.toString().trim();
    }

    /**
     * 解析主联系人摘要。
     */
    private String resolvePrimaryContactSummary(List<ContactVO> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return "未提供";
        }
        ContactVO primary = contacts.stream()
            .filter(contact -> contact != null && Integer.valueOf(1).equals(contact.getIsPrimary()))
            .findFirst()
            .orElse(contacts.getFirst());
        if (primary == null) {
            return "未提供";
        }
        StringBuilder builder = new StringBuilder(StrUtil.blankToDefault(primary.getName(), "未命名联系人"));
        if (StrUtil.isNotBlank(primary.getPosition())) {
            builder.append(" / ").append(primary.getPosition());
        }
        if (StrUtil.isNotBlank(primary.getPhone())) {
            builder.append(" / ").append(primary.getPhone());
        }
        return builder.toString();
    }

    /**
     * 格式化跟进摘要。
     */
    private String formatFollowUpSummary(List<FollowUpVO> recentFollowUps) {
        if (recentFollowUps == null || recentFollowUps.isEmpty()) {
            return "近期暂无有效跟进记录";
        }
        List<String> summaries = new ArrayList<>();
        for (FollowUpVO followUp : recentFollowUps) {
            if (followUp == null) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(formatDateTime(followUp.getFollowTime())).append(" ");
            builder.append(StrUtil.blankToDefault(followUp.getTypeName(), StrUtil.blankToDefault(followUp.getType(), "跟进")));
            builder.append("：");
            builder.append(StrUtil.blankToDefault(firstNonBlank(followUp.getSummary(), followUp.getContent()), "暂无内容"));
            summaries.add(builder.toString());
        }
        return String.join("；", summaries);
    }

    /**
     * 格式化任务摘要。
     */
    private String formatTaskSummary(CustomerDetailVO detail) {
        if (detail.getTasks() == null || detail.getTasks().isEmpty()) {
            return "暂无待跟进任务";
        }
        return detail.getTasks().stream()
            .limit(3)
            .map(task -> {
                StringBuilder builder = new StringBuilder(StrUtil.blankToDefault(task.getTitle(), "未命名任务"));
                if (StrUtil.isNotBlank(task.getStatus())) {
                    builder.append("（").append(task.getStatus()).append("）");
                }
                return builder.toString();
            })
            .reduce((left, right) -> left + "；" + right)
            .orElse("暂无待跟进任务");
    }

    /**
     * 处理firstNonBlank方法逻辑。
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 格式化日期时间。
     */
    private String formatDateTime(java.util.Date date) {
        return date == null ? "未提供" : DateUtil.formatDateTime(date);
    }

    /**
     * 加载客户名称MAP。
     */
    private Map<Long, String> loadCustomerNameMap(List<Knowledge> knowledges) {
        Set<Long> customerIds = new LinkedHashSet<>();
        for (Knowledge knowledge : knowledges) {
            if (knowledge.getCustomerId() != null) {
                customerIds.add(knowledge.getCustomerId());
            }
        }
        if (customerIds.isEmpty()) {
            return new LinkedHashMap<>();
        }

        List<Customer> customers = customerMapper.selectBatchIds(customerIds);
        Map<Long, String> customerNameMap = new LinkedHashMap<>();
        for (Customer customer : customers) {
            customerNameMap.put(customer.getCustomerId(), customer.getCompanyName());
        }
        return customerNameMap;
    }

    /**
     * 转换为ReferenceItem。
     */
    private KnowledgeAiSearchVO.ReferenceItem toReferenceItem(Knowledge knowledge, String customerName) {
        KnowledgeAiSearchVO.ReferenceItem item = new KnowledgeAiSearchVO.ReferenceItem();
        item.setKnowledgeId(knowledge.getKnowledgeId());
        item.setName(knowledge.getName());
        item.setType(knowledge.getType());
        item.setCustomerName(customerName);
        item.setSummary(knowledge.getSummary());
        item.setExcerpt(extractSnippet(knowledge.getContentText(), null));
        item.setMatchPercent(0);
        item.setFileSize(knowledge.getFileSize());
        item.setCreateTime(knowledge.getCreateTime());
        return item;
    }

    /**
     * 处理calculateOverallMatchPercent方法逻辑。
     */
    private int calculateOverallMatchPercent(List<KnowledgeAiSearchVO.ReferenceItem> references) {
        if (references.isEmpty()) {
            return 0;
        }
        return references.stream()
            .map(KnowledgeAiSearchVO.ReferenceItem::getMatchPercent)
            .max(Integer::compareTo)
            .orElse(0);
    }

    /**
     * 处理calculateLocalMatchPercent方法逻辑。
     */
    private int calculateLocalMatchPercent(String keyword, KnowledgeVO knowledge) {
        int score = 55;
        if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(knowledge.getName(), ""), keyword)) {
            score += 20;
        }
        if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(knowledge.getSummary(), ""), keyword)) {
            score += 12;
        }
        if (StrUtil.containsIgnoreCase(StrUtil.blankToDefault(knowledge.getContentText(), ""), keyword)) {
            score += 18;
        }
        return Math.min(score, 98);
    }

    /**
     * 转换为MatchPercent。
     */
    private int toMatchPercent(Double score) {
        if (score == null) {
            return 86;
        }
        if (score <= 1) {
            return Math.max(1, Math.min(99, (int) Math.round(score * 100)));
        }
        return Math.max(1, Math.min(99, (int) Math.round(score)));
    }

    /**
     * 处理extractSnippet方法逻辑。
     */
    private String extractSnippet(String content, String keyword) {
        String normalized = normalizeSearchableContent(content);
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        if (keyword != null) {
            int index = normalized.toLowerCase().indexOf(keyword.toLowerCase());
            if (index >= 0) {
                int start = Math.max(0, index - 60);
                int end = Math.min(normalized.length(), index + keyword.length() + 120);
                String snippet = normalized.substring(start, end).trim();
                if (start > 0) {
                    snippet = "..." + snippet;
                }
                if (end < normalized.length()) {
                    snippet = snippet + "...";
                }
                return snippet;
            }
        }
        return normalized.length() > MAX_AI_SEARCH_CONTEXT_LENGTH
            ? normalized.substring(0, MAX_AI_SEARCH_CONTEXT_LENGTH) + "..."
            : normalized;
    }

    /**
     * 处理extractSearchableContent方法逻辑。
     */
    private String extractSearchableContent(MultipartFile file) {
        if (file == null) {
            return null;
        }
        try (InputStream inputStream = file.getInputStream()) {
            return extractSearchableContent(inputStream, file.getContentType(), file.getOriginalFilename());
        } catch (Exception e) {
            log.warn("提取知识库文件正文失败: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * 处理extractSearchableContent方法逻辑。
     */
    private String extractSearchableContent(String filePath, String mimeType, String fileName) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
            return extractSearchableContent(inputStream, mimeType, fileName);
        } catch (Exception e) {
            log.warn("提取知识库文件正文失败: {}", fileName, e);
            return null;
        }
    }

    /**
     * 处理extractSearchableContent方法逻辑。
     */
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

        return normalizeSearchableContent(text);
    }

    /**
     * 标准化Searchable内容。
     */
    private String normalizeSearchableContent(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String sanitized = removeUnsupportedTextCharacters(text);
        if (StrUtil.isBlank(sanitized)) {
            return null;
        }
        String normalized = sanitized.replaceAll("\\s+", " ").trim();
        if (normalized.length() > MAX_SEARCHABLE_CONTENT_LENGTH) {
            normalized = normalized.substring(0, MAX_SEARCHABLE_CONTENT_LENGTH);
        }
        return normalized.isBlank() ? null : normalized;
    }

    /**
     * 移除Unsupported文本Characters。
     */
    private String removeUnsupportedTextCharacters(String text) {
        StringBuilder sanitized = new StringBuilder(text.length());
        boolean modified = false;
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '\u0000') {
                modified = true;
                continue;
            }
            if (Character.isISOControl(current) && !Character.isWhitespace(current)) {
                modified = true;
                continue;
            }
            sanitized.append(current);
        }
        return modified ? sanitized.toString() : text;
    }

    /**
     * 判断是否Plain文本文件。
     */
    private boolean isPlainTextFile(String mimeType, String fileName) {
        if (mimeType != null && (mimeType.startsWith("text/") || "application/json".equals(mimeType))) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt")
            || lower.endsWith(".md")
            || lower.endsWith(".markdown")
            || lower.endsWith(".csv")
            || lower.endsWith(".json")
            || lower.endsWith(".xml")
            || lower.endsWith(".yaml")
            || lower.endsWith(".yml")
            || lower.endsWith(".log");
    }

    /**
     * 判断是否文档文件。
     */
    private boolean isDocumentFile(String mimeType, String fileName) {
        if (mimeType != null) {
            if ("application/pdf".equals(mimeType)
                || "application/msword".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)
                || "application/vnd.ms-excel".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(mimeType)
                || "application/vnd.ms-powerpoint".equals(mimeType)
                || "application/vnd.openxmlformats-officedocument.presentationml.presentation".equals(mimeType)) {
                return true;
            }
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf")
            || lower.endsWith(".doc")
            || lower.endsWith(".docx")
            || lower.endsWith(".xls")
            || lower.endsWith(".xlsx")
            || lower.endsWith(".ppt")
            || lower.endsWith(".pptx");
    }
}
