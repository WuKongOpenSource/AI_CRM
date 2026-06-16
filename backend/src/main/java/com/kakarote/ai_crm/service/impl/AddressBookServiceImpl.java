package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.Const;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.enums.EmployeeStatusEnum;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.entity.BO.AddressBookQueryBO;
import com.kakarote.ai_crm.entity.BO.KnowledgeQueryBO;
import com.kakarote.ai_crm.entity.BO.ScheduleQueryBO;
import com.kakarote.ai_crm.entity.BO.TaskQueryBO;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.VO.AddressBookDetailVO;
import com.kakarote.ai_crm.entity.VO.AddressBookEmployeeVO;
import com.kakarote.ai_crm.entity.VO.AddressBookRecentRecordVO;
import com.kakarote.ai_crm.entity.VO.KnowledgeVO;
import com.kakarote.ai_crm.entity.VO.ScheduleVO;
import com.kakarote.ai_crm.entity.VO.TaskVO;
import com.kakarote.ai_crm.mapper.AddressBookMapper;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.FileStorageService;
import com.kakarote.ai_crm.service.IAddressBookService;
import com.kakarote.ai_crm.service.IKnowledgeService;
import com.kakarote.ai_crm.service.IScheduleService;
import com.kakarote.ai_crm.service.ITaskService;
import com.kakarote.ai_crm.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AddressBookServiceImpl implements IAddressBookService {

    private static final int RELATED_LIMIT = 8;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ManagerDeptMapper deptMapper;

    @Autowired
    private DataPermissionService dataPermissionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private IScheduleService scheduleService;

    @Autowired
    private IKnowledgeService knowledgeService;

    @Override
    public BasePage<AddressBookEmployeeVO> queryPageList(AddressBookQueryBO queryBO) {
        if (queryBO == null) {
            queryBO = new AddressBookQueryBO();
        }
        prepareQuery(queryBO, "addressBook:list");
        BasePage<AddressBookEmployeeVO> page = addressBookMapper.queryPageList(queryBO.parse(), queryBO);
        fillEmployeePresentation(page.getList());
        return page;
    }

    @Override
    public AddressBookDetailVO getDetail(Long userId) {
        dataPermissionService.assertUserDataAccessByPermission("addressBook:detail", userId);
        AddressBookDetailVO detail = addressBookMapper.getDetail(userId);
        if (detail == null) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "员工不存在或无权限访问");
        }
        fillEmployeePresentation(List.of(detail));
        detail.setRelatedTasks(loadRelatedTasks(userId));
        detail.setRelatedSchedules(loadRelatedSchedules(userId));
        detail.setRelatedAttachments(loadRelatedAttachments(userId));
        detail.setRecentRecords(buildRecentRecords(detail));
        return detail;
    }

    private void prepareQuery(AddressBookQueryBO queryBO, String permission) {
        if (queryBO == null) {
            return;
        }
        if (StrUtil.isNotBlank(queryBO.getEmployeeStatus())) {
            queryBO.setEmployeeStatus(EmployeeStatusEnum.normalize(queryBO.getEmployeeStatus()));
        }
        if (queryBO.getDeptId() != null) {
            List<ManagerDept> allDepts = deptMapper.selectList(null);
            Set<Long> deptIds = new LinkedHashSet<>();
            collectChildDeptIds(allDepts, queryBO.getDeptId(), deptIds, Const.AUTH_DATA_RECURSION_NUM);
            queryBO.setDeptIds(new ArrayList<>(deptIds));
        }
        DataPermissionContext context = dataPermissionService.createContextByPermission(permission);
        queryBO.setAllData(context.isAllData());
        queryBO.setUserIds(context.getUserIds());
    }

    private void collectChildDeptIds(List<ManagerDept> allDepts, Long parentId, Set<Long> result, int depth) {
        if (parentId == null || depth <= 0 || !result.add(parentId)) {
            return;
        }
        for (ManagerDept dept : allDepts) {
            if (Objects.equals(parentId, dept.getParentId())) {
                collectChildDeptIds(allDepts, dept.getDeptId(), result, depth - 1);
            }
        }
    }

    private void fillEmployeePresentation(List<? extends AddressBookEmployeeVO> employees) {
        if (CollUtil.isEmpty(employees)) {
            return;
        }
        for (AddressBookEmployeeVO employee : employees) {
            String normalized = EmployeeStatusEnum.normalize(employee.getEmployeeStatus());
            employee.setEmployeeStatus(normalized);
            employee.setEmployeeStatusName(EmployeeStatusEnum.getName(normalized));
            if (StrUtil.isNotBlank(employee.getImg())) {
                try {
                    employee.setImgUrl(fileStorageService.getUrl(employee.getImg()));
                } catch (Exception ignored) {
                }
            }
        }
    }

    private List<TaskVO> loadRelatedTasks(Long userId) {
        if (!hasPermission("task:view")) {
            return List.of();
        }
        TaskQueryBO queryBO = new TaskQueryBO();
        queryBO.setPage(1);
        queryBO.setLimit(RELATED_LIMIT);
        queryBO.setAssignedTo(userId);
        return taskService.queryPageList(queryBO).getList();
    }

    private List<ScheduleVO> loadRelatedSchedules(Long userId) {
        if (!hasPermission("schedule:view")) {
            return List.of();
        }
        ScheduleQueryBO queryBO = new ScheduleQueryBO();
        queryBO.setPage(1);
        queryBO.setLimit(RELATED_LIMIT);
        queryBO.setParticipantUserId(userId);
        return scheduleService.queryPageList(queryBO).getList();
    }

    private List<KnowledgeVO> loadRelatedAttachments(Long userId) {
        if (!hasPermission("knowledge:view")) {
            return List.of();
        }
        KnowledgeQueryBO queryBO = new KnowledgeQueryBO();
        queryBO.setPage(1);
        queryBO.setLimit(RELATED_LIMIT);
        queryBO.setEmployeeId(userId);
        return knowledgeService.queryPageList(queryBO).getList();
    }

    private boolean hasPermission(String permission) {
        try {
            return permissionService.hasPermission(permission);
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<AddressBookRecentRecordVO> buildRecentRecords(AddressBookDetailVO detail) {
        List<AddressBookRecentRecordVO> records = new ArrayList<>();
        for (TaskVO task : detail.getRelatedTasks()) {
            Date recordTime = firstDate(task.getCompletedTime(), task.getDueDate(), task.getCreateTime());
            records.add(buildRecord("task", task.getTitle(), task.getDescription(), recordTime));
        }
        for (ScheduleVO schedule : detail.getRelatedSchedules()) {
            records.add(buildRecord("schedule", schedule.getTitle(), schedule.getDescription(), schedule.getStartTime()));
        }
        for (KnowledgeVO knowledge : detail.getRelatedAttachments()) {
            records.add(buildRecord("attachment", knowledge.getName(), knowledge.getSummary(), knowledge.getCreateTime()));
        }
        return records.stream()
                .filter(record -> record.getRecordTime() != null)
                .sorted(Comparator.comparing(AddressBookRecentRecordVO::getRecordTime).reversed())
                .limit(RELATED_LIMIT)
                .toList();
    }

    private AddressBookRecentRecordVO buildRecord(String type, String title, String description, Date recordTime) {
        AddressBookRecentRecordVO record = new AddressBookRecentRecordVO();
        record.setType(type);
        record.setTitle(title);
        record.setDescription(description);
        record.setRecordTime(recordTime);
        return record;
    }

    private Date firstDate(Date... dates) {
        if (dates == null) {
            return null;
        }
        for (Date date : dates) {
            if (date != null) {
                return date;
            }
        }
        return null;
    }
}
