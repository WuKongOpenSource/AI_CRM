package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import org.springframework.context.annotation.Lazy;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskAiParseBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.TaskAiParseVO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 任务服务实现
 */
@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {

    @Autowired
    @Lazy
    private DynamicChatClientProvider chatClientProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AI_TASK_PARSE_PROMPT = """
        你是一个专业的 CRM 助手。请分析以下自然语言任务描述，提取结构化信息并以 JSON 格式返回。

        当前时间: %s

        用户输入:
        %s

        请严格按以下 JSON 格式返回，不要包含任何其他文字、代码块标记或解释：
        {
          "title": "简洁明确的任务标题",
          "dueDate": "截止日期时间，格式 yyyy-MM-dd HH:mm（根据描述推断，如'明天下午两点'则计算实际日期）",
          "priority": "优先级，只能是: high, medium, low（根据紧急程度和关键词判断，如'高优先级'='high'）",
          "taskType": "任务类型，只能是: 跟进, 文档, 会议, 电话, 其他（根据内容推断）",
          "customerName": "关联客户名称（如果能从描述中识别，否则为空字符串）",
          "participantNames": "参与人名称，多人用逗号分隔（如果能从描述中识别，否则为空字符串）",
          "assignedToName": "负责人名称（如果能从描述中识别，否则为空字符串）",
          "description": "任务的详细描述或补充说明"
        }
        """;

    @Override
    public Long addTask(TaskAddBO taskAddBO) {
        Task task = BeanUtil.copyProperties(taskAddBO, Task.class);
        if (StrUtil.isEmpty(task.getStatus())) {
            task.setStatus("pending");
        }
        if (StrUtil.isEmpty(task.getPriority())) {
            task.setPriority("medium");
        }
        if (task.getAssignedTo() == null) {
            task.setAssignedTo(UserUtil.getUserId());
        }
        if (task.getGeneratedByAi() == null) {
            task.setGeneratedByAi(0);
        }
        save(task);
        return task.getTaskId();
    }

    @Override
    public void updateTask(TaskUpdateBO taskUpdateBO) {
        Task task = getById(taskUpdateBO.getTaskId());
        if (ObjectUtil.isNull(task)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "任务不存在");
        }
        BeanUtil.copyProperties(taskUpdateBO, task, "taskId", "createUserId", "createTime");
        updateById(task);
    }

    @Override
    public void deleteTask(Long taskId) {
        Task task = getById(taskId);
        if (ObjectUtil.isNull(task)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "任务不存在");
        }
        removeById(taskId);
    }

    @Override
    public BasePage<TaskVO> queryPageList(TaskQueryBO queryBO) {
        BasePage<TaskVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        fillTaskNames(page.getList());
        return page;
    }

    @Override
    public void updateStatus(Long taskId, String status) {
        Task task = getById(taskId);
        if (ObjectUtil.isNull(task)) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "任务不存在");
        }
        task.setStatus(status);
        if ("completed".equals(status)) {
            task.setCompletedTime(new Date());
        }
        updateById(task);
    }

    @Override
    public List<TaskVO> getMyTasks(String filter) {
        Long userId = UserUtil.getUserId();
        Date today = DateUtil.beginOfDay(new Date());
        Date weekEnd = DateUtil.endOfWeek(new Date());

        // 使用 mapper 查询以获取关联的负责人姓名
        List<TaskVO> tasks = baseMapper.getMyTasksFiltered(userId, filter, today, weekEnd);
        fillTaskNames(tasks);
        return tasks;
    }

    /**
     * 获取优先级中文名称（忽略大小写）
     */
    private String getPriorityName(String priority) {
        if (priority == null) return null;
        return switch (priority.toLowerCase()) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> priority;
        };
    }

    /**
     * 获取状态中文名称（忽略大小写）
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status.toLowerCase()) {
            case "pending" -> "待处理";
            case "in_progress" -> "进行中";
            case "completed" -> "已完成";
            default -> status;
        };
    }

    /**
     * 填充任务的中文名称字段，并标准化 priority/status 为大写
     */
    private void fillTaskNames(TaskVO task) {
        // 标准化为大写（前端期望大写格式）
        if (task.getPriority() != null) {
            task.setPriority(task.getPriority().toUpperCase());
        }
        if (task.getStatus() != null) {
            task.setStatus(task.getStatus().toUpperCase());
        }
        task.setPriorityName(getPriorityName(task.getPriority()));
        task.setStatusName(getStatusName(task.getStatus()));
    }

    /**
     * 批量填充任务列表的中文名称字段
     */
    private void fillTaskNames(List<TaskVO> tasks) {
        if (tasks != null) {
            tasks.forEach(this::fillTaskNames);
        }
    }

    @Override
    public TaskAiParseVO aiParseTask(TaskAiParseBO parseBO) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String prompt = String.format(AI_TASK_PARSE_PROMPT, now, parseBO.getContent());

        try {
            String response = chatClientProvider.getChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("AI 任务解析原始响应: {}", response);
            return parseTaskAiResponse(response);
        } catch (Exception e) {
            log.error("AI 任务解析失败，返回默认值", e);
            return buildFallbackTaskResult(parseBO.getContent());
        }
    }

    private TaskAiParseVO parseTaskAiResponse(String response) {
        try {
            String json = response.trim();
            if (json.startsWith("```")) {
                json = json.replaceFirst("```(?:json)?\\s*", "");
                json = json.replaceFirst("\\s*```$", "");
            }

            JsonNode root = objectMapper.readTree(json);
            TaskAiParseVO vo = new TaskAiParseVO();
            vo.setTitle(getTextOrDefault(root, "title", ""));
            vo.setDueDate(getTextOrDefault(root, "dueDate", ""));
            vo.setPriority(getTextOrDefault(root, "priority", "medium"));
            vo.setTaskType(getTextOrDefault(root, "taskType", "其他"));
            vo.setCustomerName(getTextOrDefault(root, "customerName", ""));
            vo.setParticipantNames(getTextOrDefault(root, "participantNames", ""));
            vo.setAssignedToName(getTextOrDefault(root, "assignedToName", ""));
            vo.setDescription(getTextOrDefault(root, "description", ""));
            return vo;
        } catch (Exception e) {
            log.warn("AI 任务解析响应 JSON 解析失败: {}", e.getMessage());
            return buildFallbackTaskResult("");
        }
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        if (root.has(field) && !root.get(field).isNull()) {
            String value = root.get(field).asText();
            return StrUtil.isNotBlank(value) ? value : defaultValue;
        }
        return defaultValue;
    }

    private TaskAiParseVO buildFallbackTaskResult(String content) {
        TaskAiParseVO vo = new TaskAiParseVO();
        vo.setTitle(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        vo.setDueDate("");
        vo.setPriority("medium");
        vo.setTaskType("其他");
        vo.setCustomerName("");
        vo.setParticipantNames("");
        vo.setAssignedToName("");
        vo.setDescription(content);
        return vo;
    }
}
