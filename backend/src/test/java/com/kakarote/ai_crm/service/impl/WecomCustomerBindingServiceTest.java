package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.entity.BO.WecomCustomerBindBO;
import com.kakarote.ai_crm.entity.PO.WecomCustomerBinding;
import com.kakarote.ai_crm.entity.PO.WecomExternalCustomer;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.WecomCustomerBindingMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
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

class WecomCustomerBindingServiceTest {

    @Test
    void bindShouldRejectExternalCustomerAlreadyBoundToAnotherCustomer() {
        WecomCustomerBindingServiceImpl service = newService();
        WecomCustomerBindingMapper bindingMapper = mapper(service, "bindingMapper");
        WecomExternalCustomerMapper externalCustomerMapper = mapper(service, "externalCustomerMapper");
        CustomerMapper customerMapper = mapper(service, "customerMapper");

        WecomCustomerBinding existing = new WecomCustomerBinding();
        existing.setId(1L);
        existing.setExternalCustomerId(200L);
        existing.setCustomerId(100L);
        existing.setStatus(1);
        WecomExternalCustomer externalCustomer = new WecomExternalCustomer();
        externalCustomer.setId(200L);
        when(bindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(existing));
        when(externalCustomerMapper.selectById(200L)).thenReturn(externalCustomer);
        when(customerMapper.selectById(101L)).thenReturn(new com.kakarote.ai_crm.entity.PO.Customer());

        WecomCustomerBindBO bindBO = new WecomCustomerBindBO();
        bindBO.setCustomerId(101L);
        bindBO.setExternalCustomerId(200L);

        assertThatThrownBy(() -> service.bindCustomer(bindBO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already bound");
        verify(bindingMapper, never()).insert(any(WecomCustomerBinding.class));
    }

    @Test
    void bindShouldCreateActiveBindingAndMarkExternalCustomerBound() {
        WecomCustomerBindingServiceImpl service = newService();
        WecomCustomerBindingMapper bindingMapper = mapper(service, "bindingMapper");
        WecomExternalCustomerMapper externalCustomerMapper = mapper(service, "externalCustomerMapper");
        CustomerMapper customerMapper = mapper(service, "customerMapper");

        WecomExternalCustomer externalCustomer = new WecomExternalCustomer();
        externalCustomer.setId(200L);
        externalCustomer.setBindStatus("UNBOUND");
        when(bindingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(externalCustomerMapper.selectById(200L)).thenReturn(externalCustomer);
        when(customerMapper.selectById(100L)).thenReturn(new com.kakarote.ai_crm.entity.PO.Customer());

        WecomCustomerBindBO bindBO = new WecomCustomerBindBO();
        bindBO.setCustomerId(100L);
        bindBO.setExternalCustomerId(200L);

        service.bindCustomer(bindBO);

        ArgumentCaptor<WecomCustomerBinding> inserted = ArgumentCaptor.forClass(WecomCustomerBinding.class);
        verify(bindingMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getCustomerId()).isEqualTo(100L);
        assertThat(inserted.getValue().getExternalCustomerId()).isEqualTo(200L);
        assertThat(inserted.getValue().getStatus()).isEqualTo(1);
        assertThat(inserted.getValue().getBindTime()).isInstanceOf(Date.class);
        assertThat(externalCustomer.getBindStatus()).isEqualTo("BOUND");
        assertThat(externalCustomer.getCustomerId()).isEqualTo(100L);
        verify(externalCustomerMapper).updateById(externalCustomer);
    }

    @SuppressWarnings("unchecked")
    private static <T> T mapper(WecomCustomerBindingServiceImpl service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private static WecomCustomerBindingServiceImpl newService() {
        WecomCustomerBindingServiceImpl service = new WecomCustomerBindingServiceImpl();
        ReflectionTestUtils.setField(service, "bindingMapper", mock(WecomCustomerBindingMapper.class));
        ReflectionTestUtils.setField(service, "externalCustomerMapper", mock(WecomExternalCustomerMapper.class));
        ReflectionTestUtils.setField(service, "customerMapper", mock(CustomerMapper.class));
        return service;
    }
}
