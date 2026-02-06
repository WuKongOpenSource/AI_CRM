package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.FollowUp;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.FollowUpMapper;
import com.kakarote.ai_crm.service.IFollowUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 跟进记录服务实现
 */
@Service
public class FollowUpServiceImpl extends ServiceImpl<FollowUpMapper, FollowUp> implements IFollowUpService {

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFollowUp(FollowUpAddBO followUpAddBO) {
        FollowUp followUp = BeanUtil.copyProperties(followUpAddBO, FollowUp.class);
        if (followUp.getFollowTime() == null) {
            followUp.setFollowTime(new Date());
        }
        save(followUp);

        // Update customer's last contact time
        Customer customer = customerMapper.selectById(followUpAddBO.getCustomerId());
        if (customer != null) {
            customer.setLastContactTime(new Date());
            if (followUpAddBO.getNextFollowTime() != null) {
                customer.setNextFollowTime(followUpAddBO.getNextFollowTime());
            }
            customerMapper.updateById(customer);
        }

        return followUp.getFollowUpId();
    }

    @Override
    public void deleteFollowUp(Long followUpId) {
        FollowUp followUp = getById(followUpId);
        if (ObjectUtil.isNull(followUp)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "跟进记录不存在");
        }
        removeById(followUpId);
    }

    @Override
    public List<FollowUpVO> queryByCustomer(Long customerId) {
        List<FollowUp> followUps = lambdaQuery()
            .eq(FollowUp::getCustomerId, customerId)
            .orderByDesc(FollowUp::getFollowTime)
            .list();
        return BeanUtil.copyToList(followUps, FollowUpVO.class);
    }

    @Override
    public BasePage<FollowUpVO> queryPageList(FollowUpQueryBO queryBO) {
        BasePage<FollowUpVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        return page;
    }
}
