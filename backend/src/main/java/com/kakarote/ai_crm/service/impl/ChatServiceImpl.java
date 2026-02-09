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

        当用户提到"今天"、"明天"、"本周几"、"下周"等时间时，请严格按照上述日期计算。

        你可以：
        1. 创建、查询和修改客户信息（包括更新客户阶段、等级、金额等）
        2. 管理客户联系人
        3. 创建、查看和修改任务（包括修改截止日期、优先级、状态等）
        4. 查询知识库文档

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

        List<Message> history = buildMessageHistory(sessionId);

        String attachmentContext = buildAttachmentContext(attachments);
        String ragContext = buildRagContext(content);
        String enhancedSystemPrompt = buildSystemPrompt() + ragContext;

        String enhancedContent = content;
        if (StrUtil.isNotBlank(attachmentContext)) {
            enhancedContent = content + "\n\n" + attachmentContext;
        }

        List<Media> mediaList = buildMediaList(attachments);
        StringBuilder fullResponse = new StringBuilder();

        log.debug("开始 AI 对话，启用工具调用...");

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
            })
            .mapNotNull(chatResponse -> {
                if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                    return chatResponse.getResult().getOutput().getText();
                }
                return null;
            })
            .doOnComplete(() -> {
                log.debug("AI 对话完成，响应长度: {}", fullResponse.length());
                saveMessage(sessionId, "assistant", fullResponse.toString());
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

        List<Message> history = buildMessageHistory(sessionId);

        String attachmentContext = buildAttachmentContext(attachments);
        String ragContext = buildRagContext(content);
        String enhancedSystemPrompt = buildSystemPrompt() + ragContext;

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

            String response = requestSpec.call().content();

            saveMessage(sessionId, "assistant", response);
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
