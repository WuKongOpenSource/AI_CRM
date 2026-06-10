package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.WeKnoraConfig;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.SessionCreateBO;
import com.kakarote.ai_crm.entity.PO.ChatAttachment;
import com.kakarote.ai_crm.entity.PO.ChatMessage;
import com.kakarote.ai_crm.entity.PO.ChatSession;
import com.kakarote.ai_crm.entity.VO.ChatMessageVO;
import com.kakarote.ai_crm.entity.VO.ChatSessionVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.mapper.ChatMessageMapper;
import com.kakarote.ai_crm.mapper.ChatSessionMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IChatAttachmentService;
import com.kakarote.ai_crm.service.IChatService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
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
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * AI聊天服务实现 - 使用Spring AI ChatClient
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
    private WeKnoraConfig weKnoraConfig;

    private static final int MAX_EXTRACTED_TEXT_LENGTH = 3000;

    private final Tika tika = new Tika();

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

        **实时数据查询规则**：
        当用户请求查询客户、联系人、任务、日程、跟进记录、知识库文件等业务数据时，必须调用对应工具获取最新数据。
        不要仅根据历史对话中的旧结果回答，也不要因为用户重复提问就复述上一次的查询结果。
        如果用户没有指定筛选条件，例如"查询客户列表"，应按无筛选条件查询并展示工具返回的列表。

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
        );
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
        if (currentUserId != null) {
            AiContextHolder.setContext(sessionId, currentUserId);
            log.debug("设置 AI 上下文: sessionId={}, userId={}", sessionId, currentUserId);
        }

        Long messageId = saveMessage(sessionId, "user", content);

        if (CollUtil.isNotEmpty(attachments)) {
            chatAttachmentService.saveBatchAttachments(messageId, attachments);
        }

        // 当前用户消息会在本轮 request.user(...) 中单独传入，历史上下文需要排除它，避免模型误判为重复请求。
        List<Message> history = buildMessageHistory(sessionId, messageId);

        String attachmentContext = buildAttachmentContext(attachments);
        // RAG 不再自动注入，改为由 KnowledgeTools 按需 Tool Calling 调用
        String enhancedSystemPrompt = buildSystemPrompt();

        String enhancedContent = content;
        if (StrUtil.isNotBlank(attachmentContext)) {
            enhancedContent = content + "\n\n" + attachmentContext;
        }

        List<Media> mediaList = buildMediaList(attachments);
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<Integer> promptTokensRef = new AtomicReference<>(0);
        AtomicReference<Integer> completionTokensRef = new AtomicReference<>(0);
        AtomicReference<Integer> totalTokensRef = new AtomicReference<>(0);
        AtomicReference<String> modelNameRef = new AtomicReference<>(null);

        log.debug("开始 AI 对话，启用工具调用...");

        if (!chatClientProvider.isApiKeyConfigured()) {
            String tip = "请先在系统设置-系统参数设置-AI/API设置中配置AI大模型相关信息";
            saveMessage(sessionId, "assistant", tip);
            AiContextHolder.clear();
            return Flux.just(tip);
        }

        ChatClient chatClient = chatClientProvider.getChatClient();

        final String finalContent = enhancedContent;
        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
            .system(enhancedSystemPrompt)
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
                log.debug("AI 对话完成，响应长度: {}, tokens: prompt={}, completion={}, total={}",
                    fullResponse.length(), promptTokensRef.get(), completionTokensRef.get(), totalTokensRef.get());
                saveMessage(sessionId, "assistant", fullResponse.toString(),
                    promptTokensRef.get(), completionTokensRef.get(),
                    totalTokensRef.get(), modelNameRef.get());
                updateSessionTime(sessionId);
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
        if (currentUserId != null) {
            AiContextHolder.setContext(sessionId, currentUserId);
            log.debug("设置 AI 上下文: sessionId={}, userId={}", sessionId, currentUserId);
        }

        Long messageId = saveMessage(sessionId, "user", content);

        if (CollUtil.isNotEmpty(attachments)) {
            chatAttachmentService.saveBatchAttachments(messageId, attachments);
        }

        // 当前用户消息会在本轮 request.user(...) 中单独传入，历史上下文需要排除它，避免模型误判为重复请求。
        List<Message> history = buildMessageHistory(sessionId, messageId);

        String attachmentContext = buildAttachmentContext(attachments);
        // RAG 不再自动注入，改为由 KnowledgeTools 按需 Tool Calling 调用
        String enhancedSystemPrompt = buildSystemPrompt();

        String enhancedContent = content;
        if (StrUtil.isNotBlank(attachmentContext)) {
            enhancedContent = content + "\n\n" + attachmentContext;
        }

        List<Media> mediaList = buildMediaList(attachments);

        try {
            ChatClient chatClient = chatClientProvider.getChatClient();

            final String finalContent = enhancedContent;
            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(enhancedSystemPrompt)
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

            saveMessage(sessionId, "assistant", response, promptTokens, completionTokens, totalTokens, modelName);
            updateSessionTime(sessionId);

            return response;
        } catch (Exception e) {
            String errorMsg = "抱歉，处理您的请求时发生错误。请稍后重试。";
            saveMessage(sessionId, "assistant", errorMsg);
            return errorMsg;
        } finally {
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
    private List<Media> buildMediaList(List<ChatSendBO.AttachmentDTO> attachments) {
        if (CollUtil.isEmpty(attachments)) {
            return Collections.emptyList();
        }

        List<Media> mediaList = new ArrayList<>();
        for (ChatSendBO.AttachmentDTO att : attachments) {
            if (att.getMimeType() != null && att.getMimeType().startsWith("image/")) {
                try {
                    String imageUrl = fileStorageService.getUrl(att.getFilePath());
                    MimeType mimeType = MimeType.valueOf(att.getMimeType());
                    Media media = Media.builder()
                            .mimeType(mimeType)
                            .data(URI.create(imageUrl).toURL())
                            .build();
                    mediaList.add(media);
                    log.debug("添加图片媒体: {}", att.getFileName());
                } catch (Exception e) {
                    log.warn("构建图片媒体失败: {}", att.getFileName(), e);
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
            String text = tika.parseToString(is);
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

    private List<Message> buildMessageHistory(Long sessionId, Long excludeMessageId) {
        List<ChatMessage> dbMessages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
                .last("LIMIT 20")
        );

        List<Message> messages = new ArrayList<>();
        for (ChatMessage dbMsg : dbMessages) {
            if (excludeMessageId != null && excludeMessageId.equals(dbMsg.getMessageId())) {
                continue;
            }
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

    private String buildRagContext(String query) {
        if (!weKnoraClient.isEnabled() || !weKnoraConfig.getSearch().isAutoRagEnabled()) {
            return "";
        }

        try {
            log.debug("RAG 检索开始: query={}", query);
            List<WeKnoraChunk> chunks = weKnoraClient.searchKnowledge(query);

            if (chunks.isEmpty()) {
                log.debug("RAG 检索无结果");
                return "";
            }

            StringBuilder context = new StringBuilder();
            context.append("\n\n## 相关文档参考\n");
            context.append("以下是与用户问题相关的知识库文档内容，请基于这些内容来辅助回答用户问题：\n\n");

            for (int i = 0; i < chunks.size(); i++) {
                WeKnoraChunk chunk = chunks.get(i);
                context.append(String.format("### [%d] %s\n", i + 1, chunk.getKnowledgeTitle()));
                context.append(chunk.getContent());
                context.append("\n\n");
            }

            context.append("---\n");
            context.append("注意：以上内容仅供参考，请结合用户问题进行准确回答。如果文档内容与用户问题无关，可忽略。\n");

            log.debug("RAG 上下文构建完成: {} 个片段", chunks.size());
            return context.toString();
        } catch (Exception e) {
            log.warn("RAG 检索失败: {}", e.getMessage());
            return "";
        }
    }
}
