package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.service.IProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Project and project-task AI tools.
 */
@Slf4j
@Component
public class ProjectTools {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private IProjectService projectService;

    @Tool(description = "查询项目列表。当用户在“项目”技能下要查看、搜索、筛选项目时调用。支持按项目名称、描述、客户名称模糊搜索，也可按状态筛选。")
    public String listProjects(
            @ToolParam(description = "搜索关键词，可搜索项目名称、描述或客户名称", required = false) String keyword,
            @ToolParam(description = "项目状态：all/not_started/in_progress/completed/paused/archived 或中文状态", required = false) String status,
            @ToolParam(description = "返回数量，默认 10，最多 20", required = false) String limit) {
        try {
            ProjectBO.Query query = new ProjectBO.Query();
            query.setKeyword(normalizeOptionalText(keyword));
            query.setStatus(normalizeProjectStatus(status));
            query.setPage(1);
            query.setLimit(parseLimit(limit));
            BasePage<ProjectVO> page = projectService.queryPageList(query);
            List<ProjectVO> projects = page.getRecords();
            if (projects.isEmpty()) {
                return "未找到符合条件的项目。";
            }
            StringBuilder sb = new StringBuilder("## 项目列表\n");
            sb.append("- 总数: ").append(page.getTotal()).append("\n\n");
            for (ProjectVO project : projects) {
                sb.append(formatProjectLine(project)).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Project tool listProjects failed", e);
            return "查询项目失败: " + e.getMessage();
        }
    }

    @Tool(description = "查看项目详情。当用户询问某个项目的状态、负责人、关联客户、泳道、任务列表或任务数量时调用。优先使用项目ID。")
    public String getProjectDetail(
            @ToolParam(description = "项目ID，数字类型，必填") String projectIdStr,
            @ToolParam(description = "可选任务关键词，用于在项目详情中筛选任务", required = false) String taskKeyword) {
        try {
            Long projectId = parseRequiredLong(projectIdStr, "项目ID");
            ProjectVO project = projectService.getProject(projectId, normalizeOptionalText(taskKeyword));
            return formatProjectDetail(project);
        } catch (Exception e) {
            log.error("Project tool getProjectDetail failed", e);
            return "查看项目详情失败: " + e.getMessage();
        }
    }

    @Tool(description = "创建项目。当用户在“项目”技能下要求新建、创建一个项目时调用。项目名称必填；可选描述、关联客户、负责人、开始日期、截止日期和状态。")
    public String createProject(
            @ToolParam(description = "项目名称，必填") String name,
            @ToolParam(description = "项目描述", required = false) String description,
            @ToolParam(description = "关联客户ID，数字类型；如果不知道ID可留空并传客户名称", required = false) String customerIdStr,
            @ToolParam(description = "关联客户名称", required = false) String customerName,
            @ToolParam(description = "负责人用户ID，数字类型；留空默认当前用户", required = false) String ownerIdStr,
            @ToolParam(description = "开始日期，格式 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "截止日期，格式 yyyy-MM-dd", required = false) String dueDate,
            @ToolParam(description = "项目状态：not_started/in_progress/completed/paused/archived 或中文状态，默认未开始", required = false) String status) {
        try {
            String projectName = normalizeRequiredText(name, "项目名称");
            ProjectBO.Create bo = new ProjectBO.Create();
            bo.setName(projectName);
            bo.setDescription(normalizeOptionalText(description));
            bo.setCustomerId(parseOptionalLong(customerIdStr, "客户ID"));
            bo.setCustomerName(normalizeOptionalText(customerName));
            bo.setOwnerId(parseOptionalLong(ownerIdStr, "负责人用户ID"));
            bo.setStartDate(parseDate(startDate, "开始日期"));
            bo.setDueDate(parseDate(dueDate, "截止日期"));
            bo.setStatus(normalizeProjectStatus(status));
            ProjectVO project = projectService.createProject(bo);
            return "项目创建成功。\n" + formatProjectLine(project);
        } catch (Exception e) {
            log.error("Project tool createProject failed", e);
            return "创建项目失败: " + e.getMessage();
        }
    }

    @Tool(description = "更新项目。当用户要求修改项目名称、描述、状态、负责人、关联客户、开始日期或截止日期时调用。项目ID必填；未提供的字段保持不变。")
    public String updateProject(
            @ToolParam(description = "项目ID，数字类型，必填") String projectIdStr,
            @ToolParam(description = "新的项目名称", required = false) String name,
            @ToolParam(description = "新的项目描述", required = false) String description,
            @ToolParam(description = "新的关联客户ID，数字类型", required = false) String customerIdStr,
            @ToolParam(description = "新的关联客户名称", required = false) String customerName,
            @ToolParam(description = "新的负责人用户ID，数字类型", required = false) String ownerIdStr,
            @ToolParam(description = "新的开始日期，格式 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(description = "新的截止日期，格式 yyyy-MM-dd", required = false) String dueDate,
            @ToolParam(description = "新的项目状态：not_started/in_progress/completed/paused/archived 或中文状态", required = false) String status) {
        try {
            Long projectId = parseRequiredLong(projectIdStr, "项目ID");
            ProjectVO existing = projectService.getProject(projectId);
            ProjectBO.Update bo = new ProjectBO.Update();
            bo.setProjectId(projectId);
            bo.setName(StrUtil.blankToDefault(normalizeOptionalText(name), existing.getName()));
            bo.setDescription(description == null ? existing.getDescription() : description);
            bo.setCustomerId(hasText(customerIdStr) ? parseOptionalLong(customerIdStr, "客户ID") : existing.getCustomerId());
            bo.setCustomerName(customerName == null ? existing.getCustomerName() : customerName);
            bo.setOwnerId(hasText(ownerIdStr) ? parseOptionalLong(ownerIdStr, "负责人用户ID") : existing.getOwnerId());
            bo.setStartDate(hasText(startDate) ? parseDate(startDate, "开始日期") : existing.getStartDate());
            bo.setDueDate(hasText(dueDate) ? parseDate(dueDate, "截止日期") : existing.getDueDate());
            bo.setStatus(hasText(status) ? normalizeProjectStatus(status) : existing.getStatus());
            ProjectVO project = projectService.updateProject(bo);
            return "项目更新成功。\n" + formatProjectLine(project);
        } catch (Exception e) {
            log.error("Project tool updateProject failed", e);
            return "更新项目失败: " + e.getMessage();
        }
    }

    @Tool(description = "删除项目。当用户明确要求删除某个项目时调用。必须提供项目ID。删除是不可恢复操作，调用前应确认用户意图明确。")
    public String deleteProject(
            @ToolParam(description = "项目ID，数字类型，必填") String projectIdStr) {
        try {
            Long projectId = parseRequiredLong(projectIdStr, "项目ID");
            ProjectVO project = projectService.getProject(projectId);
            projectService.deleteProject(projectId);
            return "项目已删除: " + project.getName() + "（项目ID: " + projectId + "）。";
        } catch (Exception e) {
            log.error("Project tool deleteProject failed", e);
            return "删除项目失败: " + e.getMessage();
        }
    }

    @Tool(description = "在项目内创建任务。当用户在“项目”技能下要求给某个项目新增任务、创建项目任务时调用。项目ID和任务标题必填；泳道可用泳道ID或泳道名称指定，未指定则进入默认泳道。")
    public String createProjectTask(
            @ToolParam(description = "项目ID，数字类型，必填") String projectIdStr,
            @ToolParam(description = "任务标题，必填") String title,
            @ToolParam(description = "任务描述", required = false) String description,
            @ToolParam(description = "泳道ID，数字类型", required = false) String laneIdStr,
            @ToolParam(description = "泳道名称，如未开始、进行中、已完成", required = false) String laneName,
            @ToolParam(description = "负责人用户ID，数字类型；留空默认当前用户", required = false) String ownerIdStr,
            @ToolParam(description = "关联客户ID，数字类型；留空默认使用项目关联客户", required = false) String customerIdStr,
            @ToolParam(description = "关联客户名称", required = false) String customerName,
            @ToolParam(description = "优先级：low/medium/high/urgent 或中文", required = false) String priority,
            @ToolParam(description = "截止日期，格式 yyyy-MM-dd", required = false) String dueDate) {
        try {
            Long projectId = parseRequiredLong(projectIdStr, "项目ID");
            ProjectVO project = projectService.getProject(projectId);
            ProjectBO.TaskSave bo = new ProjectBO.TaskSave();
            bo.setTitle(normalizeRequiredText(title, "任务标题"));
            bo.setDescription(normalizeOptionalText(description));
            bo.setLaneId(resolveLaneId(project, laneIdStr, laneName));
            bo.setOwnerId(parseOptionalLong(ownerIdStr, "负责人用户ID"));
            bo.setCustomerId(parseOptionalLong(customerIdStr, "客户ID"));
            bo.setCustomerName(normalizeOptionalText(customerName));
            bo.setPriority(normalizeTaskPriority(priority));
            bo.setDueDate(parseDate(dueDate, "截止日期"));
            bo.setGeneratedByAi(true);
            bo.setAiSourceText("由项目技能 AI 对话创建");
            ProjectVO updated = projectService.addTask(projectId, bo);
            ProjectVO.ProjectTaskVO created = updated.getTasks().stream()
                    .filter(task -> bo.getTitle().equals(task.getTitle()))
                    .findFirst()
                    .orElse(null);
            return "项目任务创建成功。\n项目: " + updated.getName()
                    + "\n" + (created == null ? "- 任务: " + bo.getTitle() : formatTaskLine(created));
        } catch (Exception e) {
            log.error("Project tool createProjectTask failed", e);
            return "创建项目任务失败: " + e.getMessage();
        }
    }

    @Tool(description = "更新项目任务。当用户要求修改项目内某个任务的标题、描述、泳道、负责人、关联客户、优先级或截止日期时调用。项目ID和任务ID必填；未提供的字段保持不变。")
    public String updateProjectTask(
            @ToolParam(description = "项目ID，数字类型，必填") String projectIdStr,
            @ToolParam(description = "任务ID，数字类型，必填") String taskIdStr,
            @ToolParam(description = "新的任务标题", required = false) String title,
            @ToolParam(description = "新的任务描述", required = false) String description,
            @ToolParam(description = "新的泳道ID，数字类型", required = false) String laneIdStr,
            @ToolParam(description = "新的泳道名称", required = false) String laneName,
            @ToolParam(description = "新的负责人用户ID，数字类型", required = false) String ownerIdStr,
            @ToolParam(description = "新的关联客户ID，数字类型", required = false) String customerIdStr,
            @ToolParam(description = "新的关联客户名称", required = false) String customerName,
            @ToolParam(description = "新的优先级：low/medium/high/urgent 或中文", required = false) String priority,
            @ToolParam(description = "新的截止日期，格式 yyyy-MM-dd", required = false) String dueDate) {
        try {
            Long projectId = parseRequiredLong(projectIdStr, "项目ID");
            Long taskId = parseRequiredLong(taskIdStr, "任务ID");
            ProjectVO project = projectService.getProject(projectId);
            ProjectVO.ProjectTaskVO existing = findTask(project, taskId);
            ProjectBO.TaskSave bo = new ProjectBO.TaskSave();
            bo.setTaskId(taskId);
            bo.setTitle(StrUtil.blankToDefault(normalizeOptionalText(title), existing.getTitle()));
            bo.setDescription(description == null ? existing.getDescription() : description);
            bo.setLaneId(resolveLaneId(project, laneIdStr, laneName, existing.getLaneId()));
            bo.setOwnerId(hasText(ownerIdStr) ? parseOptionalLong(ownerIdStr, "负责人用户ID") : existing.getOwnerId());
            bo.setCustomerId(hasText(customerIdStr) ? parseOptionalLong(customerIdStr, "客户ID") : existing.getCustomerId());
            bo.setCustomerName(customerName == null ? existing.getCustomerName() : customerName);
            bo.setPriority(hasText(priority) ? normalizeTaskPriority(priority) : existing.getPriority());
            bo.setDueDate(hasText(dueDate) ? parseDate(dueDate, "截止日期") : existing.getDueDate());
            bo.setParticipantIds(existing.getParticipantIds());
            bo.setParticipantNames(existing.getParticipantNames());
            bo.setHasAttachments(Boolean.TRUE.equals(existing.getHasAttachments()));
            bo.setHasSchedule(Boolean.TRUE.equals(existing.getHasSchedule()));
            ProjectVO updated = projectService.updateTask(projectId, bo);
            return "项目任务更新成功。\n" + formatTaskLine(findTask(updated, taskId));
        } catch (Exception e) {
            log.error("Project tool updateProjectTask failed", e);
            return "更新项目任务失败: " + e.getMessage();
        }
    }

    @Tool(description = "移动项目任务到指定泳道。当用户要求把项目任务移到未开始、进行中、已完成等泳道时调用。")
    public String moveProjectTask(
            @ToolParam(description = "项目ID，数字类型，必填") String projectIdStr,
            @ToolParam(description = "任务ID，数字类型，必填") String taskIdStr,
            @ToolParam(description = "目标泳道ID，数字类型", required = false) String laneIdStr,
            @ToolParam(description = "目标泳道名称，如未开始、进行中、已完成", required = false) String laneName) {
        try {
            Long projectId = parseRequiredLong(projectIdStr, "项目ID");
            Long taskId = parseRequiredLong(taskIdStr, "任务ID");
            ProjectVO project = projectService.getProject(projectId);
            Long laneId = resolveLaneId(project, laneIdStr, laneName);
            if (laneId == null) {
                return "移动项目任务失败: 缺少目标泳道，请提供泳道ID或泳道名称。";
            }
            ProjectBO.TaskMove moveBO = new ProjectBO.TaskMove();
            moveBO.setTaskId(taskId);
            moveBO.setLaneId(laneId);
            ProjectVO updated = projectService.moveTask(projectId, moveBO);
            return "项目任务移动成功。\n" + formatTaskLine(findTask(updated, taskId));
        } catch (Exception e) {
            log.error("Project tool moveProjectTask failed", e);
            return "移动项目任务失败: " + e.getMessage();
        }
    }

    @Tool(description = "删除项目任务。当用户明确要求删除某个项目内任务时调用。项目ID和任务ID必填。")
    public String deleteProjectTask(
            @ToolParam(description = "项目ID，数字类型，必填") String projectIdStr,
            @ToolParam(description = "任务ID，数字类型，必填") String taskIdStr) {
        try {
            Long projectId = parseRequiredLong(projectIdStr, "项目ID");
            Long taskId = parseRequiredLong(taskIdStr, "任务ID");
            ProjectVO project = projectService.getProject(projectId);
            ProjectVO.ProjectTaskVO task = findTask(project, taskId);
            projectService.deleteTask(projectId, taskId);
            return "项目任务已删除: " + task.getTitle() + "（任务ID: " + taskId + "）。";
        } catch (Exception e) {
            log.error("Project tool deleteProjectTask failed", e);
            return "删除项目任务失败: " + e.getMessage();
        }
    }

    private String formatProjectLine(ProjectVO project) {
        return "- " + project.getName()
                + "（项目ID: " + project.getProjectId()
                + "，状态: " + projectStatusLabel(project.getStatus())
                + "，负责人: " + StrUtil.blankToDefault(project.getOwnerName(), "未指定")
                + "，关联客户: " + StrUtil.blankToDefault(project.getCustomerName(), "未关联")
                + "，任务: " + nullSafe(project.getTaskCount()) + " 个 / 未完成 " + nullSafe(project.getIncompleteTaskCount()) + " 个）";
    }

    private String formatProjectDetail(ProjectVO project) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(project.getName()).append("\n");
        sb.append("- 项目ID: ").append(project.getProjectId()).append("\n");
        sb.append("- 状态: ").append(projectStatusLabel(project.getStatus())).append("\n");
        sb.append("- 负责人: ").append(StrUtil.blankToDefault(project.getOwnerName(), "未指定")).append("\n");
        sb.append("- 关联客户: ").append(StrUtil.blankToDefault(project.getCustomerName(), "未关联")).append("\n");
        sb.append("- 描述: ").append(StrUtil.blankToDefault(project.getDescription(), "暂无")).append("\n");
        sb.append("- 泳道: ");
        if (project.getLanes().isEmpty()) {
            sb.append("暂无");
        } else {
            sb.append(project.getLanes().stream()
                    .map(lane -> lane.getName() + "(laneId=" + lane.getLaneId() + ")")
                    .toList());
        }
        sb.append("\n\n### 任务\n");
        if (project.getTasks().isEmpty()) {
            sb.append("暂无任务");
        } else {
            project.getTasks().forEach(task -> sb.append(formatTaskLine(task)).append("\n"));
        }
        return sb.toString();
    }

    private String formatTaskLine(ProjectVO.ProjectTaskVO task) {
        return "- " + task.getTitle()
                + "（任务ID: " + task.getTaskId()
                + "，状态: " + StrUtil.blankToDefault(task.getStatus(), "未设置")
                + "，优先级: " + taskPriorityLabel(task.getPriority())
                + "，负责人: " + StrUtil.blankToDefault(task.getOwnerName(), "未指定")
                + "，截止: " + formatDate(task.getDueDate()) + "）";
    }

    private ProjectVO.ProjectTaskVO findTask(ProjectVO project, Long taskId) {
        return project.getTasks().stream()
                .filter(task -> Objects.equals(task.getTaskId(), taskId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到任务ID: " + taskId));
    }

    private Long resolveLaneId(ProjectVO project, String laneIdStr, String laneName) {
        return resolveLaneId(project, laneIdStr, laneName, null);
    }

    private Long resolveLaneId(ProjectVO project, String laneIdStr, String laneName, Long fallbackLaneId) {
        Long parsedLaneId = parseOptionalLong(laneIdStr, "泳道ID");
        if (parsedLaneId != null) {
            return parsedLaneId;
        }
        String normalizedLaneName = normalizeOptionalText(laneName);
        if (normalizedLaneName == null) {
            return fallbackLaneId;
        }
        String needle = normalizedLaneName.toLowerCase(Locale.ROOT);
        return project.getLanes().stream()
                .filter(lane -> StrUtil.isNotBlank(lane.getName()))
                .filter(lane -> {
                    String name = lane.getName().toLowerCase(Locale.ROOT);
                    return name.equals(needle) || name.contains(needle) || needle.contains(name);
                })
                .map(ProjectVO.ProjectLaneVO::getLaneId)
                .findFirst()
                .orElse(fallbackLaneId);
    }

    private String normalizeProjectStatus(String status) {
        String value = normalizeOptionalText(status);
        if (value == null || "all".equalsIgnoreCase(value) || "全部".equals(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "IN_PROGRESS", "进行中" -> "IN_PROGRESS";
            case "COMPLETED", "DONE", "已完成", "完成" -> "COMPLETED";
            case "PAUSED", "暂停", "已暂停" -> "PAUSED";
            case "ARCHIVED", "归档", "已归档" -> "ARCHIVED";
            case "NOT_STARTED", "未开始", "待开始" -> "NOT_STARTED";
            default -> normalized;
        };
    }

    private String normalizeTaskPriority(String priority) {
        String value = normalizeOptionalText(priority);
        if (value == null) {
            return "MEDIUM";
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "URGENT", "紧急", "最高" -> "URGENT";
            case "HIGH", "高", "重要" -> "HIGH";
            case "LOW", "低", "不急" -> "LOW";
            default -> "MEDIUM";
        };
    }

    private String projectStatusLabel(String status) {
        return switch (StrUtil.blankToDefault(status, "NOT_STARTED").toUpperCase(Locale.ROOT)) {
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "PAUSED" -> "已暂停";
            case "ARCHIVED" -> "已归档";
            default -> "未开始";
        };
    }

    private String taskPriorityLabel(String priority) {
        return switch (StrUtil.blankToDefault(priority, "MEDIUM").toUpperCase(Locale.ROOT)) {
            case "URGENT" -> "紧急";
            case "HIGH" -> "高";
            case "LOW" -> "低";
            default -> "中";
        };
    }

    private Date parseDate(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Date.from(LocalDate.parse(normalized).atStartOfDay(DEFAULT_ZONE).toInstant());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + "格式无效，请使用 yyyy-MM-dd");
        }
    }

    private Long parseRequiredLong(String value, String fieldName) {
        Long parsed = parseOptionalLong(value, fieldName);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return parsed;
    }

    private Long parseOptionalLong(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "格式无效");
        }
    }

    private int parseLimit(String limit) {
        Long parsed = parseOptionalLong(limit, "返回数量");
        if (parsed == null) {
            return 10;
        }
        return Math.max(1, Math.min(parsed.intValue(), 20));
    }

    private String normalizeRequiredText(String value, String fieldName) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean hasText(String value) {
        return StrUtil.isNotBlank(value) && !"null".equalsIgnoreCase(value.trim());
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return "未设置";
        }
        return date.toInstant().atZone(DEFAULT_ZONE).toLocalDate().toString();
    }
}
