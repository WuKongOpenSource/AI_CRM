package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.ai.tools.support.AiToolCustomerResolver;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.ScheduleAddBO;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.service.IContactService;
import com.kakarote.ai_crm.service.IFollowUpService;
import com.kakarote.ai_crm.service.IScheduleService;
import com.kakarote.ai_crm.service.ITaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerScopedAiToolsTest {

    @Mock
    private ITaskService taskService;

    @Mock
    private IScheduleService scheduleService;

    @Mock
    private IFollowUpService followUpService;

    @Mock
    private IContactService contactService;

    @Mock
    private AiToolCustomerResolver customerResolver;

    private TaskTools taskTools;
    private ScheduleTools scheduleTools;
    private FollowupTools followupTools;

    @BeforeEach
    void setUp() {
        taskTools = new TaskTools();
        ReflectionTestUtils.setField(taskTools, "taskService", taskService);
        ReflectionTestUtils.setField(taskTools, "customerResolver", customerResolver);

        scheduleTools = new ScheduleTools();
        ReflectionTestUtils.setField(scheduleTools, "scheduleService", scheduleService);
        ReflectionTestUtils.setField(scheduleTools, "contactService", contactService);
        ReflectionTestUtils.setField(scheduleTools, "customerResolver", customerResolver);

        followupTools = new FollowupTools();
        ReflectionTestUtils.setField(followupTools, "followUpService", followUpService);
        ReflectionTestUtils.setField(followupTools, "contactService", contactService);
        ReflectionTestUtils.setField(followupTools, "customerResolver", customerResolver);
    }

    @Test
    void createTask_shouldAssociateResolvedCurrentCustomer() {
        Customer customerA = customer(1L, "客户A");
        when(customerResolver.resolveForCreate(
            null, null, "关联该客户创建任务", "创建任务失败", "创建任务"))
            .thenReturn(new AiToolCustomerResolver.CustomerResolveResult(customerA, null));
        when(taskService.addTask(any(TaskAddBO.class))).thenReturn(101L);

        String result = taskTools.createTask(null, "准备报价", null, null, "medium", "2026-04-28");

        ArgumentCaptor<TaskAddBO> captor = ArgumentCaptor.forClass(TaskAddBO.class);
        verify(taskService).addTask(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerA.getCustomerId());
        assertThat(result).contains("customerId: 1");
    }

    @Test
    void createSchedule_shouldAssociateExplicitlyResolvedCustomer() {
        Customer customerB = customer(2L, "客户B");
        when(customerResolver.resolveForCreate(
            null, "客户B", "关联该客户创建日程", "创建日程失败", "创建日程"))
            .thenReturn(new AiToolCustomerResolver.CustomerResolveResult(customerB, null));
        when(scheduleService.addSchedule(any(ScheduleAddBO.class))).thenReturn(202L);

        String result = scheduleTools.createSchedule(
            null, "客户B方案会", "2026-04-22 10:00", null, "meeting", "客户B", null, null, null);

        ArgumentCaptor<ScheduleAddBO> captor = ArgumentCaptor.forClass(ScheduleAddBO.class);
        verify(scheduleService).addSchedule(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerB.getCustomerId());
        assertThat(result).contains("customerId: 2");
    }

    @Test
    void createFollowUp_shouldAllowBlankCustomerNameWhenResolverProvidesBoundCustomer() {
        Customer customerA = customer(1L, "客户A");
        when(customerResolver.resolveForCreate(
            null, null, "创建跟进记录", "创建跟进失败", "创建跟进记录"))
            .thenReturn(new AiToolCustomerResolver.CustomerResolveResult(customerA, null));
        when(followUpService.addFollowUp(any(FollowUpAddBO.class))).thenReturn(303L);

        String result = followupTools.createFollowUp(
            null, null, "visit", "已沟通采购计划", "2026-04-21 09:30", null, null);

        ArgumentCaptor<FollowUpAddBO> captor = ArgumentCaptor.forClass(FollowUpAddBO.class);
        verify(followUpService).addFollowUp(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerA.getCustomerId());
        assertThat(result).contains("customerId: 1");
    }

    @Test
    void createFollowUp_shouldUseOtherForQuotationWithoutExplicitChannel() {
        FollowUpAddBO followUp = createFollowUpAndCapture("email", "\u4eca\u65e5\u5b8c\u6210\u62a5\u4ef7");

        assertThat(followUp.getType()).isEqualTo("other");
    }

    @Test
    void createFollowUp_shouldUseEmailOnlyWhenEmailKeywordExists() {
        FollowUpAddBO followUp = createFollowUpAndCapture(null, "\u53d1\u90ae\u4ef6\u62a5\u4ef7");

        assertThat(followUp.getType()).isEqualTo("email");
    }

    @Test
    void createFollowUp_shouldDetectPhoneMeetingAndVisitChannels() {
        assertThat(createFollowUpAndCapture(null, "\u7535\u8bdd\u6c9f\u901a\u62a5\u4ef7").getType()).isEqualTo("call");
        assertThat(createFollowUpAndCapture(null, "\u4f1a\u8bae\u786e\u8ba4\u5408\u540c").getType()).isEqualTo("meeting");
        assertThat(createFollowUpAndCapture(null, "\u4e0a\u95e8\u62dc\u8bbf\u5ba2\u6237").getType()).isEqualTo("visit");
    }

    private FollowUpAddBO createFollowUpAndCapture(String requestedType, String content) {
        Customer customer = customer(9L, "Acme");
        when(customerResolver.resolveForCreate(any(), any(), any(), any(), any()))
            .thenReturn(new AiToolCustomerResolver.CustomerResolveResult(customer, null));
        when(followUpService.addFollowUp(any(FollowUpAddBO.class))).thenReturn(909L);

        followupTools.createFollowUp(null, null, requestedType, content, "2026-05-25 10:00", null, null);

        ArgumentCaptor<FollowUpAddBO> captor = ArgumentCaptor.forClass(FollowUpAddBO.class);
        verify(followUpService).addFollowUp(captor.capture());
        FollowUpAddBO followUp = captor.getValue();
        clearInvocations(followUpService, customerResolver);
        return followUp;
    }

    private Customer customer(Long id, String companyName) {
        Customer customer = new Customer();
        customer.setCustomerId(id);
        customer.setCompanyName(companyName);
        customer.setStatus(1);
        return customer;
    }
}
