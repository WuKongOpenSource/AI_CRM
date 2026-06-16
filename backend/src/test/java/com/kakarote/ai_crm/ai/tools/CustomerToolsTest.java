package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.state.PendingCustomerCreationStore;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.ICustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerToolsTest {

    private static final Long SESSION_ID = 9001L;
    private static final Long USER_ID = 1001L;

    @Mock
    private ICustomerService customerService;

    @Mock
    private ICustomFieldService customFieldService;

    private CustomerTools customerTools;
    private PendingCustomerCreationStore pendingCustomerCreationStore;

    @BeforeEach
    void setUp() {
        customerTools = new CustomerTools();
        pendingCustomerCreationStore = new PendingCustomerCreationStore();
        ReflectionTestUtils.setField(customerTools, "customerService", customerService);
        ReflectionTestUtils.setField(customerTools, "customFieldService", customFieldService);
        ReflectionTestUtils.setField(customerTools, "pendingCustomerCreationStore", pendingCustomerCreationStore);
        when(customFieldService.resolveOptionLabel(anyString(), anyString(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(2, String.class));
        AiContextHolder.setContext(SESSION_ID, USER_ID);
    }

    @AfterEach
    void tearDown() {
        pendingCustomerCreationStore.clear(SESSION_ID);
        AiContextHolder.clear();
        AiContextHolder.clearSession(SESSION_ID);
    }

    @Test
    void createCustomer_shouldRequireConfirmationWhenDuplicateExists() {
        when(customerService.findCustomersByExactCompanyName("测试客户"))
            .thenReturn(List.of(buildExistingCustomer()));

        String result = customerTools.createCustomer(
            "测试客户",
            "互联网",
            "A",
            "李四",
            "13900000000",
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertTrue(result.contains("尚未创建新客户"));
        assertTrue(result.contains("confirmPendingCustomerCreation"));
        verify(customerService, never()).addCustomer(any());
    }

    @Test
    void confirmPendingCustomerCreation_shouldCreateStoredDraftAfterUserConfirmation() {
        when(customerService.findCustomersByExactCompanyName("测试客户"))
            .thenReturn(List.of(buildExistingCustomer()));
        when(customerService.addCustomer(any())).thenReturn(4001L);

        customerTools.createCustomer(
            "测试客户",
            "互联网",
            "A",
            "李四",
            "13900000000",
            null,
            null,
            null,
            null,
            null,
            null
        );

        String result = customerTools.confirmPendingCustomerCreation();

        assertTrue(result.contains("已创建成功"));
        assertTrue(result.contains("4001"));
        verify(customerService, times(1)).addCustomer(any());
    }

    @Test
    void cancelPendingCustomerCreation_shouldClearPendingDraft() {
        when(customerService.findCustomersByExactCompanyName("测试客户"))
            .thenReturn(List.of(buildExistingCustomer()));

        customerTools.createCustomer(
            "测试客户",
            "互联网",
            "A",
            "李四",
            "13900000000",
            null,
            null,
            null,
            null,
            null,
            null
        );

        String cancelResult = customerTools.cancelPendingCustomerCreation();
        String confirmResult = customerTools.confirmPendingCustomerCreation();

        assertTrue(cancelResult.contains("已取消重复客户创建请求"));
        assertTrue(confirmResult.contains("当前没有待确认"));
        verify(customerService, never()).addCustomer(any());
    }

    private Customer buildExistingCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(3001L);
        customer.setCompanyName("测试客户");
        customer.setLevel("A");
        customer.setStage("lead");
        customer.setPrimaryContactName("张三");
        customer.setPrimaryContactPhone("13800000000");
        return customer;
    }
}
