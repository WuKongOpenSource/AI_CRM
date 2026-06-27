export const SIDEBAR_MODULE_KEYS = ['recent', 'customer', 'candidate', 'product', 'project', 'relation', 'addressBook'] as const

export type SidebarModuleKey = (typeof SIDEBAR_MODULE_KEYS)[number]

export const DEFAULT_SIDEBAR_MODULE_ORDER: SidebarModuleKey[] = [
  'recent',
  'customer',
  'candidate',
  'product',
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

  for (let defaultIndex = 0; defaultIndex < DEFAULT_SIDEBAR_MODULE_ORDER.length; defaultIndex += 1) {
    const key = DEFAULT_SIDEBAR_MODULE_ORDER[defaultIndex]
    if (!seen.has(key)) {
      let insertIndex = normalized.length
      for (let index = defaultIndex - 1; index >= 0; index -= 1) {
        const previousKey = DEFAULT_SIDEBAR_MODULE_ORDER[index]
        const previousIndex = normalized.indexOf(previousKey)
        if (previousIndex >= 0) {
          insertIndex = previousIndex + 1
          break
        }
      }
      if (insertIndex === normalized.length) {
        for (let index = defaultIndex + 1; index < DEFAULT_SIDEBAR_MODULE_ORDER.length; index += 1) {
          const nextKey = DEFAULT_SIDEBAR_MODULE_ORDER[index]
          const nextIndex = normalized.indexOf(nextKey)
          if (nextIndex >= 0) {
            insertIndex = nextIndex
            break
          }
        }
      }
      normalized.splice(insertIndex, 0, key)
      seen.add(key)
    }
  }

  return normalized
}

export function getSidebarModuleOrderStorageKey(userId?: string | number): string {
  const normalizedUserId = String(userId || 'user')
  return `${STORAGE_KEY_PREFIX}:${normalizedUserId}`
}

export function readStoredSidebarModuleOrder(userId?: string | number): SidebarModuleKey[] | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = window.localStorage.getItem(getSidebarModuleOrderStorageKey(userId))
    if (!raw) return null
    return normalizeSidebarModuleOrder(JSON.parse(raw))
  } catch {
    return null
  }
}

export function writeStoredSidebarModuleOrder(
  order: readonly SidebarModuleKey[],
  userId?: string | number
) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(
      getSidebarModuleOrderStorageKey(userId),
      JSON.stringify(normalizeSidebarModuleOrder([...order]))
    )
  } catch {
    // Ignore storage failures.
  }
}
