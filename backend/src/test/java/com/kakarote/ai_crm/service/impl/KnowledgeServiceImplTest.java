package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.PO.KnowledgeTag;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.ContactVO;
import com.kakarote.ai_crm.entity.VO.CustomerDetailVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.mapper.KnowledgeTagMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.WeKnoraClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceImplTest {

    @Mock
    private KnowledgeMapper knowledgeMapper;

    @Mock
    private KnowledgeTagMapper knowledgeTagMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private IGlobalSearchIndexService globalSearchIndexService;

    @Mock
    private WeKnoraClient weKnoraClient;

    @Mock
    private FileStorageService fileStorageService;

    private KnowledgeServiceImpl knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeServiceImpl();
        ReflectionTestUtils.setField(knowledgeService, "baseMapper", knowledgeMapper);
        ReflectionTestUtils.setField(knowledgeService, "knowledgeTagMapper", knowledgeTagMapper);
        ReflectionTestUtils.setField(knowledgeService, "customerMapper", customerMapper);
        ReflectionTestUtils.setField(knowledgeService, "globalSearchIndexService", globalSearchIndexService);
        ReflectionTestUtils.setField(knowledgeService, "weKnoraClient", weKnoraClient);
        ReflectionTestUtils.setField(knowledgeService, "fileStorageService", fileStorageService);
    }

    @AfterEach
    void tearDown() {
        AiContextHolder.clear();
    }

    @Test
    void shouldRemoveNullBytesAndControlCharactersBeforePersistingSearchableContent() {
        String normalized = ReflectionTestUtils.invokeMethod(
            knowledgeService,
            "normalizeSearchableContent",
            "Alpha\u0000Beta\u0007 \n Gamma\t"
        );

        assertThat(normalized).isEqualTo("AlphaBeta Gamma");
    }

    @Test
    void shouldReturnNullWhenSearchableContentOnlyContainsUnsupportedCharacters() {
        String normalized = ReflectionTestUtils.invokeMethod(
            knowledgeService,
            "normalizeSearchableContent",
            "\u0000\u0007\t  \n"
        );

        assertThat(normalized).isNull();
    }

    @Test
    void uploadFileRefreshesKnowledgeSearchIndexAfterInsert() {
        AiContextHolder.setContext(9001L, 7L);
        when(knowledgeMapper.insert(any(Knowledge.class))).thenAnswer(invocation -> {
            Knowledge knowledge = invocation.getArgument(0);
            knowledge.setKnowledgeId(5001L);
            return 1;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "demo.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        Long knowledgeId = knowledgeService.uploadFile(file, "document", null, "summary");

        assertThat(knowledgeId).isEqualTo(5001L);
        verify(globalSearchIndexService).refreshKnowledgeIndex(5001L);
    }

    @Test
    void archiveExistingStandaloneFileRefreshesKnowledgeSearchIndexAfterInsert() {
        AiContextHolder.setContext(9002L, 7L);
        when(knowledgeMapper.insert(any(Knowledge.class))).thenAnswer(invocation -> {
            Knowledge knowledge = invocation.getArgument(0);
            knowledge.setKnowledgeId(5002L);
            return 1;
        });
        when(fileStorageService.getFileStream("docs/demo.txt"))
                .thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        Long knowledgeId = knowledgeService.archiveExistingStandaloneFile(
                "demo.txt",
                "docs/demo.txt",
                5L,
                "text/plain",
                null,
                "summary"
        );

        assertThat(knowledgeId).isEqualTo(5002L);
        verify(globalSearchIndexService).refreshKnowledgeIndex(5002L);
    }

    @Test
    void archiveTextRefreshesKnowledgeSearchIndexAfterInsert() {
        AiContextHolder.setContext(9003L, 7L);
        when(knowledgeMapper.insert(any(Knowledge.class))).thenAnswer(invocation -> {
            Knowledge knowledge = invocation.getArgument(0);
            knowledge.setKnowledgeId(5003L);
            return 1;
        });

        Long knowledgeId = knowledgeService.archiveText(
                "note.txt",
                "hello",
                "document",
                null,
                "summary"
        );

        assertThat(knowledgeId).isEqualTo(5003L);
        verify(globalSearchIndexService).refreshKnowledgeIndex(5003L);
    }

    @Test
    void archiveTextStoresTextFileAndQueuesWeKnoraUploadWhenEnabled() {
        AiContextHolder.setContext(9004L, 7L);
        KnowledgeServiceImpl self = mock(KnowledgeServiceImpl.class);
        ReflectionTestUtils.setField(knowledgeService, "self", self);
        when(weKnoraClient.isEnabled()).thenReturn(true);
        when(knowledgeMapper.insert(any(Knowledge.class))).thenAnswer(invocation -> {
            Knowledge knowledge = invocation.getArgument(0);
            knowledge.setKnowledgeId(5004L);
            return 1;
        });

        Long knowledgeId = knowledgeService.archiveText(
                "note",
                "hello",
                "email",
                null,
                "summary"
        );

        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Knowledge> knowledgeCaptor = ArgumentCaptor.forClass(Knowledge.class);
        verify(fileStorageService).upload(
                any(ByteArrayInputStream.class),
                org.mockito.ArgumentMatchers.eq(5L),
                pathCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("text/plain;charset=UTF-8")
        );
        verify(knowledgeMapper).insert(knowledgeCaptor.capture());
        Knowledge inserted = knowledgeCaptor.getValue();
        assertThat(knowledgeId).isEqualTo(5004L);
        assertThat(inserted.getName()).isEqualTo("note.txt");
        assertThat(inserted.getType()).isEqualTo("email");
        assertThat(inserted.getFilePath()).isEqualTo(pathCaptor.getValue());
        assertThat(inserted.getFileSize()).isEqualTo(5L);
        assertThat(inserted.getMimeType()).isEqualTo("text/plain");
        assertThat(inserted.getContentText()).isEqualTo("hello");
        assertThat(inserted.getWeKnoraParseStatus()).isEqualTo("pending");
        verify(self).asyncUploadToWeKnora(5004L, pathCaptor.getValue(), "note.txt");
    }

    @Test
    void updateCustomerRefreshesKnowledgeSearchIndex() {
        Knowledge knowledge = new Knowledge();
        knowledge.setKnowledgeId(5001L);
        knowledge.setCustomerId(1001L);
        Customer customer = new Customer();
        customer.setCustomerId(2002L);

        when(knowledgeMapper.selectById(5001L)).thenReturn(knowledge);
        when(customerMapper.selectById(2002L)).thenReturn(customer);

        knowledgeService.updateCustomer(5001L, 2002L);

        ArgumentCaptor<Knowledge> knowledgeCaptor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeMapper).updateById(knowledgeCaptor.capture());
        assertThat(knowledgeCaptor.getValue().getCustomerId()).isEqualTo(2002L);
        verify(globalSearchIndexService).refreshKnowledgeIndex(5001L);
    }

    @Test
    void addTagRefreshesKnowledgeSearchIndexAfterInsert() {
        Knowledge knowledge = new Knowledge();
        knowledge.setKnowledgeId(5001L);

        when(knowledgeMapper.selectById(5001L)).thenReturn(knowledge);
        when(knowledgeTagMapper.selectCount(any())).thenReturn(0L);

        knowledgeService.addTag(5001L, "方案");

        ArgumentCaptor<KnowledgeTag> tagCaptor = ArgumentCaptor.forClass(KnowledgeTag.class);
        verify(knowledgeTagMapper).insert(tagCaptor.capture());
        assertThat(tagCaptor.getValue().getKnowledgeId()).isEqualTo(5001L);
        assertThat(tagCaptor.getValue().getTagName()).isEqualTo("方案");
        verify(globalSearchIndexService).refreshKnowledgeIndex(5001L);
    }

    @Test
    void deleteKnowledgeRemovesKnowledgeSearchIndex() {
        Knowledge knowledge = new Knowledge();
        knowledge.setKnowledgeId(5001L);

        when(knowledgeMapper.selectById(5001L)).thenReturn(knowledge);

        knowledgeService.deleteKnowledge(5001L);

        verify(knowledgeMapper).deleteById(5001L);
        verify(knowledgeTagMapper).delete(any());
        verify(globalSearchIndexService).deleteByEntity("knowledge", 5001L);
    }

    @Test
    void targetedCustomerContextIncludesAiContactAndTaskSignals() {
        CustomerDetailVO detail = new CustomerDetailVO();
        detail.setCompanyName("悟空软件");
        detail.setAiStatusDetection("预算明确");
        detail.setAiInsight("优先推进续约");

        ContactVO contact = new ContactVO();
        contact.setName("张三");
        contact.setPosition("CTO");
        contact.setPhone("13800000000");
        contact.setIsPrimary(1);
        detail.setContacts(List.of(contact));

        Task task = new Task();
        task.setTitle("确认预算");
        task.setStatus("todo");
        detail.setTasks(List.of(task));

        FollowUpVO followUp = new FollowUpVO();
        followUp.setFollowTime(new Date(0L));
        followUp.setTypeName("电话");
        followUp.setSummary("已确认需求");

        String context = ReflectionTestUtils.invokeMethod(
                knowledgeService,
                "buildTargetedCustomerContext",
                detail,
                List.of(followUp)
        );

        assertThat(context)
                .contains("AI 状态判断: 预算明确")
                .contains("AI 洞察: 优先推进续约")
                .contains("主联系人: 张三 / CTO / 13800000000")
                .contains("近期任务: 确认预算")
                .contains("已确认需求");
    }
}
