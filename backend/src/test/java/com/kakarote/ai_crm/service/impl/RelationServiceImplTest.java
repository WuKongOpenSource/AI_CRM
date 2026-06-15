package com.kakarote.ai_crm.service.impl;

import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.LoginUser;
import com.kakarote.ai_crm.entity.BO.RelationAddBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.VO.RelationVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.RelationMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private FileStorageService fileStorageService;

    @Mock
    private ICustomFieldService customFieldService;

    private RelationServiceImpl relationService;

    @BeforeEach
    void setUp() {
        relationService = new RelationServiceImpl(
                relationMapper,
                contactMapper,
                customerMapper,
                fileStorageService,
                customFieldService
        );
        setCurrentUser(1001L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addRelationDefaultsSourceTypeStatusAndOwner() {
        Customer customer = new Customer();
        customer.setCustomerId(2001L);
        customer.setCompanyName("Acme");
        customer.setStatus(1);
        when(customerMapper.selectById(2001L)).thenReturn(customer);

        RelationAddBO addBO = new RelationAddBO();
        addBO.setName("Alice");
        addBO.setPhone("13800000000");
        addBO.setCustomerId(2001L);

        relationService.addRelation(addBO);

        ArgumentCaptor<Relation> relationCaptor = ArgumentCaptor.forClass(Relation.class);
        verify(relationMapper).insert(relationCaptor.capture());
        Relation saved = relationCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getSource()).isEqualTo("manual");
        assertThat(saved.getRelationType()).isEqualTo("other");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getCreateUserId()).isEqualTo(1001L);
        assertThat(saved.getUpdateUserId()).isEqualTo(1001L);
    }

    @Test
    void getOwnedRelationRejectsOtherUsers() {
        Relation relation = new Relation();
        relation.setRelationId(3001L);
        relation.setName("Bob");
        relation.setStatus(1);
        relation.setCreateUserId(2002L);
        when(relationMapper.selectById(3001L)).thenReturn(relation);

        assertThatThrownBy(() -> relationService.getOwnedRelation(3001L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getOwnedRelationVOUsesConfiguredRelationLabels() {
        Relation relation = new Relation();
        relation.setRelationId(3001L);
        relation.setName("Bob");
        relation.setStatus(1);
        relation.setCreateUserId(1001L);
        when(relationMapper.selectById(3001L)).thenReturn(relation);

        RelationVO relationVO = new RelationVO();
        relationVO.setRelationId(3001L);
        relationVO.setRelationType("decision_maker");
        relationVO.setSource("manual");
        when(relationMapper.getRelationById(3001L, 1001L)).thenReturn(relationVO);
        when(customFieldService.resolveOptionLabel("relation", "relation_type", "decision_maker"))
                .thenReturn("关键决策人");
        when(customFieldService.resolveOptionLabel("relation", "source", "manual"))
                .thenReturn("手动录入");

        RelationVO result = relationService.getOwnedRelationVO(3001L);

        assertThat(result.getRelationTypeName()).isEqualTo("关键决策人");
        assertThat(result.getSourceName()).isEqualTo("手动录入");
    }

    private void setCurrentUser(Long userId) {
        ManagerUser managerUser = new ManagerUser();
        managerUser.setUserId(userId);
        managerUser.setUsername("user" + userId);
        managerUser.setStatus(1);

        LoginUser loginUser = new LoginUser();
        loginUser.setUser(managerUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }
}
