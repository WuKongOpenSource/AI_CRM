import type {
  MigrationPreflightResult,
  SyncJobErrorRecord,
  SyncJobModuleRecord,
  SyncJobRecord
} from '../syncData'

export const migrationPreflightReadyFixture: MigrationPreflightResult = {
  tenantId: '1',
  companyId: '100',
  ready: true,
  errors: [],
  warnings: [
    {
      code: 'module_unavailable',
      module: 'contracts',
      message: '合同源数据暂不会迁移。'
    }
  ],
  modules: [
    {
      key: 'customers',
      label: '客户',
      sourceTable: 'wk_crm_customer',
      targetTable: 'crm_customer',
      status: 'supported',
      rowCount: 12,
      message: '将参与全量迁移。'
    },
    {
      key: 'contracts',
      label: '合同',
      sourceTable: 'wk_crm_contract',
      targetTable: null,
      status: 'unavailable',
      rowCount: 3,
      message: '目标业务模型暂未接入，当前不会迁移。'
    }
  ],
  rowCounts: {
    customers: 12,
    contracts: 3
  },
  rerun: {
    existingBinding: true,
    existingMappings: true,
    mappingCount: 12,
    message: '检测到已有映射，重复执行会复用目标 ID 并执行更新。'
  },
  cleanup: {
    enabled: false,
    message: '未启用同步前清理，重复执行将按 sync_mapping 幂等更新目标数据。'
  },
  incremental: {
    applicationAvailable: false,
    status: 'reserved',
    message: '增量事件目前仅审计记录，尚未实现对目标业务表的增删改应用。'
  }
}

export const runningSyncJobFixture: SyncJobRecord = {
  job_id: '9001',
  sync_mode: 'full',
  status: 'running',
  total_count: 100,
  success_count: 25,
  fail_count: 0
}

export const runningSyncModuleFixture: SyncJobModuleRecord = {
  module_name: 'customers',
  status: 'running',
  total_count: 100,
  success_count: 25,
  fail_count: 0
}

export const syncJobErrorFixture: SyncJobErrorRecord = {
  id: '1',
  module_name: 'customers',
  source_table: 'wk_crm_customer',
  source_company_id: '100',
  source_id: '200',
  error_message: 'Missing required customer name'
}
