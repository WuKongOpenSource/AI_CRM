import axios, { AxiosInstance } from 'axios'

export type SyncSafeId = string
export type SyncCountValue = number | string

export interface OldCompanyOption {
  companyId: SyncSafeId
  companyName: string
  customerCount: SyncCountValue
  contactCount: SyncCountValue
  userCount: SyncCountValue
  followUpCount: SyncCountValue
}

export interface CompanyBinding {
  bindingId: SyncSafeId
  tenantId: SyncSafeId
  sourceSystem: string
  sourceDb: string
  sourceCompanyId: SyncSafeId
  sourceCompanyName?: string
  syncDirection: string
  fullSyncStatus: string
  fullSyncJobId?: SyncSafeId | null
  lastFullSyncAt?: string | null
  incrementalEnabled: boolean
  lastIncrementalEventTime?: string | null
  lastIncrementalOffset?: string | null
  crmToAicrmEnabled?: boolean
  aicrmToCrmEnabled?: boolean
  lastCrmToAicrmEventTime?: string | null
  lastCrmToAicrmOffset?: string | null
  lastAicrmToCrmEventTime?: string | null
  lastAicrmToCrmOffset?: string | null
  status: number
  remark?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface BindCompanyRequest {
  tenantId: SyncSafeId
  companyId: SyncSafeId
  incrementalEnabled?: boolean
  crmToAicrmEnabled?: boolean
  aicrmToCrmEnabled?: boolean
  remark?: string
}

export interface StartFullSyncResult {
  jobId: SyncSafeId
  bindingId: SyncSafeId
  status: string
}

export interface SyncPreflightIssue {
  code: string
  message: string
  module?: string | null
}

export interface MigrationModuleCoverage {
  key: string
  label: string
  sourceTable: string
  targetTable?: string | null
  status: 'supported' | 'skipped' | 'unavailable' | 'blocked' | string
  rowCount: SyncCountValue
  message?: string | null
}

export interface SyncRerunInfo {
  existingBinding: boolean
  existingMappings: boolean
  mappingCount: SyncCountValue
  message: string
}

export interface SyncCleanupInfo {
  enabled: boolean
  message: string
}

export interface SyncIncrementalCapability {
  applicationAvailable: boolean
  status: string
  message: string
  crmToAicrmAvailable?: boolean
  aicrmToCrmAvailable?: boolean
  conflictPolicy?: string
}

export interface MigrationPreflightResult {
  tenantId: SyncSafeId
  companyId: SyncSafeId
  ready: boolean
  errors: SyncPreflightIssue[]
  warnings: SyncPreflightIssue[]
  modules: MigrationModuleCoverage[]
  rowCounts: Record<string, SyncCountValue>
  rerun: SyncRerunInfo
  cleanup: SyncCleanupInfo
  incremental: SyncIncrementalCapability
}

export interface SyncCapabilities {
  incrementalApplicationAvailable: boolean
  incrementalStatus: string
  incrementalMessage: string
  crmToAicrmAvailable?: boolean
  aicrmToCrmAvailable?: boolean
}

export type SyncJobRecord = Record<string, unknown>
export type SyncJobModuleRecord = Record<string, unknown>
export type SyncJobErrorRecord = Record<string, unknown>
export type SyncApiError = Error & { status?: number }

function getSyncApiBaseUrl(): string {
  const raw = import.meta.env.VITE_SYNC_API_BASE_URL
  if (typeof raw !== 'string' || !raw.trim()) {
    return '/syncapi'
  }
  return raw.trim().replace(/\/$/, '')
}

const syncService: AxiosInstance = axios.create({
  baseURL: getSyncApiBaseUrl(),
  timeout: 600000,
  headers: {
    'Content-Type': 'application/json'
  }
})

syncService.interceptors.response.use(
  response => response.data,
  error => {
    const message = error.response?.data?.message
      || error.response?.data?.msg
      || error.message
      || '同步服务请求失败'
    const syncError = new Error(message) as SyncApiError
    syncError.status = error.response?.status
    return Promise.reject(syncError)
  }
)

export function queryOldCompanies(managerPhone?: string): Promise<OldCompanyOption[]> {
  const phone = managerPhone?.trim()
  return syncService.get('/sync/old-companies', {
    params: phone ? { managerPhone: phone } : undefined
  })
}

export function queryCompanyBindings(): Promise<CompanyBinding[]> {
  return syncService.get('/sync/bindings')
}

export function bindCompany(data: BindCompanyRequest): Promise<CompanyBinding> {
  return syncService.post('/sync/bindings', data)
}

export function queryMigrationPreflight(params: {
  tenantId: SyncSafeId
  companyId: SyncSafeId
  incrementalEnabled?: boolean
}): Promise<MigrationPreflightResult> {
  return syncService.get('/sync/preflight', { params })
}

export function querySyncCapabilities(): Promise<SyncCapabilities> {
  return syncService.get('/sync/capabilities')
}

export function startFullSync(bindingId: SyncSafeId): Promise<StartFullSyncResult> {
  return syncService.post(`/sync/bindings/${bindingId}/full-sync`)
}

export function getSyncJob(jobId: SyncSafeId): Promise<SyncJobRecord> {
  return syncService.get(`/sync/jobs/${jobId}`)
}

export function getSyncJobModules(jobId: SyncSafeId): Promise<SyncJobModuleRecord[]> {
  return syncService.get(`/sync/jobs/${jobId}/modules`)
}

export function getSyncJobErrors(jobId: SyncSafeId): Promise<SyncJobErrorRecord[]> {
  return syncService.get(`/sync/jobs/${jobId}/errors`)
}
