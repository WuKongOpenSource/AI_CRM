package com.kakarote.ai_crm.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kakarote.ai_crm.entity.BO.AgentAddBO;
import com.kakarote.ai_crm.entity.BO.AgentUpdateBO;
import com.kakarote.ai_crm.entity.PO.AiAgent;
import com.kakarote.ai_crm.entity.VO.AgentVO;

import java.util.List;

/**
 * AI智能体服务接口
 */
public interface IAiAgentService extends IService<AiAgent> {

    /**
     * 添加智能体
     */
    Long addAgent(AgentAddBO agentAddBO);

    /**
     * 更新智能体
     */
    void updateAgent(AgentUpdateBO agentUpdateBO);

    /**
     * 删除智能体
     */
    void deleteAgent(Long agentId);

    /**
     * 查询已启用的智能体
     */
    List<AgentVO> queryEnabled();

    /**
     * 查询全部智能体
     */
    List<AgentVO> queryAll();
}
