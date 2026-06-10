package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kakarote.ai_crm.common.BasePage;
import com.kakarote.ai_crm.common.auth.DataPermissionContext;
import com.kakarote.ai_crm.common.enums.CustomerStageEnum;
import com.kakarote.ai_crm.entity.BO.GlobalSearchQueryBO;
import com.kakarote.ai_crm.entity.PO.Contact;
import com.kakarote.ai_crm.entity.PO.ContactTag;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.CustomerTag;
import com.kakarote.ai_crm.entity.PO.GlobalSearchIndex;
import com.kakarote.ai_crm.entity.PO.Knowledge;
import com.kakarote.ai_crm.entity.PO.KnowledgeTag;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.Relation;
import com.kakarote.ai_crm.entity.PO.Schedule;
import com.kakarote.ai_crm.entity.PO.Task;
import com.kakarote.ai_crm.entity.VO.CustomFieldVO;
import com.kakarote.ai_crm.entity.VO.GlobalSearchResultVO;
import com.kakarote.ai_crm.mapper.ContactMapper;
import com.kakarote.ai_crm.mapper.ContactTagMapper;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.CustomerTagMapper;
import com.kakarote.ai_crm.mapper.GlobalSearchIndexMapper;
import com.kakarote.ai_crm.mapper.KnowledgeMapper;
import com.kakarote.ai_crm.mapper.KnowledgeTagMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.RelationMapper;
import com.kakarote.ai_crm.mapper.ScheduleMapper;
import com.kakarote.ai_crm.mapper.TaskMapper;
import com.kakarote.ai_crm.service.DataPermissionService;
import com.kakarote.ai_crm.service.ICustomFieldService;
import com.kakarote.ai_crm.service.IGlobalSearchIndexService;
import com.kakarote.ai_crm.service.PermissionService;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GlobalSearchIndexServiceImpl extends ServiceImpl<GlobalSearchIndexMapper, GlobalSearchIndex>
        implements IGlobalSearchIndexService {

    private static final String ENTITY_CUSTOMER = "customer";
    private static final String ENTITY_CONTACT = "contact";
    private static final String ENTITY_RELATION = "relation";
    private static final String ENTITY_TASK = "task";
    private static final String ENTITY_SCHEDULE = "schedule";
    private static final String ENTITY_KNOWLEDGE = "knowledge";

    private static final Set<String> SEARCHABLE_CUSTOM_FIELD_TYPES = Set.of("text", "textarea", "select", "multiselect");
    private static final int SUMMARY_LIMIT = 180;
    private static final int KNOWLEDGE_CONTENT_SEARCH_LIMIT = 10000;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerTagMapper customerTagMapper;

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private ContactTagMapper contactTagMapper;

    @Autowired
    private RelationMapper relationMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private ScheduleMapper scheduleMapper;

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired
    private KnowledgeTagMapper knowledgeTagMapper;

    @Autowired
    private ManageUserMapper manageUserMapper;

    @Autowired
    private ICustomFieldService customFieldService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DataPermissionService dataPermissionService;

    /**
     * 分页查询全局搜索索引列表。
     */
    @Override
    public BasePage<GlobalSearchResultVO> queryPageList(GlobalSearchQueryBO queryBO) {
        queryBO.setTenantId(UserUtil.getTenantId());
        queryBO.setCurrentUserId(UserUtil.getUserId());
        queryBO.setKeyword(normalizeSearchText(queryBO.getKeyword()));
        applyPermissionScope(queryBO);

        if (!hasAnyEnabledModule(queryBO) || isDisabledRequestedEntity(queryBO)) {
            BasePage<GlobalSearchResultVO> emptyPage = new BasePage<>(queryBO.getPage(), queryBO.getLimit());
            emptyPage.setTotal(0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        BasePage<GlobalSearchResultVO> page = queryBO.parse();
        page.setSearchCount(false);
        page.setOptimizeCountSql(false);
        page.setOptimizeJoinOfCountSql(false);

        Long total = baseMapper.queryPageCount(queryBO);
        page.setTotal(total == null ? 0L : total);
        if (page.getTotal() <= 0) {
            page.setRecords(Collections.emptyList());
            return page;
        }

        baseMapper.queryPageList(page, queryBO);
        return page;
    }

    /**
     * 刷新客户索引。
     */
    @Override
    public void refreshCustomerIndex(Long customerId) {
        if (customerId == null) {
            return;
        }

        Customer customer = customerMapper.selectByIdIgnoreDataPermission(customerId);
        if (customer == null || !Objects.equals(customer.getStatus(), 1)) {
            deleteByEntity(ENTITY_CUSTOMER, customerId);
            return;
        }

        Map<Long, String> userNameMap = getUserDisplayNames(collectUserIds(customer.getOwnerId()));
        List<String> tags = customerTagMapper.getTagsByCustomerId(customerId);

        GlobalSearchIndex index = new GlobalSearchIndex();
        index.setTenantId(customer.getTenantId());
        index.setEntityType(ENTITY_CUSTOMER);
        index.setEntityId(customer.getCustomerId());
        index.setTitle(customer.getCompanyName());
        index.setSubtitle(buildSubtitle("客户", firstNonBlank(customer.getIndustry(), getStageLabel(customer.getStage()))));
        index.setSummary(truncateSummary(firstNonBlank(customer.getAiStatusDetection(), customer.getRemark(), customer.getPrimaryContactName())));
        index.setCustomerId(customer.getCustomerId());
        index.setCustomerName(customer.getCompanyName());
        index.setOwnerUserId(customer.getOwnerId());
        index.setRoutePath("/customer/" + customer.getCustomerId());
        index.setSortTime(firstNonNull(customer.getUpdateTime(), customer.getCreateTime()));
        index.setSearchText(normalizeSearchText(joinFragments(
                customer.getSearchText(),
                customer.getCompanyName(),
                customer.getIndustry(),
                getStageLabel(customer.getStage()),
                getLevelLabel(customer.getLevel()),
                customer.getSource(),
                customer.getAddress(),
                customer.getWebsite(),
                customer.getPrimaryContactName(),
                customer.getPrimaryContactPhone(),
                customer.getPrimaryContactPosition(),
                userNameMap.get(customer.getOwnerId()),
                tags,
                customer.getRemark(),
                customer.getAiStatusDetection(),
                customer.getAiInsight()
        )));
        baseMapper.upsert(fillSearchFallback(index));
    }

    /**
     * 刷新客户关联索引。
     */
    @Override
    public void refreshCustomerRelatedIndexes(Long customerId) {
        if (customerId == null) {
            return;
        }

        contactMapper.selectByCustomerIdIgnoreDataPermission(customerId)
                .forEach(contact -> refreshContactIndex(contact.getContactId()));
        taskMapper.selectByCustomerIdIgnoreDataPermission(customerId)
                .forEach(task -> refreshTaskIndex(task.getTaskId()));
        scheduleMapper.selectByCustomerIdIgnoreDataPermission(customerId)
                .forEach(schedule -> refreshScheduleIndex(schedule.getScheduleId()));
        knowledgeMapper.selectByCustomerIdIgnoreDataPermission(customerId)
                .forEach(knowledge -> refreshKnowledgeIndex(knowledge.getKnowledgeId()));
    }

    /**
     * 刷新联系人索引。
     */
    @Override
    public void refreshContactIndex(Long contactId) {
        if (contactId == null) {
            return;
        }

        Contact contact = contactMapper.selectByIdIgnoreDataPermission(contactId);
        if (contact == null || !Objects.equals(contact.getStatus(), 1)) {
            deleteByEntity(ENTITY_CONTACT, contactId);
            return;
        }

        Customer customer = customerMapper.selectByIdIgnoreDataPermission(contact.getCustomerId());
        Map<String, Object> customFields = customFieldService.getCustomFieldValues(ENTITY_CONTACT, contactId);
        List<String> tags = contactTagMapper.getTagsByContactId(contactId);

        GlobalSearchIndex index = new GlobalSearchIndex();
        index.setTenantId(contact.getTenantId());
        index.setEntityType(ENTITY_CONTACT);
        index.setEntityId(contact.getContactId());
        index.setTitle(contact.getName());
        index.setSubtitle(buildSubtitle("联系人", customer != null ? customer.getCompanyName() : null));
        index.setSummary(truncateSummary(firstNonBlank(contact.getPosition(), contact.getPhone(), contact.getEmail(), contact.getNotes())));
        index.setCustomerId(contact.getCustomerId());
        index.setCustomerName(customer != null ? customer.getCompanyName() : null);
        index.setCustomerOwnerId(customer != null ? customer.getOwnerId() : null);
        index.setRoutePath(contact.getCustomerId() == null
                ? "/customer"
                : "/customer/" + contact.getCustomerId() + "?openContactId=" + contact.getContactId());
        index.setSortTime(firstNonNull(contact.getUpdateTime(), contact.getLastContactTime(), contact.getCreateTime()));
        index.setSearchText(normalizeSearchText(joinFragments(
                contact.getName(),
                contact.getPosition(),
                contact.getPhone(),
                contact.getEmail(),
                contact.getWechat(),
                customer != null ? customer.getCompanyName() : null,
                tags,
                extractSearchableCustomFieldText(ENTITY_CONTACT, customFields),
                contact.getNotes()
        )));
        baseMapper.upsert(fillSearchFallback(index));
    }

    /**
     * 刷新关系人索引。
     */
    @Override
    public void refreshRelationIndex(Long relationId) {
        if (relationId == null) {
            return;
        }

        Relation relation = relationMapper.selectById(relationId);
        if (relation == null || !Objects.equals(relation.getStatus(), 1)) {
            deleteByEntity(ENTITY_RELATION, relationId);
            return;
        }

        Customer linkedCustomer = relation.getCustomerId() == null
                ? null
                : customerMapper.selectByIdIgnoreDataPermission(relation.getCustomerId());
        Customer sourceCustomer = relation.getSourceCustomerId() == null
                ? null
                : customerMapper.selectByIdIgnoreDataPermission(relation.getSourceCustomerId());
        Map<String, Object> customFields = customFieldService.getCustomFieldValues(ENTITY_RELATION, relationId);

        GlobalSearchIndex index = new GlobalSearchIndex();
        index.setTenantId(relation.getTenantId());
        index.setEntityType(ENTITY_RELATION);
        index.setEntityId(relation.getRelationId());
        index.setTitle(relation.getName());
        index.setSubtitle(buildSubtitle("关系", linkedCustomer != null ? linkedCustomer.getCompanyName() : null));
        index.setSummary(truncateSummary(firstNonBlank(relation.getRemark(), relation.getPhone(), relation.getEmail())));
        index.setCustomerId(relation.getCustomerId());
        index.setCustomerName(linkedCustomer != null ? linkedCustomer.getCompanyName() : null);
        index.setOwnerUserId(relation.getCreateUserId());
        index.setCreateUserId(relation.getCreateUserId());
        index.setRoutePath("/relation?openRelationId=" + relation.getRelationId());
        index.setSortTime(firstNonNull(relation.getUpdateTime(), relation.getCreateTime()));
        index.setSearchText(normalizeSearchText(joinFragments(
                relation.getName(),
                relation.getPhone(),
                relation.getWechat(),
                relation.getEmail(),
                relation.getRelationType(),
                relation.getSource(),
                linkedCustomer != null ? linkedCustomer.getCompanyName() : null,
                sourceCustomer != null ? sourceCustomer.getCompanyName() : null,
                extractSearchableCustomFieldText(ENTITY_RELATION, customFields),
                relation.getRemark()
        )));
        baseMapper.upsert(fillSearchFallback(index));
    }

    /**
     * 刷新任务索引。
     */
    @Override
    public void refreshTaskIndex(Long taskId) {
        if (taskId == null) {
            return;
        }

        Task task = taskMapper.selectByIdIgnoreDataPermission(taskId);
        if (task == null) {
            deleteByEntity(ENTITY_TASK, taskId);
            return;
        }

        Customer customer = customerMapper.selectByIdIgnoreDataPermission(task.getCustomerId());
        Relation relation = task.getRelationId() == null ? null : relationMapper.selectById(task.getRelationId());
        Map<Long, String> userNameMap = getUserDisplayNames(collectUserIds(task.getAssignedTo(), task.getCreateUserId()));

        GlobalSearchIndex index = new GlobalSearchIndex();
        index.setTenantId(task.getTenantId());
        index.setEntityType(ENTITY_TASK);
        index.setEntityId(task.getTaskId());
        index.setTitle(task.getTitle());
        index.setSubtitle(buildSubtitle("任务", firstNonBlank(customer != null ? customer.getCompanyName() : null, relation != null ? relation.getName() : null)));
        index.setSummary(truncateSummary(firstNonBlank(task.getDescription(), task.getParticipantNames(), task.getTaskType())));
        index.setCustomerId(task.getCustomerId());
        index.setCustomerName(customer != null ? customer.getCompanyName() : null);
        index.setCustomerOwnerId(customer != null ? customer.getOwnerId() : null);
        index.setAssignedUserId(task.getAssignedTo());
        index.setOwnerUserId(relation != null ? relation.getCreateUserId() : null);
        index.setRoutePath("/task?openTaskId=" + task.getTaskId());
        index.setSortTime(firstNonNull(task.getDueDate(), task.getUpdateTime(), task.getCreateTime()));
        index.setSearchText(normalizeSearchText(joinFragments(
                task.getTitle(),
                task.getDescription(),
                task.getTaskType(),
                task.getParticipantNames(),
                getTaskPriorityLabel(task.getPriority()),
                getTaskStatusLabel(task.getStatus()),
                customer != null ? customer.getCompanyName() : null,
                relation != null ? relation.getName() : null,
                userNameMap.get(task.getAssignedTo()),
                userNameMap.get(task.getCreateUserId())
        )));
        baseMapper.upsert(fillSearchFallback(index));
    }

    /**
     * 刷新日程索引。
     */
    @Override
    public void refreshScheduleIndex(Long scheduleId) {
        if (scheduleId == null) {
            return;
        }

        Schedule schedule = scheduleMapper.selectByIdIgnoreDataPermission(scheduleId);
        if (schedule == null) {
            deleteByEntity(ENTITY_SCHEDULE, scheduleId);
            return;
        }

        Customer customer = customerMapper.selectByIdIgnoreDataPermission(schedule.getCustomerId());
        Relation relation = schedule.getRelationId() == null ? null : relationMapper.selectById(schedule.getRelationId());
        Contact contact = schedule.getContactId() == null
                ? null
                : contactMapper.selectByIdIgnoreDataPermission(schedule.getContactId());

        Set<Long> participantIds = parseParticipantUserIds(schedule.getParticipantUserIds());
        participantIds.add(schedule.getCreateUserId());
        Map<Long, String> userNameMap = getUserDisplayNames(participantIds);
        String participantNames = parseParticipantUserIds(schedule.getParticipantUserIds()).stream()
                .map(userNameMap::get)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(", "));

        GlobalSearchIndex index = new GlobalSearchIndex();
        index.setTenantId(schedule.getTenantId());
        index.setEntityType(ENTITY_SCHEDULE);
        index.setEntityId(schedule.getScheduleId());
        index.setTitle(schedule.getTitle());
        index.setSubtitle(buildSubtitle("日程", firstNonBlank(customer != null ? customer.getCompanyName() : null, relation != null ? relation.getName() : null, getScheduleTypeLabel(schedule.getType()))));
        index.setSummary(truncateSummary(firstNonBlank(schedule.getLocation(), schedule.getDescription(), participantNames)));
        index.setCustomerId(schedule.getCustomerId());
        index.setCustomerName(customer != null ? customer.getCompanyName() : null);
        index.setCustomerOwnerId(customer != null ? customer.getOwnerId() : null);
        index.setCreateUserId(schedule.getCreateUserId());
        index.setOwnerUserId(relation != null ? relation.getCreateUserId() : null);
        index.setParticipantUserIds(schedule.getParticipantUserIds());
        index.setRoutePath("/calendar?openScheduleId=" + schedule.getScheduleId());
        index.setSortTime(firstNonNull(schedule.getStartTime(), schedule.getUpdateTime(), schedule.getCreateTime()));
        index.setSearchText(normalizeSearchText(joinFragments(
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getLocation(),
                getScheduleTypeLabel(schedule.getType()),
                customer != null ? customer.getCompanyName() : null,
                relation != null ? relation.getName() : null,
                contact != null ? contact.getName() : null,
                participantNames,
                userNameMap.get(schedule.getCreateUserId())
        )));
        baseMapper.upsert(fillSearchFallback(index));
    }

    /**
     * 刷新知识索引。
     */
    @Override
    public void refreshKnowledgeIndex(Long knowledgeId) {
        if (knowledgeId == null) {
            return;
        }

        Knowledge knowledge = knowledgeMapper.selectByIdIgnoreDataPermission(knowledgeId);
        if (knowledge == null || Objects.equals(knowledge.getStatus(), 2)) {
            deleteByEntity(ENTITY_KNOWLEDGE, knowledgeId);
            return;
        }

        Customer customer = customerMapper.selectByIdIgnoreDataPermission(knowledge.getCustomerId());
        Relation relation = knowledge.getRelationId() == null ? null : relationMapper.selectById(knowledge.getRelationId());
        Map<Long, String> userNameMap = getUserDisplayNames(collectUserIds(knowledge.getUploadUserId()));
        List<String> tags = knowledgeTagMapper.getTagsByKnowledgeId(knowledgeId);

        GlobalSearchIndex index = new GlobalSearchIndex();
        index.setTenantId(knowledge.getTenantId());
        index.setEntityType(ENTITY_KNOWLEDGE);
        index.setEntityId(knowledge.getKnowledgeId());
        index.setTitle(knowledge.getName());
        index.setSubtitle(buildSubtitle("知识库", firstNonBlank(customer != null ? customer.getCompanyName() : null, relation != null ? relation.getName() : null, getKnowledgeTypeLabel(knowledge.getType()))));
        index.setSummary(truncateSummary(firstNonBlank(knowledge.getSummary(), knowledge.getContentText())));
        index.setCustomerId(knowledge.getCustomerId());
        index.setCustomerName(customer != null ? customer.getCompanyName() : null);
        index.setCustomerOwnerId(customer != null ? customer.getOwnerId() : null);
        index.setOwnerUserId(relation != null ? relation.getCreateUserId() : null);
        index.setUploadUserId(knowledge.getUploadUserId());
        index.setRoutePath("/knowledge?openKnowledgeId=" + knowledge.getKnowledgeId());
        index.setSortTime(firstNonNull(knowledge.getUpdateTime(), knowledge.getCreateTime()));
        index.setSearchText(normalizeSearchText(joinFragments(
                knowledge.getName(),
                getKnowledgeTypeLabel(knowledge.getType()),
                customer != null ? customer.getCompanyName() : null,
                relation != null ? relation.getName() : null,
                userNameMap.get(knowledge.getUploadUserId()),
                tags,
                knowledge.getSummary(),
                truncateText(knowledge.getContentText(), KNOWLEDGE_CONTENT_SEARCH_LIMIT)
        )));
        baseMapper.upsert(fillSearchFallback(index));
    }

    /**
     * 删除按Entity。
     */
    @Override
    public void deleteByEntity(String entityType, Long entityId) {
        if (StrUtil.isBlank(entityType) || entityId == null || UserUtil.getTenantId() == null) {
            return;
        }
        baseMapper.deleteByEntity(UserUtil.getTenantId(), entityType, entityId);
    }

    /**
     * 删除联系人索引按客户ID。
     */
    @Override
    public void deleteContactIndexesByCustomerId(Long customerId) {
        if (customerId == null || UserUtil.getTenantId() == null) {
            return;
        }
        baseMapper.deleteByCustomerIdAndEntityType(UserUtil.getTenantId(), customerId, ENTITY_CONTACT);
    }

    /**
     * 处理applyPermissionScope方法逻辑。
     */
    private void applyPermissionScope(GlobalSearchQueryBO queryBO) {
        applyScopedModule(queryBO, ENTITY_CUSTOMER, "customer:view");
        applyScopedModule(queryBO, ENTITY_CONTACT, "contact:view");
        applyRelationScope(queryBO);
        applyScopedModule(queryBO, ENTITY_TASK, "task:view");
        applyScheduleScope(queryBO);
        applyScopedModule(queryBO, ENTITY_KNOWLEDGE, "knowledge:view");
    }

    /**
     * 处理apply范围内模块方法逻辑。
     */
    private void applyScopedModule(GlobalSearchQueryBO queryBO, String module, String permission) {
        boolean enabled = permissionService.hasPermission(permission);
        DataPermissionContext context = enabled ? dataPermissionService.createContext(module) : DataPermissionContext.none();
        boolean allData = enabled && context.isAllData();
        List<Long> userIds = enabled && context.getUserIds() != null ? context.getUserIds() : Collections.emptyList();

        switch (module) {
            case ENTITY_CUSTOMER -> {
                queryBO.setCustomerEnabled(enabled);
                queryBO.setCustomerAllData(allData);
                queryBO.setCustomerUserIds(userIds);
            }
            case ENTITY_CONTACT -> {
                queryBO.setContactEnabled(enabled);
                queryBO.setContactAllData(allData);
                queryBO.setContactUserIds(userIds);
            }
            case ENTITY_TASK -> {
                queryBO.setTaskEnabled(enabled);
                queryBO.setTaskAllData(allData);
                queryBO.setTaskUserIds(userIds);
            }
            case ENTITY_KNOWLEDGE -> {
                queryBO.setKnowledgeEnabled(enabled);
                queryBO.setKnowledgeAllData(allData);
                queryBO.setKnowledgeUserIds(userIds);
            }
            default -> {
            }
        }
    }

    /**
     * 处理applyScheduleScope方法逻辑。
     */
    private void applyScheduleScope(GlobalSearchQueryBO queryBO) {
        boolean enabled = permissionService.hasPermission("schedule:view");
        DataPermissionContext context = enabled ? dataPermissionService.createContextByPermission("schedule:view") : DataPermissionContext.none();
        queryBO.setScheduleEnabled(enabled);
        queryBO.setScheduleAllData(enabled && context.isAllData());
        queryBO.setScheduleUserIds(enabled && context.getUserIds() != null ? context.getUserIds() : Collections.emptyList());
    }

    private void applyRelationScope(GlobalSearchQueryBO queryBO) {
        queryBO.setRelationEnabled(permissionService.hasPermission("relation:view"));
    }

    /**
     * 判断是否存在ANY启用项模块。
     */
    private boolean hasAnyEnabledModule(GlobalSearchQueryBO queryBO) {
        return Boolean.TRUE.equals(queryBO.getCustomerEnabled())
                || Boolean.TRUE.equals(queryBO.getContactEnabled())
                || Boolean.TRUE.equals(queryBO.getRelationEnabled())
                || Boolean.TRUE.equals(queryBO.getTaskEnabled())
                || Boolean.TRUE.equals(queryBO.getScheduleEnabled())
                || Boolean.TRUE.equals(queryBO.getKnowledgeEnabled());
    }

    /**
     * 判断是否DisabledRequestedEntity。
     */
    private boolean isDisabledRequestedEntity(GlobalSearchQueryBO queryBO) {
        if (StrUtil.isBlank(queryBO.getEntityType())) {
            return false;
        }
        return switch (queryBO.getEntityType()) {
            case ENTITY_CUSTOMER -> !Boolean.TRUE.equals(queryBO.getCustomerEnabled());
            case ENTITY_CONTACT -> !Boolean.TRUE.equals(queryBO.getContactEnabled());
            case ENTITY_RELATION -> !Boolean.TRUE.equals(queryBO.getRelationEnabled());
            case ENTITY_TASK -> !Boolean.TRUE.equals(queryBO.getTaskEnabled());
            case ENTITY_SCHEDULE -> !Boolean.TRUE.equals(queryBO.getScheduleEnabled());
            case ENTITY_KNOWLEDGE -> !Boolean.TRUE.equals(queryBO.getKnowledgeEnabled());
            default -> true;
        };
    }

    /**
     * 获取用户Display名称。
     */
    private Map<Long, String> getUserDisplayNames(Collection<Long> userIds) {
        Set<Long> ids = userIds == null
                ? Collections.emptySet()
                : userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        return manageUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(
                        ManagerUser::getUserId,
                        this::getUserDisplayName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    /**
     * 处理collectUserIds方法逻辑。
     */
    private Set<Long> collectUserIds(Long... userIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (userIds == null) {
            return result;
        }
        for (Long userId : userIds) {
            if (userId != null) {
                result.add(userId);
            }
        }
        return result;
    }

    /**
     * 获取用户Display名称。
     */
    private String getUserDisplayName(ManagerUser user) {
        if (user == null) {
            return null;
        }
        return firstNonBlank(user.getRealname(), user.getUsername());
    }

    /**
     * 处理extractSearchableCustomFieldText方法逻辑。
     */
    private String extractSearchableCustomFieldText(String entityType, Map<String, Object> customFields) {
        if (customFields == null || customFields.isEmpty()) {
            return null;
        }

        Set<String> searchableFieldNames = customFieldService.getEnabledFieldsByEntity(entityType).stream()
                .filter(field -> !"system".equalsIgnoreCase(field.getFieldSource()))
                .filter(field -> Boolean.TRUE.equals(field.getIsSearchable()))
                .filter(field -> SEARCHABLE_CUSTOM_FIELD_TYPES.contains(StrUtil.blankToDefault(field.getFieldType(), "").toLowerCase(Locale.ROOT)))
                .map(CustomFieldVO::getFieldName)
                .collect(Collectors.toSet());

        if (searchableFieldNames.isEmpty()) {
            return null;
        }

        List<String> fragments = new ArrayList<>();
        for (Map.Entry<String, Object> entry : customFields.entrySet()) {
            if (!searchableFieldNames.contains(entry.getKey())) {
                continue;
            }
            appendFragment(fragments, entry.getValue());
        }
        return joinFragments(fragments);
    }

    /**
     * 解析参与人用户ID。
     */
    private Set<Long> parseParticipantUserIds(String participantUserIds) {
        if (StrUtil.isBlank(participantUserIds)) {
            return new LinkedHashSet<>();
        }

        Set<Long> result = new LinkedHashSet<>();
        for (String part : participantUserIds.split(",")) {
            String value = StrUtil.trim(part);
            if (StrUtil.isBlank(value)) {
                continue;
            }
            try {
                result.add(Long.parseLong(value));
            } catch (NumberFormatException exception) {
                log.warn("Ignore invalid participant_user_ids value: {}", value);
            }
        }
        return result;
    }

    /**
     * 填充搜索兜底。
     */
    private GlobalSearchIndex fillSearchFallback(GlobalSearchIndex index) {
        index.setSearchText(firstNonBlank(index.getSearchText(), normalizeSearchText(joinFragments(index.getTitle(), index.getSubtitle(), index.getSummary()))));
        index.setSortTime(firstNonNull(index.getSortTime(), new Date()));
        return index;
    }

    /**
     * 构建Subtitle。
     */
    private String buildSubtitle(String entityLabel, String context) {
        return StrUtil.isBlank(context) ? entityLabel : entityLabel + " • " + context;
    }

    /**
     * 获取阶段Label。
     */
    private String getStageLabel(String stage) {
        // 真相源：crm_custom_field.options（回退内置枚举）
        String label = customFieldService.resolveOptionLabel("customer", "stage", stage);
        return StrUtil.isNotBlank(label) ? label : CustomerStageEnum.getNameByCode(stage);
    }

    /**
     * 获取LevelLabel。
     */
    private String getLevelLabel(String level) {
        if (StrUtil.isBlank(level)) {
            return null;
        }
        // 真相源：crm_custom_field.options
        return customFieldService.resolveOptionLabel("customer", "level", level);
    }

    /**
     * 获取任务优先级Label。
     */
    private String getTaskPriorityLabel(String priority) {
        if (StrUtil.isBlank(priority)) {
            return null;
        }
        return switch (priority.toLowerCase(Locale.ROOT)) {
            case "high" -> "高优先级";
            case "medium" -> "中优先级";
            case "low" -> "低优先级";
            default -> priority;
        };
    }

    /**
     * 获取任务状态Label。
     */
    private String getTaskStatusLabel(String status) {
        if (StrUtil.isBlank(status)) {
            return null;
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "pending" -> "待处理";
            case "in_progress" -> "进行中";
            case "completed" -> "已完成";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }

    /**
     * 获取日程类型Label。
     */
    private String getScheduleTypeLabel(String type) {
        if (StrUtil.isBlank(type)) {
            return null;
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "meeting" -> "会议";
            case "call" -> "电话";
            case "visit" -> "拜访";
            case "other" -> "其他";
            default -> type;
        };
    }

    /**
     * 获取知识类型Label。
     */
    private String getKnowledgeTypeLabel(String type) {
        if (StrUtil.isBlank(type)) {
            return null;
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "meeting" -> "会议记录";
            case "email" -> "邮件";
            case "recording" -> "录音";
            case "document" -> "文档";
            case "proposal" -> "方案";
            case "contract" -> "合同";
            default -> type;
        };
    }

    /**
     * 处理joinFragments方法逻辑。
     */
    private String joinFragments(Object... values) {
        List<String> fragments = new ArrayList<>();
        if (values != null) {
            for (Object value : values) {
                appendFragment(fragments, value);
            }
        }
        return String.join(" ", fragments);
    }

    /**
     * 处理appendFragment方法逻辑。
     */
    private void appendFragment(List<String> fragments, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> appendFragment(fragments, item));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> appendFragment(fragments, item));
            return;
        }

        String text = StrUtil.trim(String.valueOf(value));
        if (StrUtil.isBlank(text) || "null".equalsIgnoreCase(text)) {
            return;
        }
        fragments.add(text);
    }

    /**
     * 标准化搜索文本。
     */
    private String normalizeSearchText(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String normalized = text.toLowerCase(Locale.ROOT)
                .replace('，', ' ')
                .replace('。', ' ')
                .replace('；', ' ')
                .replace('：', ' ')
                .replace('、', ' ')
                .replace('（', ' ')
                .replace('）', ' ')
                .replace('【', ' ')
                .replace('】', ' ');
        normalized = normalized.replaceAll("[\\s\\p{Punct}]+", " ").trim();
        return StrUtil.emptyToNull(normalized);
    }

    /**
     * 处理truncate摘要方法逻辑。
     */
    private String truncateSummary(String text) {
        String normalized = StrUtil.trim(text);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        return truncateText(normalized, SUMMARY_LIMIT);
    }

    /**
     * 处理truncateText方法逻辑。
     */
    private String truncateText(String text, int maxLength) {
        if (StrUtil.isBlank(text) || maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /**
     * 处理firstNonNull方法逻辑。
     */
    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 处理firstNonBlank方法逻辑。
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return null;
    }
}
