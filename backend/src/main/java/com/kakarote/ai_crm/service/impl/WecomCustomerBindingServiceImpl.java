package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.WecomCustomerBindBO;
import com.kakarote.ai_crm.entity.BO.WecomCustomerUnbindBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.WecomCustomerBinding;
import com.kakarote.ai_crm.entity.PO.WecomExternalCustomer;
import com.kakarote.ai_crm.entity.VO.WecomCustomerBindingVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.WecomCustomerBindingMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
import com.kakarote.ai_crm.utils.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WecomCustomerBindingServiceImpl {

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INACTIVE = 0;
    private static final String BIND_STATUS_BOUND = "BOUND";
    private static final String BIND_STATUS_UNBOUND = "UNBOUND";

    @Autowired
    private WecomCustomerBindingMapper bindingMapper;

    @Autowired
    private WecomExternalCustomerMapper externalCustomerMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Transactional(rollbackFor = Exception.class)
    public WecomCustomerBinding bindCustomer(WecomCustomerBindBO bindBO) {
        if (bindBO == null || bindBO.getCustomerId() == null || bindBO.getExternalCustomerId() == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "customerId and externalCustomerId are required");
        }
        Customer customer = customerMapper.selectById(bindBO.getCustomerId());
        if (customer == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST, "Customer does not exist");
        }
        WecomExternalCustomer externalCustomer = externalCustomerMapper.selectById(bindBO.getExternalCustomerId());
        if (externalCustomer == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST, "WeCom customer does not exist");
        }

        List<WecomCustomerBinding> existingBindings = bindingMapper.selectList(
                new LambdaQueryWrapper<WecomCustomerBinding>()
                        .eq(WecomCustomerBinding::getExternalCustomerId, bindBO.getExternalCustomerId())
                        .eq(WecomCustomerBinding::getStatus, STATUS_ACTIVE)
        );
        for (WecomCustomerBinding existing : existingBindings) {
            if (Objects.equals(existing.getCustomerId(), bindBO.getCustomerId())) {
                return existing;
            }
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID,
                    "WeCom external customer is already bound to another customer");
        }

        WecomCustomerBinding binding = new WecomCustomerBinding();
        binding.setCustomerId(bindBO.getCustomerId());
        binding.setExternalCustomerId(bindBO.getExternalCustomerId());
        binding.setExternalUserId(externalCustomer.getExternalUserId());
        binding.setCorpId(externalCustomer.getCorpId());
        binding.setBindUserId(UserUtil.getUserIdOrNull());
        binding.setBindTime(new Date());
        binding.setStatus(STATUS_ACTIVE);
        binding.setRemark(bindBO.getRemark());
        bindingMapper.insert(binding);

        externalCustomer.setCustomerId(bindBO.getCustomerId());
        externalCustomer.setBindStatus(BIND_STATUS_BOUND);
        externalCustomerMapper.updateById(externalCustomer);
        return binding;
    }

    @Transactional(rollbackFor = Exception.class)
    public void unbindCustomer(WecomCustomerUnbindBO unbindBO) {
        WecomCustomerBinding binding = findActiveBinding(unbindBO);
        if (binding == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_DATA_DOES_NOT_EXIST, "Active binding does not exist");
        }
        binding.setStatus(STATUS_INACTIVE);
        binding.setUnbindTime(new Date());
        bindingMapper.updateById(binding);

        long remainingActiveBindings = bindingMapper.selectCount(Wrappers.<WecomCustomerBinding>lambdaQuery()
                .eq(WecomCustomerBinding::getExternalCustomerId, binding.getExternalCustomerId())
                .eq(WecomCustomerBinding::getStatus, STATUS_ACTIVE));
        if (remainingActiveBindings == 0) {
            WecomExternalCustomer externalCustomer = externalCustomerMapper.selectById(binding.getExternalCustomerId());
            if (externalCustomer != null) {
                externalCustomer.setCustomerId(null);
                externalCustomer.setBindStatus(BIND_STATUS_UNBOUND);
                externalCustomerMapper.updateById(externalCustomer);
            }
        }
    }

    public List<WecomCustomerBindingVO> queryByCustomerId(Long customerId) {
        if (customerId == null) {
            return List.of();
        }
        List<WecomCustomerBinding> bindings = bindingMapper.selectList(Wrappers.<WecomCustomerBinding>lambdaQuery()
                .eq(WecomCustomerBinding::getCustomerId, customerId)
                .eq(WecomCustomerBinding::getStatus, STATUS_ACTIVE)
                .orderByDesc(WecomCustomerBinding::getBindTime));
        if (bindings.isEmpty()) {
            return List.of();
        }
        List<Long> externalCustomerIds = bindings.stream()
                .map(WecomCustomerBinding::getExternalCustomerId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, WecomExternalCustomer> externalById = externalCustomerIds.isEmpty()
                ? Map.of()
                : externalCustomerMapper.selectList(Wrappers.<WecomExternalCustomer>lambdaQuery()
                        .in(WecomExternalCustomer::getId, externalCustomerIds))
                .stream()
                .collect(Collectors.toMap(WecomExternalCustomer::getId, item -> item, (left, right) -> left));
        List<WecomCustomerBindingVO> result = new ArrayList<>();
        for (WecomCustomerBinding binding : bindings) {
            WecomCustomerBindingVO vo = BeanUtil.copyProperties(binding, WecomCustomerBindingVO.class);
            WecomExternalCustomer externalCustomer = externalById.get(binding.getExternalCustomerId());
            if (externalCustomer != null) {
                vo.setExternalCustomerName(externalCustomer.getName());
                vo.setExternalCustomerAvatar(externalCustomer.getAvatar());
            }
            result.add(vo);
        }
        return result;
    }

    private WecomCustomerBinding findActiveBinding(WecomCustomerUnbindBO unbindBO) {
        if (unbindBO == null) {
            return null;
        }
        LambdaQueryWrapper<WecomCustomerBinding> wrapper = Wrappers.<WecomCustomerBinding>lambdaQuery()
                .eq(WecomCustomerBinding::getStatus, STATUS_ACTIVE);
        if (unbindBO.getBindingId() != null) {
            wrapper.eq(WecomCustomerBinding::getId, unbindBO.getBindingId());
        } else {
            wrapper.eq(unbindBO.getCustomerId() != null, WecomCustomerBinding::getCustomerId, unbindBO.getCustomerId())
                    .eq(unbindBO.getExternalCustomerId() != null, WecomCustomerBinding::getExternalCustomerId, unbindBO.getExternalCustomerId());
        }
        return bindingMapper.selectList(wrapper.last("LIMIT 1")).stream().findFirst().orElse(null);
    }
}
