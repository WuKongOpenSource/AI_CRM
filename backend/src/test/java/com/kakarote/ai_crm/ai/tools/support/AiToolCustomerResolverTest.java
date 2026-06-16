package com.kakarote.ai_crm.ai.tools.support;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.service.ICustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiToolCustomerResolverTest {

    private static final Long SESSION_ID = 1001L;

    @Mock
    private ICustomerService customerService;

    @Mock
    private AiCustomerMatcher aiCustomerMatcher;

    private AiToolCustomerResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AiToolCustomerResolver();
        ReflectionTestUtils.setField(resolver, "customerService", customerService);
        ReflectionTestUtils.setField(resolver, "aiCustomerMatcher", aiCustomerMatcher);
    }

    @AfterEach
    void tearDown() {
        AiContextHolder.clear();
        AiContextHolder.clearSession(SESSION_ID);
    }

    @Test
    void resolveForCreate_shouldFallbackToBoundCustomerWhenNoCustomerSpecified() {
        Customer customerA = customer(1L, "客户A");
        AiContextHolder.setContext(SESSION_ID, 10L, 20L, customerA.getCustomerId());
        when(customerService.getById(customerA.getCustomerId())).thenReturn(customerA);

        AiToolCustomerResolver.CustomerResolveResult result = resolver.resolveForCreate(
            null, null, "关联该客户创建任务", "创建任务失败", "创建任务");

        assertThat(result.customer()).isSameAs(customerA);
        assertThat(result.errorMessage()).isNull();
        verify(aiCustomerMatcher, never()).match(anyString());
    }

    @Test
    void resolveForCreate_shouldUseExplicitNameBeforeBoundCustomer() {
        Customer customerA = customer(1L, "客户A");
        Customer customerB = customer(2L, "客户B");
        AiContextHolder.setContext(SESSION_ID, 10L, 20L, customerA.getCustomerId());
        when(aiCustomerMatcher.match("客户B")).thenReturn(
            AiCustomerMatcher.CustomerMatchResult.matched("客户B", customerB, List.of(customerB)));

        AiToolCustomerResolver.CustomerResolveResult result = resolver.resolveForCreate(
            null, "客户B", "关联该客户创建日程", "创建日程失败", "创建日程");

        assertThat(result.customer()).isSameAs(customerB);
        assertThat(result.errorMessage()).isNull();
        verify(customerService, never()).getById(customerA.getCustomerId());
    }

    @Test
    void resolveForCreate_shouldTreatPronounsAsBoundCustomerReferences() {
        Customer customerA = customer(1L, "客户A");
        AiContextHolder.setContext(SESSION_ID, 10L, 20L, customerA.getCustomerId());
        when(customerService.getById(customerA.getCustomerId())).thenReturn(customerA);

        AiToolCustomerResolver.CustomerResolveResult result = resolver.resolveForCreate(
            null, "这个客户", "创建跟进记录", "创建跟进失败", "创建跟进记录");

        assertThat(result.customer()).isSameAs(customerA);
        assertThat(result.errorMessage()).isNull();
        verify(aiCustomerMatcher, never()).match(anyString());
    }

    @Test
    void resolveForCreate_shouldUseExplicitIdBeforeNameAndBoundCustomer() {
        Customer customerA = customer(1L, "客户A");
        Customer customerC = customer(3L, "客户C");
        AiContextHolder.setContext(SESSION_ID, 10L, 20L, customerA.getCustomerId());
        when(customerService.getById(customerC.getCustomerId())).thenReturn(customerC);

        AiToolCustomerResolver.CustomerResolveResult result = resolver.resolveForCreate(
            String.valueOf(customerC.getCustomerId()), "客户B", "关联该客户创建任务", "创建任务失败", "创建任务");

        assertThat(result.customer()).isSameAs(customerC);
        assertThat(result.errorMessage()).isNull();
        verify(aiCustomerMatcher, never()).match(anyString());
        verify(customerService, never()).getById(customerA.getCustomerId());
    }

    @Test
    void resolveForCreate_shouldAllowUnboundTaskOrScheduleWhenCustomerMissing() {
        AiToolCustomerResolver.CustomerResolveResult result = resolver.resolveForCreate(
            null, null, "关联该客户创建任务", "创建任务失败", "创建任务");

        assertThat(result.customer()).isNull();
        assertThat(result.errorMessage()).isNull();
        verify(aiCustomerMatcher, never()).match(anyString());
    }

    private Customer customer(Long id, String companyName) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setCompanyName(companyName);
        customer.setStatus(1);
        return customer;
    }
}
