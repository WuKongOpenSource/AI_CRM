package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.ai.state.PendingCustomerCreationStore;
import com.kakarote.ai_crm.ai.tools.KnowledgeTools;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.SessionCreateBO;
import com.kakarote.ai_crm.entity.PO.ChatAttachment;
import com.kakarote.ai_crm.entity.PO.ChatMessage;
import com.kakarote.ai_crm.entity.PO.ChatSession;
import com.kakarote.ai_crm.entity.VO.ChatMessageVO;
import com.kakarote.ai_crm.entity.VO.ChatSessionVO;
import com.kakarote.ai_crm.mapper.ChatMessageMapper;
import com.kakarote.ai_crm.mapper.ChatSessionMapper;
import com.kakarote.ai_crm.service.*;
import com.kakarote.ai_crm.utils.AiMediaUtil;
import com.kakarote.ai_crm.utils.DocumentTextExtractor;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * AI聊天服务实现 - 使用Spring AI ChatClient。
 * 负责串起消息落库、附件补充、RAG 路由、流式/非流式响应以及 AI 上下文清理，保证工具调用阶段仍能拿到正确的用户与租户信息。
 */
@Slf4j
@Service
public class ChatServiceImpl implements IChatService {

    @Autowired
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Autowired
    private IChatAttachmentService chatAttachmentService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WeKnoraClient weKnoraClient;

    @Autowired
    private PendingCustomerCreationStore pendingCustomerCreationStore;

    @Autowired
    private KnowledgeTools knowledgeTools;

    @Autowired
    private AiQuotaService aiQuotaService;

    private static final int MAX_EXTRACTED_TEXT_LENGTH = 3000;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        你是一个AI驱动的CRM系统助手，帮助用户管理客户关系。

        **重要：当前日期时间信息**
        - 今天是：%s（%s）
        - 本周日期对照：
          %s
        - 日期格式统一使用：yyyy-MM-dd
        - 时间格式统一使用：yyyy-MM-dd HH:mm

        当用户提到"今天"、"明天"、"本周几"、"下周"等时间时，请严格按照上述日期计算。

        你可以：
        1. 创建、查询和修改客户信息（包括更新客户阶段、等级、金额等）
        2. 管理客户联系人
        3. 创建、查看和修改任务（包括修改截止日期、优先级、状态等）
        4. 查询知识库文档
        5. 创建和查询跟进记录（支持记录拜访、电话、会议、邮件等跟进活动）
        6. 创建和查询日程安排（会议、电话、拜访等有具体时间的安排）

        **数据类型识别规则（严格按以下优先级执行）**：

        【规则1：跟进记录识别】
        出现过去式表述（"已""已经""今天做了""完成了""拜访了""沟通了""谈了""聊了""见了""打了电话""发了邮件"等），
        识别为跟进记录，调用 createFollowUp。
        示例："今天拜访了XX公司王总" → createFollowUp

        【规则2：日程识别 - 明确时间点】
        出现未来的明确时间点（"10:00""下午3点""上午9点半""明天14:00""后天早上10点"等），
        识别为日程，调用 createSchedule。
        示例："明天下午2点和XX公司开会" → createSchedule

        【规则3：任务识别 - 无具体时间的待办】
        没有具体时间点，但有"需要""要""计划""准备""安排""跟进"等待办动作词，
        识别为任务，调用 createTask。
        示例："需要给XX公司准备方案" → createTask

        【规则4：仅截止时间 = 任务，不是日程】
        只有截止时间（"本周完成""周五之前""月底前""三天内"），没有具体执行时间点，
        识别为任务（将截止时间设为 dueDate），不是日程。
        示例："本周内完成XX公司报价" → createTask(dueDate=本周日)

        【规则5：复合语句必须拆分】
        一句话包含多个动作时，必须分别识别并分别调用对应的工具，不要合并。
        示例："今天拜访了XX公司，明天10点和他们开会，另外需要准备合同"
        → 分别调用：createFollowUp（拜访记录）+ createSchedule（会议日程）+ createTask（准备合同）

        【规则6：时间优先级】
        明确时间点 > 截止时间 > 无时间。
        同一动作如果既有具体时间点又有截止期，按时间点创建日程。

        【规则7：关键业务节点】
        当提到"签约""回款""成交""合同签订""付款""续约"等关键业务词时：
        - 如果是过去式（已签约/已回款/成交了），创建跟进记录（内容前加"【关键节点】"），
          同时调用 queryCustomers 查找客户，再调用 updateCustomer 更新客户阶段（签约/成交→closed，回款→closed）
        - 如果是未来式（准备签约/计划回款），按规则2-4正常识别为日程或任务

        【规则8：自动提取客户名和联系人名】
        从用户输入中自动识别并提取：
        - 客户名：带"公司""集团""科技""有限""工厂""企业"等后缀的词语
        - 联系人：带"总""经理""主任""工""姐""哥""先生""女士"等称呼的人名（如"王总""李经理""张工"）
        创建任何记录时，都应尽量关联提取到的客户名和联系人名，传给对应工具参数。

        请用中文回复，保持专业友好。
        当用户请求创建或查询数据时，使用提供的工具函数来完成操作。
        回复要简洁明了，重点突出关键信息。
        """;

    /**
     * 动态构建包含当前日期的 System Prompt
     */
    private String buildSystemPrompt() {
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE);
        String weekCalendar = buildWeekCalendar(today);

        return String.format(SYSTEM_PROMPT_TEMPLATE,
            today.toString(),
            dayOfWeek,
            weekCalendar
        ) + """

        【重复客户确认规则】
        1. 当你准备创建客户时，先调用 createCustomer，让系统检查是否存在同名客户。
        2. 如果 createCustomer 返回“已存在同名客户”“待确认”“尚未创建”等信息，你必须明确告诉用户发现了重复客户，并询问是否继续创建；此时绝不能声称已经创建成功。
        3. 只有当用户明确表示“确认创建”“继续创建”“仍然创建”“是，创建新客户”等同意语义时，才调用 confirmPendingCustomerCreation。
        4. 如果用户表示“不创建”“取消”“算了”，调用 cancelPendingCustomerCreation。
        5. 在重复客户尚未确认前，不要把该客户当作已经创建成功，也不要把后续依赖这个新客户的动作当成已完成。
        
        【工具结果回写规则】
        1. 当工具返回系统中的客户名称、联系人名称、时间、ID等字段时，回复中必须优先使用工具返回的原始值。
        2. 尤其是客户名称，必须展示系统匹配到的完整公司全称，禁止简称、缩写、省略“有限公司”“科技”“集团”等后缀。
        3. 如果工具结果中包含“系统客户全称”字段，必须原样展示该字段对应的名称，不要改写成更短的客户名。
        4. 不要根据用户原话重新生成客户简称；创建成功、查询结果、摘要说明里的客户名都以工具返回结果为准。
        5. 如果创建日程、任务或跟进的工具返回“未找到客户”或“创建失败”，必须明确告诉用户数据没有创建成功，并询问是否需要先创建该客户；禁止继续输出“创建成功”。
        """;
    }

    private String buildWeekCalendar(LocalDate today) {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        StringBuilder sb = new StringBuilder();
        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            String marker = date.equals(today) ? "（今天）" : "";
            sb.append(String.format("%s=%s%s", dayNames[i], date.toString(), marker));
            if (i < 6) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSession(SessionCreateBO sessionCreateBO) {
        ChatSession session = new ChatSession();
        session.setTitle(sessionCreateBO.getTitle());
        session.setAgentId(sessionCreateBO.getAgentId());
        session.setCustomerId(sessionCreateBO.getCustomerId());
        session.setUserId(UserUtil.getUserId());
        session.setCreateTime(new Date());
        session.setUpdateTime(new Date());
        chatSessionMapper.insert(session);
        return session.getSessionId();
    }

    @Override
    public List<ChatSessionVO> getSessionList() {
        Long userId = UserUtil.getUserId();
        List<ChatSession> sessions = chatSessionMapper.selectList(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
        );
        return BeanUtil.copyToList(sessions, ChatSessionVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (ObjectUtil.isNull(session)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "会话不存在");
        }
        chatMessageMapper.delete(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
        );
        chatSessionMapper.deleteById(sessionId);
        pendingCustomerCreationStore.clear(sessionId);
        Long tenantId = UserUtil.getTenantId();
        if (tenantId != null) {
            weKnoraClient.clearConversationSession(tenantId, sessionId);
        }
        AiContextHolder.clearSession(sessionId);
    }

    @Override
    public List<ChatMessageVO> getMessageList(Long sessionId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
        );
        List<ChatMessageVO> voList = BeanUtil.copyToList(messages, ChatMessageVO.class);

        // 批量查询附件
        List<Long> messageIds = messages.stream()
                .map(ChatMessage::getMessageId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(messageIds)) {
            List<ChatAttachment> allAttachments = chatAttachmentService.getByMessageIds(messageIds);
            Map<Long, List<ChatAttachment>> attachmentMap = allAttachments.stream()
                    .collect(Collectors.groupingBy(ChatAttachment::getMessageId));

            for (ChatMessageVO vo : voList) {
                List<ChatAttachment> msgAttachments = attachmentMap.get(vo.getMessageId());
                if (CollUtil.isNotEmpty(msgAttachments)) {
                    vo.setAttachments(msgAttachments.stream().map(att -> {
                        ChatMessageVO.AttachmentVO attVO = new ChatMessageVO.AttachmentVO();
                        attVO.setId(att.getId());
                        attVO.setFileName(att.getFileName());
                        attVO.setFilePath(att.getFilePath());
                        attVO.setFileSize(att.getFileSize());
                        attVO.setMimeType(att.getMimeType());
                        try {
                            attVO.setAccessUrl(fileStorageService.getUrl(att.getFilePath()));
                        } catch (Exception e) {
                            log.warn("获取附件URL失败: {}", att.getFilePath(), e);
                        }
                        return attVO;
                    }).collect(Collectors.toList()));
                }
            }
        }

        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Flux<String> streamChat(ChatSendBO sendBO) {
        Long sessionId = sendBO.getSessionId();
        String content = sendBO.getContent();
        List<ChatSendBO.AttachmentDTO> attachments = sendBO.getAttachments();

        Long currentUserId = UserUtil.getUserIdOrNull();
        Long currentTenantId = UserUtil.getTenantId();
        if (currentUserId != null) {
            // Spring Security 上下文不会自动透传到 Reactor 线程，这里先把会话级上下文放进自定义 Holder 供工具调用读取。
            AiContextHolder.setContext(sessionId, currentUserId, currentTenantId);
            log.debug("设置 AI 上下文: sessionId={}, userId={}, tenantId={}", sessionId, currentUserId, currentTenantId);
        }

        Long messageId = saveMessage(sessionId, "user", content);

        if (CollUtil.isNotEmpty(attachments)) {
            chatAttachmentService.saveBatchAttachments(messageId, attachments);
        }

        boolean ragEnabled = Boolean.TRUE.equals(sendBO.getRagEnabled());
        log.debug("聊天请求 RAG 开关: sessionId={}, tenantId={}, ragEnabled={}", sessionId, currentTenantId, ragEnabled);

        String routedKnowledgeResponse;
        try {
            routedKnowledgeResponse = tryHandleKnowledgeQuestion(content, attachments, ragEnabled);
        } catch (BusinessException exception) {
            saveMessage(sessionId, "assistant", exception.getMsg());
            updateSessionTime(sessionId);
            AiContextHolder.clear();
            return Flux.just(exception.getMsg());
        }
        if (routedKnowledgeResponse != null) {
            saveMessage(sessionId, "assistant", routedKnowledgeResponse);
            updateSessionTime(sessionId);
            // 这里直接返回前，先清理当前会话的 AiContextHolder。
            AiContextHolder.clear();
            return Flux.just(routedKnowledgeResponse);
        }

        List<Message> history = buildMessageHistory(sessionId);

        String knowledgeToolPrompt = buildKnowledgeToolPrompt(ragEnabled);
        String attachmentContext = buildAttachmentContext(attachments);
        // RAG 不再自动注入，改为由 KnowledgeTools 按需 Tool Calling 调用
        String enhancedSystemPrompt = buildSystemPrompt();
        if (StrUtil.isNotBlank(knowledgeToolPrompt)) {
            enhancedSystemPrompt = enhancedSystemPrompt + "\n\n" + knowledgeToolPrompt;
        }

        String enhancedContent = content;
        if (StrUtil.isNotBlank(attachmentContext)) {
            enhancedContent = enhancedContent + "\n\n" + attachmentContext;
        }

        AiModelCapabilities capabilities = chatClientProvider.getCurrentCapabilities();
        if (containsImageAttachment(attachments) && !capabilities.isSupportsVision()) {
            enhancedContent = enhancedContent + "\n\n[系统提示] 当前配置的模型不支持图片直传，请仅基于可提取文本回答；如需图片理解，请切换到支持视觉的模型。";
        }

        List<Media> mediaList = buildMediaList(attachments, capabilities);
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<Integer> promptTokensRef = new AtomicReference<>(0);
        AtomicReference<Integer> completionTokensRef = new AtomicReference<>(0);
        AtomicReference<Integer> totalTokensRef = new AtomicReference<>(0);
        AtomicReference<String> modelNameRef = new AtomicReference<>(null);

        String unavailableTip = resolveAiUnavailableTip(currentTenantId);
        if (unavailableTip != null) {
            saveMessage(sessionId, "assistant", unavailableTip);
            updateSessionTime(sessionId);
            AiContextHolder.clear();
            return Flux.just(unavailableTip);
        }

        String quotaTip = aiQuotaService.resolveQuotaFailureMessage(
            currentTenantId, "chat", enhancedSystemPrompt, history, enhancedContent
        );
        if (quotaTip != null) {
            saveMessage(sessionId, "assistant", quotaTip);
            updateSessionTime(sessionId);
            AiContextHolder.clear();
            return Flux.just(quotaTip);
        }

        log.debug("开始 AI 对话，启用工具调用...");

        if (!chatClientProvider.isApiKeyConfigured()) {
            String tip = "请先在系统设置-系统参数设置-AI/API设置中配置AI大模型相关信息";
            saveMessage(sessionId, "assistant", tip);
            AiContextHolder.clear();
            return Flux.just(tip);
        }

        ChatClient chatClient = chatClientProvider.getChatClient();

        final String finalSystemPrompt = enhancedSystemPrompt;
        final String finalContent = enhancedContent;
        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
            .system(finalSystemPrompt)
            .messages(history);

        if (CollUtil.isNotEmpty(mediaList)) {
            requestSpec.user(u -> u.text(finalContent).media(mediaList.toArray(new Media[0])));
        } else {
            requestSpec.user(finalContent);
        }

        return requestSpec
            .stream()
            .chatResponse()
            .doOnNext(chatResponse -> {
                if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                    String text = chatResponse.getResult().getOutput().getText();
                    if (text != null) {
                        fullResponse.append(text);
                    }
                    if (chatResponse.getResult().getOutput().getToolCalls() != null
                        && !chatResponse.getResult().getOutput().getToolCalls().isEmpty()) {
                        log.debug("工具调用: {}", chatResponse.getResult().getOutput().getToolCalls());
                    }
                }
                // 捕获 token 用量（通常在最后一个 chunk 中携带）
                if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                    var usage = chatResponse.getMetadata().getUsage();
                    if (usage.getPromptTokens() > 0 || usage.getCompletionTokens() > 0) {
                        promptTokensRef.set(usage.getPromptTokens());
                        completionTokensRef.set(usage.getCompletionTokens());
                        totalTokensRef.set(usage.getTotalTokens());
                    }
                }
                if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getModel() != null) {
                    modelNameRef.set(chatResponse.getMetadata().getModel());
                }
            })
            .mapNotNull(chatResponse -> {
                if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                    return chatResponse.getResult().getOutput().getText();
                }
                return null;
            })
            .doOnComplete(() -> {
                // SSE 场景下只有在流完成后才能拿到最终累积文本与较完整的 token 统计，因此持久化放在完成回调里统一处理。
                AiQuotaService.TokenUsageSnapshot usage = aiQuotaService.resolveTokenUsage(
                    promptTokensRef.get(),
                    completionTokensRef.get(),
                    totalTokensRef.get(),
                    finalSystemPrompt,
                    history,
                    finalContent,
                    fullResponse.toString()
                );
                log.debug("AI 对话完成，响应长度: {}, tokens: prompt={}, completion={}, total={}",
                    fullResponse.length(), usage.promptTokens(), usage.completionTokens(), usage.totalTokens());
                saveMessage(sessionId, "assistant", fullResponse.toString(),
                    usage.promptTokens(), usage.completionTokens(),
                    usage.totalTokens(), modelNameRef.get());
                aiQuotaService.consumeResolvedTokens(currentTenantId, "chat", usage);
                updateSessionTime(sessionId);
                // 会话结束后清理当前会话的 AiContextHolder。
                AiContextHolder.clear();
            })
            .doOnError(error -> {
                log.error("AI 对话错误: {}", error.getMessage(), error);
                saveMessage(sessionId, "assistant", "抱歉，处理您的请求时发生错误。请稍后重试。");
                AiContextHolder.clear();
            });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String chat(ChatSendBO sendBO) {
        Long sessionId = sendBO.getSessionId();
        String content = sendBO.getContent();
        List<ChatSendBO.AttachmentDTO> attachments = sendBO.getAttachments();

        Long currentUserId = UserUtil.getUserIdOrNull();
        Long currentTenantId = UserUtil.getTenantId();
        if (currentUserId != null) {
            AiContextHolder.setContext(sessionId, currentUserId, currentTenantId);
            log.debug("设置 AI 上下文: sessionId={}, userId={}, tenantId={}", sessionId, currentUserId, currentTenantId);
        }

        Long messageId = saveMessage(sessionId, "user", content);

        if (CollUtil.isNotEmpty(attachments)) {
            chatAttachmentService.saveBatchAttachments(messageId, attachments);
        }

        boolean ragEnabled = Boolean.TRUE.equals(sendBO.getRagEnabled());
        log.debug("聊天请求 RAG 开关: sessionId={}, tenantId={}, ragEnabled={}", sessionId, currentTenantId, ragEnabled);

        String routedKnowledgeResponse;
        try {
            routedKnowledgeResponse = tryHandleKnowledgeQuestion(content, attachments, ragEnabled);
        } catch (BusinessException exception) {
            saveMessage(sessionId, "assistant", exception.getMsg());
            updateSessionTime(sessionId);
            return exception.getMsg();
        }
        if (routedKnowledgeResponse != null) {
            saveMessage(sessionId, "assistant", routedKnowledgeResponse);
            updateSessionTime(sessionId);
            return routedKnowledgeResponse;
        }

        List<Message> history = buildMessageHistory(sessionId);

        String knowledgeToolPrompt = buildKnowledgeToolPrompt(ragEnabled);
        String attachmentContext = buildAttachmentContext(attachments);
        // RAG 不再自动注入，改为由 KnowledgeTools 按需 Tool Calling 调用
        String enhancedSystemPrompt = buildSystemPrompt();
        if (StrUtil.isNotBlank(knowledgeToolPrompt)) {
            enhancedSystemPrompt = enhancedSystemPrompt + "\n\n" + knowledgeToolPrompt;
        }

        String enhancedContent = content;
        if (StrUtil.isNotBlank(attachmentContext)) {
            enhancedContent = enhancedContent + "\n\n" + attachmentContext;
        }

        AiModelCapabilities capabilities = chatClientProvider.getCurrentCapabilities();
        if (containsImageAttachment(attachments) && !capabilities.isSupportsVision()) {
            enhancedContent = enhancedContent + "\n\n[系统提示] 当前配置的模型不支持图片直传，请仅基于可提取文本回答；如需图片理解，请切换到支持视觉的模型。";
        }

        List<Media> mediaList = buildMediaList(attachments, capabilities);

        String unavailableTip = resolveAiUnavailableTip(currentTenantId);
        if (unavailableTip != null) {
            saveMessage(sessionId, "assistant", unavailableTip);
            updateSessionTime(sessionId);
            return unavailableTip;
        }

        String quotaTip = aiQuotaService.resolveQuotaFailureMessage(
            currentTenantId, "chat", enhancedSystemPrompt, history, enhancedContent
        );
        if (quotaTip != null) {
            saveMessage(sessionId, "assistant", quotaTip);
            updateSessionTime(sessionId);
            return quotaTip;
        }

        try {
            ChatClient chatClient = chatClientProvider.getChatClient();

            final String finalSystemPrompt = enhancedSystemPrompt;
            final String finalContent = enhancedContent;
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(finalSystemPrompt)
                .messages(history);

            if (CollUtil.isNotEmpty(mediaList)) {
                requestSpec.user(u -> u.text(finalContent).media(mediaList.toArray(new Media[0])));
            } else {
                requestSpec.user(finalContent);
            }

            var chatResponse = requestSpec.call().chatResponse();
            String response = chatResponse.getResult().getOutput().getText();

            int promptTokens = 0, completionTokens = 0, totalTokens = 0;
            String modelName = null;
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                var usage = chatResponse.getMetadata().getUsage();
                promptTokens = usage.getPromptTokens();
                completionTokens = usage.getCompletionTokens();
                totalTokens = usage.getTotalTokens();
            }
            if (chatResponse.getMetadata() != null) {
                modelName = chatResponse.getMetadata().getModel();
            }

            AiQuotaService.TokenUsageSnapshot usageSnapshot = aiQuotaService.resolveTokenUsage(
                promptTokens,
                completionTokens,
                totalTokens,
                finalSystemPrompt,
                history,
                finalContent,
                response
            );

            saveMessage(sessionId, "assistant", response,
                usageSnapshot.promptTokens(), usageSnapshot.completionTokens(), usageSnapshot.totalTokens(), modelName);
            aiQuotaService.consumeResolvedTokens(currentTenantId, "chat", usageSnapshot);
            updateSessionTime(sessionId);

            return response;
        } catch (BusinessException e) {
            saveMessage(sessionId, "assistant", e.getMsg());
            updateSessionTime(sessionId);
            return e.getMsg();
        } catch (Exception e) {
            String errorMsg = "抱歉，处理您的请求时发生错误。请稍后重试。";
            saveMessage(sessionId, "assistant", errorMsg);
            updateSessionTime(sessionId);
            return errorMsg;
        } finally {
            // 非流式分支在 finally 清理当前会话的 AiContextHolder。
            AiContextHolder.clear();
        }
    }

    /**
     * 构建附件上下文信息（文本提取）
     */
    private String buildAttachmentContext(List<ChatSendBO.AttachmentDTO> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("[用户上传了以下文件]\n");

        for (ChatSendBO.AttachmentDTO att : attachments) {
            String mimeType = att.getMimeType();
            String fileName = att.getFileName();

            if (mimeType != null && mimeType.startsWith("image/")) {
                context.append(String.format("- 图片: %s（已作为图片传入，请直接分析图片内容）\n", fileName));
            } else if (isTextFile(mimeType, fileName)) {
                String textContent = extractFileText(att.getFilePath());
                if (StrUtil.isNotBlank(textContent)) {
                    context.append(String.format("- 文本文件: %s，内容如下：\n```\n%s\n```\n", fileName, textContent));
                } else {
                    context.append(String.format("- 文本文件: %s（无法读取内容）\n", fileName));
                }
            } else if (isDocumentFile(mimeType, fileName)) {
                String textContent = extractDocumentText(att.getFilePath());
                if (StrUtil.isNotBlank(textContent)) {
                    context.append(String.format("- 文档: %s，提取的文本内容如下：\n```\n%s\n```\n", fileName, textContent));
                } else {
                    context.append(String.format("- 文档: %s（无法提取文本内容）\n", fileName));
                }
            } else {
                context.append(String.format("- 文件: %s（类型: %s）\n", fileName, mimeType));
            }
        }

        return context.toString();
    }

    /**
     * 构建图片 Media 列表（用于多模态模型）
     */
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
                    log.debug("添加图片媒体: {}", att.getFileName());
                } catch (Exception e) {
                    log.warn("构建图片媒体失败: {}", att.getFileName(), e);
                }
            }
        }
        return mediaList;
    }

    private boolean containsImageAttachment(List<ChatSendBO.AttachmentDTO> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return false;
        }
        return attachments.stream().anyMatch(att -> att.getMimeType() != null && att.getMimeType().startsWith("image/"));
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
        try (InputStream is = fileStorageService.getFileStream(filePath)) {
            if (is == null) return null;
            byte[] bytes = is.readNBytes(MAX_EXTRACTED_TEXT_LENGTH * 3);
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (text.length() > MAX_EXTRACTED_TEXT_LENGTH) {
                text = text.substring(0, MAX_EXTRACTED_TEXT_LENGTH) + "\n...(内容过长已截断)";
            }
            return text;
        } catch (Exception e) {
            log.warn("读取文本文件失败: {}", filePath, e);
            return null;
        }
    }

    private String extractDocumentText(String filePath) {
        try (InputStream is = fileStorageService.getFileStream(filePath)) {
            if (is == null) return null;
            String text = DocumentTextExtractor.parseToString(is, null, filePath);
            if (StrUtil.isBlank(text)) return null;
            if (text.length() > MAX_EXTRACTED_TEXT_LENGTH) {
                text = text.substring(0, MAX_EXTRACTED_TEXT_LENGTH) + "\n...(内容过长已截断)";
            }
            return text.trim();
        } catch (Exception e) {
            log.warn("Tika提取文档文本失败: {}", filePath, e);
            return null;
        }
    }

    private List<Message> buildMessageHistory(Long sessionId) {
        List<ChatMessage> dbMessages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
                .last("LIMIT 20")
        );

        List<Message> messages = new ArrayList<>();
        for (ChatMessage dbMsg : dbMessages) {
            switch (dbMsg.getRole()) {
                case "user":
                    messages.add(new UserMessage(dbMsg.getContent()));
                    break;
                case "assistant":
                    messages.add(new AssistantMessage(dbMsg.getContent()));
                    break;
                case "system":
                    messages.add(new SystemMessage(dbMsg.getContent()));
                    break;
            }
        }
        return messages;
    }

    private Long saveMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(new Date());
        chatMessageMapper.insert(message);

        if ("user".equals(role)) {
            updateSessionTitleIfNeeded(sessionId, content);
        }

        return message.getMessageId();
    }

    private Long saveMessage(Long sessionId, String role, String content,
                             Integer promptTokens, Integer completionTokens,
                             Integer totalTokens, String modelName) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setPromptTokens(promptTokens != null ? promptTokens : 0);
        message.setCompletionTokens(completionTokens != null ? completionTokens : 0);
        message.setTokensUsed(totalTokens != null ? totalTokens : 0);
        message.setModelName(modelName);
        message.setCreateTime(new Date());
        chatMessageMapper.insert(message);

        if ("user".equals(role)) {
            updateSessionTitleIfNeeded(sessionId, content);
        }

        return message.getMessageId();
    }

    private void updateSessionTitleIfNeeded(Long sessionId, String userContent) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null && ("新对话".equals(session.getTitle()) || session.getTitle() == null || session.getTitle().isEmpty())) {
            String title = userContent.length() > 30
                ? userContent.substring(0, 30) + "..."
                : userContent;
            session.setTitle(title);
            chatSessionMapper.updateById(session);
            log.debug("自动更新会话标题: sessionId={}, title={}", sessionId, title);
        }
    }

    private void updateSessionTime(Long sessionId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setUpdateTime(new Date());
            chatSessionMapper.updateById(session);
        }
    }

    private String tryHandleKnowledgeQuestion(String content, List<ChatSendBO.AttachmentDTO> attachments, boolean ragEnabled) {
        Long sessionId = AiContextHolder.getCurrentSessionId();
        Long tenantId = AiContextHolder.getCurrentTenantId();
        log.debug("知识库前置路由检查: sessionId={}, tenantId={}, ragEnabled={}, hasAttachments={}, content={}",
                sessionId, tenantId, ragEnabled, CollUtil.isNotEmpty(attachments), abbreviateForLog(content));

        if (!ragEnabled) {
            log.debug("知识库前置路由跳过: RAG 开关未开启, sessionId={}, tenantId={}", sessionId, tenantId);
            return null;
        }

        if (CollUtil.isNotEmpty(attachments)) {
            log.debug("检测到知识库问题，但当前消息包含附件，保留通用聊天链路处理: sessionId={}, tenantId={}",
                    sessionId, tenantId);
            return null;
        }

        boolean preferAnswerFirst = shouldPreferKnowledgeAnswer(content);
        try {
            if (preferAnswerFirst) {
                String askResponse = knowledgeTools.askKnowledgeQuestion(content, null);
                if (isUsableKnowledgeAnswerResponse(askResponse)) {
                    log.debug("知识库问句优先由 askKnowledgeQuestion 命中: sessionId={}, tenantId={}, responseLength={}",
                            sessionId, tenantId, askResponse.length());
                    return askResponse;
                }

                log.debug("askKnowledgeQuestion 未命中或结果不可用，回退 searchKnowledgeContent: sessionId={}, tenantId={}",
                        sessionId, tenantId);
                String searchResponse = knowledgeTools.searchKnowledgeContent(content);
                if (isUsableKnowledgeSearchResponse(searchResponse)) {
                    log.debug("知识库问句已由 searchKnowledgeContent 兜底处理: sessionId={}, tenantId={}, responseLength={}",
                            sessionId, tenantId, searchResponse.length());
                    return searchResponse;
                }
                return StrUtil.isNotBlank(askResponse) ? askResponse : searchResponse;
            }

            String searchResponse = knowledgeTools.searchKnowledgeContent(content);
            if (isUsableKnowledgeSearchResponse(searchResponse)) {
                log.debug("知识库原文检索优先由 searchKnowledgeContent 命中: sessionId={}, tenantId={}, responseLength={}",
                        sessionId, tenantId, searchResponse.length());
                return searchResponse;
            }

            log.debug("searchKnowledgeContent 未命中或结果不可用，回退 askKnowledgeQuestion: sessionId={}, tenantId={}",
                    sessionId, tenantId);
            String askResponse = knowledgeTools.askKnowledgeQuestion(content, null);
            if (isUsableKnowledgeAnswerResponse(askResponse)) {
                log.debug("知识库原文检索已由 askKnowledgeQuestion 兜底处理: sessionId={}, tenantId={}, responseLength={}",
                        sessionId, tenantId, askResponse.length());
                return askResponse;
            }
            return StrUtil.isNotBlank(searchResponse) ? searchResponse : askResponse;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("服务端前置处理知识库问题失败，将回退到通用聊天链路: sessionId={}, tenantId={}, error={}",
                    sessionId, tenantId, e.getMessage(), e);
            return null;
        }
    }

    private boolean shouldPreferKnowledgeAnswer(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }
        if (hasExplicitKnowledgeSearchIntent(content)) {
            return false;
        }
        return looksLikeKnowledgeQuestion(content);
    }

    private boolean hasExplicitKnowledgeSearchIntent(String content) {
        return StrUtil.containsAnyIgnoreCase(content,
                "原文", "出处", "片段", "摘录", "节选", "第几页", "哪一页", "页码",
                "全文", "原句", "原话", "命中文档", "相关文档", "原始内容");
    }

    private boolean looksLikeKnowledgeQuestion(String content) {
        return StrUtil.containsAny(content, "?", "？")
                || StrUtil.containsAnyIgnoreCase(content,
                "请问", "是什么", "什么意思", "多少", "多大", "多久", "怎么", "怎样", "如何",
                "为什么", "为何", "吗", "么", "是否", "能否", "有没有", "哪种", "哪个",
                "哪些", "几类", "几种", "几次", "谁", "何时", "啥");
    }

    private boolean isUsableKnowledgeAnswerResponse(String response) {
        if (StrUtil.isBlank(response)) {
            return false;
        }
        return !response.contains("当前未能直接从知识库生成回答")
                && !response.contains("知识库问答失败")
                && !response.contains("知识库问答功能未启用")
                && !response.contains("问题不能为空")
                && !response.contains("无法确定当前租户");
    }

    private boolean isUsableKnowledgeSearchResponse(String response) {
        if (StrUtil.isBlank(response)) {
            return false;
        }
        return !response.contains("未找到与")
                && !response.contains("语义检索失败")
                && !response.contains("功能未启用");
    }

    private String resolveAiUnavailableTip(Long tenantId) {
        if (!chatClientProvider.isApiKeyConfigured()) {
            return "请先在系统设置中配置 AI 服务，或切换到赠送额度模式。";
        }
        if (tenantId == null) {
            return "当前租户信息缺失，本次AI对话失败";
        }
        return null;
    }

    private String buildKnowledgeToolPrompt(boolean ragEnabled) {
        if (!ragEnabled) {
            log.debug("跳过知识库工具提示: RAG 开关未开启");
            return "";
        }
        log.debug("构建知识库工具提示: RAG 开关已开启");

        return """
                【知识库问题处理规则】
                1. 优先使用知识库工具，不要直接凭空回答。
                2. 如果用户是在提问、希望直接得到结论、解释条款或总结信息，优先调用 askKnowledgeQuestion。
                3. 只有当用户明确要求查看原文、出处、片段、页码或命中文档时，才优先调用 searchKnowledgeContent。
                4. 若 askKnowledgeQuestion 没有拿到可用答案，再尝试 searchKnowledgeContent；若 searchKnowledgeContent 先拿到片段但用户还需要结论，再调用 askKnowledgeQuestion。
                """;
    }

    private String abbreviateForLog(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() > 120 ? normalized.substring(0, 120) + "..." : normalized;
    }
}
