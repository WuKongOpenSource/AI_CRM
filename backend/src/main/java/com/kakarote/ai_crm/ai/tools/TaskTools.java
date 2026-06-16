package com.kakarote.ai_crm.ai.tools;

import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.ai.context.AiContextHolder;
import com.kakarote.ai_crm.ai.tools.support.AiToolCustomerResolver;
import com.kakarote.ai_crm.ai.tools.support.AiToolPermission;
import com.kakarote.ai_crm.entity.BO.ProjectBO;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.VO.ProjectVO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.service.IProjectService;
import com.kakarote.ai_crm.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 任务相关 AI Tool - 用于 Spring AI Function Calling
 */
@Slf4j
@Component
public class TaskTools {

    @Autowired
    private ITaskService taskService;

    @Autowired
    private AiToolCustomerResolver customerResolver;

    @Autowired
    private IProjectService projectService;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 获取任务。
     */
    @Tool(description = "获取待办任务列表。当用户查询任务、待办事项、今日任务、本周任务时调用。")
    @AiToolPermission(value = "task:view", action = "查看任务")
    public String getTasks(
            @ToolParam(description = "筛选条件：all(全部)/today(今天)/thisWeek(本周)/overdue(已逾期)，默认all", required = false) String filter) {

        try {
            String actualFilter = filter != null ? filter : "all";
            List<TaskVO> tasks = taskService.getMyTasks(actualFilter);

            if (tasks.isEmpty()) {
                return switch (actualFilter) {
                    case "today" -> "今天没有待办任务。";
                    case "thisWeek" -> "本周没有待办任务。";
                    case "overdue" -> "没有逾期任务，做得很好！";
                    default -> "当前没有待办任务。";
                };
            }

            StringBuilder sb = new StringBuilder();
            String title = switch (actualFilter) {
                case "today" -> "今日任务";
                case "thisWeek" -> "本周任务";
                case "overdue" -> "逾期任务";
                default -> "待办任务";
            };
            sb.append(String.format("## %s（共%d项）\n\n", title, tasks.size()));

            for (TaskVO task : tasks) {
                sb.append(String.format("- **%s** [%s优先级]",
                    task.getTitle(),
                    getPriorityLabel(task.getPriority())));

                if (task.getDueDate() != null) {
                    sb.append(String.format("，截止: %s", dateFormat.format(task.getDueDate())));
                }
                if (task.getCustomerName() != null) {
                    sb.append(String.format("，客户: %s", task.getCustomerName()));
                }
                if (task.getRelationName() != null) {
                    sb.append(String.format("，关系人: %s", task.getRelationName()));
                }
                if (task.getGeneratedByAi() != null && task.getGeneratedByAi() == 1) {
                    sb.append(" [AI生成]");
                }
                sb.append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            return "获取任务列表失败: " + e.getMessage();
        }
    }

    /**
     * 创建任务。
     */
    @Tool(description = "创建新任务。当用户描述需要做但还没做的事情，且没有具体执行时间点时调用。只有截止日期也用此工具。客户解析优先级：显式customerIdStr > 显式客户名称 > 当前客户对话绑定客户。当前客户对话中，如果用户只说“这个客户/当前客户/他们”等，不要把代词作为customerName，留空即可让工具默认关联当前客户。如果传入客户名称但系统中不存在该客户，工具会中止创建并提示先创建客户。")
    @AiToolPermission(value = "task:create", action = "创建任务")
    public String createTask(
            @ToolParam(description = "Optional CRM customer ID returned by createCustomer or confirmPendingCustomerCreation. Explicit ID has highest priority.", required = false) String customerIdStr,
            @ToolParam(description = "任务标题，必填") String title,
            @ToolParam(description = "任务描述", required = false) String description,
            @ToolParam(description = "关联客户名称（公司名）", required = false) String customerName,
            @ToolParam(description = "优先级：high/medium/low，默认medium", required = false) String priority,
            @ToolParam(description = "截止日期，格式：yyyy-MM-dd", required = false) String dueDate) {

        log.info("【Tool调用】createTask 被调用: customerIdStr={}, title={}, customerName={}, priority={}, dueDate={}",
            customerIdStr, title, customerName, priority, dueDate);

        try {
            Long currentProjectId = AiContextHolder.getCurrentProjectId();
            if (currentProjectId != null) {
                return createProjectTaskFromCurrentContext(
                    currentProjectId, customerIdStr, title, description, customerName, priority, dueDate);
            }

            Long customerId = null;
            String matchedCompanyName = null;
            Long currentRelationId = AiContextHolder.getCurrentRelationId();
            boolean useRelationContext = currentRelationId != null
                && !hasTextValue(customerIdStr)
                && !hasTextValue(customerName);
            if (!useRelationContext) {
                AiToolCustomerResolver.CustomerResolveResult customerResolve = customerResolver.resolveForCreate(
                    customerIdStr, customerName, "关联该客户创建任务", "创建任务失败", "创建任务");
                if (customerResolve.errorMessage() != null) {
                    return customerResolve.errorMessage();
                }
                Customer resolvedCustomer = customerResolve.customer();
                if (resolvedCustomer != null) {
                    customerId = resolvedCustomer.getCustomerId();
                    matchedCompanyName = resolvedCustomer.getCompanyName();
                }
            }

            TaskAddBO bo = new TaskAddBO();
            bo.setTitle(title);
            bo.setDescription(description);
            bo.setCustomerId(customerId);
            if (useRelationContext) {
                bo.setRelationId(currentRelationId);
            }
            Long currentEmployeeId = AiContextHolder.getCurrentEmployeeId();
            if (currentEmployeeId != null) {
                bo.setAssignedTo(currentEmployeeId);
            }
            bo.setPriority(priority != null ? priority : "medium");

            if (hasTextValue(dueDate)) {
                bo.setDueDate(dateFormat.parse(dueDate));
            }

            bo.setGeneratedByAi(1);

            Long taskId = taskService.addTask(bo);

            StringBuilder result = new StringBuilder();
            result.append(String.format("任务「%s」创建成功！任务ID: %d。", title, taskId));
            result.append("\n- 优先级: ").append(getPriorityLabel(priority != null ? priority : "medium"));
            if (matchedCompanyName != null) {
                result.append("\n- 公司名称: ").append(matchedCompanyName);
            }
            if (customerId != null) {
                result.append("\n- customerId: ").append(customerId);
            }
            if (useRelationContext) {
                result.append("\n- relationId: ").append(currentRelationId);
            }
            if (currentEmployeeId != null) {
                result.append("\n- employeeId: ").append(currentEmployeeId);
            }
            if (hasTextValue(dueDate)) {
                result.append("\n- 截止日期: ").append(dueDate);
            }
            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】createTask 失败: {}", e.getMessage(), e);
            return "创建任务失败: " + e.getMessage();
        }
    }

    private String createProjectTaskFromCurrentContext(
            Long projectId,
            String customerIdStr,
            String title,
            String description,
            String customerName,
            String priority,
            String dueDate) throws Exception {
        ProjectBO.TaskSave bo = new ProjectBO.TaskSave();
        bo.setTitle(title);
        bo.setDescription(description);
        bo.setCustomerId(parseOptionalLong(customerIdStr));
        bo.setCustomerName(customerName);
        bo.setPriority(priority != null ? priority : "medium");
        if (hasTextValue(dueDate)) {
            bo.setDueDate(dateFormat.parse(dueDate));
        }
        bo.setGeneratedByAi(true);
        bo.setAiSourceText("由项目 AI 对话创建");

        ProjectVO updated = projectService.addTask(projectId, bo);
        return "项目任务创建成功。\n项目: " + StrUtil.blankToDefault(updated.getName(), String.valueOf(projectId))
            + "\n- 任务: " + title
            + "\n- 优先级: " + getPriorityLabel(priority != null ? priority : "medium")
            + (hasTextValue(dueDate) ? "\n- 截止日期: " + dueDate : "");
    }

    private Long parseOptionalLong(String value) {
        if (!hasTextValue(value)) {
            return null;
        }
        return Long.parseLong(StrUtil.trim(value));
    }

    @Tool(description = "修改任务信息。当用户要修改、编辑任务的截止日期、标题、描述、优先级、状态等信息时调用。")
    @AiToolPermission(value = "task:edit", action = "编辑任务")
    public String updateTask(
            @ToolParam(description = "任务ID，数字类型，必填") String taskIdStr,
            @ToolParam(description = "任务标题", required = false) String title,
            @ToolParam(description = "任务描述", required = false) String description,
            @ToolParam(description = "截止日期，格式：yyyy-MM-dd", required = false) String dueDate,
            @ToolParam(description = "优先级：high/medium/low", required = false) String priority,
            @ToolParam(description = "状态：pending/in_progress/completed", required = false) String status) {

        log.info("【Tool调用】updateTask 被调用: taskId={}, title={}, dueDate={}, priority={}, status={}",
            taskIdStr, title, dueDate, priority, status);

        try {
            if (taskIdStr == null || taskIdStr.isEmpty() || "null".equalsIgnoreCase(taskIdStr)) {
                return "修改任务失败: 缺少任务ID参数";
            }

            Long taskId;
            try {
                taskId = Long.parseLong(taskIdStr);
            } catch (NumberFormatException e) {
                return "修改任务失败: 任务ID格式无效";
            }

            TaskUpdateBO bo = new TaskUpdateBO();
            bo.setTaskId(taskId);

            if (title != null && !title.isEmpty() && !"null".equalsIgnoreCase(title)) {
                bo.setTitle(title);
            }
            if (description != null && !description.isEmpty() && !"null".equalsIgnoreCase(description)) {
                bo.setDescription(description);
            }
            if (priority != null && !priority.isEmpty() && !"null".equalsIgnoreCase(priority)) {
                bo.setPriority(priority);
            }
            if (status != null && !status.isEmpty() && !"null".equalsIgnoreCase(status)) {
                bo.setStatus(status);
            }

            if (dueDate != null && !dueDate.isEmpty() && !"null".equalsIgnoreCase(dueDate)) {
                try {
                    bo.setDueDate(dateFormat.parse(dueDate));
                } catch (Exception e) {
                    log.warn("截止日期格式无效: {}", dueDate);
                    return "修改任务失败: 截止日期格式无效，请使用 yyyy-MM-dd 格式";
                }
            }

            taskService.updateTask(bo);

            StringBuilder result = new StringBuilder();
            result.append("任务已修改成功！");
            if (title != null && !title.isEmpty() && !"null".equalsIgnoreCase(title)) {
                result.append("\n- 标题: ").append(title);
            }
            if (dueDate != null && !dueDate.isEmpty() && !"null".equalsIgnoreCase(dueDate)) {
                result.append("\n- 截止日期: ").append(dueDate);
            }
            if (priority != null && !priority.isEmpty() && !"null".equalsIgnoreCase(priority)) {
                result.append("\n- 优先级: ").append(getPriorityLabel(priority));
            }
            if (status != null && !status.isEmpty() && !"null".equalsIgnoreCase(status)) {
                result.append("\n- 状态: ").append(getStatusLabel(status));
            }

            return result.toString();
        } catch (Exception e) {
            log.error("【Tool调用】updateTask 失败: {}", e.getMessage(), e);
            return "修改任务失败: " + e.getMessage();
        }
    }

    /**
     * 更新任务状态。
     */
    @Tool(description = "更新任务状态。当用户只需要完成任务、标记任务状态时调用（简化版）。")
    @AiToolPermission(value = "task:update_status", action = "更新任务状态")
    public String updateTaskStatus(
            @ToolParam(description = "任务ID，数字类型") String taskIdStr,
            @ToolParam(description = "新状态：pending/in_progress/completed") String status) {

        try {
            if (taskIdStr == null || taskIdStr.isEmpty() || "null".equalsIgnoreCase(taskIdStr)) {
                return "更新任务状态失败: 缺少任务ID参数";
            }
            Long taskId;
            try {
                taskId = Long.parseLong(taskIdStr);
            } catch (NumberFormatException e) {
                return "更新任务状态失败: 任务ID格式无效";
            }

            taskService.updateStatus(taskId, status);
            return String.format("任务状态已更新为「%s」。", getStatusLabel(status));
        } catch (Exception e) {
            return "更新任务状态失败: " + e.getMessage();
        }
    }

    /**
     * 获取状态Label。
     */
    private String getStatusLabel(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status.toLowerCase()) {
            case "pending" -> "待处理";
            case "in_progress" -> "进行中";
            case "completed" -> "已完成";
            default -> status;
        };
    }

    /**
     * 判断是否存在文本值。
     */
    private boolean hasTextValue(String value) {
        return StrUtil.isNotBlank(value) && !"null".equalsIgnoreCase(StrUtil.trim(value));
    }

    /**
     * 获取优先级Label。
     */
    private String getPriorityLabel(String priority) {
        if (priority == null) {
            return "中";
        }
        return switch (priority.toLowerCase()) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> priority;
        };
    }
}
