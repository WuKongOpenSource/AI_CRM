export const SIDEBAR_MODULE_KEYS = ['recent', 'customer', 'project', 'relation', 'addressBook'] as const

export type SidebarModuleKey = (typeof SIDEBAR_MODULE_KEYS)[number]

export const DEFAULT_SIDEBAR_MODULE_ORDER: SidebarModuleKey[] = [
  'recent',
  'customer',
  'project',
  'relation',
  'addressBook'
]

const SIDEBAR_MODULE_KEY_SET = new Set<string>(SIDEBAR_MODULE_KEYS)
const STORAGE_KEY_PREFIX = 'wk_ai_crm:main_layout:sidebar_module_order:v1'

export function normalizeSidebarModuleOrder(input?: unknown): SidebarModuleKey[] {
  const normalized: SidebarModuleKey[] = []
  const seen = new Set<SidebarModuleKey>()

  if (Array.isArray(input)) {
    for (const item of input) {
      if (typeof item !== 'string') continue
      const key = item.trim()
      if (!SIDEBAR_MODULE_KEY_SET.has(key)) continue
      const moduleKey = key as SidebarModuleKey
      if (seen.has(moduleKey)) continue
      seen.add(moduleKey)
      normalized.push(moduleKey)
    }
  }

  for (const key of DEFAULT_SIDEBAR_MODULE_ORDER) {
    if (!seen.has(key)) {
      normalized.push(key)
    }
  }

  return normalized
}

export function getSidebarModuleOrderStorageKey(userId?: string | number, tenantId?: string | number): string {
  const normalizedTenantId = String(tenantId || 'tenant')
  const normalizedUserId = String(userId || 'user')
  return `${STORAGE_KEY_PREFIX}:${normalizedTenantId}:${normalizedUserId}`
}

export function readStoredSidebarModuleOrder(userId?: string | number, tenantId?: string | number): SidebarModuleKey[] | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = window.localStorage.getItem(getSidebarModuleOrderStorageKey(userId, tenantId))
    if (!raw) return null
    return normalizeSidebarModuleOrder(JSON.parse(raw))
  } catch {
    return null
  }
}

export function writeStoredSidebarModuleOrder(
  order: readonly SidebarModuleKey[],
  userId?: string | number,
  tenantId?: string | number
) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(
      getSidebarModuleOrderStorageKey(userId, tenantId),
      JSON.stringify(normalizeSidebarModuleOrder([...order]))
    )
  } catch {
    // Ignore storage failures.
  }
}
