package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.RelationQueryBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.RelationMapper;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelationServiceImplTest {

    @Mock
    private RelationMapper relationMapper;

    @Mock
    private ContactMapper contactMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerLogoService customerLogoService;

    @Mock
    private ICustomFieldService customFieldService;

    @Mock
    private IGlobalSearchIndexService globalSearchIndexService;

    @InjectMocks
    private RelationServiceImpl relationService;

    @BeforeEach
    void setUp() {
        AiContextHolder.bindThreadContext(1L, 100L);
        ReflectionTestUtils.setField(relationService, "baseMapper", relationMapper);
    }

    @AfterEach
    void tearDown() {
        AiContextHolder.clearThreadContext();
    }

    @Test
    void addFromContactReturnsExistingRelationForCurrentUser() {
        Relation existing = new Relation();
        existing.setRelationId(9001L);
        existing.setCreateUserId(100L);
        existing.setSourceContactId(3001L);
        existing.setStatus(1);
        when(relationMapper.selectOne(any())).thenReturn(existing);

        Long relationId = relationService.addFromContact(3001L);

        assertThat(relationId).isEqualTo(9001L);
        verify(contactMapper, never()).selectById(any());
        verify(relationMapper, never()).insert(any(Relation.class));
    }

    @Test
    void getOwnedRelationRejectsRelationOwnedByAnotherUser() {
        Relation relation = new Relation();
        relation.setRelationId(9002L);
        relation.setCreateUserId(200L);
        relation.setStatus(1);
        when(relationMapper.selectById(9002L)).thenReturn(relation);

        assertThatThrownBy(() -> relationService.getOwnedRelation(9002L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void addFromContactCopiesContactFieldsWhenNoExistingRelation() {
        when(relationMapper.selectOne(any())).thenReturn(null);
        Contact contact = new Contact();
        contact.setContactId(3002L);
        contact.setCustomerId(7001L);
        contact.setName("张三");
        contact.setPhone("13800000000");
        contact.setEmail("zhangsan@example.com");
        contact.setWechat("zhangsan_wx");
        contact.setNotes("重点联系人");
        Customer customer = new Customer();
        customer.setCustomerId(7001L);
        customer.setCompanyName("星河科技");
        when(contactMapper.selectById(3002L)).thenReturn(contact);
        when(customerMapper.selectById(7001L)).thenReturn(customer);

        relationService.addFromContact(3002L);

        ArgumentCaptor<Relation> relationCaptor = ArgumentCaptor.forClass(Relation.class);
        verify(relationMapper).insert(relationCaptor.capture());
        Relation savedRelation = relationCaptor.getValue();
        assertThat(savedRelation.getCustomerId()).isEqualTo(7001L);
        assertThat(savedRelation.getCompany()).isNull();
        assertThat(savedRelation.getAvatar()).isNull();
    }

    @Test
    void toRelationVOResolvesRelationAvatarAndLinkedCustomerFields() {
        Customer customer = new Customer();
        customer.setCustomerId(9001L);
        customer.setCompanyName("星河科技");
        customer.setLogo("customers/star-logo.png");

        when(customFieldService.resolveOptionLabel("relation", "relationType", "partner"))
                .thenReturn("合作伙伴");
        when(customFieldService.resolveOptionLabel("relation", "source", "manual"))
                .thenReturn("手动创建");
        when(customerMapper.selectByIdIgnoreDataPermission(9001L)).thenReturn(customer);
        when(customerLogoService.resolveLogoUrl("customers/star-logo.png"))
                .thenReturn("https://cdn.example.com/customers/star-logo.png");
        when(customerLogoService.resolveLogoUrl("relation/avatar.png"))
                .thenReturn("https://cdn.example.com/relation/avatar.png");

        Relation relation = new Relation();
        relation.setRelationId(1001L);
        relation.setName("张三");
        relation.setAvatar("relation/avatar.png");
        relation.setRelationType("partner");
        relation.setSource("manual");
        relation.setCustomerId(9001L);

        RelationVO vo = ReflectionTestUtils.invokeMethod(relationService, "toRelationVO", relation);

        assertThat(vo).isNotNull();
        assertThat(vo.getCustomerId()).isEqualTo(9001L);
        assertThat(vo.getCustomerName()).isEqualTo("星河科技");
        assertThat(vo.getCustomerLogo()).isEqualTo("customers/star-logo.png");
        assertThat(vo.getCustomerLogoUrl()).isEqualTo("https://cdn.example.com/customers/star-logo.png");
        assertThat(vo.getAvatar()).isEqualTo("relation/avatar.png");
        assertThat(vo.getAvatarUrl()).isEqualTo("https://cdn.example.com/relation/avatar.png");
        assertThat(vo.getCompany()).isNull();
        assertThat(vo.getRelationTypeName()).isEqualTo("合作伙伴");
        assertThat(vo.getSourceName()).isEqualTo("手动创建");
    }

    @Test
    void getOwnedRelationVOUsesJoinedCustomerFieldsFromRelationMapper() {
        Relation relation = new Relation();
        relation.setRelationId(1002L);
        relation.setName("李四");
        relation.setCreateUserId(100L);
        relation.setStatus(1);
        relation.setRelationType("partner");
        relation.setSource("manual");
        relation.setCustomerId(9002L);

        RelationVO joinedVO = new RelationVO();
        joinedVO.setRelationId(1002L);
        joinedVO.setName("李四");
        joinedVO.setAvatar("relation/lisi-avatar.png");
        joinedVO.setRelationType("partner");
        joinedVO.setSource("manual");
        joinedVO.setCustomerId(9002L);
        joinedVO.setCustomerName("银河客户");
        joinedVO.setCustomerLogo("customers/galaxy-logo.png");

        when(relationMapper.selectById(1002L)).thenReturn(relation);
        when(relationMapper.getRelationById(1002L, 100L)).thenReturn(joinedVO);
        when(customFieldService.resolveOptionLabel("relation", "relationType", "partner"))
                .thenReturn("合作伙伴");
        when(customFieldService.resolveOptionLabel("relation", "source", "manual"))
                .thenReturn("手动创建");
        when(customerLogoService.resolveLogoUrl("customers/galaxy-logo.png"))
                .thenReturn("https://cdn.example.com/customers/galaxy-logo.png");
        when(customerLogoService.resolveLogoUrl("relation/lisi-avatar.png"))
                .thenReturn("https://cdn.example.com/relation/lisi-avatar.png");

        RelationVO vo = relationService.getOwnedRelationVO(1002L);

        assertThat(vo.getCustomerId()).isEqualTo(9002L);
        assertThat(vo.getCustomerName()).isEqualTo("银河客户");
        assertThat(vo.getCustomerLogo()).isEqualTo("customers/galaxy-logo.png");
        assertThat(vo.getCustomerLogoUrl()).isEqualTo("https://cdn.example.com/customers/galaxy-logo.png");
        assertThat(vo.getAvatar()).isEqualTo("relation/lisi-avatar.png");
        assertThat(vo.getAvatarUrl()).isEqualTo("https://cdn.example.com/relation/lisi-avatar.png");
        assertThat(vo.getCompany()).isNull();
        assertThat(vo.getRelationTypeName()).isEqualTo("合作伙伴");
        assertThat(vo.getSourceName()).isEqualTo("手动创建");
    }

    @Test
    void queryPageListUsesJoinedCustomerFieldsFromRelationMapper() {
        RelationVO joinedVO = new RelationVO();
        joinedVO.setRelationId(1003L);
        joinedVO.setName("王五");
        joinedVO.setAvatar("relation/wangwu-avatar.png");
        joinedVO.setRelationType("partner");
        joinedVO.setSource("manual");
        joinedVO.setCustomerId(9003L);
        joinedVO.setCustomerName("星云客户");
        joinedVO.setCustomerLogo("customers/nebula-logo.png");

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            BasePage<RelationVO> page = invocation.getArgument(0, BasePage.class);
            page.setRecords(List.of(joinedVO));
            page.setTotal(1);
            return page;
        }).when(relationMapper).queryPageList(any(), any(RelationQueryBO.class), anyLong());
        when(customFieldService.resolveOptionLabel("relation", "relationType", "partner"))
                .thenReturn("合作伙伴");
        when(customFieldService.resolveOptionLabel("relation", "source", "manual"))
                .thenReturn("手动创建");
        when(customerLogoService.resolveLogoUrl("customers/nebula-logo.png"))
                .thenReturn("https://cdn.example.com/customers/nebula-logo.png");
        when(customerLogoService.resolveLogoUrl("relation/wangwu-avatar.png"))
                .thenReturn("https://cdn.example.com/relation/wangwu-avatar.png");

        BasePage<RelationVO> page = relationService.queryPageList(new RelationQueryBO());

        assertThat(page.getRecords()).hasSize(1);
        RelationVO vo = page.getRecords().get(0);
        assertThat(vo.getCustomerId()).isEqualTo(9003L);
        assertThat(vo.getCustomerName()).isEqualTo("星云客户");
        assertThat(vo.getCustomerLogo()).isEqualTo("customers/nebula-logo.png");
        assertThat(vo.getCustomerLogoUrl()).isEqualTo("https://cdn.example.com/customers/nebula-logo.png");
        assertThat(vo.getAvatar()).isEqualTo("relation/wangwu-avatar.png");
        assertThat(vo.getAvatarUrl()).isEqualTo("https://cdn.example.com/relation/wangwu-avatar.png");
        assertThat(vo.getCompany()).isNull();
        assertThat(vo.getRelationTypeName()).isEqualTo("合作伙伴");
        assertThat(vo.getSourceName()).isEqualTo("手动创建");
    }
}
