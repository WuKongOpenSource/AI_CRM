package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.FollowUpAddBO;
import com.kakarote.ai_crm.entity.BO.FollowUpAiParseBO;
import com.kakarote.ai_crm.entity.BO.FollowUpQueryBO;
import com.kakarote.ai_crm.entity.PO.FollowUp;
import com.kakarote.ai_crm.entity.VO.FollowUpAiParseVO;
import com.kakarote.ai_crm.entity.VO.FollowUpVO;

import java.util.List;

/**
 * 跟进记录服务接口
 */
public interface IFollowUpService extends IService<FollowUp> {

    /**
     * 添加跟进记录
     */
    Long addFollowUp(FollowUpAddBO followUpAddBO);

    /**
     * 删除跟进记录
     */
    void deleteFollowUp(Long followUpId);

    /**
     * 按客户查询跟进记录
     */
    List<FollowUpVO> queryByCustomer(Long customerId);

    /**
     * 分页查询跟进记录
     */
    BasePage<FollowUpVO> queryPageList(FollowUpQueryBO queryBO);

    /**
     * AI 解析跟进内容
     */
    FollowUpAiParseVO aiParseFollowUp(FollowUpAiParseBO parseBO);
}
