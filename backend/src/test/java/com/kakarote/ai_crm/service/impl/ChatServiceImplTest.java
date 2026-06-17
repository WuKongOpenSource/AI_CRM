package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.AiMode;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.ai.app.ChatApplicationRegistry;
import com.kakarote.ai_crm.ai.provider.AiModelCapabilities;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.ChatSendBO;
import com.kakarote.ai_crm.entity.BO.SessionCreateBO;
import com.kakarote.ai_crm.entity.BO.SessionPinBO;
import com.kakarote.ai_crm.entity.PO.ChatMessage;
import com.kakarote.ai_crm.entity.PO.ChatSession;
import com.kakarote.ai_crm.entity.PO.Product;
import com.kakarote.ai_crm.entity.PO.Project;
import com.kakarote.ai_crm.entity.PO.ProjectTask;
import com.kakarote.ai_crm.mapper.ChatMessageMapper;
import com.kakarote.ai_crm.mapper.ProductMapper;
import com.kakarote.ai_crm.mapper.ChatSessionMapper;
import com.kakarote.ai_crm.mapper.ProjectMapper;
import com.kakarote.ai_crm.mapper.ProjectTaskMapper;
import com.kakarote.ai_crm.ai.tools.KnowledgeTools;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    @AfterEach
    void tearDown() {
        AiContextHolder.clear();
    }

    @Test
    void createSessionRejectsMultipleBoundObjects() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ReflectionTestUtils.setField(service, "chatApplicationRegistry", new ChatApplicationRegistry());
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        AiContextHolder.setContext(2001L, 7L);

        SessionCreateBO bo = new SessionCreateBO();
        bo.setCustomerId(11L);
        bo.setProductId(22L);

        assertThatThrownBy(() -> service.createSession(bo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能绑定一个业务对象");

        verify(chatSessionMapper, never()).insert(any(ChatSession.class));
    }

    @Test
    void createSessionRejectsUnavailableProduct() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ReflectionTestUtils.setField(service, "chatApplicationRegistry", new ChatApplicationRegistry());
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        AiContextHolder.setContext(2002L, 7L);

        SessionCreateBO bo = new SessionCreateBO();
        bo.setProductId(900L);
        when(productMapper.selectById(900L)).thenReturn(null);

        assertThatThrownBy(() -> service.createSession(bo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("产品不存在或无权限访问");

        verify(chatSessionMapper, never()).insert(any(ChatSession.class));
    }

    @Test
    void updateSessionPinPinsCurrentUserSessionWithoutChangingUpdateTime() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        AiContextHolder.setContext(1001L, 7L);

        ChatSession session = new ChatSession();
        session.setSessionId(1001L);
        session.setUserId(7L);
        session.setPinned(false);
        when(chatSessionMapper.selectById(1001L)).thenReturn(session);

        SessionPinBO bo = new SessionPinBO();
        bo.setPinned(true);
        service.updateSessionPin(1001L, bo);

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).updateById(sessionCaptor.capture());
        ChatSession updated = sessionCaptor.getValue();
        assertThat(updated.getPinned()).isTrue();
        assertThat(updated.getPinnedTime()).isNotNull();
        assertThat(updated.getUpdateTime()).isNull();
    }

    @Test
    void chatRejectsMissingSessionBeforePersistingMessage() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
        ReflectionTestUtils.setField(service, "chatApplicationRegistry", new ChatApplicationRegistry());
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        ReflectionTestUtils.setField(service, "chatMessageMapper", chatMessageMapper);

        ChatSendBO sendBO = new ChatSendBO();
        sendBO.setSessionId(404L);
        sendBO.setContent("hello");

        assertThatThrownBy(() -> service.chat(sendBO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("会话不存在");

        verify(chatMessageMapper, never()).insert(any(ChatMessage.class));
    }

    @Test
    void knowledgeRoutePrefersQuestionAnswerOverSearchSnippets() {
        ChatServiceImpl service = new ChatServiceImpl();
        KnowledgeTools knowledgeTools = mock(KnowledgeTools.class);
        ReflectionTestUtils.setField(service, "knowledgeTools", knowledgeTools);
        AiContextHolder.setContext(3003L, 7L);
        when(knowledgeTools.askKnowledgeQuestion("客户合同怎么约定?", null)).thenReturn("这是知识库问答结论");
        when(knowledgeTools.searchKnowledgeContent("客户合同怎么约定?")).thenReturn("这是片段检索结果");

        String response = ReflectionTestUtils.invokeMethod(
                service,
                "tryHandleKnowledgeQuestion",
                "客户合同怎么约定?",
                Collections.emptyList(),
                true,
                List.of()
        );

        assertThat(response).isEqualTo("这是知识库问答结论");
        verify(knowledgeTools, never()).searchKnowledgeContent("客户合同怎么约定?");
    }

    @Test
    void chatBindsProductContextFromSendRequest() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        DynamicChatClientProvider chatClientProvider = mock(DynamicChatClientProvider.class);

        ReflectionTestUtils.setField(service, "chatApplicationRegistry", new ChatApplicationRegistry());
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        ReflectionTestUtils.setField(service, "chatMessageMapper", chatMessageMapper);
        ReflectionTestUtils.setField(service, "productMapper", productMapper);
        ReflectionTestUtils.setField(service, "chatClientProvider", chatClientProvider);

        ChatSession session = new ChatSession();
        session.setSessionId(3001L);
        session.setTitle("Existing session");
        when(chatSessionMapper.selectById(3001L)).thenReturn(session);

        Product product = new Product();
        product.setProductId(501L);
        product.setProductName("CRM Suite");
        product.setStatus("enabled");
        when(productMapper.selectById(501L)).thenReturn(product);
        when(chatMessageMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(chatClientProvider.getRuntimeConfigSnapshot(null, null))
                .thenReturn(new DynamicChatClientProvider.AiRuntimeConfigSnapshot(
                        null, null, null, null, null, AiModelCapabilities.builder().build(), AiMode.CUSTOM));

        ChatSendBO sendBO = new ChatSendBO();
        sendBO.setSessionId(3001L);
        sendBO.setContent("介绍一下这个产品");
        sendBO.setProductId(501L);

        service.chat(sendBO);

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).updateById(sessionCaptor.capture());
        ChatSession updated = sessionCaptor.getValue();
        assertThat(updated.getProductId()).isEqualTo(501L);
        assertThat(updated.getAppCode()).isEqualTo("product");
    }

    @Test
    void chatBindsProjectContextFromSendRequest() {
        ChatServiceImpl service = new ChatServiceImpl();
        ChatSessionMapper chatSessionMapper = mock(ChatSessionMapper.class);
        ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectTaskMapper projectTaskMapper = mock(ProjectTaskMapper.class);
        DynamicChatClientProvider chatClientProvider = mock(DynamicChatClientProvider.class);

        ReflectionTestUtils.setField(service, "chatApplicationRegistry", new ChatApplicationRegistry());
        ReflectionTestUtils.setField(service, "chatSessionMapper", chatSessionMapper);
        ReflectionTestUtils.setField(service, "chatMessageMapper", chatMessageMapper);
        ReflectionTestUtils.setField(service, "projectMapper", projectMapper);
        ReflectionTestUtils.setField(service, "projectTaskMapper", projectTaskMapper);
        ReflectionTestUtils.setField(service, "chatClientProvider", chatClientProvider);

        ChatSession session = new ChatSession();
        session.setSessionId(3002L);
        session.setTitle("Existing session");
        when(chatSessionMapper.selectById(3002L)).thenReturn(session);

        Project project = new Project();
        project.setProjectId(701L);
        project.setName("Implementation");
        when(projectMapper.selectById(701L)).thenReturn(project);

        ProjectTask task = new ProjectTask();
        task.setTaskId(801L);
        task.setProjectId(701L);
        task.setTitle("Prepare launch");
        when(projectTaskMapper.selectById(801L)).thenReturn(task);
        when(chatMessageMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(chatClientProvider.getRuntimeConfigSnapshot(null, null))
                .thenReturn(new DynamicChatClientProvider.AiRuntimeConfigSnapshot(
                        null, null, null, null, null, AiModelCapabilities.builder().build(), AiMode.CUSTOM));

        ChatSendBO sendBO = new ChatSendBO();
        sendBO.setSessionId(3002L);
        sendBO.setContent("总结一下这个任务");
        sendBO.setProjectId(701L);
        sendBO.setProjectTaskId(801L);

        service.chat(sendBO);

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).updateById(sessionCaptor.capture());
        ChatSession updated = sessionCaptor.getValue();
        assertThat(updated.getProjectId()).isEqualTo(701L);
        assertThat(updated.getProjectTaskId()).isEqualTo(801L);
        assertThat(updated.getAppCode()).isEqualTo("project");
    }
}
