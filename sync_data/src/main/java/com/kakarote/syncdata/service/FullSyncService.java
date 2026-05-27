package com.kakarote.syncdata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.db.MappingRepository;
import com.kakarote.syncdata.db.TargetSchema;
import com.kakarote.syncdata.util.Rows;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Pattern;

@Service
public class FullSyncService {

    private static final Logger log = LoggerFactory.getLogger(FullSyncService.class);
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]{0,99}");
    private static final Set<String> CRM_TARGET_TABLES = Set.of(
            "crm_tenant", "manager_dept", "manager_role", "manager_user", "manager_user_role",
            "crm_custom_field", "crm_customer", "crm_contact", "crm_follow_up", "crm_schedule", "crm_task"
    );
    private static final List<Long> MIGRATED_ROLE_VIEW_MENU_IDS = List.of(
            1002L, 1102L, 1202L, 1302L, 1402L, 2202L
    );
    private static final Map<Integer, CustomFieldDefinition> SUPPORTED_WK_CUSTOM_FIELD_TYPES = Map.of(
            1, new CustomFieldDefinition("text", "VARCHAR(500)"),
            2, new CustomFieldDefinition("textarea", "TEXT"),
            3, new CustomFieldDefinition("select", "VARCHAR(100)"),
            4, new CustomFieldDefinition("date", "DATE"),
            5, new CustomFieldDefinition("number", "DECIMAL(15,2)"),
            6, new CustomFieldDefinition("number", "DECIMAL(15,2)"),
            9, new CustomFieldDefinition("multiselect", "TEXT"),
            13, new CustomFieldDefinition("datetime", "TIMESTAMP")
    );
    private static final String CUSTOM_FIELD_POOL_COLUMN_PREFIX = "field_";
    private static final String CUSTOM_FIELD_POOL_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int CUSTOM_FIELD_POOL_SUFFIX_LENGTH = 6;
    private static final int CUSTOM_FIELD_POOL_MAX_ATTEMPTS = 20;

    private final JdbcTemplate oldCrm;
    private final JdbcTemplate target;
    private final SyncProperties properties;
    private final TargetSchema targetSchema;
    private final MappingRepository mappingRepository;
    private final CompanyBindingRepository bindingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, Long> defaultUserCache = new ConcurrentHashMap<>();
    private final Map<String, Long> contactCustomerCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> primaryContactCache = new ConcurrentHashMap<>();
    private final Map<String, Long> batchIdCache = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> menuExistsCache = new ConcurrentHashMap<>();
    private final Set<String> customFieldReservedColumns = ConcurrentHashMap.newKeySet();
    private final Set<Long> activeJobIds = ConcurrentHashMap.newKeySet();
    private final LinkedHashMap<String, List<Object[]>> pendingBatchUpdates = new LinkedHashMap<>();
    private final ExecutorService fullSyncExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sync-data-full-sync");
        thread.setDaemon(true);
        return thread;
    });
    private String encodedResetPassword;
    private SyncScope currentScope = SyncScope.all();
    private boolean batchWritesActive;

    /**
     * 注入老库、目标库、同步配置、结构准备、映射仓储和绑定仓储。
     */
    public FullSyncService(JdbcTemplate oldCrmJdbcTemplate,
                           JdbcTemplate targetJdbcTemplate,
                           SyncProperties properties,
                           TargetSchema targetSchema,
                           MappingRepository mappingRepository,
                           CompanyBindingRepository bindingRepository) {
        this.oldCrm = oldCrmJdbcTemplate;
        this.target = targetJdbcTemplate;
        this.properties = properties;
        this.targetSchema = targetSchema;
        this.mappingRepository = mappingRepository;
        this.bindingRepository = bindingRepository;
    }

    /**
     * 执行所有 company_id 的全量同步。
     */
    public synchronized long syncAll() {
        return sync(SyncScope.all(), null);
    }

    /**
     * 执行指定 ai_crm tenant_id 与 wk_crm company_id 的全量同步。
     */
    public synchronized long syncCompany(Long tenantId, Long companyId, Long bindingId) {
        validateCompanyScope(tenantId, companyId);
        return sync(SyncScope.company(tenantId, companyId, bindingId), null);
    }

    /**
     * 异步启动指定绑定关系的全量同步，立即返回可查询的任务编号。
     */
    public long startCompanyJob(Long tenantId, Long companyId, Long bindingId) {
        validateCompanyScope(tenantId, companyId);
        long jobId = mappingRepository.startJob("full");
        activeJobIds.add(jobId);
        bindingRepository.markFullSyncRunning(bindingId, jobId);
        fullSyncExecutor.submit(() -> {
            try {
                sync(SyncScope.company(tenantId, companyId, bindingId), jobId);
            } catch (RuntimeException ex) {
                log.warn("Async full sync failed. jobId={}, message={}", jobId, ex.getMessage());
            }
        });
        return jobId;
    }

    /**
     * 恢复当前进程没有持有的运行中任务状态，通常用于页面刷新或服务重启后的首次查询。
     */
    public void recoverInactiveRunningJobs() {
        mappingRepository.markRunningJobsInterruptedExcept(activeJobIds);
        bindingRepository.markRunningFullSyncInterruptedExcept(activeJobIds);
    }

    /**
     * 关闭后台全量同步执行器。
     */
    @PreDestroy
    public void shutdown() {
        fullSyncExecutor.shutdownNow();
    }

    private void validateCompanyScope(Long tenantId, Long companyId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required.");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required.");
        }
    }

    /**
     * 按同步范围编排完整全量同步流程，并维护任务状态和统计数据。
     */
    private synchronized long sync(SyncScope scope, Long existingJobId) {
        currentScope = scope;
        clearRuntimeCaches();
        Long jobId = existingJobId;
        long total = 0;
        long success = 0;
        long fail = 0;
        try {
            assertSourceReady();
            if (properties.isDryRun() && jobId == null) {
                logDryRunSummary(scope);
                return -1L;
            }

            encodedResetPassword = passwordEncoder.encode(properties.getResetPassword());

            if (jobId == null) {
                jobId = mappingRepository.startJob("full");
            }
            activeJobIds.add(jobId);
            bindingRepository.markFullSyncRunning(scope.bindingId(), jobId);
            if (properties.isDryRun()) {
                logDryRunSummary(scope);
                mappingRepository.finishJob(jobId, 0, 0, 0);
                bindingRepository.markFullSyncFinished(scope.bindingId(), jobId, true);
                return jobId;
            }
            if (properties.isTruncateBeforeSync()) {
                log.warn("Deleting rows previously created by sync mappings before full sync.");
                if (scope.isCompanyScoped()) {
                    mappingRepository.deleteMappedTargetRows(scope.sourceCompanyId());
                } else {
                    mappingRepository.deleteMappedTargetRows();
                }
            }

            Map<Long, Long> tenantMap = syncTenants(jobId);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncDepartments(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncRoles(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncUsers(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncUserRoles(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            Map<OldFieldKey, CustomFieldMeta> fieldMap = syncCustomFields(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncCustomers(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncContacts(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncCustomValues(jobId, "customer_custom_values", "wk_crm_customer_data",
                    "wk_crm_customer", "crm_customer", "customer_id", "crm_customer", fieldMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncCustomValues(jobId, "contact_custom_values", "wk_crm_contacts_data",
                    "wk_crm_contacts", "crm_contact", "contact_id", "crm_contact", fieldMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncFollowUps(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncSchedules(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncProjectTasks(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            syncWorkTasks(jobId, tenantMap);
            total += lastStats.total;
            success += lastStats.success;
            fail += lastStats.fail;

            refreshCustomerDenormalizedFields();
            if (properties.isPopulateSearchIndex()) {
                refreshGlobalSearchIndex();
            }

            mappingRepository.finishJob(jobId, total, success, fail);
            bindingRepository.markFullSyncFinished(scope.bindingId(), jobId, fail == 0);
            log.info("Full sync finished. jobId={}, total={}, success={}, fail={}", jobId, total, success, fail);
            return jobId;
        } catch (RuntimeException ex) {
            if (jobId != null) {
                mappingRepository.failJob(jobId, ex.getMessage());
                bindingRepository.markFullSyncFailed(scope.bindingId(), jobId);
            }
            throw ex;
        } finally {
            if (jobId != null) {
                activeJobIds.remove(jobId);
            }
            currentScope = SyncScope.all();
        }
    }

    private ModuleStats lastStats = new ModuleStats();

    /**
     * 同步租户数据，指定 company_id 同步时会复用已绑定的 tenant_id。
     */
    private Map<Long, Long> syncTenants(long jobId) {
        Map<Long, Long> tenantMap = new HashMap<>();
        if (currentScope.isCompanyScoped()) {
            ensureBoundTenant(currentScope.tenantId(), currentScope.sourceCompanyId());
            tenantMap.put(currentScope.sourceCompanyId(), currentScope.tenantId());
            lastStats = new ModuleStats(1, 1, 0);
            mappingRepository.finishModule(jobId, "tenants", "sync_company_binding", "crm_tenant",
                    lastStats.total, lastStats.success, lastStats.fail, "bound tenant");
            return tenantMap;
        }
        List<Map<String, Object>> rows = oldTableExists("wk_admin_company")
                ? oldCrm.queryForList("SELECT * FROM wk_admin_company ORDER BY company_id")
                : List.of();

        if (rows.isEmpty()) {
            for (Long companyId : loadDistinctCompanyIds()) {
                tenantMap.put(companyId, ensureSyntheticTenant(companyId));
            }
            lastStats = new ModuleStats(tenantMap.size(), tenantMap.size(), 0);
            mappingRepository.finishModule(jobId, "tenants", "wk_admin_company", "crm_tenant",
                    lastStats.total, lastStats.success, lastStats.fail, "synthetic tenants");
            return tenantMap;
        }

        ModuleStats stats = new ModuleStats(rows.size(), 0, 0);
        for (Map<String, Object> row : rows) {
            Long companyId = Rows.longValue(row, "company_id");
            if (companyId == null) {
                stats.fail++;
                continue;
            }
            try {
                Long tenantId = upsertTenantFromCompany(row, companyId);
                tenantMap.put(companyId, tenantId);
                stats.success++;
            } catch (Exception ex) {
                stats.fail++;
                mappingRepository.recordError(jobId, "tenants", "wk_admin_company", companyId,
                        String.valueOf(companyId), ex.getMessage());
            }
        }
        lastStats = stats;
        mappingRepository.finishModule(jobId, "tenants", "wk_admin_company", "crm_tenant",
                stats.total, stats.success, stats.fail, "ok");
        log.info("Synced tenants. total={}, success={}, fail={}", stats.total, stats.success, stats.fail);
        return tenantMap;
    }

    /**
     * 确保绑定的 tenant_id 在目标租户表中存在，并写入公司映射关系。
     */
    private void ensureBoundTenant(Long tenantId, Long companyId) {
        target.update("""
                INSERT INTO crm_tenant (
                    tenant_id, tenant_name, status, max_users,
                    gift_credit_total, gift_credit_used,
                    purchased_credit_total, purchased_credit_used,
                    remark,
                    create_time, update_time
                )
                VALUES (?, ?, 1, 200, 300, 0, 0, 0, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (tenant_id) DO NOTHING
                """, tenantId, "WK CRM " + companyId, "Bound to wk_crm.company_id=" + companyId);
        mappingRepository.upsertMapping("wk_admin_company", companyId, companyId, "crm_tenant", tenantId, tenantId);
    }

    /**
     * 将 wk_admin_company 行转换并写入目标 crm_tenant。
     */
    private Long upsertTenantFromCompany(Map<String, Object> row, Long companyId) {
        long tenantId = mappingRepository.getOrCreateTargetId("wk_admin_company", companyId, companyId,
                "crm_tenant", null);
        Integer companyStatus = Rows.intValue(row, "company_status");
        String phone = Rows.str(row, "phone");
        String tenantName = phone == null ? "WK CRM " + companyId : "WK CRM " + phone;
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("tenant_id", tenantId);
        values.put("tenant_name", Rows.trimToLength(tenantName, 100));
        values.put("contact_phone", Rows.trimToLength(phone, 20));
        values.put("status", companyStatus == null || companyStatus == 1 ? 1 : 0);
        values.put("max_users", 200);
        values.put("gift_credit_total", 300L);
        values.put("gift_credit_used", 0L);
        values.put("purchased_credit_total", 0L);
        values.put("purchased_credit_used", 0L);
        values.put("remark", "Migrated from wk_admin_company.company_id=" + companyId);
        values.put("create_time", Rows.timestamp(row, "create_time"));
        values.put("update_time", Timestamp.valueOf(LocalDateTime.now()));
        upsert("crm_tenant", "tenant_id", values);
        mappingRepository.refreshTenant("wk_admin_company", companyId, companyId, "crm_tenant", tenantId);
        return tenantId;
    }

    /**
     * 当老库缺少公司表时，为指定 company_id 创建一个兜底租户。
     */
    private Long ensureSyntheticTenant(Long companyId) {
        long safeCompanyId = companyId == null ? 0L : companyId;
        long tenantId = mappingRepository.getOrCreateTargetId("wk_admin_company", safeCompanyId, safeCompanyId,
                "crm_tenant", null);
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("tenant_id", tenantId);
        values.put("tenant_name", "WK CRM " + safeCompanyId);
        values.put("status", 1);
        values.put("max_users", 200);
        values.put("gift_credit_total", 300L);
        values.put("gift_credit_used", 0L);
        values.put("purchased_credit_total", 0L);
        values.put("purchased_credit_used", 0L);
        values.put("remark", "Synthetic tenant created by full sync.");
        values.put("create_time", Timestamp.valueOf(LocalDateTime.now()));
        values.put("update_time", Timestamp.valueOf(LocalDateTime.now()));
        upsert("crm_tenant", "tenant_id", values);
        mappingRepository.refreshTenant("wk_admin_company", safeCompanyId, safeCompanyId, "crm_tenant", tenantId);
        return tenantId;
    }

    /**
     * 同步老系统部门数据到 manager_dept，并转换父子部门 ID。
     */
    private void syncDepartments(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "departments", "wk_admin_dept", "manager_dept",
                "company_id, dept_id", null,
                row -> String.valueOf(Rows.longValue(row, "dept_id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_admin_dept", "manager_dept",
                        row -> String.valueOf(Rows.longValue(row, "dept_id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldDeptId = Rows.longValue(row, "dept_id");
                    long deptId = mappingRepository.getOrCreateTargetId("wk_admin_dept", companyId, oldDeptId,
                            "manager_dept", tenantId);
                    Long oldParentId = Rows.longValue(row, "parent_id");
                    Long parentId = 0L;
                    if (oldParentId != null && oldParentId > 0) {
                        parentId = mappingRepository.getOrCreateTargetId("wk_admin_dept", companyId, oldParentId,
                                "manager_dept", tenantId);
                    }
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("dept_id", deptId);
                    values.put("dept_name", Rows.trimToLength(Rows.strOrDefault(row, "name", "未命名部门"), 100));
                    values.put("parent_id", parentId);
                    values.put("sort_order", Rows.intValue(row, "num"));
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("tenant_id", tenantId);
                    upsert("manager_dept", "dept_id", values);
                });
    }

    /**
     * 同步老系统角色数据到 manager_role，并补齐创建人和更新人映射。
     */
    private void syncRoles(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "roles", "wk_admin_role", "manager_role",
                "company_id, role_id", null,
                row -> String.valueOf(Rows.longValue(row, "role_id")),
                row -> Rows.longValue(row, "company_id"),
                rows -> {
                    preloadPrimaryMappings(rows, "wk_admin_role", "manager_role",
                            row -> String.valueOf(Rows.longValue(row, "role_id")),
                            row -> Rows.longValue(row, "company_id"),
                            tenantMap);
                    preloadRoleMenuMappings(rows, tenantMap);
                },
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldRoleId = Rows.longValue(row, "role_id");
                    long roleId = mappingRepository.getOrCreateTargetId("wk_admin_role", companyId, oldRoleId,
                            "manager_role", tenantId);
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("role_id", roleId);
                    values.put("role_name", Rows.trimToLength(Rows.strOrDefault(row, "role_name", "迁移角色"), 64));
                    values.put("realm", "wk_role_" + roleId);
                    values.put("description", Rows.trimToLength(Rows.str(row, "remark"), 255));
                    Integer dataScope = normalizeDataType(Rows.intValue(row, "data_type"));
                    Long createUserId = resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId);
                    Long updateUserId = resolveUserId(companyId, Rows.longValue(row, "update_user_id"), tenantId);
                    values.put("data_type", dataScope);
                    values.put("create_user_id", createUserId);
                    values.put("update_user_id", updateUserId);
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("update_time", Rows.timestamp(row, "update_time"));
                    values.put("tenant_id", tenantId);
                    upsert("manager_role", "role_id", values);
                    ensureRoleViewPermissions(companyId, oldRoleId, roleId, tenantId, dataScope, createUserId, updateUserId);
                });
    }

    /**
     * 同步老系统用户数据到 manager_user，并统一重置迁移用户密码。
     */
    private void syncUsers(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "users", "wk_admin_user", "manager_user",
                "company_id, user_id", null,
                row -> String.valueOf(Rows.longValue(row, "user_id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_admin_user", "manager_user",
                        row -> String.valueOf(Rows.longValue(row, "user_id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldUserId = Rows.longValue(row, "user_id");
                    long userId = mappingRepository.getOrCreateTargetId("wk_admin_user", companyId, oldUserId,
                            "manager_user", tenantId);
                    Long oldDeptId = Rows.longValue(row, "dept_id");
                    Long deptId = oldDeptId == null || oldDeptId <= 0
                            ? null
                            : mappingRepository.findTargetId("wk_admin_dept", companyId, oldDeptId, "manager_dept");
                    Long oldParentId = Rows.longValue(row, "parent_id");
                    Long parentId = oldParentId == null || oldParentId <= 0
                            ? 0L
                            : mappingRepository.findTargetId("wk_admin_user", companyId, oldParentId, "manager_user");
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("user_id", userId);
                    values.put("username", Rows.trimToLength(Rows.strOrDefault(row, "username", "wk_user_" + oldUserId), 64));
                    values.put("password", encodedResetPassword);
                    values.put("salt", Rows.trimToLength(Rows.str(row, "salt"), 64));
                    values.put("img", Rows.trimToLength(Rows.firstNonBlank(Rows.str(row, "wx_avatar"), Rows.str(row, "img")), 255));
                    values.put("create_time", valueOrNow(Rows.timestamp(row, "create_time")));
                    values.put("realname", Rows.trimToLength(Rows.strOrDefault(row, "realname", "未命名用户"), 64));
                    values.put("num", Rows.trimToLength(Rows.str(row, "num"), 64));
                    values.put("mobile", Rows.trimToLength(Rows.firstNonBlank(Rows.str(row, "mobile"), Rows.str(row, "wx_mobile")), 32));
                    values.put("email", Rows.trimToLength(Rows.str(row, "email"), 128));
                    values.put("sex", Rows.intValue(row, "sex"));
                    values.put("dept_id", deptId);
                    values.put("post", Rows.trimToLength(Rows.str(row, "post"), 64));
                    values.put("status", normalizeUserStatus(Rows.intValue(row, "status")));
                    values.put("parent_id", parentId);
                    values.put("tenant_id", tenantId);
                    upsert("manager_user", "user_id", values);
                });
    }

    /**
     * 同步老系统用户角色关系到 manager_user_role。
     */
    private void syncUserRoles(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "user_roles", "wk_admin_user_role", "manager_user_role",
                "company_id, id", null,
                row -> String.valueOf(Rows.longValue(row, "id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_admin_user_role", "manager_user_role",
                        row -> String.valueOf(Rows.longValue(row, "id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldUserId = Rows.longValue(row, "user_id");
                    Long oldRoleId = Rows.longValue(row, "role_id");
                    Long userId = mappingRepository.findTargetId("wk_admin_user", companyId, oldUserId, "manager_user");
                    Long roleId = mappingRepository.findTargetId("wk_admin_role", companyId, oldRoleId, "manager_role");
                    if (userId == null || roleId == null) {
                        return;
                    }
                    long id = mappingRepository.getOrCreateTargetId("wk_admin_user_role", companyId,
                            Rows.longValue(row, "id"), "manager_user_role", tenantId);
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("id", id);
                    values.put("user_id", userId);
                    values.put("role_id", roleId);
                    values.put("create_user_id", resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId));
                    values.put("update_user_id", resolveUserId(companyId, Rows.longValue(row, "update_user_id"), tenantId));
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("update_time", Rows.timestamp(row, "update_time"));
                    values.put("tenant_id", tenantId);
                    upsert("manager_user_role", "id", values);
                });
    }

    /**
     * 同步客户和联系人自定义字段定义，并在目标业务表补充动态字段列。
     */
    private Map<OldFieldKey, CustomFieldMeta> syncCustomFields(long jobId, Map<Long, Long> tenantMap) {
        Map<OldFieldKey, CustomFieldMeta> fieldMap = new HashMap<>();
        lastStats = processRows(jobId, "custom_fields", "wk_crm_field", "crm_custom_field",
                "company_id, field_id", "label IN (2, 3)",
                row -> String.valueOf(Rows.longValue(row, "field_id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_crm_field", "crm_custom_field",
                        row -> String.valueOf(Rows.longValue(row, "field_id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldFieldId = Rows.longValue(row, "field_id");
                    String entityType = Objects.equals(Rows.intValue(row, "label"), 3) ? "contact" : "customer";
                    String targetTable = "customer".equals(entityType) ? "crm_customer" : "crm_contact";
                    long fieldId = mappingRepository.getOrCreateTargetId("wk_crm_field", companyId, oldFieldId,
                            "crm_custom_field", tenantId);
                    CustomFieldDefinition fieldDefinition = mapCustomFieldDefinition(Rows.intValue(row, "type"));
                    if (fieldDefinition == null) {
                        log.info("Skipped unsupported WK custom field. companyId={}, fieldId={}, type={}",
                                companyId, oldFieldId, Rows.intValue(row, "type"));
                        return;
                    }
                    FieldPoolSlot poolSlot = acquireCustomFieldPoolSlot(entityType, fieldDefinition, tenantId, fieldId);
                    String rawFieldName = Rows.strOrDefault(row, "field_name", "field");
                    String columnName = poolSlot.columnName();
                    String fieldType = poolSlot.fieldType();
                    String columnType = poolSlot.columnType();

                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("field_id", fieldId);
                    values.put("entity_type", entityType);
                    values.put("field_name", columnName);
                    values.put("field_label", Rows.trimToLength(Rows.strOrDefault(row, "name", rawFieldName), 100));
                    values.put("field_type", fieldType);
                    values.put("field_source", "custom");
                    values.put("column_name", columnName);
                    values.put("column_type", columnType);
                    values.put("default_value", Rows.trimToLength(Rows.str(row, "default_value"), 500));
                    values.put("placeholder", Rows.trimToLength(Rows.str(row, "input_tips"), 200));
                    values.put("is_required", Objects.equals(Rows.intValue(row, "is_null"), 1) ? 1 : 0);
                    values.put("is_searchable", 1);
                    values.put("is_show_in_list", Objects.equals(Rows.intValue(row, "is_hidden"), 1) ? 0 : 1);
                    values.put("is_unique", Objects.equals(Rows.intValue(row, "is_unique"), 1) ? 1 : 0);
                    values.put("options", toOptionsJson(Rows.str(row, "options")));
                    values.put("validation_rules", null);
                    values.put("sort_order", Rows.intValue(row, "sorting"));
                    values.put("status", Objects.equals(Rows.intValue(row, "is_hidden"), 1) ? 0 : 1);
                    values.put("create_user_id", resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId));
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("update_time", Rows.timestamp(row, "update_time"));
                    values.put("tenant_id", tenantId);
                    upsert("crm_custom_field", "field_id", values);
                    fieldMap.put(new OldFieldKey(companyId, oldFieldId),
                            new CustomFieldMeta(entityType, companyId, oldFieldId, fieldId, columnName, fieldType));
                });
        return fieldMap;
    }

    /**
     * 同步客户主数据到 crm_customer，并生成搜索文本和基础冗余字段。
     */
    private void syncCustomers(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "customers", "wk_crm_customer", "crm_customer",
                "company_id, customer_id", null,
                row -> String.valueOf(Rows.longValue(row, "customer_id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_crm_customer", "crm_customer",
                        row -> String.valueOf(Rows.longValue(row, "customer_id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldCustomerId = Rows.longValue(row, "customer_id");
                    long customerId = mappingRepository.getOrCreateTargetId("wk_crm_customer", companyId, oldCustomerId,
                            "crm_customer", tenantId);
                    Long ownerId = resolveUserId(companyId,
                            Rows.longValue(row, "owner_user_id") == null ? Rows.longValue(row, "create_user_id") : Rows.longValue(row, "owner_user_id"),
                            tenantId);
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("customer_id", customerId);
                    values.put("company_name", Rows.trimToLength(Rows.strOrDefault(row, "customer_name", "未命名客户"), 255));
                    values.put("industry", null);
                    values.put("stage", Objects.equals(Rows.intValue(row, "deal_status"), 1) ? "closed" : "lead");
                    values.put("owner_id", ownerId);
                    values.put("level", null);
                    values.put("source", "wk_crm");
                    values.put("address", Rows.trimToLength(Rows.joinNonBlank(" ", Rows.str(row, "address"),
                            Rows.str(row, "location"), Rows.str(row, "detail_address")), 500));
                    values.put("website", Rows.trimToLength(Rows.str(row, "website"), 255));
                    values.put("logo", Rows.trimToLength(Rows.str(row, "data_img"), 500));
                    values.put("quotation", null);
                    values.put("last_contact_time", Rows.timestamp(row, "last_time"));
                    values.put("next_follow_time", Rows.timestamp(row, "next_time"));
                    values.put("remark", Rows.str(row, "remark"));
                    values.put("primary_contact_phone", Rows.trimToLength(Rows.firstNonBlank(Rows.str(row, "mobile"),
                            Rows.str(row, "telephone")), 50));
                    values.put("contact_count", 0);
                    values.put("tag_names", "");
                    values.put("search_text", buildCustomerSearchText(row));
                    values.put("status", Objects.equals(Rows.intValue(row, "status"), 3) ? 0 : 1);
                    values.put("create_user_id", resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId));
                    values.put("update_user_id", resolveUserId(companyId, Rows.longValue(row, "update_user_id"), tenantId));
                    values.put("create_time", valueOrNow(Rows.timestamp(row, "create_time")));
                    values.put("update_time", Rows.timestamp(row, "update_time"));
                    values.put("tenant_id", tenantId);
                    upsert("crm_customer", "customer_id", values);
                });
    }

    /**
     * 同步联系人数据到 crm_contact，并关联已同步的客户映射。
     */
    private void syncContacts(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "contacts", "wk_crm_contacts", "crm_contact",
                "company_id, contacts_id", null,
                row -> String.valueOf(Rows.longValue(row, "contacts_id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_crm_contacts", "crm_contact",
                        row -> String.valueOf(Rows.longValue(row, "contacts_id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldContactId = Rows.longValue(row, "contacts_id");
                    Long oldCustomerId = Rows.longValue(row, "customer_id");
                    Long customerId = mappingRepository.findTargetId("wk_crm_customer", companyId, oldCustomerId,
                            "crm_customer");
                    if (customerId == null) {
                        throw new IllegalStateException("Missing customer mapping for contact " + oldContactId);
                    }
                    long contactId = mappingRepository.getOrCreateTargetId("wk_crm_contacts", companyId, oldContactId,
                            "crm_contact", tenantId);
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("contact_id", contactId);
                    values.put("customer_id", customerId);
                    values.put("name", Rows.trimToLength(Rows.strOrDefault(row, "name", "未命名联系人"), 100));
                    values.put("position", Rows.trimToLength(Rows.str(row, "post"), 100));
                    values.put("phone", Rows.trimToLength(Rows.firstNonBlank(Rows.str(row, "mobile"),
                            Rows.str(row, "telephone")), 50));
                    values.put("email", Rows.trimToLength(Rows.str(row, "email"), 100));
                    values.put("wechat", null);
                    values.put("is_primary", isPrimaryContact(companyId, oldCustomerId, oldContactId) ? 1 : 0);
                    values.put("last_contact_time", Rows.timestamp(row, "last_time"));
                    values.put("notes", Rows.joinNonBlank("\n", Rows.str(row, "remark"), Rows.str(row, "address")));
                    values.put("status", 1);
                    values.put("create_user_id", resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId));
                    values.put("update_user_id", resolveUserId(companyId, Rows.longValue(row, "update_user_id"), tenantId));
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("update_time", Rows.timestamp(row, "update_time"));
                    values.put("tenant_id", tenantId);
                    upsert("crm_contact", "contact_id", values);
                });
    }

    /**
     * 同步老系统自定义字段值，并写入目标客户或联系人动态字段列。
     */
    private void syncCustomValues(long jobId, String moduleName, String sourceTable, String sourceEntityTable,
                                  String targetTable, String targetIdColumn, String mappingTargetTable,
                                  Map<OldFieldKey, CustomFieldMeta> fieldMap) {
        String entityType = "crm_customer".equals(targetTable) ? "customer" : "contact";
        lastStats = processRows(jobId, moduleName, sourceTable, targetTable,
                "company_id, id", null,
                row -> String.valueOf(Rows.longValue(row, "id")),
                row -> Rows.longValue(row, "company_id"),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long fieldId = Rows.longValue(row, "field_id");
                    CustomFieldMeta fieldMeta = fieldMap.get(new OldFieldKey(companyId, fieldId));
                    if (fieldMeta == null || !entityType.equals(fieldMeta.entityType())) {
                        return;
                    }
                    Long oldEntityId = Rows.longValue(row, "data_id");
                    if (oldEntityId == null) {
                        oldEntityId = findEntityIdByBatch(sourceEntityTable, targetTable, companyId, Rows.str(row, "batch_id"));
                    }
                    if (oldEntityId == null) {
                        return;
                    }
                    Long targetId = mappingRepository.findTargetId(sourceEntityTable, companyId, oldEntityId,
                            mappingTargetTable);
                    if (targetId == null) {
                        return;
                    }
                    updateDynamicColumn(targetTable, targetIdColumn, targetId, fieldMeta.columnName(),
                            normalizeCustomFieldValue(Rows.str(row, "value"), fieldMeta.fieldType()));
                });
    }

    /**
     * 同步老系统客户跟进记录到 crm_follow_up。
     */
    private void syncFollowUps(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "follow_ups", "wk_crm_activity", "crm_follow_up",
                "company_id, id", "type = 1 AND COALESCE(status, 1) = 1 AND activity_type IN (2, 3)",
                row -> String.valueOf(Rows.longValue(row, "id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_crm_activity", "crm_follow_up",
                        row -> String.valueOf(Rows.longValue(row, "id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long activityId = Rows.longValue(row, "id");
                    Integer activityType = Rows.intValue(row, "activity_type");
                    Long activityTypeId = Rows.longValue(row, "activity_type_id");
                    Long targetCustomerId;
                    Long targetContactId = null;
                    if (Objects.equals(activityType, 2)) {
                        targetCustomerId = mappingRepository.findTargetId("wk_crm_customer", companyId,
                                activityTypeId, "crm_customer");
                    } else {
                        Long oldCustomerId = findCustomerIdForContact(companyId, activityTypeId);
                        targetCustomerId = mappingRepository.findTargetId("wk_crm_customer", companyId,
                                oldCustomerId, "crm_customer");
                        targetContactId = mappingRepository.findTargetId("wk_crm_contacts", companyId,
                                activityTypeId, "crm_contact");
                    }
                    if (targetCustomerId == null) {
                        throw new IllegalStateException("Missing customer mapping for activity " + activityId);
                    }
                    long followUpId = mappingRepository.getOrCreateTargetId("wk_crm_activity", companyId, activityId,
                            "crm_follow_up", tenantId);
                    String category = Rows.str(row, "category");
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("follow_up_id", followUpId);
                    values.put("customer_id", targetCustomerId);
                    values.put("contact_id", targetContactId);
                    values.put("type", mapFollowUpType(category));
                    values.put("content", Rows.strOrDefault(row, "content", "无内容"));
                    values.put("summary", null);
                    values.put("scene_type", Rows.trimToLength(category, 100));
                    values.put("ai_generated", 0);
                    values.put("follow_time", valueOrNow(Rows.timestamp(row, "create_time")));
                    values.put("next_follow_time", Rows.timestamp(row, "next_time"));
                    values.put("create_user_id", resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId));
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("tenant_id", tenantId);
                    upsert("crm_follow_up", "follow_up_id", values);
                });
    }

    /**
     * 同步老系统日程数据到 crm_schedule。
     */
    private void syncSchedules(long jobId, Map<Long, Long> tenantMap) {
        lastStats = processRows(jobId, "schedules", "wk_oa_event", "crm_schedule",
                "company_id, event_id", null,
                row -> String.valueOf(Rows.longValue(row, "event_id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings("wk_oa_event", "crm_schedule",
                        row -> String.valueOf(Rows.longValue(row, "event_id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long eventId = Rows.longValue(row, "event_id");
                    long scheduleId = mappingRepository.getOrCreateTargetId("wk_oa_event", companyId, eventId,
                            "crm_schedule", tenantId);
                    Timestamp startTime = Rows.timestamp(row, "start_time");
                    if (startTime == null) {
                        throw new IllegalStateException("Schedule start_time is null: " + eventId);
                    }
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("schedule_id", scheduleId);
                    values.put("title", Rows.trimToLength(Rows.strOrDefault(row, "title", "未命名日程"), 255));
                    values.put("description", null);
                    values.put("start_time", startTime);
                    values.put("end_time", Rows.timestamp(row, "end_time"));
                    values.put("type", "meeting");
                    values.put("customer_id", null);
                    values.put("contact_id", null);
                    values.put("location", null);
                    values.put("participant_user_ids", mapUserIdList(companyId, Rows.str(row, "owner_user_ids"), tenantId));
                    values.put("create_user_id", resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId));
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("update_time", Rows.timestamp(row, "update_time"));
                    values.put("tenant_id", tenantId);
                    upsert("crm_schedule", "schedule_id", values);
                });
    }

    /**
     * 同步项目任务表中的任务数据到统一 crm_task。
     */
    private void syncProjectTasks(long jobId, Map<Long, Long> tenantMap) {
        syncTaskTable(jobId, tenantMap, "project_tasks", "wk_project_task", "project",
                "company_id, task_id", "COALESCE(ishidden, 0) = 0");
    }

    /**
     * 同步办公任务表中的任务数据到统一 crm_task。
     */
    private void syncWorkTasks(long jobId, Map<Long, Long> tenantMap) {
        syncTaskTable(jobId, tenantMap, "work_tasks", "wk_work_task", "work",
                "company_id, task_id", "COALESCE(ishidden, 0) = 0");
    }

    /**
     * 将不同老任务表按统一规则转换并写入 crm_task。
     */
    private void syncTaskTable(long jobId, Map<Long, Long> tenantMap, String moduleName, String sourceTable,
                               String taskType, String orderBy, String whereClause) {
        lastStats = processRows(jobId, moduleName, sourceTable, "crm_task",
                orderBy, whereClause,
                row -> String.valueOf(Rows.longValue(row, "task_id")),
                row -> Rows.longValue(row, "company_id"),
                preloadPrimaryMappings(sourceTable, "crm_task",
                        row -> String.valueOf(Rows.longValue(row, "task_id")),
                        row -> Rows.longValue(row, "company_id"),
                        tenantMap),
                row -> {
                    Long companyId = Rows.longValue(row, "company_id");
                    Long tenantId = tenantForCompany(companyId, tenantMap);
                    Long oldTaskId = Rows.longValue(row, "task_id");
                    long taskId = mappingRepository.getOrCreateTargetId(sourceTable, companyId, oldTaskId,
                            "crm_task", tenantId);
                    Long mainUserId = Rows.longValue(row, "main_user_id");
                    Long assignedTo = resolveUserId(companyId, mainUserId, tenantId);
                    Long customerId = firstMappedCustomerFromIds(companyId, Rows.str(row, "relation_ids"));
                    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
                    values.put("task_id", taskId);
                    values.put("title", Rows.trimToLength(Rows.strOrDefault(row, "name", "未命名任务"), 255));
                    values.put("description", Rows.str(row, "description"));
                    values.put("due_date", Rows.timestamp(row, "stop_time"));
                    values.put("priority", mapPriority(Rows.intValue(row, "priority")));
                    values.put("status", mapTaskStatus(Rows.intValue(row, "status")));
                    values.put("assigned_to", assignedTo);
                    values.put("customer_id", customerId);
                    values.put("generated_by_ai", 0);
                    values.put("ai_context", null);
                    values.put("task_type", taskType);
                    values.put("participant_names", null);
                    values.put("high_value", false);
                    values.put("completed_time", Rows.timestamp(row, "finish_time"));
                    values.put("create_user_id", resolveUserId(companyId, Rows.longValue(row, "create_user_id"), tenantId));
                    values.put("update_user_id", resolveUserId(companyId, Rows.longValue(row, "update_user_id"), tenantId));
                    values.put("create_time", Rows.timestamp(row, "create_time"));
                    values.put("update_time", Rows.timestamp(row, "update_time"));
                    values.put("tenant_id", tenantId);
                    upsert("crm_task", "task_id", values);
                });
    }

    /**
     * 预先批量创建当前页的一对一映射，避免逐行写 sync_mapping。
     */
    private BatchPreprocessor preloadPrimaryMappings(String sourceTable,
                                                     String targetTable,
                                                     Function<Map<String, Object>, String> sourceIdExtractor,
                                                     Function<Map<String, Object>, Long> companyIdExtractor,
                                                     Map<Long, Long> tenantMap) {
        return rows -> preloadPrimaryMappings(rows, sourceTable, targetTable, sourceIdExtractor, companyIdExtractor, tenantMap);
    }

    private void preloadPrimaryMappings(List<Map<String, Object>> rows,
                                        String sourceTable,
                                        String targetTable,
                                        Function<Map<String, Object>, String> sourceIdExtractor,
                                        Function<Map<String, Object>, Long> companyIdExtractor,
                                        Map<Long, Long> tenantMap) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, List<String>> sourceIdsByCompany = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String sourceId = safeExtract(sourceIdExtractor, row);
            if (sourceId == null || sourceId.isBlank() || "null".equalsIgnoreCase(sourceId)) {
                continue;
            }
            Long companyId = safeExtract(companyIdExtractor, row);
            sourceIdsByCompany.computeIfAbsent(companyId, ignored -> new ArrayList<>()).add(sourceId);
        }
        sourceIdsByCompany.forEach((companyId, sourceIds) ->
                mappingRepository.getOrCreateTargetIds(sourceTable, companyId, sourceIds, targetTable,
                        tenantForCompany(companyId, tenantMap)));
    }

    /**
     * 角色模块会为每个角色补充固定菜单权限，提前批量创建权限映射。
     */
    private void preloadRoleMenuMappings(List<Map<String, Object>> rows, Map<Long, Long> tenantMap) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, List<String>> sourceIdsByCompany = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Long companyId = Rows.longValue(row, "company_id");
            Long oldRoleId = Rows.longValue(row, "role_id");
            if (oldRoleId == null) {
                continue;
            }
            for (Long menuId : MIGRATED_ROLE_VIEW_MENU_IDS) {
                if (menuExists(menuId)) {
                    sourceIdsByCompany.computeIfAbsent(companyId, ignored -> new ArrayList<>())
                            .add(oldRoleId + ":" + menuId);
                }
            }
        }
        sourceIdsByCompany.forEach((companyId, sourceIds) ->
                mappingRepository.getOrCreateTargetIds("wk_admin_role_menu", companyId, sourceIds,
                        "manager_role_menu", tenantForCompany(companyId, tenantMap)));
    }

    /**
     * 分页读取源表数据、逐行执行转换写入，并记录模块统计和行级错误。
     */
    private ModuleStats processRows(long jobId, String moduleName, String sourceTable, String targetTable,
                                    String orderBy, String whereClause,
                                    Function<Map<String, Object>, String> sourceIdExtractor,
                                    Function<Map<String, Object>, Long> companyIdExtractor,
                                    RowProcessor rowProcessor) {
        return processRows(jobId, moduleName, sourceTable, targetTable, orderBy, whereClause,
                sourceIdExtractor, companyIdExtractor, null, rowProcessor);
    }

    /**
     * 分页读取源表数据，允许在逐行处理前按页预热映射或依赖缓存。
     */
    private ModuleStats processRows(long jobId, String moduleName, String sourceTable, String targetTable,
                                    String orderBy, String whereClause,
                                    Function<Map<String, Object>, String> sourceIdExtractor,
                                    Function<Map<String, Object>, Long> companyIdExtractor,
                                    BatchPreprocessor batchPreprocessor,
                                    RowProcessor rowProcessor) {
        if (!oldTableExists(sourceTable)) {
            mappingRepository.finishModule(jobId, moduleName, sourceTable, targetTable, 0, 0, 0,
                    "source table skipped");
            log.warn("Source table {} does not exist, skipped module {}.", sourceTable, moduleName);
            return new ModuleStats();
        }
        String where = buildWhere(whereClause);
        Long total = oldCrm.queryForObject("SELECT COUNT(*) FROM " + sourceTable + where, Long.class);
        ModuleStats stats = new ModuleStats(total == null ? 0 : total, 0, 0);
        mappingRepository.startModule(jobId, moduleName, sourceTable, targetTable, stats.total);
        int offset = 0;
        beginBatchWrites();
        RuntimeException fatal = null;
        try {
            while (offset < stats.total) {
                List<Map<String, Object>> rows = oldCrm.queryForList(
                        "SELECT * FROM " + sourceTable + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
                        properties.getBatchSize(), offset);
                if (batchPreprocessor != null) {
                    batchPreprocessor.prepare(rows);
                }
                for (Map<String, Object> row : rows) {
                    try {
                        rowProcessor.process(row);
                        stats.success++;
                    } catch (Exception ex) {
                        stats.fail++;
                        String sourceId = safeExtract(sourceIdExtractor, row);
                        Long companyId = safeExtract(companyIdExtractor, row);
                        mappingRepository.recordError(jobId, moduleName, sourceTable, companyId, sourceId,
                                Rows.trimToLength(ex.getMessage(), 2000));
                        log.warn("Failed to sync {} row {}: {}", sourceTable, sourceId, ex.getMessage());
                    }
                }
                flushBatchWrites();
                offset += rows.size();
                if (rows.isEmpty()) {
                    break;
                }
                mappingRepository.updateModuleProgress(jobId, moduleName, stats.total, stats.success, stats.fail);
                log.info("Module {} progress: {}/{}", moduleName, Math.min(offset, stats.total), stats.total);
            }
        } catch (RuntimeException ex) {
            fatal = ex;
            mappingRepository.failModule(jobId, moduleName, sourceTable, targetTable,
                    stats.total, stats.success, stats.fail, Rows.trimToLength(ex.getMessage(), 2000));
            throw ex;
        } finally {
            try {
                if (fatal == null) {
                    flushBatchWrites();
                }
            } finally {
                batchWritesActive = false;
                pendingBatchUpdates.clear();
            }
        }
        mappingRepository.finishModule(jobId, moduleName, sourceTable, targetTable,
                stats.total, stats.success, stats.fail, "ok");
        log.info("Synced {}. total={}, success={}, fail={}", moduleName, stats.total, stats.success, stats.fail);
        return stats;
    }

    /**
     * 结合模块过滤条件和当前同步范围生成源表 WHERE 子句。
     */
    private String buildWhere(String whereClause) {
        List<String> conditions = new ArrayList<>();
        if (whereClause != null && !whereClause.isBlank()) {
            conditions.add("(" + whereClause + ")");
        }
        if (currentScope.isCompanyScoped()) {
            conditions.add("company_id = " + currentScope.sourceCompanyId());
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    /**
     * 生成当前同步范围内目标表映射 ID 的子查询 SQL。
     */
    private String mappingTargetsSql(String targetTable) {
        validateSafeIdentifier(targetTable);
        String sql = "SELECT target_id FROM sync_mapping WHERE target_table = '" + targetTable + "'";
        if (currentScope.isCompanyScoped()) {
            sql += " AND source_company_id = " + currentScope.sourceCompanyId();
        }
        return sql;
    }

    /**
     * 按主键对目标表执行插入或更新，并自动过滤目标表不存在的字段。
     */
    private void upsert(String tableName, String keyColumn, LinkedHashMap<String, Object> values) {
        if (!CRM_TARGET_TABLES.contains(tableName) && !"crm_global_search_index".equals(tableName)) {
            throw new IllegalArgumentException("Unexpected target table: " + tableName);
        }
        validateSafeIdentifier(keyColumn);
        values.keySet().forEach(this::validateSafeIdentifier);
        if (!values.containsKey(keyColumn)) {
            throw new IllegalStateException("Missing conflict key " + keyColumn + " for " + tableName);
        }
        String columns = String.join(", ", values.keySet());
        String placeholders = String.join(", ", values.keySet().stream().map(column -> "?").toList());
        List<String> updateColumns = values.keySet().stream()
                .filter(column -> !column.equals(keyColumn))
                .map(column -> column + " = EXCLUDED." + column)
                .toList();
        String conflict = updateColumns.isEmpty()
                ? " DO NOTHING"
                : " DO UPDATE SET " + String.join(", ", updateColumns);
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ") " +
                "ON CONFLICT (" + keyColumn + ")" + conflict;
        executeOrBatch(sql, values.values().toArray());
    }

    /**
     * Migrated roles must have matching menu permissions; data permission is read from manager_role_menu.data_scope.
     */
    private void ensureRoleViewPermissions(Long companyId, Long oldRoleId, long roleId, Long tenantId,
                                           Integer dataScope, Long createUserId, Long updateUserId) {
        if (oldRoleId == null) {
            return;
        }
        for (Long menuId : MIGRATED_ROLE_VIEW_MENU_IDS) {
            if (!menuExists(menuId)) {
                continue;
            }
            long roleMenuId = mappingRepository.getOrCreateTargetId(
                    "wk_admin_role_menu",
                    companyId,
                    oldRoleId + ":" + menuId,
                    "manager_role_menu",
                    tenantId
            );
            executeOrBatch("""
                    INSERT INTO manager_role_menu(
                        id, role_id, menu_id, create_user_id, update_user_id,
                        create_time, update_time, data_scope
                    )
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                    ON CONFLICT (id)
                    DO UPDATE SET role_id = EXCLUDED.role_id,
                                  menu_id = EXCLUDED.menu_id,
                                  update_user_id = EXCLUDED.update_user_id,
                                  update_time = CURRENT_TIMESTAMP,
                                  data_scope = EXCLUDED.data_scope
                    """, roleMenuId, roleId, menuId, createUserId, updateUserId, dataScope);
        }
    }

    private boolean menuExists(Long menuId) {
        if (menuId == null) {
            return false;
        }
        return menuExistsCache.computeIfAbsent(menuId, ignored -> {
            Integer count = target.queryForObject(
                    "SELECT COUNT(*) FROM manager_menu WHERE menu_id = ?",
                    Integer.class,
                    menuId
            );
            return count != null && count > 0;
        });
    }

    /**
     * 更新目标表中的动态自定义字段列。
     */
    private void updateDynamicColumn(String tableName, String idColumn, Long targetId, String columnName, Object value) {
        validateSafeIdentifier(tableName);
        validateSafeIdentifier(idColumn);
        validateSafeIdentifier(columnName);
        if (targetId == null) {
            return;
        }
        executeOrBatch("UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + idColumn + " = ?",
                value, targetId);
    }

    private void beginBatchWrites() {
        pendingBatchUpdates.clear();
        batchWritesActive = true;
    }

    private void executeOrBatch(String sql, Object... args) {
        if (!batchWritesActive) {
            target.update(sql, args);
            return;
        }
        pendingBatchUpdates.computeIfAbsent(sql, ignored -> new ArrayList<>()).add(args);
    }

    private void flushBatchWrites() {
        if (pendingBatchUpdates.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<Object[]>> entry : pendingBatchUpdates.entrySet()) {
            target.batchUpdate(entry.getKey(), entry.getValue());
        }
        pendingBatchUpdates.clear();
    }

    /**
     * 刷新客户表中的联系人数量、首要联系人和搜索文本等冗余字段。
     */
    private void refreshCustomerDenormalizedFields() {
        target.update("""
                UPDATE crm_customer c
                SET contact_count = COALESCE((
                    SELECT COUNT(*) FROM crm_contact ct WHERE ct.customer_id = c.customer_id
                ), 0)
                WHERE c.customer_id IN (%s)
                """.formatted(mappingTargetsSql("crm_customer")));
        target.update("""
                UPDATE crm_customer c
                SET primary_contact_name = (
                    SELECT ct.name
                    FROM crm_contact ct
                    WHERE ct.customer_id = c.customer_id
                    ORDER BY ct.is_primary DESC, ct.create_time ASC NULLS LAST, ct.contact_id ASC
                    LIMIT 1
                ),
                primary_contact_phone = (
                    SELECT ct.phone
                    FROM crm_contact ct
                    WHERE ct.customer_id = c.customer_id
                    ORDER BY ct.is_primary DESC, ct.create_time ASC NULLS LAST, ct.contact_id ASC
                    LIMIT 1
                ),
                primary_contact_position = (
                    SELECT ct.position
                    FROM crm_contact ct
                    WHERE ct.customer_id = c.customer_id
                    ORDER BY ct.is_primary DESC, ct.create_time ASC NULLS LAST, ct.contact_id ASC
                    LIMIT 1
                )
                WHERE c.customer_id IN (%s)
                """.formatted(mappingTargetsSql("crm_customer")));
        target.update("""
                UPDATE crm_customer
                SET search_text = LOWER(TRIM(CONCAT_WS(' ',
                    company_name, industry, source, address, website, remark,
                    primary_contact_name, primary_contact_phone, primary_contact_position, tag_names, level, stage
                )))
                WHERE customer_id IN (%s)
                """.formatted(mappingTargetsSql("crm_customer")));
    }

    /**
     * 将已同步的客户、联系人、任务和日程写入全局搜索索引。
     */
    private void refreshGlobalSearchIndex() {
        try {
            target.update("""
                    INSERT INTO crm_global_search_index (
                        tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
                        customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
                        upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
                    )
                    SELECT c.tenant_id, 'customer', c.customer_id, c.company_name,
                           'customer', LEFT(COALESCE(c.remark, ''), 200),
                           LOWER(TRIM(CONCAT_WS(' ', c.company_name, c.industry, c.source, c.address,
                               c.website, c.remark, c.primary_contact_name, c.primary_contact_phone,
                               c.primary_contact_position, c.tag_names, c.level, c.stage))),
                           c.customer_id, c.company_name, c.owner_id, c.owner_id, NULL,
                           NULL, c.create_user_id, NULL, '/customers/' || c.customer_id, c.update_time
                    FROM crm_customer c
                    WHERE c.customer_id IN (%s)
                    ON CONFLICT (tenant_id, entity_type, entity_id)
                    DO UPDATE SET title = EXCLUDED.title,
                                  subtitle = EXCLUDED.subtitle,
                                  summary = EXCLUDED.summary,
                                  search_text = EXCLUDED.search_text,
                                  customer_id = EXCLUDED.customer_id,
                                  customer_name = EXCLUDED.customer_name,
                                  owner_user_id = EXCLUDED.owner_user_id,
                                  customer_owner_id = EXCLUDED.customer_owner_id,
                                  create_user_id = EXCLUDED.create_user_id,
                                  route_path = EXCLUDED.route_path,
                                  sort_time = EXCLUDED.sort_time,
                                  update_time = CURRENT_TIMESTAMP
                    """.formatted(mappingTargetsSql("crm_customer")));
            target.update("""
                    INSERT INTO crm_global_search_index (
                        tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
                        customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
                        upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
                    )
                    SELECT ct.tenant_id, 'contact', ct.contact_id, ct.name,
                           COALESCE(c.company_name, 'contact'), LEFT(COALESCE(ct.notes, ''), 200),
                           LOWER(TRIM(CONCAT_WS(' ', ct.name, ct.position, ct.phone, ct.email, ct.wechat,
                               ct.notes, c.company_name))),
                           ct.customer_id, c.company_name, c.owner_id, c.owner_id, NULL,
                           NULL, ct.create_user_id, NULL, '/contacts/' || ct.contact_id, ct.update_time
                    FROM crm_contact ct
                    LEFT JOIN crm_customer c ON c.customer_id = ct.customer_id
                    WHERE ct.contact_id IN (%s)
                    ON CONFLICT (tenant_id, entity_type, entity_id)
                    DO UPDATE SET title = EXCLUDED.title,
                                  subtitle = EXCLUDED.subtitle,
                                  summary = EXCLUDED.summary,
                                  search_text = EXCLUDED.search_text,
                                  customer_id = EXCLUDED.customer_id,
                                  customer_name = EXCLUDED.customer_name,
                                  owner_user_id = EXCLUDED.owner_user_id,
                                  customer_owner_id = EXCLUDED.customer_owner_id,
                                  create_user_id = EXCLUDED.create_user_id,
                                  route_path = EXCLUDED.route_path,
                                  sort_time = EXCLUDED.sort_time,
                                  update_time = CURRENT_TIMESTAMP
                    """.formatted(mappingTargetsSql("crm_contact")));
            target.update("""
                    INSERT INTO crm_global_search_index (
                        tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
                        customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
                        upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
                    )
                    SELECT t.tenant_id, 'task', t.task_id, t.title,
                           COALESCE(c.company_name, 'task'), LEFT(COALESCE(t.description, ''), 200),
                           LOWER(TRIM(CONCAT_WS(' ', t.title, t.description, t.priority, t.status,
                               t.task_type, c.company_name))),
                           t.customer_id, c.company_name, c.owner_id, c.owner_id, t.assigned_to,
                           NULL, t.create_user_id, NULL, '/tasks/' || t.task_id, t.update_time
                    FROM crm_task t
                    LEFT JOIN crm_customer c ON c.customer_id = t.customer_id
                    WHERE t.task_id IN (%s)
                    ON CONFLICT (tenant_id, entity_type, entity_id)
                    DO UPDATE SET title = EXCLUDED.title,
                                  subtitle = EXCLUDED.subtitle,
                                  summary = EXCLUDED.summary,
                                  search_text = EXCLUDED.search_text,
                                  customer_id = EXCLUDED.customer_id,
                                  customer_name = EXCLUDED.customer_name,
                                  owner_user_id = EXCLUDED.owner_user_id,
                                  customer_owner_id = EXCLUDED.customer_owner_id,
                                  assigned_user_id = EXCLUDED.assigned_user_id,
                                  create_user_id = EXCLUDED.create_user_id,
                                  route_path = EXCLUDED.route_path,
                                  sort_time = EXCLUDED.sort_time,
                                  update_time = CURRENT_TIMESTAMP
                    """.formatted(mappingTargetsSql("crm_task")));
            target.update("""
                    INSERT INTO crm_global_search_index (
                        tenant_id, entity_type, entity_id, title, subtitle, summary, search_text,
                        customer_id, customer_name, owner_user_id, customer_owner_id, assigned_user_id,
                        upload_user_id, create_user_id, participant_user_ids, route_path, sort_time
                    )
                    SELECT s.tenant_id, 'schedule', s.schedule_id, s.title,
                           COALESCE(c.company_name, 'schedule'), LEFT(COALESCE(s.description, ''), 200),
                           LOWER(TRIM(CONCAT_WS(' ', s.title, s.description, s.type, s.location,
                               c.company_name, ct.name))),
                           s.customer_id, c.company_name, c.owner_id, c.owner_id, NULL,
                           NULL, s.create_user_id, s.participant_user_ids, '/schedules/' || s.schedule_id,
                           s.start_time
                    FROM crm_schedule s
                    LEFT JOIN crm_customer c ON c.customer_id = s.customer_id
                    LEFT JOIN crm_contact ct ON ct.contact_id = s.contact_id
                    WHERE s.schedule_id IN (%s)
                    ON CONFLICT (tenant_id, entity_type, entity_id)
                    DO UPDATE SET title = EXCLUDED.title,
                                  subtitle = EXCLUDED.subtitle,
                                  summary = EXCLUDED.summary,
                                  search_text = EXCLUDED.search_text,
                                  customer_id = EXCLUDED.customer_id,
                                  customer_name = EXCLUDED.customer_name,
                                  owner_user_id = EXCLUDED.owner_user_id,
                                  customer_owner_id = EXCLUDED.customer_owner_id,
                                  create_user_id = EXCLUDED.create_user_id,
                                  participant_user_ids = EXCLUDED.participant_user_ids,
                                  route_path = EXCLUDED.route_path,
                                  sort_time = EXCLUDED.sort_time,
                                  update_time = CURRENT_TIMESTAMP
                    """.formatted(mappingTargetsSql("crm_schedule")));
            log.info("Global search index refreshed for synced CRM rows.");
        } catch (Exception ex) {
            throw new IllegalStateException("Global search index refresh failed.", ex);
        }
    }

    /**
     * 根据 company_id 获取目标 tenant_id，缺失时创建兜底租户。
     */
    private Long tenantForCompany(Long companyId, Map<Long, Long> tenantMap) {
        Long safeCompanyId = companyId == null ? 0L : companyId;
        return tenantMap.computeIfAbsent(safeCompanyId, this::ensureSyntheticTenant);
    }

    /**
     * 清理一次同步过程中的运行时缓存和模块统计。
     */
    private void clearRuntimeCaches() {
        defaultUserCache.clear();
        contactCustomerCache.clear();
        primaryContactCache.clear();
        batchIdCache.clear();
        menuExistsCache.clear();
        customFieldReservedColumns.clear();
        lastStats = new ModuleStats();
    }

    /**
     * 将老系统用户 ID 转换为目标用户 ID，缺失时返回租户默认同步用户。
     */
    private Long resolveUserId(Long companyId, Long oldUserId, Long tenantId) {
        if (oldUserId != null && oldUserId > 0) {
            Long targetUserId = mappingRepository.findTargetId("wk_admin_user", companyId, oldUserId, "manager_user");
            if (targetUserId != null) {
                return targetUserId;
            }
        }
        return defaultUserForTenant(companyId, tenantId);
    }

    /**
     * 为租户创建或获取一个兜底同步用户。
     */
    private Long defaultUserForTenant(Long companyId, Long tenantId) {
        String key = tenantId == null ? "0" : String.valueOf(tenantId);
        return defaultUserCache.computeIfAbsent(key, ignored -> {
            long userId = mappingRepository.getOrCreateTargetId("sync_default_user", companyId, "tenant_" + key,
                    "manager_user", tenantId);
            LinkedHashMap<String, Object> values = new LinkedHashMap<>();
            values.put("user_id", userId);
            values.put("username", Rows.trimToLength("sync_" + key, 64));
            values.put("password", encodedResetPassword);
            values.put("salt", null);
            values.put("create_time", Timestamp.valueOf(LocalDateTime.now()));
            values.put("realname", "数据同步用户");
            values.put("num", "SYNC");
            values.put("mobile", null);
            values.put("email", null);
            values.put("sex", 0);
            values.put("dept_id", null);
            values.put("post", null);
            values.put("status", 2);
            values.put("parent_id", 0L);
            values.put("tenant_id", tenantId);
            upsert("manager_user", "user_id", values);
            return userId;
        });
    }

    /**
     * 将老系统用户 ID 列表转换为目标用户 ID 列表。
     */
    private String mapUserIdList(Long companyId, String oldUserIds, Long tenantId) {
        String parsed = Rows.parseIdList(oldUserIds);
        if (parsed == null) {
            return null;
        }
        List<String> mapped = new ArrayList<>();
        for (String part : parsed.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            Long userId = resolveUserId(companyId, Long.parseLong(part), tenantId);
            if (userId != null) {
                mapped.add(String.valueOf(userId));
            }
        }
        return Rows.commaJoin(mapped);
    }

    /**
     * 根据联系人 ID 查询其所属的老系统客户 ID，并缓存查询结果。
     */
    private Long findCustomerIdForContact(Long companyId, Long oldContactId) {
        if (oldContactId == null) {
            return null;
        }
        String key = companyId + ":" + oldContactId;
        return contactCustomerCache.computeIfAbsent(key, ignored -> {
            List<Long> ids = oldCrm.queryForList("""
                    SELECT customer_id
                    FROM wk_crm_contacts
                    WHERE contacts_id = ? AND (company_id = ? OR ? IS NULL)
                    LIMIT 1
                    """, Long.class, oldContactId, companyId, companyId);
            return ids.isEmpty() ? null : ids.getFirst();
        });
    }

    /**
     * 判断某个联系人是否为老系统客户记录上的首要联系人。
     */
    private boolean isPrimaryContact(Long companyId, Long oldCustomerId, Long oldContactId) {
        if (oldCustomerId == null || oldContactId == null) {
            return false;
        }
        String key = companyId + ":" + oldCustomerId + ":" + oldContactId;
        return primaryContactCache.computeIfAbsent(key, ignored -> {
            List<Long> ids = oldCrm.queryForList("""
                    SELECT contacts_id
                    FROM wk_crm_customer
                    WHERE customer_id = ? AND company_id = ?
                    LIMIT 1
                    """, Long.class, oldCustomerId, companyId);
            return !ids.isEmpty() && Objects.equals(ids.getFirst(), oldContactId);
        });
    }

    /**
     * 通过老系统自定义值 batch_id 反查客户或联系人实体 ID。
     */
    private Long findEntityIdByBatch(String sourceEntityTable, String targetTable, Long companyId, String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return null;
        }
        String key = sourceEntityTable + ":" + companyId + ":" + batchId;
        return batchIdCache.computeIfAbsent(key, ignored -> {
            String idColumn = "crm_customer".equals(targetTable) ? "customer_id" : "contacts_id";
            List<Long> ids = oldCrm.queryForList("SELECT " + idColumn + " FROM " + sourceEntityTable +
                    " WHERE batch_id = ? AND company_id = ? LIMIT 1", Long.class, batchId, companyId);
            return ids.isEmpty() ? null : ids.getFirst();
        });
    }

    /**
     * 从老任务关联 ID 列表中找到第一个已同步到目标库的客户 ID。
     */
    private Long firstMappedCustomerFromIds(Long companyId, String relationIds) {
        String parsed = Rows.parseIdList(relationIds);
        if (parsed == null) {
            return null;
        }
        for (String part : parsed.split(",")) {
            if (part.isBlank()) {
                continue;
            }
            Long customerId = mappingRepository.findTargetId("wk_crm_customer", companyId, Long.parseLong(part),
                    "crm_customer");
            if (customerId != null) {
                return customerId;
            }
        }
        return null;
    }

    /**
     * 从各个老系统业务表中汇总出现过的 company_id。
     */
    private List<Long> loadDistinctCompanyIds() {
        List<String> tables = List.of("wk_crm_customer", "wk_crm_contacts", "wk_admin_user", "wk_admin_dept",
                "wk_crm_activity", "wk_oa_event", "wk_project_task", "wk_work_task");
        List<Long> ids = new ArrayList<>();
        for (String table : tables) {
            if (!oldTableExists(table)) {
                continue;
            }
            ids.addAll(oldCrm.queryForList("SELECT DISTINCT company_id FROM " + table + " WHERE company_id IS NOT NULL",
                    Long.class));
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    /**
     * 检查老 wk_crm 数据库中是否存在指定表。
     */
    private boolean oldTableExists(String tableName) {
        Integer count = oldCrm.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * 校验全量同步所需的基础源表是否可用。
     */
    private void assertSourceReady() {
        if (!oldTableExists("wk_crm_customer")) {
            throw new IllegalStateException("Old CRM table wk_crm_customer does not exist. Check MySQL database wk_crm.");
        }
    }

    /**
     * dry-run 模式下输出源表行数，帮助确认同步范围。
     */
    private void logDryRunSummary(SyncScope scope) {
        List<String> tables = List.of("wk_admin_company", "wk_admin_dept", "wk_admin_user", "wk_admin_role",
                "wk_admin_user_role", "wk_crm_field", "wk_crm_customer", "wk_crm_contacts",
                "wk_crm_customer_data", "wk_crm_contacts_data", "wk_crm_activity", "wk_oa_event",
                "wk_project_task", "wk_work_task");
        for (String table : tables) {
            if (!oldTableExists(table)) {
                log.info("Dry-run: {} missing", table);
                continue;
            }
            String where = scope.isCompanyScoped() && oldTableHasCompanyId(table)
                    ? " WHERE company_id = " + scope.sourceCompanyId()
                    : "";
            Long count = oldCrm.queryForObject("SELECT COUNT(*) FROM " + table + where, Long.class);
            log.info("Dry-run: {} rows={}", table, count);
        }
    }

    /**
     * 判断老系统表是否包含 company_id 字段。
     */
    private boolean oldTableHasCompanyId(String tableName) {
        Integer count = oldCrm.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = 'company_id'
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * 基于客户基础字段拼接目标客户搜索文本。
     */
    private String buildCustomerSearchText(Map<String, Object> row) {
        String text = Rows.joinNonBlank(" ", Rows.str(row, "customer_name"), Rows.str(row, "mobile"),
                Rows.str(row, "telephone"), Rows.str(row, "email"), Rows.str(row, "website"),
                Rows.str(row, "address"), Rows.str(row, "location"), Rows.str(row, "detail_address"),
                Rows.str(row, "remark"), Rows.str(row, "last_content"));
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

    /**
     * 将老系统跟进分类映射为新系统跟进类型。
     */
    private String mapFollowUpType(String category) {
        if (category == null) {
            return "other";
        }
        String normalized = category.toLowerCase(Locale.ROOT);
        if (normalized.contains("call") || normalized.contains("电话")) {
            return "call";
        }
        if (normalized.contains("mail") || normalized.contains("email") || normalized.contains("邮件")) {
            return "email";
        }
        if (normalized.contains("visit") || normalized.contains("拜访")) {
            return "visit";
        }
        if (normalized.contains("meeting") || normalized.contains("会议")) {
            return "meeting";
        }
        return "other";
    }

    /**
     * 将老系统任务优先级映射为新系统优先级枚举。
     */
    private String mapPriority(Integer priority) {
        if (priority == null) {
            return "medium";
        }
        if (priority >= 3) {
            return "high";
        }
        if (priority == 1) {
            return "low";
        }
        return "medium";
    }

    /**
     * 将老系统任务状态映射为新系统任务状态。
     */
    private String mapTaskStatus(Integer status) {
        if (status == null) {
            return "pending";
        }
        return switch (status) {
            case 2 -> "in_progress";
            case 3, 5, 6 -> "completed";
            default -> "pending";
        };
    }

    /**
     * 将老系统自定义字段类型映射为新系统字段类型。
     */
    private CustomFieldDefinition mapCustomFieldDefinition(Integer oldType) {
        return oldType == null ? null : SUPPORTED_WK_CUSTOM_FIELD_TYPES.get(oldType);
    }

    private FieldPoolSlot acquireCustomFieldPoolSlot(String entityType, CustomFieldDefinition definition,
                                                     Long tenantId, long targetFieldId) {
        FieldPoolSlot existing = findReusableCustomFieldSlot(targetFieldId, definition.fieldType());
        if (existing != null) {
            reserveCustomFieldColumn(tenantId, entityType, existing.columnName());
            ensureCustomFieldPoolColumn(entityType, existing);
            return existing;
        }

        Set<String> usedColumns = usedCustomFieldColumns(entityType, tenantId);
        usedColumns.addAll(reservedCustomFieldColumns(tenantId, entityType));

        for (Map<String, Object> row : target.queryForList("""
                SELECT column_name, column_type, field_type
                FROM crm_custom_field_pool
                WHERE entity_type = ? AND field_type = ?
                ORDER BY create_time ASC, pool_id ASC
                """, entityType, definition.fieldType())) {
            String columnName = Rows.str(row, "column_name");
            if (columnName == null || usedColumns.contains(columnName)) {
                continue;
            }
            FieldPoolSlot slot = new FieldPoolSlot(columnName,
                    Rows.strOrDefault(row, "column_type", definition.columnType()),
                    definition.fieldType());
            reserveCustomFieldColumn(tenantId, entityType, slot.columnName());
            ensureCustomFieldPoolColumn(entityType, slot);
            return slot;
        }

        FieldPoolSlot slot = createCustomFieldPoolSlot(entityType, definition);
        reserveCustomFieldColumn(tenantId, entityType, slot.columnName());
        ensureCustomFieldPoolColumn(entityType, slot);
        return slot;
    }

    private FieldPoolSlot findReusableCustomFieldSlot(long targetFieldId, String expectedFieldType) {
        List<Map<String, Object>> rows = target.queryForList("""
                SELECT column_name, column_type, field_type
                FROM crm_custom_field
                WHERE field_id = ?
                LIMIT 1
                """, targetFieldId);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.getFirst();
        String columnName = Rows.str(row, "column_name");
        String fieldType = Rows.str(row, "field_type");
        if (columnName == null || !columnName.startsWith(CUSTOM_FIELD_POOL_COLUMN_PREFIX)
                || !Objects.equals(fieldType, expectedFieldType)) {
            return null;
        }
        return new FieldPoolSlot(columnName, Rows.str(row, "column_type"), fieldType);
    }

    private Set<String> usedCustomFieldColumns(String entityType, Long tenantId) {
        Set<String> columns = new HashSet<>();
        List<String> used;
        if (tenantId != null) {
            used = target.queryForList("""
                    SELECT column_name
                    FROM crm_custom_field
                    WHERE entity_type = ? AND tenant_id = ?
                    """, String.class, entityType, tenantId);
        } else {
            used = target.queryForList("""
                    SELECT column_name
                    FROM crm_custom_field
                    WHERE entity_type = ?
                    """, String.class, entityType);
        }
        used.stream().filter(Objects::nonNull).forEach(columns::add);
        return columns;
    }

    private Set<String> reservedCustomFieldColumns(Long tenantId, String entityType) {
        String prefix = customFieldReservationPrefix(tenantId, entityType);
        Set<String> columns = new HashSet<>();
        for (String key : customFieldReservedColumns) {
            if (key.startsWith(prefix)) {
                columns.add(key.substring(prefix.length()));
            }
        }
        return columns;
    }

    private void reserveCustomFieldColumn(Long tenantId, String entityType, String columnName) {
        customFieldReservedColumns.add(customFieldReservationPrefix(tenantId, entityType) + columnName);
    }

    private String customFieldReservationPrefix(Long tenantId, String entityType) {
        return (tenantId == null ? "0" : tenantId) + ":" + entityType + ":";
    }

    private FieldPoolSlot createCustomFieldPoolSlot(String entityType, CustomFieldDefinition definition) {
        for (int i = 0; i < CUSTOM_FIELD_POOL_MAX_ATTEMPTS; i++) {
            String columnName = CUSTOM_FIELD_POOL_COLUMN_PREFIX + randomCustomFieldPoolSuffix();
            if (customFieldPoolColumnExists(entityType, columnName)) {
                continue;
            }
            FieldPoolSlot slot = new FieldPoolSlot(columnName, definition.columnType(), definition.fieldType());
            ensureCustomFieldPoolRecord(entityType, slot);
            log.info("Created custom field pool slot. entityType={}, columnName={}, fieldType={}",
                    entityType, slot.columnName(), slot.fieldType());
            return slot;
        }
        throw new IllegalStateException("Unable to create a unique custom field pool column for " + entityType);
    }

    private boolean customFieldPoolColumnExists(String entityType, String columnName) {
        Integer count = target.queryForObject("""
                SELECT COUNT(*)
                FROM crm_custom_field_pool
                WHERE entity_type = ? AND column_name = ?
                """, Integer.class, entityType, columnName);
        return count != null && count > 0;
    }

    private String randomCustomFieldPoolSuffix() {
        StringBuilder builder = new StringBuilder(CUSTOM_FIELD_POOL_SUFFIX_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < CUSTOM_FIELD_POOL_SUFFIX_LENGTH; i++) {
            builder.append(CUSTOM_FIELD_POOL_CHARS.charAt(random.nextInt(CUSTOM_FIELD_POOL_CHARS.length())));
        }
        return builder.toString();
    }

    private void ensureCustomFieldPoolColumn(String entityType, FieldPoolSlot slot) {
        targetSchema.addCustomFieldColumnIfMissing(customFieldTargetTable(entityType), slot.columnName(), slot.columnType());
        ensureCustomFieldPoolRecord(entityType, slot);
    }

    private void ensureCustomFieldPoolRecord(String entityType, FieldPoolSlot slot) {
        target.update("""
                INSERT INTO crm_custom_field_pool(
                    pool_id, entity_type, column_name, column_type, field_type, column_created, create_time
                )
                VALUES (?, ?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP)
                ON CONFLICT (entity_type, column_name)
                DO UPDATE SET column_type = EXCLUDED.column_type,
                              field_type = EXCLUDED.field_type,
                              column_created = TRUE
                """, mappingRepository.nextId(), entityType, slot.columnName(), slot.columnType(), slot.fieldType());
    }

    private String customFieldTargetTable(String entityType) {
        return switch (entityType) {
            case "customer" -> "crm_customer";
            case "contact" -> "crm_contact";
            default -> throw new IllegalArgumentException("Unsupported custom field entity type: " + entityType);
        };
    }

    private Object normalizeCustomFieldValue(String value, String fieldType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return switch (fieldType) {
            case "text" -> Rows.trimToLength(trimmed, 500);
            case "select" -> Rows.trimToLength(trimmed, 100);
            case "number" -> new BigDecimal(trimmed.replace(",", ""));
            case "date" -> Date.valueOf(trimmed.length() > 10 ? trimmed.substring(0, 10) : trimmed);
            case "datetime" -> Timestamp.valueOf(normalizeTimestampText(trimmed));
            case "checkbox" -> normalizeBooleanText(trimmed);
            default -> trimmed;
        };
    }

    private String normalizeTimestampText(String value) {
        String normalized = value.replace('T', ' ');
        if (normalized.length() == 10) {
            return normalized + " 00:00:00";
        }
        if (normalized.length() == 16) {
            return normalized + ":00";
        }
        return normalized;
    }

    private Boolean normalizeBooleanText(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (Set.of("1", "true", "yes", "y", "on", "是", "开", "开启").contains(normalized)) {
            return true;
        }
        if (Set.of("0", "false", "no", "n", "off", "否", "关", "关闭").contains(normalized)) {
            return false;
        }
        throw new IllegalArgumentException("Unsupported boolean value: " + value);
    }

    /**
     * 规范化角色数据权限类型，异常值统一降级为本人数据权限。
     */
    private Integer normalizeDataType(Integer dataType) {
        if (dataType == null || dataType < 1 || dataType > 5) {
            return 5;
        }
        return dataType;
    }

    /**
     * 规范化老系统用户状态到新系统用户状态。
     */
    private Integer normalizeUserStatus(Integer status) {
        if (status == null) {
            return 2;
        }
        return switch (status) {
            case 0 -> 0;
            case 1 -> 1;
            default -> 2;
        };
    }

    /**
     * 时间为空时使用当前时间作为兜底值。
     */
    private Timestamp valueOrNow(Timestamp timestamp) {
        return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()) : timestamp;
    }

    /**
     * 将老系统字段选项转换为新系统可保存的 JSON 文本。
     */
    private String toOptionsJson(String options) {
        if (options == null || options.isBlank()) {
            return null;
        }
        String trimmed = options.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return trimmed;
        }
        List<Map<String, String>> values = java.util.Arrays.stream(trimmed.split("[,，\\n]"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> Map.of("value", value, "label", value))
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /**
     * 将任意字段名清洗为安全的小写 SQL 标识符片段。
     */
    private String sanitizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "field";
        }
        String sanitized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        if (sanitized.isBlank() || Character.isDigit(sanitized.charAt(0))) {
            sanitized = "f_" + sanitized;
        }
        return sanitized;
    }

    /**
     * 清洗并截断动态字段标识符，确保长度和格式符合目标库要求。
     */
    private String trimIdentifier(String identifier) {
        String sanitized = sanitizeIdentifier(identifier);
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        validateSafeIdentifier(sanitized);
        return sanitized;
    }

    /**
     * 校验动态 SQL 中使用的表名或字段名是否安全。
     */
    private void validateSafeIdentifier(String value) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Unsafe identifier: " + value);
        }
    }

    /**
     * 安全执行字段提取器，提取失败时返回 null 以便继续记录错误。
     */
    private <T> T safeExtract(Function<Map<String, Object>, T> extractor, Map<String, Object> row) {
        try {
            return extractor.apply(row);
        } catch (Exception ignored) {
            return null;
        }
    }

    @FunctionalInterface
    private interface RowProcessor {
        /**
         * 处理一行源数据并写入目标库。
         */
        void process(Map<String, Object> row) throws Exception;
    }

    @FunctionalInterface
    private interface BatchPreprocessor {
        /**
         * 在当前分页逐行处理前预热映射或依赖缓存。
         */
        void prepare(List<Map<String, Object>> rows);
    }

    private static final class ModuleStats {
        private long total;
        private long success;
        private long fail;

        /**
         * 创建空的模块统计对象。
         */
        private ModuleStats() {
        }

        /**
         * 使用指定总数、成功数和失败数创建模块统计对象。
         */
        private ModuleStats(long total, long success, long fail) {
            this.total = total;
            this.success = success;
            this.fail = fail;
        }
    }

    /**
     * 老系统自定义字段在 company_id 范围内的唯一键。
     */
    private record OldFieldKey(Long companyId, Long fieldId) {
    }

    /**
     * 已同步自定义字段在目标库中的元数据。
     */
    private record CustomFieldMeta(String entityType, Long companyId, Long oldFieldId, Long targetFieldId,
                                   String columnName, String fieldType) {
    }

    /**
     * 描述当前全量同步范围，支持全库或单 company_id。
     */
    private record CustomFieldDefinition(String fieldType, String columnType) {
    }

    private record FieldPoolSlot(String columnName, String columnType, String fieldType) {
    }

    private record SyncScope(Long tenantId, Long sourceCompanyId, Long bindingId) {

        /**
         * 创建全库同步范围。
         */
        private static SyncScope all() {
            return new SyncScope(null, null, null);
        }

        /**
         * 创建指定租户和 company_id 的同步范围。
         */
        private static SyncScope company(Long tenantId, Long sourceCompanyId, Long bindingId) {
            return new SyncScope(tenantId, sourceCompanyId, bindingId);
        }

        /**
         * 判断当前范围是否限定到单个 company_id。
         */
        private boolean isCompanyScoped() {
            return tenantId != null && sourceCompanyId != null;
        }
    }
}
