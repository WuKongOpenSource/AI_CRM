package com.kakarote.syncdata.service;

import com.kakarote.syncdata.SyncProperties;
import com.kakarote.syncdata.db.CompanyBindingRepository;
import com.kakarote.syncdata.mq.RocketMqSyncSettings;
import com.kakarote.syncdata.model.CompanyBinding;
import com.kakarote.syncdata.model.OldCompanyOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CompanyBindingService {

    private final JdbcTemplate oldCrm;
    private final CompanyBindingRepository bindingRepository;
    private final SyncProperties properties;

    /**
     * 注入老库读取、目标库结构准备、绑定关系持久化和默认同步配置。
     */
    public CompanyBindingService(@Qualifier("oldCrmJdbcTemplate") JdbcTemplate oldCrmJdbcTemplate,
                                 CompanyBindingRepository bindingRepository,
                                 SyncProperties properties) {
        this.oldCrm = oldCrmJdbcTemplate;
        this.bindingRepository = bindingRepository;
        this.properties = properties;
    }

    /**
     * 从 wk_crm 源库统计各 company_id 的基础数据量，生成可绑定公司列表。
     */
    public List<OldCompanyOption> listOldCompanies() {
        return listOldCompanies(null);
    }

    /**
     * 从 wk_crm 源库统计各 company_id 的基础数据量，生成可绑定公司列表。
     * 传入手机号时，仅返回 wk_admin_company.company_manage 等于该手机号的企业。
     * 企业展示名称来自 wk_admin_config 中 name = companyName 的 value。
     */
    public List<OldCompanyOption> listOldCompanies(String managerPhone) {
        String normalizedManagerPhone = normalizePhone(managerPhone);
        Map<Long, CompanyStats> stats = new LinkedHashMap<>();
        Set<Long> allowedCompanyIds = loadManagedCompanies(stats, normalizedManagerPhone);
        if (normalizedManagerPhone != null && allowedCompanyIds.isEmpty()) {
            return List.of();
        }

        mergeCount(stats, "wk_crm_customer", "customerCount", allowedCompanyIds);
        mergeCount(stats, "wk_crm_contacts", "contactCount", allowedCompanyIds);
        mergeCount(stats, "wk_admin_user", "userCount", allowedCompanyIds);
        mergeCount(stats, "wk_crm_activity", "followUpCount", allowedCompanyIds);

        loadCompanyNames(stats, normalizedManagerPhone);

        List<OldCompanyOption> options = new ArrayList<>();
        for (Map.Entry<Long, CompanyStats> entry : stats.entrySet()) {
            Long companyId = entry.getKey();
            CompanyStats stat = entry.getValue();
            options.add(new OldCompanyOption(
                    companyId,
                    stat.companyName == null ? "WK CRM " + companyId : stat.companyName,
                    stat.customerCount,
                    stat.contactCount,
                    stat.userCount,
                    stat.followUpCount
            ));
        }
        return options.stream()
                .sorted(java.util.Comparator.comparing(OldCompanyOption::companyId))
                .toList();
    }

    /**
     * 根据手机号提前加载可管理的公司，作为后续统计聚合的白名单。
     */
    private Set<Long> loadManagedCompanies(Map<Long, CompanyStats> stats, String managerPhone) {
        Set<Long> allowedCompanyIds = new LinkedHashSet<>();
        if (managerPhone == null) {
            return allowedCompanyIds;
        }
        if (!oldTableExists("wk_admin_company") || !oldColumnExists("wk_admin_company", "company_manage")) {
            return allowedCompanyIds;
        }
        List<Map<String, Object>> companies = oldCrm.queryForList("""
                SELECT company_id, company_manage
                FROM wk_admin_company
                WHERE company_id IS NOT NULL
                  AND company_manage = ?
                ORDER BY company_id
                """, managerPhone);
        for (Map<String, Object> row : companies) {
            Long companyId = ((Number) row.get("company_id")).longValue();
            allowedCompanyIds.add(companyId);
            stats.computeIfAbsent(companyId, ignored -> new CompanyStats());
        }
        return allowedCompanyIds;
    }

    /**
     * 从 wk_admin_config 补充公司展示名称，按 company_id 读取 name = companyName 的 value。
     */
    private void loadCompanyNames(Map<Long, CompanyStats> stats, String managerPhone) {
        if (oldTableExists("wk_admin_config")
                && oldColumnExists("wk_admin_config", "company_id")
                && oldColumnExists("wk_admin_config", "name")
                && oldColumnExists("wk_admin_config", "value")) {
            List<Map<String, Object>> configs = loadCompanyNameConfigs(stats, managerPhone);
            for (Map<String, Object> row : configs) {
                Long companyId = rowLong(row, "company_id");
                if (companyId == null) {
                    continue;
                }
                CompanyStats companyStats = stats.computeIfAbsent(companyId, ignored -> new CompanyStats());
                companyStats.companyName = rowString(row, "company_name");
            }
        }

        if (managerPhone == null) {
            loadFallbackCompanyIds(stats);
        }
    }

    /**
     * 查询 wk_admin_config 中保存的企业名称；手机号过滤场景只查询已经允许的 company_id。
     */
    private List<Map<String, Object>> loadCompanyNameConfigs(Map<Long, CompanyStats> stats, String managerPhone) {
        if (managerPhone != null) {
            if (stats.isEmpty()) {
                return List.of();
            }
            String placeholders = String.join(", ", stats.keySet().stream().map(ignored -> "?").toList());
            List<Object> params = new ArrayList<>();
            params.add("companyName");
            params.addAll(stats.keySet());
            return oldCrm.queryForList("""
                    SELECT company_id, `value` AS company_name
                    FROM wk_admin_config
                    WHERE company_id IS NOT NULL
                      AND `name` = ?
                      AND company_id IN (%s)
                    ORDER BY company_id
                    """.formatted(placeholders), params.toArray());
        }

        return oldCrm.queryForList("""
                SELECT company_id, `value` AS company_name
                FROM wk_admin_config
                WHERE company_id IS NOT NULL
                  AND `name` = ?
                ORDER BY company_id
                """, "companyName");
    }

    /**
     * 当配置表没有企业名称时，仍保留 wk_admin_company 中存在的企业 ID。
     */
    private void loadFallbackCompanyIds(Map<Long, CompanyStats> stats) {
        if (oldTableExists("wk_admin_company") && oldColumnExists("wk_admin_company", "company_id")) {
            List<Map<String, Object>> companies = oldCrm.queryForList("""
                    SELECT company_id
                    FROM wk_admin_company
                    WHERE company_id IS NOT NULL
                    ORDER BY company_id
                    """);
            for (Map<String, Object> row : companies) {
                Long companyId = rowLong(row, "company_id");
                if (companyId != null) {
                    stats.computeIfAbsent(companyId, ignored -> new CompanyStats());
                }
            }
        }
    }

    /**
     * 初始化同步元数据表，并返回所有已配置的绑定关系。
     */
    public List<CompanyBinding> listBindings() {
        return bindingRepository.listBindings();
    }

    /**
     * 创建或更新 ai_crm 租户到 wk_crm company_id 的绑定关系。
     */
    public CompanyBinding bind(Long tenantId, Long companyId, Boolean incrementalEnabled,
                               String mqTopic, String mqGroup, String remark) {
        return bind(tenantId, companyId, incrementalEnabled, null, mqTopic, mqGroup, null, null, remark);
    }

    public CompanyBinding bind(Long tenantId, Long companyId,
                               Boolean crmToAicrmEnabled,
                               Boolean aicrmToCrmEnabled,
                               String crmToAicrmTopic,
                               String crmToAicrmGroup,
                               String aicrmToCrmTopic,
                               String aicrmToCrmGroup,
                               String remark) {
        String companyName = listOldCompanies().stream()
                .filter(option -> option.companyId().equals(companyId))
                .map(OldCompanyOption::companyName)
                .findFirst()
                .orElse("WK CRM " + companyId);
        String unifiedTopic = firstNonBlank(
                crmToAicrmTopic,
                aicrmToCrmTopic,
                RocketMqSyncSettings.topic(properties),
                properties.getRocketmq().getCrmToAicrm().getTopic(),
                properties.getRocketmq().getAicrmToCrm().getTopic(),
                properties.getIncremental().getMq().getTopic()
        );
        return bindingRepository.upsertBinding(
                tenantId,
                companyId,
                companyName,
                Boolean.TRUE.equals(crmToAicrmEnabled),
                Boolean.TRUE.equals(aicrmToCrmEnabled),
                unifiedTopic,
                firstNonBlank(crmToAicrmGroup,
                        RocketMqSyncSettings.crmToAicrmGroup(properties),
                        properties.getIncremental().getMq().getConsumerGroup()),
                unifiedTopic,
                firstNonBlank(aicrmToCrmGroup, RocketMqSyncSettings.aicrmToCrmGroup(properties)),
                remark
        );
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据绑定编号加载绑定关系，不存在时抛出明确异常。
     */
    public CompanyBinding getBinding(Long bindingId) {
        CompanyBinding binding = bindingRepository.findById(bindingId);
        if (binding == null) {
            throw new IllegalArgumentException("Binding does not exist: " + bindingId);
        }
        return binding;
    }

    /**
     * 将源表按 company_id 聚合后的数量合并到公司统计结果中。
     */
    private void mergeCount(Map<Long, CompanyStats> stats, String tableName, String field, Set<Long> allowedCompanyIds) {
        if (!oldTableExists(tableName)) {
            return;
        }
        List<Map<String, Object>> rows = oldCrm.queryForList("""
                SELECT company_id, COUNT(*) AS row_count
                FROM %s
                WHERE company_id IS NOT NULL
                GROUP BY company_id
                """.formatted(tableName));
        for (Map<String, Object> row : rows) {
            Long companyId = ((Number) row.get("company_id")).longValue();
            if (!allowedCompanyIds.isEmpty() && !allowedCompanyIds.contains(companyId)) {
                continue;
            }
            Long count = ((Number) row.get("row_count")).longValue();
            CompanyStats companyStats = stats.computeIfAbsent(companyId, ignored -> new CompanyStats());
            switch (field) {
                case "customerCount" -> companyStats.customerCount = count;
                case "contactCount" -> companyStats.contactCount = count;
                case "userCount" -> companyStats.userCount = count;
                case "followUpCount" -> companyStats.followUpCount = count;
                default -> throw new IllegalArgumentException("Unsupported count field: " + field);
            }
        }
    }

    /**
     * 读取 Map 中的字符串字段，并统一清理空白字符串。
     */
    private String rowString(Map<String, Object> row, String field) {
        Object value = row.get(field);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 读取 Map 中的数字 ID 字段。
     */
    private Long rowLong(Map<String, Object> row, String field) {
        Object value = row.get(field);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 统一清理前端传入的手机号，避免首尾空格导致匹配不到 company_manage。
     */
    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 检查当前连接的 wk_crm 数据库中是否存在指定源表。
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
     * 检查当前连接的 wk_crm 数据库指定源表中是否存在指定字段。
     */
    private boolean oldColumnExists(String tableName, String columnName) {
        Integer count = oldCrm.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private static final class CompanyStats {
        private String companyName;
        private Long customerCount = 0L;
        private Long contactCount = 0L;
        private Long userCount = 0L;
        private Long followUpCount = 0L;
    }
}
