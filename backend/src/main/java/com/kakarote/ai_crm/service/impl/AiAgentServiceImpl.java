package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.AgentAddBO;
import com.kakarote.ai_crm.entity.BO.AgentUpdateBO;
import com.kakarote.ai_crm.entity.PO.AiAgent;
import com.kakarote.ai_crm.entity.VO.AgentVO;
import com.kakarote.ai_crm.mapper.AiAgentMapper;
import com.kakarote.ai_crm.service.IAiAgentService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI智能体服务实现
 */
@Service
public class AiAgentServiceImpl extends ServiceImpl<AiAgentMapper, AiAgent> implements IAiAgentService {

    @Override
    public Long addAgent(AgentAddBO agentAddBO) {
        AiAgent agent = BeanUtil.copyProperties(agentAddBO, AiAgent.class, "enabled");
        // Handle Boolean to Integer conversion for enabled
        if (agentAddBO.getEnabled() != null) {
            agent.setEnabled(agentAddBO.getEnabled() ? 1 : 0);
        } else {
            agent.setEnabled(1);
        }
        if (agent.getSortOrder() == null) {
            // Get max sort order
            Integer maxOrder = lambdaQuery()
                .orderByDesc(AiAgent::getSortOrder)
                .last("LIMIT 1")
                .oneOpt()
                .map(AiAgent::getSortOrder)
                .orElse(0);
            agent.setSortOrder(maxOrder + 1);
        }
        save(agent);
        return agent.getAgentId();
    }

    @Override
    public void updateAgent(AgentUpdateBO agentUpdateBO) {
        AiAgent agent = getById(agentUpdateBO.getAgentId());
        if (ObjectUtil.isNull(agent)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "智能体不存在");
        }
        BeanUtil.copyProperties(agentUpdateBO, agent, "agentId", "createUserId", "createTime", "enabled");
        // Handle Boolean to Integer conversion for enabled
        if (agentUpdateBO.getEnabled() != null) {
            agent.setEnabled(agentUpdateBO.getEnabled() ? 1 : 0);
        }
        updateById(agent);
    }

    @Override
    public void deleteAgent(Long agentId) {
        AiAgent agent = getById(agentId);
        if (ObjectUtil.isNull(agent)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "智能体不存在");
        }
        removeById(agentId);
    }

    @Override
    public List<AgentVO> queryEnabled() {
        List<AiAgent> agents = lambdaQuery()
            .eq(AiAgent::getEnabled, 1)
            .orderByAsc(AiAgent::getSortOrder)
            .list();
        return BeanUtil.copyToList(agents, AgentVO.class);
    }

    @Override
    public List<AgentVO> queryAll() {
        List<AiAgent> agents = lambdaQuery()
            .orderByAsc(AiAgent::getSortOrder)
            .list();
        return BeanUtil.copyToList(agents, AgentVO.class);
    }
}
