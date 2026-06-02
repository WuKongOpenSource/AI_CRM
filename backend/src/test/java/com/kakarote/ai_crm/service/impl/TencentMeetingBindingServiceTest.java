package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.TencentMeetingBindBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCustomerBinding;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingCustomerBindingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TencentMeetingBindingServiceTest {

    @Test
    void bindShouldRejectMeetingAlreadyBoundToAnotherCustomer() {
        TencentMeetingCustomerBindingServiceImpl service = newService();
        TencentMeetingCustomerBindingMapper bindingMapper = mapper(service, "bindingMapper");
        TencentMeetingMapper meetingMapper = mapper(service, "meetingMapper");
        CustomerMapper customerMapper = mapper(service, "customerMapper");

        TencentMeetingCustomerBinding existing = new TencentMeetingCustomerBinding();
        existing.setMeetingId(200L);
        existing.setCustomerId(100L);
        existing.setStatus(1);
        when(bindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));
        when(meetingMapper.selectById(200L)).thenReturn(new TencentMeeting());
        when(customerMapper.selectById(101L)).thenReturn(new Customer());

        TencentMeetingBindBO bindBO = new TencentMeetingBindBO();
        bindBO.setMeetingId(200L);
        bindBO.setCustomerId(101L);

        assertThatThrownBy(() -> service.bindCustomer(bindBO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already bound");
        verify(bindingMapper, never()).insert(any(TencentMeetingCustomerBinding.class));
    }

    @Test
    void bindShouldCreateActiveBindingAndMarkMeetingBound() {
        TencentMeetingCustomerBindingServiceImpl service = newService();
        TencentMeetingCustomerBindingMapper bindingMapper = mapper(service, "bindingMapper");
        TencentMeetingMapper meetingMapper = mapper(service, "meetingMapper");
        CustomerMapper customerMapper = mapper(service, "customerMapper");

        TencentMeeting meeting = new TencentMeeting();
        meeting.setId(200L);
        meeting.setBindStatus("UNBOUND");
        Customer customer = new Customer();
        customer.setCustomerId(100L);
        customer.setCompanyName("北京科技有限公司");
        when(bindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(meetingMapper.selectById(200L)).thenReturn(meeting);
        when(customerMapper.selectById(100L)).thenReturn(customer);

        TencentMeetingBindBO bindBO = new TencentMeetingBindBO();
        bindBO.setMeetingId(200L);
        bindBO.setCustomerId(100L);

        service.bindCustomer(bindBO);

        ArgumentCaptor<TencentMeetingCustomerBinding> inserted = ArgumentCaptor.forClass(TencentMeetingCustomerBinding.class);
        verify(bindingMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getMeetingId()).isEqualTo(200L);
        assertThat(inserted.getValue().getCustomerId()).isEqualTo(100L);
        assertThat(inserted.getValue().getStatus()).isEqualTo(1);
        assertThat(inserted.getValue().getBindTime()).isInstanceOf(Date.class);
        assertThat(meeting.getBindStatus()).isEqualTo("BOUND");
        assertThat(meeting.getCustomerId()).isEqualTo(100L);
        assertThat(meeting.getCustomerName()).isEqualTo("北京科技有限公司");
        verify(meetingMapper).updateById(meeting);
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(TencentMeetingCustomerBindingServiceImpl service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static TencentMeetingCustomerBindingServiceImpl newService() {
        TencentMeetingCustomerBindingServiceImpl service = new TencentMeetingCustomerBindingServiceImpl();
        ReflectionTestUtils.setField(service, "bindingMapper", mock(TencentMeetingCustomerBindingMapper.class));
        ReflectionTestUtils.setField(service, "meetingMapper", mock(TencentMeetingMapper.class));
        ReflectionTestUtils.setField(service, "customerMapper", mock(CustomerMapper.class));
        return service;
    }
}
