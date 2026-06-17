package com.kakarote.ai_crm.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
