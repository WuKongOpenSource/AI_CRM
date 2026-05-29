package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.AiCreditRecordQueryBO;
import com.kakarote.ai_crm.entity.PO.AiCreditRecord;
import com.kakarote.ai_crm.entity.VO.AiCreditRecordVO;
import com.kakarote.ai_crm.mapper.AiCreditRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class AiCreditRecordService extends ServiceImpl<AiCreditRecordMapper, AiCreditRecord> {

    public BasePage<AiCreditRecordVO> queryPageList(AiCreditRecordQueryBO queryBO) {
        AiCreditRecordQueryBO query = queryBO != null ? queryBO : new AiCreditRecordQueryBO();
        LambdaQueryWrapper<AiCreditRecord> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getActionName())) {
            wrapper.eq(AiCreditRecord::getActionName, query.getActionName().trim());
        }
        if (StrUtil.isNotBlank(query.getModelSource())) {
            wrapper.eq(AiCreditRecord::getModelSource, query.getModelSource().trim().toLowerCase());
        }
        if (Boolean.TRUE.equals(query.getChargedOnly())) {
            wrapper.gt(AiCreditRecord::getCreditsUsed, 0L);
        }
        if (query.getStartTime() != null) {
            wrapper.ge(AiCreditRecord::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(AiCreditRecord::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(AiCreditRecord::getCreateTime)
            .orderByDesc(AiCreditRecord::getRecordId);
        return baseMapper.selectPage(query.parse(), wrapper).copy(AiCreditRecordVO.class);
    }
}
