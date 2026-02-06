package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.config.WeKnoraConfig;
import com.kakarote.ai_crm.entity.BO.SessionCreateBO;
import com.kakarote.ai_crm.entity.PO.ChatMessage;
import com.kakarote.ai_crm.entity.PO.ChatSession;
import com.kakarote.ai_crm.entity.VO.ChatMessageVO;
import com.kakarote.ai_crm.entity.VO.ChatSessionVO;
import com.kakarote.ai_crm.entity.VO.WeKnoraChunk;
import com.kakarote.ai_crm.mapper.ChatMessageMapper;
import com.kakarote.ai_crm.mapper.ChatSessionMapper;
import com.kakarote.ai_crm.service.IChatService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private WeKnoraClient weKnoraClient;

    @Autowired
    private WeKnoraConfig weKnoraConfig;

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
     * 让 LLM 知道当前的实际日期，以正确处理"今天"、"本周"等时间表达
     */
    private String buildSystemPrompt() {
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE);

        // 构建本周每天的日期对照表
        String weekCalendar = buildWeekCalendar(today);

        return String.format(SYSTEM_PROMPT_TEMPLATE,
            today.toString(),           // 如：2026-01-29
            dayOfWeek,                  // 如：星期四
            weekCalendar                // 本周日期对照表
        );
    }

    /**
     * 构建本周日期对照表，明确每天对应的日期
     */
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
        // Delete messages first
        chatMessageMapper.delete(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
        );
        // Delete session
        chatSessionMapper.deleteById(sessionId);
        // 清除 AI 上下文中该会话的数据
        AiContextHolder.clearSession(sessionId);
    }

    @Override
    public List<ChatMessageVO> getMessageList(Long sessionId) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
        );
        return BeanUtil.copyToList(messages, ChatMessageVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Flux<String> streamChat(Long sessionId, String content) {
        // 保存当前用户ID到AI上下文，供工具调用时使用
        Long currentUserId = UserUtil.getUserIdOrNull();
        if (currentUserId != null) {
            AiContextHolder.setContext(sessionId, currentUserId);
            log.debug("设置 AI 上下文: sessionId={}, userId={}", sessionId, currentUserId);
        }

        // Save user message
        saveMessage(sessionId, "user", content);

        // Build conversation history
        List<Message> history = buildMessageHistory(sessionId);

        // Build RAG context if auto RAG is enabled
        String ragContext = buildRagContext(content);
        String enhancedSystemPrompt = buildSystemPrompt() + ragContext;

        // Use Sinks for collecting the streamed response
        StringBuilder fullResponse = new StringBuilder();

        log.debug("开始 AI 对话，启用工具调用...");

        // 动态获取 ChatClient
        ChatClient chatClient = chatClientProvider.getChatClient();

        return chatClient.prompt()
            .system(enhancedSystemPrompt)
            .messages(history)
            .user(content)
            .stream()
            .chatResponse()
            .doOnNext(chatResponse -> {
                // 处理每个 ChatResponse，包括工具调用结果
                if (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null) {
                    String text = chatResponse.getResult().getOutput().getText();
                    if (text != null) {
                        fullResponse.append(text);
                    }
                    // 记录工具调用信息
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
                // Save assistant response after streaming completes
                log.debug("AI 对话完成，响应长度: {}", fullResponse.length());
                saveMessage(sessionId, "assistant", fullResponse.toString());
                // Update session time
                updateSessionTime(sessionId);
                // 清除 AI 上下文
                AiContextHolder.clear();
            })
            .doOnError(error -> {
                // Save error message
                log.error("AI 对话错误: {}", error.getMessage(), error);
                saveMessage(sessionId, "assistant", "抱歉，处理您的请求时发生错误。请稍后重试。");
                // 清除 AI 上下文
                AiContextHolder.clear();
            });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String chat(Long sessionId, String content) {
        // 保存当前用户ID到AI上下文，供工具调用时使用
        Long currentUserId = UserUtil.getUserIdOrNull();
        if (currentUserId != null) {
            AiContextHolder.setContext(sessionId, currentUserId);
            log.debug("设置 AI 上下文: sessionId={}, userId={}", sessionId, currentUserId);
        }

        // Save user message
        saveMessage(sessionId, "user", content);

        // Build conversation history
        List<Message> history = buildMessageHistory(sessionId);

        // Build RAG context if auto RAG is enabled
        String ragContext = buildRagContext(content);
        String enhancedSystemPrompt = buildSystemPrompt() + ragContext;

        try {
            // 动态获取 ChatClient
            ChatClient chatClient = chatClientProvider.getChatClient();

            // Call ChatClient
            String response = chatClient.prompt()
                .system(enhancedSystemPrompt)
                .messages(history)
                .user(content)
                .call()
                .content();

            // Save assistant response
            saveMessage(sessionId, "assistant", response);

            // Update session time
            updateSessionTime(sessionId);

            return response;
        } catch (Exception e) {
            String errorMsg = "抱歉，处理您的请求时发生错误。请稍后重试。";
            saveMessage(sessionId, "assistant", errorMsg);
            return errorMsg;
        } finally {
            // 清除 AI 上下文
            AiContextHolder.clear();
        }
    }

    private List<Message> buildMessageHistory(Long sessionId) {
        List<ChatMessage> dbMessages = chatMessageMapper.selectList(
            new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime)
                .last("LIMIT 20") // Limit context window
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

    private void saveMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(new Date());
        chatMessageMapper.insert(message);

        // 如果是用户消息，检查是否需要自动生成会话标题
        if ("user".equals(role)) {
            updateSessionTitleIfNeeded(sessionId, content);
        }
    }

    /**
     * 自动更新会话标题
     * 当会话标题为"新对话"或空时，使用用户首条消息作为标题
     */
    private void updateSessionTitleIfNeeded(Long sessionId, String userContent) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null && ("新对话".equals(session.getTitle()) || session.getTitle() == null || session.getTitle().isEmpty())) {
            // 截取用户消息前30个字符作为标题
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

    /**
     * 构建 RAG 上下文
     * 当启用自动 RAG 时，使用用户问题检索相关文档片段并注入到 System Prompt
     */
    private String buildRagContext(String query) {
        // Check if auto RAG is enabled
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
