package com.kakarote.ai_crm.ai.tools;

import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
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

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Tool(description = "获取待办任务列表。当用户查询任务、待办事项、今日任务、本周任务时调用。")
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

    @Tool(description = "创建新任务。当用户要创建、新建、添加任务或待办事项时调用。如果用户提到某个客户，请先调用 queryCustomers 获取客户ID。")
    public String createTask(
            @ToolParam(description = "任务标题，必填") String title,
            @ToolParam(description = "任务描述", required = false) String description,
            @ToolParam(description = "关联客户ID（数字）。如果用户提到客户名称，请先调用 queryCustomers 查询获取客户ID后填入", required = false) String customerIdStr,
            @ToolParam(description = "优先级：high(高)/medium(中)/low(低)，默认medium", required = false) String priority,
            @ToolParam(description = "截止日期，格式：yyyy-MM-dd", required = false) String dueDate) {

        log.info("【Tool调用】createTask 被调用: title={}, customerIdStr={}, priority={}, dueDate={}",
            title, customerIdStr, priority, dueDate);

        try {
            // 将 String 转换为 Long，处理 null 和 "null" 字符串
            Long customerId = null;
            if (customerIdStr != null && !customerIdStr.isEmpty() && !"null".equalsIgnoreCase(customerIdStr)) {
                try {
                    customerId = Long.parseLong(customerIdStr);
                } catch (NumberFormatException e) {
                    // 忽略无效的 customerId
                }
            }

            TaskAddBO bo = new TaskAddBO();
            bo.setTitle(title);
            bo.setDescription(description);
            bo.setCustomerId(customerId);
            bo.setPriority(priority != null ? priority : "medium");

            if (dueDate != null && !dueDate.isEmpty() && !"null".equalsIgnoreCase(dueDate)) {
                bo.setDueDate(dateFormat.parse(dueDate));
            }

            // Mark as AI generated
            bo.setGeneratedByAi(1);

            Long taskId = taskService.addTask(bo);

            log.info("【Tool调用】createTask 成功: taskId={}", taskId);
            return String.format("任务「%s」创建成功！任务ID: %d。优先级: %s。%s",
                title,
                taskId,
                getPriorityLabel(priority != null ? priority : "medium"),
                dueDate != null && !dueDate.isEmpty() && !"null".equalsIgnoreCase(dueDate) ? "截止日期: " + dueDate : "");
        } catch (Exception e) {
            log.error("【Tool调用】createTask 失败: {}", e.getMessage(), e);
            return "创建任务失败: " + e.getMessage();
        }
    }

    @Tool(description = "修改任务信息。当用户要修改、编辑任务的截止日期、标题、描述、优先级、状态等信息时调用。")
    public String updateTask(
            @ToolParam(description = "任务ID，数字类型，必填") String taskIdStr,
            @ToolParam(description = "任务标题", required = false) String title,
            @ToolParam(description = "任务描述", required = false) String description,
            @ToolParam(description = "截止日期，格式：yyyy-MM-dd", required = false) String dueDate,
            @ToolParam(description = "优先级：high(高)/medium(中)/low(低)", required = false) String priority,
            @ToolParam(description = "状态：pending(待处理)/in_progress(进行中)/completed(已完成)", required = false) String status) {

        log.info("【Tool调用】updateTask 被调用: taskId={}, title={}, dueDate={}, priority={}, status={}",
            taskIdStr, title, dueDate, priority, status);

        try {
            // 参数验证
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

            // 只设置非空的字段
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

            // 处理日期字段
            if (dueDate != null && !dueDate.isEmpty() && !"null".equalsIgnoreCase(dueDate)) {
                try {
                    bo.setDueDate(dateFormat.parse(dueDate));
                } catch (Exception e) {
                    log.warn("截止日期格式无效: {}", dueDate);
                    return "修改任务失败: 截止日期格式无效，请使用 yyyy-MM-dd 格式";
                }
            }

            taskService.updateTask(bo);

            log.info("【Tool调用】updateTask 成功: taskId={}", taskId);

            // 构建返回信息
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

    @Tool(description = "更新任务状态。当用户只需要完成任务、标记任务状态时调用（简化版）。")
    public String updateTaskStatus(
            @ToolParam(description = "任务ID，数字类型") String taskIdStr,
            @ToolParam(description = "新状态：pending(待处理)/in_progress(进行中)/completed(已完成)") String status) {

        try {
            // 将 String 转换为 Long，处理 null 和 "null" 字符串
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

    private String getStatusLabel(String status) {
        if (status == null) return "未知";
        return switch (status.toLowerCase()) {
            case "pending" -> "待处理";
            case "in_progress" -> "进行中";
            case "completed" -> "已完成";
            default -> status;
        };
    }

    private String getPriorityLabel(String priority) {
        if (priority == null) return "中";
        return switch (priority.toLowerCase()) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> priority;
        };
    }
}
