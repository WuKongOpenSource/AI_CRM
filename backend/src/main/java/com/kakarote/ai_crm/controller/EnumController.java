package com.kakarote.ai_crm.controller;

import com.kakarote.ai_crm.common.result.Result;
import com.kakarote.ai_crm.entity.BO.FieldOption;
import com.kakarote.ai_crm.entity.VO.EnumVO;
import com.kakarote.ai_crm.service.ICustomFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 枚举值控制器
 */
@RestController
@RequestMapping("/enum")
@Tag(name = "枚举值接口")
public class EnumController {

    @Autowired
    private ICustomFieldService customFieldService;

    private static final String DEFAULT_COLOR = "#6b7280";

    /** 内置阶段配色（新选项用默认色） */
    private static final Map<String, String> STAGE_COLORS = Map.of(
        "lead", "#6b7280",
        "qualified", "#3b82f6",
        "proposal", "#f59e0b",
        "negotiation", "#8b5cf6",
        "closed", "#22c55e",
        "lost", "#ef4444"
    );

    /** 内置级别配色 */
    private static final Map<String, String> LEVEL_COLORS = Map.of(
        "A", "#22c55e",
        "B", "#3b82f6",
        "C", "#6b7280"
    );

    /**
     * 客户阶段枚举（真相源 crm_custom_field.options，支持本地自定义/新增）。
     */
    @GetMapping("/customerStage")
    @Operation(summary = "客户阶段枚举")
    public Result<List<EnumVO>> customerStage() {
        return Result.ok(toEnumList("customer", "stage", STAGE_COLORS));
    }

    /**
     * 客户级别枚举（真相源 crm_custom_field.options，支持本地自定义/新增）。
     */
    @GetMapping("/customerLevel")
    @Operation(summary = "客户级别枚举")
    public Result<List<EnumVO>> customerLevel() {
        return Result.ok(toEnumList("customer", "level", LEVEL_COLORS));
    }

    /**
     * 关系类型枚举（真相源 crm_custom_field.options）。
     */
    @GetMapping("/relationType")
    @Operation(summary = "关系类型枚举")
    public Result<List<EnumVO>> relationType() {
        return Result.ok(toEnumList("relation", "relationType", Map.of()));
    }

    /**
     * 关系来源枚举（真相源 crm_custom_field.options）。
     */
    @GetMapping("/relationSource")
    @Operation(summary = "关系来源枚举")
    public Result<List<EnumVO>> relationSource() {
        return Result.ok(toEnumList("relation", "source", Map.of()));
    }

    /**
     * 把字段选项列表转换为枚举 VO（带配色降级）。
     */
    private List<EnumVO> toEnumList(String entityType, String fieldName, Map<String, String> colors) {
        List<FieldOption> options = customFieldService.getFieldOptions(entityType, fieldName);
        return options.stream()
            .filter(option -> option != null && option.getValue() != null)
            .map(option -> new EnumVO(
                option.getValue(),
                option.getLabel() != null ? option.getLabel() : option.getValue(),
                null,
                colors.getOrDefault(option.getValue(), DEFAULT_COLOR)))
            .collect(Collectors.toList());
    }

    /**
     * 任务状态枚举。
     */
    @GetMapping("/taskStatus")
    @Operation(summary = "任务状态枚举")
    public Result<List<EnumVO>> taskStatus() {
        return Result.ok(Arrays.asList(
            new EnumVO("pending", "待处理", "任务等待开始", "#f59e0b"),
            new EnumVO("in_progress", "进行中", "任务正在执行", "#3b82f6"),
            new EnumVO("completed", "已完成", "任务已完成", "#22c55e"),
            new EnumVO("cancelled", "已取消", "任务已取消", "#6b7280")
        ));
    }

    /**
     * 任务优先级枚举。
     */
    @GetMapping("/taskPriority")
    @Operation(summary = "任务优先级枚举")
    public Result<List<EnumVO>> taskPriority() {
        return Result.ok(Arrays.asList(
            new EnumVO("high", "高", "高优先级任务", "#ef4444"),
            new EnumVO("medium", "中", "中等优先级任务", "#f59e0b"),
            new EnumVO("low", "低", "低优先级任务", "#22c55e")
        ));
    }

    /**
     * 跟进类型枚举。
     */
    @GetMapping("/followUpType")
    @Operation(summary = "跟进类型枚举")
    public Result<List<EnumVO>> followUpType() {
        return Result.ok(Arrays.asList(
            new EnumVO("call", "电话", "电话沟通", "#3b82f6"),
            new EnumVO("meeting", "会议", "面对面或线上会议", "#8b5cf6"),
            new EnumVO("email", "邮件", "邮件沟通", "#22c55e"),
            new EnumVO("visit", "拜访", "上门拜访", "#f59e0b"),
            new EnumVO("other", "其他", "其他类型跟进", "#6b7280")
        ));
    }

    /**
     * 知识库类型枚举。
     */
    @GetMapping("/knowledgeType")
    @Operation(summary = "知识库类型枚举")
    public Result<List<EnumVO>> knowledgeType() {
        return Result.ok(Arrays.asList(
            new EnumVO("meeting", "会议记录", "会议纪要和记录", "#3b82f6"),
            new EnumVO("email", "邮件", "往来邮件记录", "#22c55e"),
            new EnumVO("recording", "录音", "通话或会议录音", "#8b5cf6"),
            new EnumVO("document", "文档", "一般文档资料", "#6b7280"),
            new EnumVO("proposal", "方案", "解决方案文档", "#f59e0b"),
            new EnumVO("contract", "合同", "合同文档", "#ef4444")
        ));
    }
}
