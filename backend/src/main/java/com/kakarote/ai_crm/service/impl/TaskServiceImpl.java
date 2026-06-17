package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.ai_crm.ai.DynamicChatClientProvider;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.TaskAddBO;
import com.kakarote.ai_crm.entity.BO.TaskAiParseBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskUpdateBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.TaskAiParseVO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务服务实现
 */
@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements ITaskService {

    @Autowired
    @Lazy
    private DynamicChatClientProvider chatClientProvider;

    @Autowired
    private CustomerMapper customerMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int HIGH_VALUE_THRESHOLD = 72;
    private static final int MEDIUM_VALUE_THRESHOLD = 58;

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
        refreshValuePriority(task.getTaskId());
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
        refreshValuePriority(task.getTaskId());
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
        if (useValuePriorityMode(queryBO)) {
            return queryValuePriorityPageList(queryBO);
        }
        BasePage<TaskVO> page = queryBO.parse();
        baseMapper.queryPageList(page, queryBO);
        fillTaskNames(page.getList());
        hydrateValuePriority(page.getList(), true);
        attachStatusCounts(page, queryBO);
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
        refreshValuePriority(taskId);
    }

    @Override
    public List<TaskVO> getMyTasks(String filter) {
        Long userId = UserUtil.getUserId();
        Date today = DateUtil.beginOfDay(new Date());
        Date weekEnd = DateUtil.endOfWeek(new Date());

        // 使用 mapper 查询以获取关联的负责人姓名
        List<TaskVO> tasks = baseMapper.getMyTasksFiltered(userId, filter, today, weekEnd);
        fillTaskNames(tasks);
        hydrateValuePriority(tasks, true);
        return tasks;
    }

    private boolean useValuePriorityMode(TaskQueryBO queryBO) {
        return queryBO != null
                && (Boolean.TRUE.equals(queryBO.getHighValueOnly())
                || "value".equalsIgnoreCase(StrUtil.blankToDefault(queryBO.getSortMode(), "")));
    }

    private BasePage<TaskVO> queryValuePriorityPageList(TaskQueryBO queryBO) {
        List<TaskVO> tasks = baseMapper.queryList(queryBO);
        fillTaskNames(tasks);
        hydrateValuePriority(tasks, true);

        List<TaskVO> rankedTasks = tasks.stream()
                .filter(task -> !Boolean.TRUE.equals(queryBO.getHighValueOnly()) || Boolean.TRUE.equals(task.getHighValue()))
                .sorted(buildValuePriorityComparator())
                .collect(Collectors.toList());

        BasePage<TaskVO> page = paginateTasks(rankedTasks, queryBO);
        attachStatusCounts(page, queryBO);
        return page;
    }

    private BasePage<TaskVO> paginateTasks(List<TaskVO> tasks, TaskQueryBO queryBO) {
        long pageNumber = Math.max(1, queryBO.getPage());
        long pageSize = Math.max(1, queryBO.getLimit());
        int fromIndex = (int) Math.min((pageNumber - 1) * pageSize, tasks.size());
        int toIndex = (int) Math.min(fromIndex + pageSize, tasks.size());

        BasePage<TaskVO> page = new BasePage<>(pageNumber, pageSize, tasks.size());
        page.setRecords(tasks.subList(fromIndex, toIndex));
        return page;
    }

    private Comparator<TaskVO> buildValuePriorityComparator() {
        return Comparator
                .comparingInt((TaskVO task) -> switch (StrUtil.blankToDefault(task.getStatus(), "")) {
                    case "PENDING" -> 0;
                    case "IN_PROGRESS" -> 1;
                    default -> 2;
                })
                .thenComparing(TaskVO::getValuePriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(task -> task.getDueDate() == null ? Long.MAX_VALUE : task.getDueDate().getTime())
                .thenComparing(task -> task.getCreateTime() == null ? Long.MAX_VALUE : task.getCreateTime().getTime());
    }

    private void attachStatusCounts(BasePage<TaskVO> page, TaskQueryBO queryBO) {
        page.setExtraData(Map.of("statusCounts", buildStatusCounts(queryBO)));
    }

    private Map<String, Long> buildStatusCounts(TaskQueryBO queryBO) {
        TaskQueryBO countQuery = new TaskQueryBO();
        BeanUtil.copyProperties(queryBO, countQuery);
        countQuery.setTaskId(null);
        countQuery.setStatus(null);

        List<TaskVO> tasks = baseMapper.queryList(countQuery);
        fillTaskNames(tasks);
        hydrateValuePriority(tasks, false);
        if (Boolean.TRUE.equals(queryBO.getHighValueOnly())) {
            tasks = tasks.stream()
                    .filter(task -> Boolean.TRUE.equals(task.getHighValue()))
                    .toList();
        }

        Date now = new Date();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("all", (long) tasks.size());
        counts.put("PENDING", countStatus(tasks, "PENDING"));
        counts.put("IN_PROGRESS", countStatus(tasks, "IN_PROGRESS"));
        counts.put("COMPLETED", countStatus(tasks, "COMPLETED"));
        counts.put("OVERDUE", tasks.stream().filter(task -> isOverdue(task, now)).count());
        return counts;
    }

    private long countStatus(List<TaskVO> tasks, String status) {
        return tasks.stream()
                .filter(task -> status.equalsIgnoreCase(StrUtil.blankToDefault(task.getStatus(), "")))
                .count();
    }

    private boolean isOverdue(TaskVO task, Date now) {
        return task.getDueDate() != null
                && task.getDueDate().before(now)
                && !"COMPLETED".equalsIgnoreCase(StrUtil.blankToDefault(task.getStatus(), ""));
    }

    @Override
    public void refreshValuePriority(Long taskId) {
        if (taskId == null) {
            return;
        }
        Task task = baseMapper.selectByIdIgnoreDataPermission(taskId);
        if (task == null) {
            return;
        }
        Customer customer = task.getCustomerId() == null ? null : customerMapper.selectByIdIgnoreDataPermission(task.getCustomerId());
        persistValuePriority(task, customer);
    }

    @Override
    public void refreshValuePriorityByCustomerId(Long customerId) {
        if (customerId == null) {
            return;
        }
        Customer customer = customerMapper.selectByIdIgnoreDataPermission(customerId);
        List<Task> tasks = baseMapper.selectByCustomerIdIgnoreDataPermission(customerId);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        tasks.forEach(task -> persistValuePriority(task, customer));
    }

    private void hydrateValuePriority(List<TaskVO> tasks, boolean persistMissing) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        Map<Long, Customer> customerMap = loadCustomerMap(tasks.stream()
                .map(TaskVO::getCustomerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        for (TaskVO task : tasks) {
            TaskValuePriorityResult result = evaluateValuePriority(toTaskPriorityInput(task), customerMap.get(task.getCustomerId()));
            boolean needsPersist = !Objects.equals(task.getValuePriorityScore(), result.score())
                    || !Objects.equals(task.getValuePriorityTier(), result.tier())
                    || !Objects.equals(task.getValuePriorityReason(), result.reason())
                    || !Objects.equals(task.getHighValue(), result.highValue());
            applyValuePriority(task, result);

            if (persistMissing && needsPersist && task.getTaskId() != null) {
                baseMapper.updateValuePriorityById(task.getTaskId(), result.score(), result.tier(), result.reason(), result.highValue());
            }
        }
    }

    private void persistValuePriority(Task task, Customer customer) {
        if (task == null || task.getTaskId() == null) {
            return;
        }
        TaskValuePriorityResult result = evaluateValuePriority(toTaskPriorityInput(task), customer);
        baseMapper.updateValuePriorityById(task.getTaskId(), result.score(), result.tier(), result.reason(), result.highValue());
        task.setValuePriorityScore(result.score());
        task.setValuePriorityTier(result.tier());
        task.setValuePriorityReason(result.reason());
        task.setHighValue(result.highValue());
    }

    private Map<Long, Customer> loadCustomerMap(Set<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return customerMapper.selectBatchIds(customerIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Customer::getCustomerId, Function.identity(), (left, right) -> left));
    }

    private void applyValuePriority(TaskVO task, TaskValuePriorityResult result) {
        task.setValuePriorityScore(result.score());
        task.setValuePriorityTier(result.tier());
        task.setValuePriorityReason(result.reason());
        task.setHighValue(result.highValue());
    }

    private TaskValuePriorityResult evaluateValuePriority(TaskPriorityInput task, Customer customer) {
        int customerValue = computeCustomerValue(customer);
        int winProbability = computeWinProbability(customer);
        int urgency = computeUrgency(task, customer);
        int actionFit = computeActionFit(task, customer);
        int manualBonus = computeManualPriorityBonus(task);
        int riskPenalty = computeRiskPenalty(task, customer);

        int score = Math.round(
                customerValue * 0.35f
                        + winProbability * 0.30f
                        + urgency * 0.20f
                        + actionFit * 0.15f
                        + manualBonus
                        - riskPenalty
        );

        if ("COMPLETED".equalsIgnoreCase(task.status())) {
            score = Math.min(score, 35);
        }

        score = clamp(score, 1, 99);
        String tier = score >= HIGH_VALUE_THRESHOLD ? "HIGH" : score >= MEDIUM_VALUE_THRESHOLD ? "MEDIUM" : "LOW";
        boolean highValue = !"COMPLETED".equalsIgnoreCase(task.status()) && score >= HIGH_VALUE_THRESHOLD;

        return new TaskValuePriorityResult(
                score,
                tier,
                buildValuePriorityReason(task, customer, score, customerValue, winProbability, urgency, actionFit),
                highValue
        );
    }

    private int computeCustomerValue(Customer customer) {
        if (customer == null) {
            return 28;
        }

        int score = 30;
        String level = StrUtil.blankToDefault(customer.getLevel(), "");
        if ("A".equalsIgnoreCase(level)) {
            score += 28;
        } else if ("B".equalsIgnoreCase(level)) {
            score += 18;
        } else if ("C".equalsIgnoreCase(level)) {
            score += 8;
        }

        score += scoreAmount(customer.getQuotation(), 10, 16, 24);

        if (customer.getContactCount() != null) {
            score += Math.min(10, Math.max(0, customer.getContactCount()) * 2);
        }

        return clamp(score, 10, 98);
    }

    private int computeWinProbability(Customer customer) {
        if (customer == null) {
            return 24;
        }

        int score = switch (StrUtil.blankToDefault(customer.getStage(), "").toLowerCase()) {
            case "lead" -> 25;
            case "qualified" -> 45;
            case "proposal" -> 65;
            case "negotiation" -> 82;
            case "closed" -> 90;
            case "lost" -> 5;
            default -> 30;
        };

        long daysSinceLastContact = daysSince(customer.getLastContactTime());
        if (daysSinceLastContact >= 0) {
            if (daysSinceLastContact <= 3) {
                score += 8;
            } else if (daysSinceLastContact <= 7) {
                score += 4;
            } else if (daysSinceLastContact > 21) {
                score -= 12;
            } else if (daysSinceLastContact > 14) {
                score -= 6;
            }
        }

        score += scoreNextFollowMomentum(customer.getNextFollowTime());
        return clamp(score, 5, 98);
    }

    private int computeUrgency(TaskPriorityInput task, Customer customer) {
        if ("COMPLETED".equalsIgnoreCase(task.status())) {
            return 0;
        }

        int score = scoreDeadline(task.dueDate(), 20, 100, 90, 75, 60, 40, 25);
        if (customer != null) {
            score = Math.max(score, scoreDeadline(customer.getNextFollowTime(), 15, 88, 82, 68, 52, 34, 20));
        }
        if ("IN_PROGRESS".equalsIgnoreCase(task.status())) {
            score = Math.min(100, score + 6);
        }
        return clamp(score, 0, 100);
    }

    private int computeActionFit(TaskPriorityInput task, Customer customer) {
        int score = customer != null ? 55 : 35;
        String stage = customer == null ? "" : StrUtil.blankToDefault(customer.getStage(), "").toLowerCase();
        String taskType = StrUtil.blankToDefault(task.taskType(), "");
        String taskText = (StrUtil.blankToDefault(task.title(), "") + " " + StrUtil.blankToDefault(task.description(), "")).toLowerCase();

        if (containsAny(taskType, "跟进", "follow") || containsAny(taskText, "跟进", "回访", "follow")) {
            score += isStage(stage, "lead", "qualified", "proposal", "negotiation") ? 18 : 10;
        }
        if (containsAny(taskType, "电话", "call") || containsAny(taskText, "电话", "沟通", "call")) {
            score += isStage(stage, "lead", "qualified", "proposal") ? 15 : 8;
        }
        if (containsAny(taskType, "会议", "meeting", "visit") || containsAny(taskText, "会议", "拜访", "演示", "meeting")) {
            score += isStage(stage, "proposal", "negotiation") ? 18 : 10;
        }
        if (containsAny(taskType, "文档", "document") || containsAny(taskText, "方案", "报价", "合同", "proposal", "quote", "contract")) {
            score += isStage(stage, "proposal", "negotiation") ? 18 : 8;
        }
        if (task.generatedByAi() != null && task.generatedByAi() == 1) {
            score += 5;
        }
        if (task.customerId() == null) {
            score -= 8;
        }
        return clamp(score, 20, 95);
    }

    private int computeManualPriorityBonus(TaskPriorityInput task) {
        return switch (StrUtil.blankToDefault(task.priority(), "")) {
            case "HIGH" -> 6;
            case "MEDIUM" -> 2;
            default -> 0;
        };
    }

    private int computeRiskPenalty(TaskPriorityInput task, Customer customer) {
        int penalty = 0;
        if (task.customerId() == null) {
            penalty += 10;
        } else if (customer == null) {
            penalty += 6;
        }

        if (customer != null) {
            if ("lost".equalsIgnoreCase(customer.getStage())) {
                penalty += 20;
            }
            if (customer.getNextFollowTime() == null) {
                penalty += 4;
            }
        }

        if (StrUtil.isBlank(task.description())) {
            penalty += 2;
        }
        return penalty;
    }

    private String buildValuePriorityReason(TaskPriorityInput task,
                                            Customer customer,
                                            int score,
                                            int customerValue,
                                            int winProbability,
                                            int urgency,
                                            int actionFit) {
        List<String> reasons = new ArrayList<>();
        String stage = customer == null ? "" : StrUtil.blankToDefault(customer.getStage(), "");

        if (StrUtil.isNotBlank(stage)) {
            reasons.add("客户处于" + getStageLabel(stage) + "阶段");
        }
        if (urgency >= 85) {
            reasons.add("当前处理窗口已经非常接近");
        } else if (urgency >= 65) {
            reasons.add("任务时效性较强");
        }
        if (hasCommercialSignal(customer) && customerValue >= 70) {
            reasons.add("已有明确预计成交金额信号");
        }
        if (actionFit >= 75) {
            reasons.add("当前任务与客户阶段高度匹配");
        }
        if (customer == null && task.customerId() == null) {
            reasons.add("未绑定客户，价值判断偏保守");
        }

        if (reasons.isEmpty()) {
            if (winProbability >= 75) {
                reasons.add("成交推进概率较高");
            } else if (customerValue >= 70) {
                reasons.add("客户价值较高");
            } else {
                reasons.add("建议按常规节奏跟进");
            }
        }

        String summary = reasons.stream().limit(3).collect(Collectors.joining("，"));
        if ("COMPLETED".equalsIgnoreCase(task.status())) {
            return summary + "，当前任务已完成，排序仅供回看参考。";
        }
        if (score >= HIGH_VALUE_THRESHOLD) {
            return summary + "，建议优先推进。";
        }
        if (score >= MEDIUM_VALUE_THRESHOLD) {
            return summary + "，建议本周持续跟进。";
        }
        return summary + "，可按计划处理。";
    }

    private TaskPriorityInput toTaskPriorityInput(TaskVO task) {
        return new TaskPriorityInput(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreateTime(),
                task.getCustomerId(),
                task.getGeneratedByAi(),
                task.getTaskType()
        );
    }

    private TaskPriorityInput toTaskPriorityInput(Task task) {
        return new TaskPriorityInput(
                task.getTaskId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreateTime(),
                task.getCustomerId(),
                task.getGeneratedByAi(),
                task.getTaskType()
        );
    }

    private int scoreAmount(BigDecimal amount, int lowScore, int mediumScore, int highScore) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (amount.compareTo(new BigDecimal("500000")) >= 0) {
            return highScore;
        }
        if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            return mediumScore;
        }
        return lowScore;
    }

    private int scoreNextFollowMomentum(Date nextFollowTime) {
        if (nextFollowTime == null) {
            return -6;
        }
        long hours = hoursUntil(nextFollowTime);
        if (hours < 0) {
            return -10;
        }
        if (hours <= 24) {
            return 8;
        }
        if (hours <= 72) {
            return 6;
        }
        if (hours <= 24 * 7L) {
            return 3;
        }
        return 0;
    }

    private int scoreDeadline(Date deadline,
                              int emptyScore,
                              int overdueScore,
                              int sameDayScore,
                              int nextDayScore,
                              int threeDayScore,
                              int sevenDayScore,
                              int relaxedScore) {
        if (deadline == null) {
            return emptyScore;
        }
        long hours = hoursUntil(deadline);
        if (hours < 0) {
            return overdueScore;
        }
        if (hours <= 24) {
            return sameDayScore;
        }
        if (hours <= 48) {
            return nextDayScore;
        }
        if (hours <= 24 * 3L) {
            return threeDayScore;
        }
        if (hours <= 24 * 7L) {
            return sevenDayScore;
        }
        return relaxedScore;
    }

    private long daysSince(Date value) {
        if (value == null) {
            return -1;
        }
        return TimeUnit.MILLISECONDS.toDays(Math.max(0, System.currentTimeMillis() - value.getTime()));
    }

    private long hoursUntil(Date value) {
        if (value == null) {
            return Long.MAX_VALUE;
        }
        return TimeUnit.MILLISECONDS.toHours(value.getTime() - System.currentTimeMillis());
    }

    private boolean hasCommercialSignal(Customer customer) {
        return customer != null && scoreAmount(customer.getQuotation(), 1, 1, 1) > 0;
    }

    private boolean isStage(String actualStage, String... stages) {
        if (StrUtil.isBlank(actualStage)) {
            return false;
        }
        for (String stage : stages) {
            if (actualStage.equalsIgnoreCase(stage)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text) || keywords == null) {
            return false;
        }
        String normalized = text.toLowerCase();
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private String getStageLabel(String stage) {
        return switch (stage.toLowerCase()) {
            case "lead" -> "线索初期";
            case "qualified" -> "需求确认";
            case "proposal" -> "方案评估";
            case "negotiation" -> "商务谈判";
            case "closed" -> "成交";
            case "lost" -> "流失";
            default -> "推进中";
        };
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    private record TaskPriorityInput(
            Long taskId,
            String title,
            String description,
            String status,
            String priority,
            Date dueDate,
            Date createTime,
            Long customerId,
            Integer generatedByAi,
            String taskType
    ) {
    }

    private record TaskValuePriorityResult(
            int score,
            String tier,
            String reason,
            boolean highValue
    ) {
    }
}
