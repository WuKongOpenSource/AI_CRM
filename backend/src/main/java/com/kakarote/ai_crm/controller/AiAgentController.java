package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.AgentAddBO;
import com.kakarote.ai_crm.entity.BO.AgentUpdateBO;
import com.kakarote.ai_crm.entity.VO.AgentVO;
import com.kakarote.ai_crm.service.IAiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI智能体控制器
 */
@RestController
@RequestMapping("/agent")
@Tag(name = "AI智能体管理")
public class AiAgentController {

    @Autowired
    private IAiAgentService aiAgentService;

    @PostMapping("/add")
    @Operation(summary = "创建智能体")
    public Result<Long> add(@Valid @RequestBody AgentAddBO agentAddBO) {
        Long agentId = aiAgentService.addAgent(agentAddBO);
        return Result.ok(agentId);
    }

    @PostMapping("/update")
    @Operation(summary = "更新智能体")
    public Result<String> update(@Valid @RequestBody AgentUpdateBO agentUpdateBO) {
        aiAgentService.updateAgent(agentUpdateBO);
        return Result.ok();
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除智能体")
    public Result<String> delete(@PathVariable("id") Long id) {
        aiAgentService.deleteAgent(id);
        return Result.ok();
    }

    @GetMapping("/queryEnabled")
    @Operation(summary = "查询已启用的智能体")
    public Result<List<AgentVO>> queryEnabled() {
        return Result.ok(aiAgentService.queryEnabled());
    }

    @GetMapping("/queryAll")
    @Operation(summary = "查询全部智能体")
    public Result<List<AgentVO>> queryAll() {
        return Result.ok(aiAgentService.queryAll());
    }
}
