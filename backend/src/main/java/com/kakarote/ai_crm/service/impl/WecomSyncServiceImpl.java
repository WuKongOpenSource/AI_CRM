package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import cn.hutool.core.util.IdUtil;
import com.kakarote.ai_crm.entity.BO.WecomSyncRunBO;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.entity.PO.ManagerDept;
import com.kakarote.ai_crm.entity.PO.ManagerUser;
import com.kakarote.ai_crm.entity.PO.WecomConversation;
import com.kakarote.ai_crm.entity.PO.WecomCorpConfig;
import com.kakarote.ai_crm.entity.PO.WecomEmployee;
import com.kakarote.ai_crm.entity.PO.WecomExternalCustomer;
import com.kakarote.ai_crm.entity.PO.WecomExternalCustomerFollow;
import com.kakarote.ai_crm.entity.PO.WecomGroupChat;
import com.kakarote.ai_crm.entity.PO.WecomMessage;
import com.kakarote.ai_crm.entity.PO.WecomSyncCursor;
import com.kakarote.ai_crm.entity.PO.WecomSyncLog;
import com.kakarote.ai_crm.entity.VO.WecomSyncStatusVO;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.mapper.ManageUserMapper;
import com.kakarote.ai_crm.mapper.ManagerDeptMapper;
import com.kakarote.ai_crm.mapper.WecomConversationMapper;
import com.kakarote.ai_crm.mapper.WecomEmployeeMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerFollowMapper;
import com.kakarote.ai_crm.mapper.WecomExternalCustomerMapper;
import com.kakarote.ai_crm.mapper.WecomGroupChatMapper;
import com.kakarote.ai_crm.mapper.WecomMessageMapper;
import com.kakarote.ai_crm.mapper.WecomSyncCursorMapper;
import com.kakarote.ai_crm.mapper.WecomSyncLogMapper;
import com.kakarote.ai_crm.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Service
public class WecomSyncServiceImpl {

    private static final String SYNC_STATUS_SUCCESS = "success";
    private static final String SYNC_STATUS_FAILED = "failed";
    private static final String CURSOR_ARCHIVE = "archive";
    private static final String CURSOR_KEY_MAIN = "main";
    private static final String CRM_CUSTOMER_STAGE_LEAD = "lead";
    private static final String CRM_CUSTOMER_SOURCE_WECOM = "企业微信";
    private static final String WECOM_BIND_STATUS_BOUND = "BOUND";
    private static final String MATCH_STATUS_MATCHED = "MATCHED";
    private static final String MATCH_STATUS_UNMATCHED_NOT_VISIBLE = "UNMATCHED_NOT_VISIBLE";
    private static final String MATCH_STATUS_UNMATCHED_API_ERROR = "UNMATCHED_API_ERROR";

    @Autowired
    private WecomTokenService tokenService;

    @Autowired
    private WecomApiClient apiClient;

    @Autowired
    private WecomAgencyDevService agencyDevService;

    @Autowired
    private WecomFinanceArchiveGateway archiveGateway;

    @Autowired
    private WecomMessageNormalizeService messageNormalizeService;

    @Autowired
    private WecomEmployeeMapper employeeMapper;

    @Autowired
    private WecomExternalCustomerMapper externalCustomerMapper;

    @Autowired
    private WecomExternalCustomerFollowMapper externalCustomerFollowMapper;

    @Autowired
    private WecomConversationMapper conversationMapper;

    @Autowired
    private WecomGroupChatMapper groupChatMapper;

    @Autowired
    private WecomMessageMapper messageMapper;

    @Autowired
    private WecomSyncCursorMapper cursorMapper;

    @Autowired
    private WecomSyncLogMapper syncLogMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ManagerDeptMapper deptMapper;

    @Autowired
    private ManageUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public WecomSyncStatusVO runSync(WecomCorpConfig config, WecomSyncRunBO runBO) {
        WecomSyncRunBO actualRunBO = runBO == null ? new WecomSyncRunBO() : runBO;
        WecomSyncLog syncLog = new WecomSyncLog();
        syncLog.setCorpId(config.getCorpId());
        syncLog.setSyncType("manual");
        syncLog.setStatus("running");
        syncLog.setFetchedCount(0);
        syncLog.setSavedCount(0);
        syncLog.setFailedCount(0);
        syncLog.setStartedAt(new Date());
        syncLogMapper.insert(syncLog);
        log.debug("WeCom sync started: syncLogId={}, corpId={}, syncEmployees={}, syncCustomers={}, syncConversations={}, archiveLimit={}",
                syncLog.getId(), config.getCorpId(), actualRunBO.getSyncEmployees(), actualRunBO.getSyncCustomers(),
                actualRunBO.getSyncConversations(), actualRunBO.getArchiveLimit());

        int fetched = 0;
        int saved = 0;
        int failed = 0;
        StringBuilder errorMessage = new StringBuilder();
        try {
            if (Boolean.TRUE.equals(actualRunBO.getSyncEmployees())) {
                log.debug("WeCom organization sync start: syncLogId={}, corpId={}", syncLog.getId(), config.getCorpId());
                int orgSaved = syncOrganization(config);
                log.debug("WeCom organization sync done: syncLogId={}, corpId={}, saved={}",
                        syncLog.getId(), config.getCorpId(), orgSaved);
                fetched += orgSaved;
                saved += orgSaved;
            }
            if (Boolean.TRUE.equals(actualRunBO.getSyncCustomers())) {
                log.debug("WeCom customer sync start: syncLogId={}, corpId={}", syncLog.getId(), config.getCorpId());
                String token = tokenService.fetchContactAccessToken(config);
                List<String> followUsers = apiClient.listFollowUsers(token);
                log.debug("WeCom customer follow users fetched: syncLogId={}, corpId={}, followUserCount={}",
                        syncLog.getId(), config.getCorpId(), followUsers == null ? 0 : followUsers.size());
                CustomerSyncResult customerResult = syncCustomersForUsers(config, token, followUsers, resolveSyncOwnerId(), false);
                log.debug("WeCom customer sync done: syncLogId={}, corpId={}, fetched={}, saved={}, failed={}, error={}",
                        syncLog.getId(), config.getCorpId(), customerResult.fetched(), customerResult.saved(),
                        customerResult.failed(), StrUtil.maxLength(customerResult.errorMessage(), 500));
                fetched += customerResult.fetched();
                saved += customerResult.saved();
                failed += customerResult.failed();
                appendError(errorMessage, customerResult.errorMessage());
            }
            if (Boolean.TRUE.equals(actualRunBO.getSyncConversations())) {
                log.debug("WeCom archive sync start: syncLogId={}, corpId={}, archiveLimit={}",
                        syncLog.getId(), config.getCorpId(), actualRunBO.getArchiveLimit());
                ArchiveSyncResult archiveResult = syncArchiveMessages(config, actualRunBO.getArchiveLimit());
                log.debug("WeCom archive sync done: syncLogId={}, corpId={}, fetched={}, saved={}",
                        syncLog.getId(), config.getCorpId(), archiveResult.fetched(), archiveResult.saved());
                fetched += archiveResult.fetched();
                saved += archiveResult.saved();
            }
            syncLog.setStatus(SYNC_STATUS_SUCCESS);
        } catch (Exception e) {
            failed++;
            syncLog.setStatus(SYNC_STATUS_FAILED);
            appendError(errorMessage, e.getMessage());
            log.debug("WeCom sync failed: syncLogId={}, corpId={}, error={}",
                    syncLog.getId(), config.getCorpId(), e.getMessage(), e);
        } finally {
            if (!errorMessage.isEmpty()) {
                syncLog.setErrorMessage(errorMessage.toString());
            }
            syncLog.setFetchedCount(fetched);
            syncLog.setSavedCount(saved);
            syncLog.setFailedCount(failed);
            syncLog.setFinishedAt(new Date());
            syncLogMapper.updateById(syncLog);
            log.debug("WeCom sync finished: syncLogId={}, corpId={}, status={}, fetched={}, saved={}, failed={}, error={}",
                    syncLog.getId(), config.getCorpId(), syncLog.getStatus(), fetched, saved, failed,
                    StrUtil.maxLength(syncLog.getErrorMessage(), 500));
        }

        WecomSyncStatusVO status = new WecomSyncStatusVO();
        status.setCorpId(config.getCorpId());
        status.setLastSyncTime(syncLog.getFinishedAt());
        status.setLastSyncStatus(syncLog.getStatus());
        status.setLastSyncError(syncLog.getErrorMessage());
        status.setFetchedCount(fetched);
        status.setSavedCount(saved);
        status.setFailedCount(failed);
        return status;
    }

    public int syncVisibleCustomers(WecomCorpConfig config, String employeeUserId, Long ownerId) {
        return syncVisibleCustomersWithResult(config, employeeUserId, ownerId).saved();
    }

    public CustomerSyncResult syncVisibleCustomersWithResult(WecomCorpConfig config, String employeeUserId, Long ownerId) {
        if (StrUtil.isBlank(employeeUserId) || ownerId == null) {
            log.debug("WeCom visible customer sync skipped: corpId={}, employeeUserId={}, ownerId={}",
                    config == null ? null : config.getCorpId(), employeeUserId, ownerId);
            return new CustomerSyncResult(0, 0, 0, null);
        }
        log.debug("WeCom visible customer sync start: corpId={}, employeeUserId={}, ownerId={}",
                config.getCorpId(), employeeUserId, ownerId);
        String token = tokenService.fetchContactAccessToken(config);
        CustomerSyncResult result = syncCustomersForUsers(config, token, List.of(employeeUserId), ownerId, true);
        log.debug("WeCom visible customer sync done: corpId={}, employeeUserId={}, ownerId={}, fetched={}, saved={}, failed={}, error={}",
                config.getCorpId(), employeeUserId, ownerId, result.fetched(), result.saved(), result.failed(),
                StrUtil.maxLength(result.errorMessage(), 500));
        return result;
    }

    private CustomerSyncResult syncCustomersForUsers(WecomCorpConfig config,
                                                     String token,
                                                     List<String> followUsers,
                                                     Long ownerId,
                                                     boolean localEmployeeUserIds) {
        int fetched = 0;
        int saved = 0;
        int failed = 0;
        StringBuilder errors = new StringBuilder();
        List<FollowUserLookup> followUserLookups = resolveCustomerFollowUserLookups(config, token, followUsers, localEmployeeUserIds);
        log.debug("WeCom customer follow users resolved: corpId={}, inputCount={}, lookupCount={}, localEmployeeUserIds={}",
                config.getCorpId(), followUsers == null ? 0 : followUsers.size(), followUserLookups.size(), localEmployeeUserIds);
        Map<String, String> followUserAliases = new LinkedHashMap<>();
        for (FollowUserLookup followUser : followUserLookups) {
            followUserAliases.put(followUser.queryUserId(), followUser.employeeUserId());
        }
        for (FollowUserLookup followUser : followUserLookups) {
            List<String> externalUserIds;
            try {
                externalUserIds = apiClient.listExternalUserIds(token, followUser.queryUserId());
                if (externalUserIds == null) {
                    externalUserIds = List.of();
                }
                log.debug("WeCom external user ids fetched: corpId={}, queryUserId={}, employeeUserId={}, externalUserCount={}",
                        config.getCorpId(), followUser.queryUserId(), followUser.employeeUserId(),
                        externalUserIds.size());
            } catch (Exception e) {
                failed++;
                appendError(errors, "follow user " + followUser.employeeUserId() + ": " + e.getMessage());
                log.debug("WeCom external user id fetch failed: corpId={}, queryUserId={}, employeeUserId={}, error={}",
                        config.getCorpId(), followUser.queryUserId(), followUser.employeeUserId(), e.getMessage(), e);
                continue;
            }
            fetched += externalUserIds.size();
            for (String externalUserId : externalUserIds) {
                try {
                    int savedCustomer = saveExternalCustomer(config, apiClient.getExternalCustomer(token, externalUserId), ownerId, followUserAliases);
                    saved += savedCustomer;
                    log.debug("WeCom external customer synced: corpId={}, externalUserId={}, ownerId={}, saved={}",
                            config.getCorpId(), externalUserId, ownerId, savedCustomer);
                } catch (Exception e) {
                    failed++;
                    appendError(errors, "external customer " + externalUserId + ": " + e.getMessage());
                    log.debug("WeCom external customer sync failed: corpId={}, externalUserId={}, ownerId={}, error={}",
                            config.getCorpId(), externalUserId, ownerId, e.getMessage(), e);
                }
            }
        }
        return new CustomerSyncResult(fetched, saved, failed, errors.isEmpty() ? null : errors.toString());
    }

    private List<FollowUserLookup> resolveCustomerFollowUserLookups(WecomCorpConfig config,
                                                                    String token,
                                                                    List<String> followUsers,
                                                                    boolean localEmployeeUserIds) {
        List<String> normalizedFollowUsers = normalizeUserIds(followUsers);
        if (normalizedFollowUsers.isEmpty()) {
            return List.of();
        }
        if (!isAgencyDevConfig(config)) {
            return normalizedFollowUsers.stream()
                    .map(userId -> new FollowUserLookup(userId, userId))
                    .toList();
        }
        Map<String, String> clearToOpenUserIds = resolveAgencyOpenUserIds(config, token,
                localEmployeeUserIds ? normalizedFollowUsers : List.of());
        Map<String, String> openToClearUserIds = new LinkedHashMap<>();
        clearToOpenUserIds.forEach((clearUserId, openUserId) -> {
            if (StrUtil.isNotBlank(openUserId)) {
                openToClearUserIds.put(openUserId, clearUserId);
            }
        });
        List<FollowUserLookup> result = new ArrayList<>();
        for (String followUser : normalizedFollowUsers) {
            String openUserId = clearToOpenUserIds.get(followUser);
            if (StrUtil.isNotBlank(openUserId)) {
                result.add(new FollowUserLookup(openUserId, followUser));
                continue;
            }
            result.add(new FollowUserLookup(followUser,
                    StrUtil.blankToDefault(openToClearUserIds.get(followUser), followUser)));
        }
        return result;
    }

    private Map<String, String> resolveAgencyOpenUserIds(WecomCorpConfig config,
                                                         String token,
                                                         List<String> extraLocalEmployeeUserIds) {
        List<String> candidates = new ArrayList<>(loadKnownEmployeeUserIds(config));
        if (extraLocalEmployeeUserIds != null) {
            candidates.addAll(extraLocalEmployeeUserIds);
        }
        List<String> candidateUserIds = normalizeUserIds(candidates);
        if (candidateUserIds.isEmpty()) {
            return Map.of();
        }
        try {
            return apiClient.convertUserIdsToOpenUserIds(token, candidateUserIds);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private List<String> loadKnownEmployeeUserIds(WecomCorpConfig config) {
        if (config == null || StrUtil.isBlank(config.getCorpId())) {
            return List.of();
        }
        List<WecomEmployee> employees = employeeMapper.selectList(Wrappers.<WecomEmployee>lambdaQuery()
                .eq(WecomEmployee::getCorpId, config.getCorpId())
                .isNotNull(WecomEmployee::getUserId));
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }
        return employees.stream()
                .map(WecomEmployee::getUserId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private List<String> normalizeUserIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String userId : userIds) {
            if (StrUtil.isNotBlank(userId)) {
                normalized.add(userId);
            }
        }
        return List.copyOf(normalized);
    }

    private boolean isAgencyDevConfig(WecomCorpConfig config) {
        return agencyDevService != null && agencyDevService.owns(config);
    }

    private void appendError(StringBuilder errors, String message) {
        if (errors == null || StrUtil.isBlank(message)) {
            return;
        }
        if (!errors.isEmpty()) {
            errors.append("; ");
        }
        errors.append(message);
    }

    public int syncOrganization(WecomCorpConfig config) {
        log.debug("WeCom organization sync preparing: corpId={}", config.getCorpId());
        String token = tokenService.fetchAppAccessToken(config);
        List<JSONObject> departments = apiClient.listDepartments(token);
        log.debug("WeCom departments fetched: corpId={}, departmentCount={}",
                config.getCorpId(), departments == null ? 0 : departments.size());
        Map<Long, Long> deptIdMap = new LinkedHashMap<>();
        int saved = 0;
        int userSaved = 0;
        Date syncedAt = new Date();
        List<JSONObject> sortedDepartments = departments.stream()
                .filter(item -> item.getLong("id") != null)
                .sorted(Comparator.comparing((JSONObject item) -> item.getLongValue("parentid"))
                        .thenComparing(item -> item.getLongValue("id")))
                .toList();
        for (JSONObject department : sortedDepartments) {
            saved += saveDepartment(config, department, deptIdMap, syncedAt);
        }

        Set<String> syncedUsers = new HashSet<>();
        for (JSONObject department : sortedDepartments) {
            Long departmentId = department.getLong("id");
            for (JSONObject user : apiClient.listDepartmentUsers(token, departmentId)) {
                String userId = user.getString("userid");
                if (StrUtil.isBlank(userId) || !syncedUsers.add(userId)) {
                    continue;
                }
                int savedUser = saveSystemUserAndEmployee(config, user, deptIdMap, syncedAt);
                saved += savedUser;
                userSaved += savedUser;
            }
        }
        log.debug("WeCom organization sync finished: corpId={}, departmentSaved={}, userSaved={}, saved={}",
                config.getCorpId(), saved - userSaved, userSaved, saved);
        return saved;
    }

    private int saveDepartment(WecomCorpConfig config, JSONObject department, Map<Long, Long> deptIdMap, Date syncedAt) {
        Long wecomDeptId = department.getLong("id");
        if (wecomDeptId == null) {
            return 0;
        }
        Long wecomParentDeptId = department.getLong("parentid");
        ManagerDept dept = deptMapper.selectOne(Wrappers.<ManagerDept>lambdaQuery()
                .eq(ManagerDept::getWecomCorpId, config.getCorpId())
                .eq(ManagerDept::getWecomDeptId, wecomDeptId)
                .last("LIMIT 1"));
        if (dept == null) {
            dept = new ManagerDept();
            dept.setCreateTime(syncedAt);
        }
        dept.setDeptName(StrUtil.blankToDefault(department.getString("name"), "WeCom Dept " + wecomDeptId));
        dept.setParentId(resolveParentDeptId(wecomParentDeptId, deptIdMap));
        dept.setSortOrder(department.getInteger("order"));
        dept.setWecomCorpId(config.getCorpId());
        dept.setWecomDeptId(wecomDeptId);
        dept.setWecomParentDeptId(wecomParentDeptId);
        dept.setWecomSyncedAt(syncedAt);
        if (dept.getDeptId() == null) {
            deptMapper.insert(dept);
        } else {
            deptMapper.updateById(dept);
        }
        deptIdMap.put(wecomDeptId, dept.getDeptId());
        return 1;
    }

    private int saveSystemUserAndEmployee(WecomCorpConfig config,
                                          JSONObject user,
                                          Map<Long, Long> deptIdMap,
                                          Date syncedAt) {
        String wecomUserId = user.getString("userid");
        if (StrUtil.isBlank(wecomUserId)) {
            return 0;
        }
        ManagerUser managerUser = findSystemUser(config.getCorpId(), wecomUserId, user);
        if (managerUser == null) {
            managerUser = new ManagerUser();
            managerUser.setUsername(resolveSystemUsername(config, user));
            managerUser.setPassword(encodeGeneratedPassword());
            managerUser.setCreateTime(syncedAt);
        }
        managerUser.setRealname(StrUtil.blankToDefault(user.getString("name"), wecomUserId));
        managerUser.setMobile(StrUtil.blankToDefault(user.getString("mobile"), managerUser.getMobile()));
        managerUser.setEmail(StrUtil.blankToDefault(user.getString("email"), managerUser.getEmail()));
        managerUser.setImg(StrUtil.blankToDefault(user.getString("avatar"), managerUser.getImg()));
        managerUser.setPost(StrUtil.blankToDefault(user.getString("position"), managerUser.getPost()));
        managerUser.setStatus(resolveManagerUserStatus(user.getInteger("status")));
        managerUser.setDeptId(resolveUserDeptId(user.getJSONArray("department"), deptIdMap));
        managerUser.setWecomCorpId(config.getCorpId());
        managerUser.setWecomUserId(wecomUserId);
        managerUser.setWecomSyncedAt(syncedAt);
        if (managerUser.getUserId() == null) {
            userMapper.insert(managerUser);
        } else {
            userMapper.updateById(managerUser);
        }
        saveWecomEmployee(config, user, managerUser.getUserId(), syncedAt);
        return 1;
    }

    private Long resolveParentDeptId(Long wecomParentDeptId, Map<Long, Long> deptIdMap) {
        if (wecomParentDeptId == null || wecomParentDeptId <= 0) {
            return 0L;
        }
        return deptIdMap.getOrDefault(wecomParentDeptId, 0L);
    }

    private Long resolveUserDeptId(JSONArray departments, Map<Long, Long> deptIdMap) {
        if (departments == null || departments.isEmpty()) {
            return null;
        }
        Long firstDeptId = departments.getLong(0);
        return firstDeptId == null ? null : deptIdMap.get(firstDeptId);
    }

    private ManagerUser findSystemUser(String corpId, String wecomUserId, JSONObject user) {
        WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                .eq(WecomEmployee::getCorpId, corpId)
                .eq(WecomEmployee::getUserId, wecomUserId)
                .isNotNull(WecomEmployee::getCrmUserId)
                .last("LIMIT 1"));
        if (employee != null && employee.getCrmUserId() != null) {
            ManagerUser mappedUser = userMapper.selectById(employee.getCrmUserId());
            if (mappedUser != null) {
                return mappedUser;
            }
        }
        ManagerUser managerUser = userMapper.selectOne(Wrappers.<ManagerUser>lambdaQuery()
                .eq(ManagerUser::getWecomCorpId, corpId)
                .eq(ManagerUser::getWecomUserId, wecomUserId)
                .last("LIMIT 1"));
        if (managerUser != null) {
            return managerUser;
        }
        String email = user.getString("email");
        if (StrUtil.isNotBlank(email)) {
            managerUser = userMapper.selectOne(Wrappers.<ManagerUser>lambdaQuery()
                    .eq(ManagerUser::getEmail, email)
                    .last("LIMIT 1"));
            if (managerUser != null) {
                return managerUser;
            }
        }
        String mobile = user.getString("mobile");
        if (StrUtil.isNotBlank(mobile)) {
            return userMapper.selectOne(Wrappers.<ManagerUser>lambdaQuery()
                    .eq(ManagerUser::getMobile, mobile)
                    .last("LIMIT 1"));
        }
        return null;
    }

    private String resolveSystemUsername(WecomCorpConfig config, JSONObject user) {
        String email = user.getString("email");
        if (StrUtil.isNotBlank(email)) {
            return StrUtil.trim(email).toLowerCase();
        }
        return syntheticWecomEmail(config.getCorpId(), user.getString("userid"));
    }

    private String encodeGeneratedPassword() {
        String password = "Wecom" + IdUtil.fastSimpleUUID().substring(0, 12);
        return passwordEncoder == null ? password : passwordEncoder.encode(password);
    }

    private int resolveManagerUserStatus(Integer wecomStatus) {
        return wecomStatus == null || wecomStatus == 1 ? 1 : 0;
    }

    private void saveWecomEmployee(WecomCorpConfig config, JSONObject user, Long crmUserId, Date syncedAt) {
        String userId = user.getString("userid");
        WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                .eq(WecomEmployee::getCorpId, config.getCorpId())
                .eq(WecomEmployee::getUserId, userId)
                .last("LIMIT 1"));
        if (employee == null) {
            employee = new WecomEmployee();
            employee.setCorpId(config.getCorpId());
            employee.setUserId(userId);
        }
        employee.setCrmUserId(crmUserId);
        employee.setName(StrUtil.blankToDefault(user.getString("name"), userId));
        employee.setAvatar(user.getString("avatar"));
        employee.setDepartmentList(jsonArrayToString(user.getJSONArray("department")));
        employee.setMobile(StrUtil.blankToDefault(user.getString("mobile"), employee.getMobile()));
        employee.setEmail(StrUtil.blankToDefault(user.getString("email"), employee.getEmail()));
        employee.setPosition(StrUtil.blankToDefault(user.getString("position"), employee.getPosition()));
        employee.setStatus(user.getInteger("status"));
        employee.setSyncedAt(syncedAt);
        if (employee.getId() == null) {
            employeeMapper.insert(employee);
        } else {
            employeeMapper.updateById(employee);
        }
    }

    private int saveEmployees(WecomCorpConfig config, List<JSONObject> employees) {
        int saved = 0;
        for (JSONObject user : employees) {
            String userId = user.getString("userid");
            if (StrUtil.isBlank(userId)) {
                continue;
            }
            WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                    .eq(WecomEmployee::getCorpId, config.getCorpId())
                    .eq(WecomEmployee::getUserId, userId)
                    .last("LIMIT 1"));
            if (employee == null) {
                employee = new WecomEmployee();
                employee.setCorpId(config.getCorpId());
                employee.setUserId(userId);
            }
            employee.setName(StrUtil.blankToDefault(user.getString("name"), userId));
            employee.setAvatar(user.getString("avatar"));
            employee.setDepartmentList(jsonArrayToString(user.getJSONArray("department")));
            employee.setMobile(StrUtil.blankToDefault(user.getString("mobile"), employee.getMobile()));
            employee.setEmail(StrUtil.blankToDefault(user.getString("email"), employee.getEmail()));
            employee.setPosition(StrUtil.blankToDefault(user.getString("position"), employee.getPosition()));
            employee.setStatus(user.getInteger("status"));
            employee.setSyncedAt(new Date());
            if (employee.getId() == null) {
                employeeMapper.insert(employee);
            } else {
                employeeMapper.updateById(employee);
            }
            saved++;
        }
        return saved;
    }

    private int saveExternalCustomer(WecomCorpConfig config, JSONObject response, Long ownerId) {
        return saveExternalCustomer(config, response, ownerId, Map.of());
    }

    private int saveExternalCustomer(WecomCorpConfig config, JSONObject response, Long ownerId,
                                     Map<String, String> followUserAliases) {
        return saveExternalCustomerRecord(config, response, ownerId, followUserAliases) == null ? 0 : 1;
    }

    private WecomExternalCustomer saveExternalCustomerRecord(WecomCorpConfig config,
                                                             JSONObject response,
                                                             Long ownerId,
                                                             Map<String, String> followUserAliases) {
        JSONObject customerJson = response.getJSONObject("external_contact");
        if (customerJson == null) {
            return null;
        }
        String externalUserId = customerJson.getString("external_userid");
        if (StrUtil.isBlank(externalUserId)) {
            return null;
        }
        WecomExternalCustomer customer = externalCustomerMapper.selectOne(Wrappers.<WecomExternalCustomer>lambdaQuery()
                .eq(WecomExternalCustomer::getCorpId, config.getCorpId())
                .eq(WecomExternalCustomer::getExternalUserId, externalUserId)
                .last("LIMIT 1"));
        Date syncedAt = new Date();
        Long crmCustomerId = saveCrmCustomer(config, customerJson, externalUserId, customer, syncedAt, ownerId);
        if (customer == null) {
            customer = new WecomExternalCustomer();
            customer.setCorpId(config.getCorpId());
            customer.setExternalUserId(externalUserId);
        }
        customer.setBindStatus(WECOM_BIND_STATUS_BOUND);
        customer.setCustomerId(crmCustomerId);
        customer.setName(StrUtil.blankToDefault(customerJson.getString("name"), externalUserId));
        customer.setAvatar(customerJson.getString("avatar"));
        customer.setType(customerJson.getInteger("type"));
        customer.setGender(customerJson.getInteger("gender"));
        customer.setUnionId(customerJson.getString("unionid"));
        customer.setPosition(customerJson.getString("position"));
        customer.setCorpName(customerJson.getString("corp_name"));
        customer.setCorpFullName(customerJson.getString("corp_full_name"));
        customer.setExternalProfile(response.toJSONString());
        customer.setSyncedAt(syncedAt);
        if (customer.getId() == null) {
            externalCustomerMapper.insert(customer);
        } else {
            externalCustomerMapper.updateById(customer);
        }
        saveExternalCustomerFollows(config, customer, response.getJSONArray("follow_user"), syncedAt, followUserAliases);
        return customer;
    }

    private void saveExternalCustomerFollows(WecomCorpConfig config,
                                             WecomExternalCustomer customer,
                                             JSONArray followUsers,
                                             Date syncedAt,
                                             Map<String, String> followUserAliases) {
        if (followUsers == null || followUsers.isEmpty() || customer == null || customer.getId() == null) {
            return;
        }
        for (Object item : followUsers) {
            JSONObject followJson = (JSONObject) item;
            String sourceEmployeeUserId = followJson.getString("userid");
            String employeeUserId = StrUtil.blankToDefault(
                    followUserAliases == null ? null : followUserAliases.get(sourceEmployeeUserId),
                    sourceEmployeeUserId);
            if (StrUtil.isBlank(employeeUserId)) {
                continue;
            }
            WecomExternalCustomerFollow follow = externalCustomerFollowMapper.selectOne(
                    Wrappers.<WecomExternalCustomerFollow>lambdaQuery()
                            .eq(WecomExternalCustomerFollow::getCorpId, config.getCorpId())
                            .eq(WecomExternalCustomerFollow::getExternalUserId, customer.getExternalUserId())
                            .eq(WecomExternalCustomerFollow::getEmployeeUserId, employeeUserId)
                            .last("LIMIT 1"));
            if (follow == null) {
                follow = new WecomExternalCustomerFollow();
                follow.setCorpId(config.getCorpId());
                follow.setExternalCustomerId(customer.getId());
                follow.setExternalUserId(customer.getExternalUserId());
                follow.setEmployeeUserId(employeeUserId);
            }
            WecomEmployee employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                    .eq(WecomEmployee::getCorpId, config.getCorpId())
                    .eq(WecomEmployee::getUserId, employeeUserId)
                    .last("LIMIT 1"));
            follow.setExternalCustomerId(customer.getId());
            follow.setEmployeeId(employee == null ? null : employee.getId());
            follow.setRemark(followJson.getString("remark"));
            follow.setDescription(followJson.getString("description"));
            follow.setAddWay(followJson.getInteger("add_way"));
            follow.setState(followJson.getString("state"));
            follow.setTagsJson(jsonArrayToString(followJson.getJSONArray("tags")));
            follow.setRelationCreateTime(resolveUnixSeconds(followJson.getLong("createtime")));
            follow.setStatus(1);
            follow.setSyncedAt(syncedAt);
            if (follow.getId() == null) {
                externalCustomerFollowMapper.insert(follow);
            } else {
                externalCustomerFollowMapper.updateById(follow);
            }
        }
    }

    private Long saveCrmCustomer(WecomCorpConfig config, JSONObject customerJson, String externalUserId,
                                 WecomExternalCustomer externalCustomer, Date syncedAt, Long ownerId) {
        Customer customer = findExistingCrmCustomer(config, externalUserId, externalCustomer);
        if (customer == null) {
            customer = new Customer();
            customer.setStage(CRM_CUSTOMER_STAGE_LEAD);
            customer.setOwnerId(ownerId);
            customer.setStatus(1);
            customer.setSource(CRM_CUSTOMER_SOURCE_WECOM);
            customer.setContactCount(1);
        } else {
            if (StrUtil.isBlank(customer.getStage())) {
                customer.setStage(CRM_CUSTOMER_STAGE_LEAD);
            }
            if (ownerId != null) {
                customer.setOwnerId(ownerId);
            } else if (customer.getOwnerId() == null) {
                customer.setOwnerId(resolveSyncOwnerId());
            }
            if (customer.getStatus() == null) {
                customer.setStatus(1);
            }
            if (customer.getContactCount() == null || customer.getContactCount() <= 0) {
                customer.setContactCount(1);
            }
        }
        customer.setCompanyName(resolveCrmCustomerName(customerJson, externalUserId));
        customer.setPrimaryContactName(StrUtil.blankToDefault(customerJson.getString("name"), externalUserId));
        customer.setPrimaryContactPosition(customerJson.getString("position"));
        customer.setSource(CRM_CUSTOMER_SOURCE_WECOM);
        customer.setWecomCustomer(true);
        customer.setWecomCorpId(config.getCorpId());
        customer.setWecomExternalUserId(externalUserId);
        customer.setWecomSyncedAt(syncedAt);
        if (customer.getCustomerId() == null) {
            customerMapper.insert(customer);
        } else {
            customerMapper.updateById(customer);
        }
        return customer.getCustomerId();
    }

    private Customer findExistingCrmCustomer(WecomCorpConfig config, String externalUserId,
                                             WecomExternalCustomer externalCustomer) {
        if (externalCustomer != null && externalCustomer.getCustomerId() != null) {
            Customer boundCustomer = customerMapper.selectById(externalCustomer.getCustomerId());
            if (boundCustomer != null) {
                return boundCustomer;
            }
        }
        return customerMapper.selectOne(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getWecomCustomer, true)
                .eq(Customer::getWecomCorpId, config.getCorpId())
                .eq(Customer::getWecomExternalUserId, externalUserId)
                .last("LIMIT 1"));
    }

    private String resolveCrmCustomerName(JSONObject customerJson, String externalUserId) {
        String corpFullName = customerJson.getString("corp_full_name");
        if (StrUtil.isNotBlank(corpFullName)) {
            return corpFullName;
        }
        String corpName = customerJson.getString("corp_name");
        if (StrUtil.isNotBlank(corpName)) {
            return corpName;
        }
        return StrUtil.blankToDefault(customerJson.getString("name"), externalUserId);
    }

    private Date resolveUnixSeconds(Long value) {
        return value == null || value <= 0 ? null : new Date(value * 1000);
    }

    private Long resolveSyncOwnerId() {
        Long userId = UserUtil.getUserIdOrNull();
        return userId == null ? UserUtil.getSuperUserId() : userId;
    }

    private String syntheticWecomEmail(String corpId, String userId) {
        String safeCorp = sanitizeEmailPart(StrUtil.blankToDefault(corpId, "corp"));
        String safeUser = sanitizeEmailPart(StrUtil.blankToDefault(userId, "user"));
        return "wecom." + safeUser + "." + safeCorp + "@external.wecom.local";
    }

    private String sanitizeEmailPart(String value) {
        String sanitized = value.toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        return StrUtil.blankToDefault(sanitized, "unknown");
    }

    /**
     * 增量拉取会话存档（事件推送 / 打开外部联系人时触发）。从游标连续拉取，最多 maxPages 页，
     * 直到某页返回不足一整页（已追平）。返回本次新入库的消息数。
     * 注意：若调用方不在请求上下文（如回调异步线程），必须先 TenantContextHolder.setTenantId(config.getTenantId())。
     * 刻意不加 @Transactional：每页（getchatdata 网络/原生调用 + 落库）各自自动提交，避免在慢速拉取期间长时间占用数据库连接；
     * 部分失败靠 msgId 去重幂等重试，不会重复入库。
     */
    public int drainArchive(WecomCorpConfig config, int maxPages) {
        if (config == null || !Boolean.TRUE.equals(config.getArchiveEnabled())) {
            log.debug("WeCom archive drain skipped: corpId={}, archiveEnabled={}",
                    config == null ? null : config.getCorpId(),
                    config == null ? null : config.getArchiveEnabled());
            return 0;
        }
        int limit = resolveArchiveLimit(null);
        int totalSaved = 0;
        int pages = Math.max(1, maxPages);
        log.debug("WeCom archive drain start: corpId={}, maxPages={}, limit={}",
                config.getCorpId(), pages, limit);
        for (int i = 0; i < pages; i++) {
            ArchiveSyncResult result = syncArchiveMessages(config, limit);
            totalSaved += result.saved();
            log.debug("WeCom archive drain page done: corpId={}, page={}/{}, fetched={}, saved={}, totalSaved={}",
                    config.getCorpId(), i + 1, pages, result.fetched(), result.saved(), totalSaved);
            if (result.fetched() < limit) {
                break;
            }
        }
        log.debug("WeCom archive drain finished: corpId={}, totalSaved={}", config.getCorpId(), totalSaved);
        return totalSaved;
    }

    private ArchiveSyncResult syncArchiveMessages(WecomCorpConfig config, Integer archiveLimit) {
        if (!Boolean.TRUE.equals(config.getArchiveEnabled())) {
            log.debug("WeCom archive sync skipped: corpId={}, archiveEnabled={}",
                    config.getCorpId(), config.getArchiveEnabled());
            return new ArchiveSyncResult(0, 0);
        }
        WecomSyncCursor cursor = cursorMapper.selectOne(Wrappers.<WecomSyncCursor>lambdaQuery()
                .eq(WecomSyncCursor::getCorpId, config.getCorpId())
                .eq(WecomSyncCursor::getCursorType, CURSOR_ARCHIVE)
                .eq(WecomSyncCursor::getCursorKey, CURSOR_KEY_MAIN)
                .last("LIMIT 1"));
        long startSeq = cursor == null || cursor.getSeq() == null ? 0L : cursor.getSeq();
        int limit = resolveArchiveLimit(archiveLimit);
        log.debug("WeCom archive page fetch start: corpId={}, startSeq={}, limit={}",
                config.getCorpId(), startSeq, limit);
        List<JSONObject> messages = archiveGateway.fetchMessages(config, startSeq, limit);
        if (messages == null) {
            messages = List.of();
        }
        log.debug("WeCom archive page fetched: corpId={}, startSeq={}, fetched={}",
                config.getCorpId(), startSeq, messages.size());
        ArchiveExternalUserConversion externalUserConversion = convertArchiveExternalUserIds(config, messages);
        long maxSeq = startSeq;
        int saved = 0;
        int skipped = 0;
        int duplicated = 0;
        for (JSONObject raw : messages) {
            Long seq = raw.getLong("seq");
            if (seq != null) {
                maxSeq = Math.max(maxSeq, seq);
            }
            if (raw.getBooleanValue(WecomFinanceSdkClient.SKIP_MESSAGE_FIELD)) {
                skipped++;
                continue;
            }
            if (shouldSkipArchiveRecord(raw)) {
                skipped++;
                continue;
            }
            Long conversationId = ensureConversation(config, raw, externalUserConversion);
            WecomMessage message = messageNormalizeService.normalize(conversationId, raw);
            message.setCorpId(config.getCorpId());
            if (seq != null) {
                message.setSeq(seq);
            }
            if (StrUtil.isNotBlank(message.getMsgId()) && messageMapper.selectCount(Wrappers.<WecomMessage>lambdaQuery()
                    .eq(WecomMessage::getMsgId, message.getMsgId())) == 0) {
                messageMapper.insert(message);
                saved++;
            } else {
                duplicated++;
            }
        }
        boolean cursorUpdated = !Objects.equals(maxSeq, startSeq);
        if (cursorUpdated) {
            if (cursor == null) {
                cursor = new WecomSyncCursor();
                cursor.setCorpId(config.getCorpId());
                cursor.setCursorType(CURSOR_ARCHIVE);
                cursor.setCursorKey(CURSOR_KEY_MAIN);
                cursor.setSeq(maxSeq);
                cursorMapper.insert(cursor);
            } else {
                cursor.setSeq(maxSeq);
                cursorMapper.updateById(cursor);
            }
        }
        reconcileArchiveCustomerRelations(config);
        log.debug("WeCom archive page processed: corpId={}, startSeq={}, maxSeq={}, fetched={}, saved={}, skipped={}, duplicated={}, cursorUpdated={}, conversionError={}",
                config.getCorpId(), startSeq, maxSeq, messages.size(), saved, skipped, duplicated, cursorUpdated,
                StrUtil.maxLength(externalUserConversion.errorMessage(), 500));
        return new ArchiveSyncResult(messages.size(), saved);
    }

    private ArchiveExternalUserConversion convertArchiveExternalUserIds(WecomCorpConfig config, List<JSONObject> messages) {
        return convertArchiveExternalUserIdList(config, collectArchiveExternalUserIds(messages));
    }

    private List<String> collectArchiveExternalUserIds(List<JSONObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Set<String> externalUserIds = new LinkedHashSet<>();
        for (JSONObject raw : messages) {
            if (raw == null
                    || raw.getBooleanValue(WecomFinanceSdkClient.SKIP_MESSAGE_FIELD)
                    || shouldSkipArchiveRecord(raw)) {
                continue;
            }
            String externalUserId = resolveExternalUserId(raw.getString("from"), raw.getJSONArray("tolist"));
            if (StrUtil.isNotBlank(externalUserId)) {
                externalUserIds.add(externalUserId);
            }
        }
        return new ArrayList<>(externalUserIds);
    }

    private boolean shouldSkipArchiveRecord(JSONObject raw) {
        if (raw == null) {
            return true;
        }
        if ("switch".equalsIgnoreCase(raw.getString("action"))) {
            return true;
        }
        if (StrUtil.isBlank(raw.getString("msgtype"))) {
            return true;
        }
        String from = raw.getString("from");
        String roomId = raw.getString("roomid");
        JSONArray toList = raw.getJSONArray("tolist");
        return StrUtil.isBlank(from) && StrUtil.isBlank(roomId) && (toList == null || toList.isEmpty());
    }

    private Long ensureConversation(WecomCorpConfig config,
                                    JSONObject raw,
                                    ArchiveExternalUserConversion externalUserConversion) {
        String from = raw.getString("from");
        JSONArray toList = raw.getJSONArray("tolist");
        String firstTo = toList == null || toList.isEmpty() ? "" : toList.getString(0);
        String roomId = raw.getString("roomid");
        String archiveExternalUserId = resolveExternalUserId(from, toList);
        String externalUserId = resolveContactExternalUserId(archiveExternalUserId, externalUserConversion);
        String employeeUserId = resolveEmployeeUserId(from, toList);
        String conversationKey = resolveConversationKey(roomId, from, firstTo, employeeUserId, externalUserId);
        WecomConversation conversation = findConversationByChatId(config, conversationKey);
        if (conversation == null && StrUtil.isBlank(roomId) && StrUtil.isNotBlank(archiveExternalUserId)
                && !StrUtil.equals(archiveExternalUserId, externalUserId)) {
            String archiveConversationKey = resolveConversationKey(roomId, from, firstTo, employeeUserId, archiveExternalUserId);
            conversation = findConversationByChatId(config, archiveConversationKey);
        }
        if (conversation == null) {
            conversation = new WecomConversation();
            conversation.setCorpId(config.getCorpId());
            conversation.setConversationType(resolveConversationType(roomId, externalUserId));
            conversation.setChatId(conversationKey);
            conversation.setTitle(conversationKey);
            conversation.setMessageCount(0);
            conversationMapper.insert(conversation);
        }
        conversation.setConversationType(resolveConversationType(roomId, externalUserId));
        conversation.setEmployeeUserId(employeeUserId);
        conversation.setExternalUserId(externalUserId);
        conversation.setArchiveExternalUserId(archiveExternalUserId);
        applyConversationRelations(config, conversation, roomId, employeeUserId, externalUserId,
                externalUserConversion == null ? null : externalUserConversion.errorMessage(), from, toList);
        conversation.setLastMsgId(raw.getString("msgid"));
        conversation.setLastMsgTime(raw.getLong("msgtime") == null ? null : new Date(raw.getLong("msgtime")));
        conversation.setLastMsgPreview(messageNormalizeService.normalize(conversation.getId(), raw).getContentText());
        conversation.setMessageCount(conversation.getMessageCount() == null ? 1 : conversation.getMessageCount() + 1);
        conversationMapper.updateById(conversation);
        return conversation.getId();
    }

    private WecomConversation findConversationByChatId(WecomCorpConfig config, String chatId) {
        if (StrUtil.isBlank(chatId)) {
            return null;
        }
        return conversationMapper.selectOne(Wrappers.<WecomConversation>lambdaQuery()
                .eq(WecomConversation::getCorpId, config.getCorpId())
                .eq(WecomConversation::getChatId, chatId)
                .last("LIMIT 1"));
    }

    private String resolveConversationKey(String roomId,
                                          String from,
                                          String firstTo,
                                          String employeeUserId,
                                          String externalUserId) {
        if (StrUtil.isNotBlank(roomId)) {
            return roomId;
        }
        if (StrUtil.isNotBlank(externalUserId) && StrUtil.isNotBlank(employeeUserId)) {
            return externalUserId + ":" + employeeUserId;
        }
        if (StrUtil.isNotBlank(from) && StrUtil.isNotBlank(firstTo)) {
            return canonicalPairKey(from, firstTo);
        }
        return from + ":" + firstTo;
    }

    private String canonicalPairKey(String first, String second) {
        return first.compareTo(second) <= 0 ? first + ":" + second : second + ":" + first;
    }

    private void applyConversationRelations(WecomCorpConfig config,
                                            WecomConversation conversation,
                                            String roomId,
                                            String employeeUserId,
                                            String externalUserId,
                                            String externalUserConversionError,
                                            String from,
                                            JSONArray toList) {
        if (StrUtil.isNotBlank(roomId)) {
            applyGroupConversationRelations(config, conversation, roomId, from, toList);
            return;
        }
        WecomEmployee employee = null;
        if (StrUtil.isNotBlank(employeeUserId)) {
            employee = employeeMapper.selectOne(Wrappers.<WecomEmployee>lambdaQuery()
                    .eq(WecomEmployee::getCorpId, config.getCorpId())
                    .eq(WecomEmployee::getUserId, employeeUserId)
                    .last("LIMIT 1"));
            if (employee != null) {
                conversation.setEmployeeId(employee.getId());
                conversation.setOwnerUserId(employee.getCrmUserId());
            }
        }
        if (StrUtil.isNotBlank(externalUserId)) {
            conversation.setContactEmployeeUserId(resolveContactEmployeeUserId(config, employeeUserId));
            WecomExternalCustomer customer = externalCustomerMapper.selectOne(Wrappers.<WecomExternalCustomer>lambdaQuery()
                    .eq(WecomExternalCustomer::getCorpId, config.getCorpId())
                    .eq(WecomExternalCustomer::getExternalUserId, externalUserId)
                    .last("LIMIT 1"));
            if (customer != null) {
                applyMatchedExternalCustomer(conversation, customer);
            } else {
                Customer crmCustomer = customerMapper.selectOne(Wrappers.<Customer>lambdaQuery()
                        .eq(Customer::getWecomCustomer, true)
                        .eq(Customer::getWecomCorpId, config.getCorpId())
                        .eq(Customer::getWecomExternalUserId, externalUserId)
                        .last("LIMIT 1"));
                if (crmCustomer != null) {
                    conversation.setCustomerId(crmCustomer.getCustomerId());
                    applyConversationPeer(conversation, resolveCustomerDisplayName(crmCustomer), crmCustomer.getLogo());
                    markConversationMatched(conversation);
                } else if (StrUtil.isNotBlank(externalUserConversionError)) {
                    markConversationUnmatched(conversation, MATCH_STATUS_UNMATCHED_API_ERROR, externalUserConversionError);
                } else {
                    try {
                        WecomExternalCustomer syncedCustomer = syncArchiveExternalCustomer(config, externalUserId);
                        if (syncedCustomer != null) {
                            applyMatchedExternalCustomer(conversation, syncedCustomer);
                        } else {
                            markConversationUnmatched(conversation, MATCH_STATUS_UNMATCHED_NOT_VISIBLE,
                                    "external customer is not visible");
                        }
                    } catch (Exception e) {
                        String errorSummary = safeErrorSummary(e);
                        markConversationUnmatched(conversation, isInvalidExternalUserError(errorSummary)
                                ? MATCH_STATUS_UNMATCHED_NOT_VISIBLE
                                : MATCH_STATUS_UNMATCHED_API_ERROR, errorSummary);
                    }
                }
            }
        } else if (employee != null) {
            applyConversationPeer(conversation, employee.getName(), employee.getAvatar());
        }
    }

    private WecomExternalCustomer syncArchiveExternalCustomer(WecomCorpConfig config, String externalUserId) {
        if (StrUtil.isBlank(externalUserId)) {
            return null;
        }
        log.debug("WeCom archive external customer fetch start: corpId={}, externalUserId={}",
                config.getCorpId(), externalUserId);
        String token = tokenService.fetchContactAccessToken(config);
        WecomExternalCustomer customer = saveExternalCustomerRecord(config, apiClient.getExternalCustomer(token, externalUserId),
                resolveSyncOwnerId(), Map.of());
        log.debug("WeCom archive external customer synced: corpId={}, externalUserId={}, customerId={}, crmCustomerId={}",
                config.getCorpId(), externalUserId,
                customer == null ? null : customer.getId(),
                customer == null ? null : customer.getCustomerId());
        return customer;
    }

    private void reconcileArchiveCustomerRelations(WecomCorpConfig config) {
        if (config == null || StrUtil.isBlank(config.getCorpId())) {
            return;
        }
        List<WecomConversation> conversations = conversationMapper.selectList(Wrappers.<WecomConversation>lambdaQuery()
                .eq(WecomConversation::getCorpId, config.getCorpId())
                .eq(WecomConversation::getConversationType, "customer")
                .and(wrapper -> wrapper.isNull(WecomConversation::getMatchStatus)
                        .or()
                        .ne(WecomConversation::getMatchStatus, MATCH_STATUS_MATCHED)
                        .or()
                        .isNull(WecomConversation::getCustomerId))
                .last("LIMIT 500"));
        if (conversations == null || conversations.isEmpty()) {
            log.debug("WeCom archive customer reconcile skipped: corpId={}, pendingCount=0", config.getCorpId());
            return;
        }
        log.debug("WeCom archive customer reconcile start: corpId={}, pendingCount={}",
                config.getCorpId(), conversations.size());
        Set<String> archiveExternalUserIds = new LinkedHashSet<>();
        for (WecomConversation conversation : conversations) {
            String archiveExternalUserId = resolveArchiveExternalUserId(conversation);
            if (StrUtil.isNotBlank(archiveExternalUserId)) {
                archiveExternalUserIds.add(archiveExternalUserId);
            }
        }
        if (archiveExternalUserIds.isEmpty()) {
            log.debug("WeCom archive customer reconcile skipped: corpId={}, archiveExternalUserCount=0",
                    config.getCorpId());
            return;
        }
        ArchiveExternalUserConversion externalUserConversion =
                convertArchiveExternalUserIdList(config, new ArrayList<>(archiveExternalUserIds));
        for (WecomConversation conversation : conversations) {
            String archiveExternalUserId = resolveArchiveExternalUserId(conversation);
            if (StrUtil.isBlank(archiveExternalUserId)) {
                continue;
            }
            String externalUserId = resolveContactExternalUserId(archiveExternalUserId, externalUserConversion);
            conversation.setArchiveExternalUserId(archiveExternalUserId);
            conversation.setExternalUserId(externalUserId);
            applyConversationRelations(config, conversation, null, conversation.getEmployeeUserId(), externalUserId,
                    externalUserConversion.errorMessage(), null, null);
            conversationMapper.updateById(conversation);
        }
        log.debug("WeCom archive customer reconcile finished: corpId={}, pendingCount={}, archiveExternalUserCount={}, conversionError={}",
                config.getCorpId(), conversations.size(), archiveExternalUserIds.size(),
                StrUtil.maxLength(externalUserConversion.errorMessage(), 500));
    }

    private ArchiveExternalUserConversion convertArchiveExternalUserIdList(WecomCorpConfig config, List<String> archiveExternalUserIds) {
        if (archiveExternalUserIds == null || archiveExternalUserIds.isEmpty()) {
            return new ArchiveExternalUserConversion(Map.of(), null);
        }
        try {
            log.debug("WeCom archive external user conversion start: corpId={}, archiveExternalUserCount={}",
                    config.getCorpId(), archiveExternalUserIds.size());
            String token = tokenService.fetchContactAccessToken(config);
            Map<String, String> converted = apiClient.convertExternalUserIds(token, archiveExternalUserIds);
            log.debug("WeCom archive external user conversion done: corpId={}, archiveExternalUserCount={}, convertedCount={}",
                    config.getCorpId(), archiveExternalUserIds.size(), converted == null ? 0 : converted.size());
            return new ArchiveExternalUserConversion(converted == null ? Map.of() : converted, null);
        } catch (Exception e) {
            log.debug("WeCom archive external user conversion failed: corpId={}, archiveExternalUserCount={}, error={}",
                    config.getCorpId(), archiveExternalUserIds.size(), e.getMessage(), e);
            return new ArchiveExternalUserConversion(Map.of(), safeErrorSummary(e));
        }
    }

    private String resolveArchiveExternalUserId(WecomConversation conversation) {
        if (conversation == null) {
            return null;
        }
        return StrUtil.blankToDefault(conversation.getArchiveExternalUserId(), conversation.getExternalUserId());
    }

    private String resolveContactExternalUserId(String archiveExternalUserId,
                                                ArchiveExternalUserConversion externalUserConversion) {
        if (StrUtil.isBlank(archiveExternalUserId)) {
            return null;
        }
        if (externalUserConversion == null || externalUserConversion.externalUserIds().isEmpty()) {
            return archiveExternalUserId;
        }
        return StrUtil.blankToDefault(externalUserConversion.externalUserIds().get(archiveExternalUserId),
                archiveExternalUserId);
    }

    private String resolveContactEmployeeUserId(WecomCorpConfig config, String employeeUserId) {
        if (StrUtil.isBlank(employeeUserId) || agencyDevService == null || !agencyDevService.owns(config)) {
            return employeeUserId;
        }
        try {
            String token = tokenService.fetchContactAccessToken(config);
            Map<String, String> openUserIds = apiClient.convertUserIdsToOpenUserIds(token, List.of(employeeUserId));
            return openUserIds == null ? employeeUserId : StrUtil.blankToDefault(openUserIds.get(employeeUserId), employeeUserId);
        } catch (Exception ignored) {
            return employeeUserId;
        }
    }

    private void applyMatchedExternalCustomer(WecomConversation conversation, WecomExternalCustomer customer) {
        conversation.setExternalCustomerId(customer.getId());
        conversation.setCustomerId(customer.getCustomerId());
        applyConversationPeer(conversation, customer.getName(), customer.getAvatar());
        markConversationMatched(conversation);
    }

    private void markConversationMatched(WecomConversation conversation) {
        conversation.setMatchStatus(MATCH_STATUS_MATCHED);
        conversation.setMatchError(null);
    }

    private void markConversationUnmatched(WecomConversation conversation, String matchStatus, String matchError) {
        conversation.setExternalCustomerId(null);
        conversation.setCustomerId(null);
        conversation.setMatchStatus(matchStatus);
        conversation.setMatchError(matchError);
    }

    private boolean isInvalidExternalUserError(String errorSummary) {
        return StrUtil.containsIgnoreCase(errorSummary, "40096")
                || StrUtil.containsIgnoreCase(errorSummary, "invalid external userid");
    }

    private String safeErrorSummary(Exception e) {
        String message = e == null ? null : e.getMessage();
        if (StrUtil.isBlank(message) && e != null) {
            message = e.getClass().getSimpleName();
        }
        String summary = StrUtil.blankToDefault(message, "unknown error")
                .replaceAll("access_token=[^&\\s]+", "access_token=***");
        return summary.length() > 500 ? summary.substring(0, 500) : summary;
    }

    private void applyGroupConversationRelations(WecomCorpConfig config,
                                                 WecomConversation conversation,
                                                 String roomId,
                                                 String from,
                                                 JSONArray toList) {
        if (StrUtil.isBlank(roomId) || groupChatMapper == null) {
            return;
        }
        WecomGroupChat groupChat = groupChatMapper.selectOne(Wrappers.<WecomGroupChat>lambdaQuery()
                .eq(WecomGroupChat::getCorpId, config.getCorpId())
                .eq(WecomGroupChat::getChatId, roomId)
                .last("LIMIT 1"));
        if (groupChat == null) {
            groupChat = fetchAndSaveGroupChat(config, roomId);
        }
        if (groupChat == null) {
            applyConversationPeer(conversation, buildGroupFallbackTitle(config, from, toList), null);
            return;
        }
        conversation.setGroupChatId(groupChat.getId());
        String groupName = StrUtil.blankToDefault(groupChat.getName(), buildGroupFallbackTitle(config, from, toList));
        conversation.setPeerName(groupName);
        if (isGeneratedConversationTitle(conversation)) {
            conversation.setTitle(groupName);
        }
    }

    private String buildGroupFallbackTitle(WecomCorpConfig config, String from, JSONArray toList) {
        List<String> participantIds = resolveGroupParticipantIds(from, toList);
        if (participantIds.isEmpty()) {
            return "群会话";
        }
        List<String> knownNames = resolveKnownParticipantNames(config, participantIds);
        if (!knownNames.isEmpty()) {
            int limit = Math.min(knownNames.size(), 3);
            String summary = String.join("、", knownNames.subList(0, limit));
            return participantIds.size() > limit ? summary + "等" + participantIds.size() + "人" : summary;
        }
        return "群会话（" + participantIds.size() + "人）";
    }

    private List<String> resolveGroupParticipantIds(String from, JSONArray toList) {
        List<String> userIds = new ArrayList<>();
        if (StrUtil.isNotBlank(from)) {
            userIds.add(from);
        }
        if (toList != null) {
            for (Object item : toList) {
                if (item != null && StrUtil.isNotBlank(item.toString())) {
                    userIds.add(item.toString());
                }
            }
        }
        return normalizeUserIds(userIds);
    }

    private List<String> resolveKnownParticipantNames(WecomCorpConfig config, List<String> userIds) {
        if (config == null || StrUtil.isBlank(config.getCorpId()) || userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Map<String, String> displayNames = new LinkedHashMap<>();
        List<String> employeeUserIds = userIds.stream()
                .filter(userId -> !isWecomExternalUser(userId))
                .toList();
        if (!employeeUserIds.isEmpty()) {
            List<WecomEmployee> employees = employeeMapper.selectList(Wrappers.<WecomEmployee>lambdaQuery()
                    .eq(WecomEmployee::getCorpId, config.getCorpId())
                    .in(WecomEmployee::getUserId, employeeUserIds));
            if (employees != null) {
                for (WecomEmployee employee : employees) {
                    if (employee != null && StrUtil.isNotBlank(employee.getUserId())) {
                        displayNames.put(employee.getUserId(), employee.getName());
                    }
                }
            }
        }
        List<String> externalUserIds = userIds.stream()
                .filter(this::isWecomExternalUser)
                .toList();
        if (!externalUserIds.isEmpty()) {
            List<WecomExternalCustomer> customers = externalCustomerMapper.selectList(Wrappers.<WecomExternalCustomer>lambdaQuery()
                    .eq(WecomExternalCustomer::getCorpId, config.getCorpId())
                    .in(WecomExternalCustomer::getExternalUserId, externalUserIds));
            if (customers != null) {
                for (WecomExternalCustomer customer : customers) {
                    if (customer != null && StrUtil.isNotBlank(customer.getExternalUserId())) {
                        displayNames.put(customer.getExternalUserId(), customer.getName());
                    }
                }
            }
        }
        List<String> names = new ArrayList<>();
        for (String userId : userIds) {
            String displayName = displayNames.get(userId);
            if (StrUtil.isNotBlank(displayName) && !StrUtil.equals(displayName, userId)) {
                names.add(displayName);
            }
        }
        return names;
    }

    private WecomGroupChat fetchAndSaveGroupChat(WecomCorpConfig config, String roomId) {
        try {
            String token = tokenService.fetchContactAccessToken(config);
            WecomGroupChat groupChat = saveCustomerGroupChat(config, roomId,
                    apiClient.getCustomerGroupChat(token, roomId));
            if (groupChat != null) {
                return groupChat;
            }
        } catch (Exception ignored) {
            // Customer group APIs only work for customer groups visible to the app.
        }
        try {
            String token = tokenService.fetchArchiveAccessToken(config);
            return saveInternalArchiveGroupChat(config, roomId,
                    apiClient.getArchiveInternalGroupChat(token, roomId));
        } catch (Exception ignored) {
            return null;
        }
    }

    private WecomGroupChat saveCustomerGroupChat(WecomCorpConfig config, String roomId, JSONObject response) {
        JSONObject groupJson = response == null ? null : response.getJSONObject("group_chat");
        if (groupJson == null) {
            return null;
        }
        String chatId = StrUtil.blankToDefault(groupJson.getString("chat_id"), roomId);
        WecomGroupChat groupChat = findOrCreateGroupChat(config, chatId);
        groupChat.setName(groupJson.getString("name"));
        groupChat.setOwnerUserId(groupJson.getString("owner"));
        JSONArray memberList = groupJson.getJSONArray("member_list");
        groupChat.setMemberList(jsonArrayToString(memberList));
        groupChat.setCustomerList(jsonArrayToString(filterCustomerGroupMembers(memberList)));
        groupChat.setStatus(1);
        groupChat.setNotice(groupJson.getString("notice"));
        groupChat.setExternalCreateTime(resolveUnixSeconds(groupJson.getLong("create_time")));
        groupChat.setSyncedAt(new Date());
        saveGroupChat(groupChat);
        return groupChat;
    }

    private WecomGroupChat saveInternalArchiveGroupChat(WecomCorpConfig config, String roomId, JSONObject response) {
        if (response == null) {
            return null;
        }
        WecomGroupChat groupChat = findOrCreateGroupChat(config, roomId);
        groupChat.setName(response.getString("roomname"));
        groupChat.setOwnerUserId(response.getString("creator"));
        groupChat.setMemberList(jsonArrayToString(response.getJSONArray("members")));
        groupChat.setCustomerList("[]");
        groupChat.setStatus(1);
        groupChat.setNotice(response.getString("notice"));
        groupChat.setExternalCreateTime(resolveUnixSeconds(response.getLong("room_create_time")));
        groupChat.setSyncedAt(new Date());
        saveGroupChat(groupChat);
        return groupChat;
    }

    private WecomGroupChat findOrCreateGroupChat(WecomCorpConfig config, String chatId) {
        WecomGroupChat groupChat = groupChatMapper.selectOne(Wrappers.<WecomGroupChat>lambdaQuery()
                .eq(WecomGroupChat::getCorpId, config.getCorpId())
                .eq(WecomGroupChat::getChatId, chatId)
                .last("LIMIT 1"));
        if (groupChat == null) {
            groupChat = new WecomGroupChat();
            groupChat.setCorpId(config.getCorpId());
            groupChat.setChatId(chatId);
        }
        return groupChat;
    }

    private void saveGroupChat(WecomGroupChat groupChat) {
        if (groupChat.getId() == null) {
            groupChatMapper.insert(groupChat);
        } else {
            groupChatMapper.updateById(groupChat);
        }
    }

    private JSONArray filterCustomerGroupMembers(JSONArray memberList) {
        JSONArray customers = new JSONArray();
        if (memberList == null) {
            return customers;
        }
        for (Object item : memberList) {
            if (item instanceof JSONObject member && member.getInteger("type") != null && member.getInteger("type") == 2) {
                customers.add(member);
            }
        }
        return customers;
    }

    private void applyConversationPeer(WecomConversation conversation, String peerName, String peerAvatar) {
        String displayName = StrUtil.blankToDefault(peerName, conversation.getChatId());
        conversation.setPeerName(displayName);
        conversation.setPeerAvatar(peerAvatar);
        if (isGeneratedConversationTitle(conversation)) {
            conversation.setTitle(displayName);
        }
    }

    private boolean isGeneratedConversationTitle(WecomConversation conversation) {
        String title = conversation.getTitle();
        if (StrUtil.isBlank(title)) {
            return true;
        }
        if (StrUtil.equals(title, conversation.getChatId())) {
            return true;
        }
        String externalUserId = conversation.getExternalUserId();
        String employeeUserId = conversation.getEmployeeUserId();
        return StrUtil.isNotBlank(externalUserId)
                && StrUtil.isNotBlank(employeeUserId)
                && (StrUtil.equals(title, externalUserId + ":" + employeeUserId)
                || StrUtil.equals(title, employeeUserId + ":" + externalUserId));
    }

    private String resolveCustomerDisplayName(Customer customer) {
        if (customer == null) {
            return null;
        }
        if (StrUtil.isNotBlank(customer.getCompanyName())) {
            return customer.getCompanyName();
        }
        return customer.getPrimaryContactName();
    }

    private String resolveConversationType(String roomId, String externalUserId) {
        if (StrUtil.isNotBlank(roomId)) {
            return "group";
        }
        return StrUtil.isNotBlank(externalUserId) ? "customer" : "employee";
    }

    private String resolveExternalUserId(String from, JSONArray toList) {
        if (isWecomExternalUser(from)) {
            return from;
        }
        if (toList != null) {
            for (Object item : toList) {
                String userId = item == null ? null : item.toString();
                if (isWecomExternalUser(userId)) {
                    return userId;
                }
            }
        }
        return null;
    }

    private String resolveEmployeeUserId(String from, JSONArray toList) {
        if (StrUtil.isNotBlank(from) && !isWecomExternalUser(from)) {
            return from;
        }
        if (toList != null) {
            for (Object item : toList) {
                String userId = item == null ? null : item.toString();
                if (StrUtil.isNotBlank(userId) && !isWecomExternalUser(userId)) {
                    return userId;
                }
            }
        }
        return null;
    }

    private boolean isWecomExternalUser(String userId) {
        return StrUtil.isNotBlank(userId) && (userId.startsWith("wm") || userId.startsWith("wo"));
    }

    private String jsonArrayToString(JSONArray array) {
        return array == null ? "[]" : array.toJSONString();
    }

    private int resolveArchiveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 1000;
        }
        return Math.min(limit, 1000);
    }

    public record CustomerSyncResult(int fetched, int saved, int failed, String errorMessage) {
    }

    private record FollowUserLookup(String queryUserId, String employeeUserId) {
    }

    public record ArchiveSyncResult(int fetched, int saved) {
    }

    private record ArchiveExternalUserConversion(Map<String, String> externalUserIds, String errorMessage) {
    }
}
