package com.kakarote.ai_crm.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.TencentMeetingBindBO;
import com.kakarote.ai_crm.entity.BO.TencentMeetingUnbindBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.TencentMeeting;
import com.kakarote.ai_crm.entity.PO.TencentMeetingCustomerBinding;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingCustomerBindingMapper;
import com.kakarote.ai_crm.mapper.TencentMeetingMapper;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class TencentMeetingCustomerBindingServiceImpl {

    @Autowired
    private TencentMeetingCustomerBindingMapper bindingMapper;

    @Autowired
    private TencentMeetingMapper meetingMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Transactional(rollbackFor = Exception.class)
    public void bindCustomer(TencentMeetingBindBO bindBO) {
        if (bindBO == null || bindBO.getMeetingId() == null || bindBO.getCustomerId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "meetingId and customerId are required");
        }
        TencentMeeting meeting = meetingMapper.selectById(bindBO.getMeetingId());
        if (meeting == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent meeting not found");
        }
        Customer customer = customerMapper.selectById(bindBO.getCustomerId());
        if (customer == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Customer not found");
        }

        List<TencentMeetingCustomerBinding> existingBindings = bindingMapper.selectList(
                Wrappers.<TencentMeetingCustomerBinding>lambdaQuery()
                        .eq(TencentMeetingCustomerBinding::getMeetingId, bindBO.getMeetingId())
                        .eq(TencentMeetingCustomerBinding::getStatus, 1));
        for (TencentMeetingCustomerBinding existing : existingBindings) {
            if (!bindBO.getCustomerId().equals(existing.getCustomerId())) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "Tencent meeting is already bound to another customer");
            }
            return;
        }

        TencentMeetingCustomerBinding binding = new TencentMeetingCustomerBinding();
        binding.setMeetingId(bindBO.getMeetingId());
        binding.setMeetingExternalId(meeting.getMeetingId());
        binding.setCustomerId(bindBO.getCustomerId());
        binding.setBindUserId(UserUtil.getUserIdOrNull());
        binding.setBindTime(new Date());
        binding.setStatus(1);
        bindingMapper.insert(binding);

        meeting.setBindStatus("BOUND");
        meeting.setCustomerId(customer.getCustomerId());
        meeting.setCustomerName(customer.getCompanyName());
        meetingMapper.updateById(meeting);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindCustomer(TencentMeetingUnbindBO unbindBO) {
        if (unbindBO == null || unbindBO.getMeetingId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "meetingId is required");
        }
        List<TencentMeetingCustomerBinding> bindings = bindingMapper.selectList(
                Wrappers.<TencentMeetingCustomerBinding>lambdaQuery()
                        .eq(TencentMeetingCustomerBinding::getMeetingId, unbindBO.getMeetingId())
                        .eq(TencentMeetingCustomerBinding::getStatus, 1));
        Date now = new Date();
        for (TencentMeetingCustomerBinding binding : bindings) {
            binding.setStatus(0);
            binding.setUnbindTime(now);
            bindingMapper.updateById(binding);
        }
        TencentMeeting meeting = meetingMapper.selectById(unbindBO.getMeetingId());
        if (meeting != null) {
            meeting.setBindStatus("UNBOUND");
            meeting.setCustomerId(null);
            meeting.setCustomerName(null);
            meetingMapper.updateById(meeting);
        }
    }
}
