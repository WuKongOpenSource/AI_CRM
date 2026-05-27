package com.kakarote.syncdata.model;

import java.sql.Timestamp;

/**
 * sync_company_binding 表中的租户与公司绑定关系只读视图。
 */
public record CompanyBinding(
        Long bindingId,
        Long tenantId,
        String sourceSystem,
        String sourceDb,
        Long sourceCompanyId,
        String sourceCompanyName,
        String syncDirection,
        String fullSyncStatus,
        Long fullSyncJobId,
        Timestamp lastFullSyncAt,
        Boolean incrementalEnabled,
        String mqTopic,
        String mqGroup,
        Timestamp lastIncrementalEventTime,
        String lastIncrementalOffset,
        Boolean crmToAicrmEnabled,
        Boolean aicrmToCrmEnabled,
        String crmToAicrmTopic,
        String crmToAicrmGroup,
        String aicrmToCrmTopic,
        String aicrmToCrmGroup,
        Timestamp lastCrmToAicrmEventTime,
        String lastCrmToAicrmOffset,
        Timestamp lastAicrmToCrmEventTime,
        String lastAicrmToCrmOffset,
        Integer status,
        String remark,
        Timestamp createTime,
        Timestamp updateTime
) {
}
