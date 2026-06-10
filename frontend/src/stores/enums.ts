import { defineStore } from 'pinia'
import { ref, type Ref } from 'vue'
import {
  getCustomerStageEnum,
  getCustomerLevelEnum,
  getRelationTypeEnum,
  getRelationSourceEnum,
  type EnumOption
} from '@/api/enum'

const CUSTOMER_STAGE_FALLBACK_OPTIONS: EnumOption[] = [
  { value: 'lead', label: '线索' },
  { value: 'qualified', label: '资格审查' },
  { value: 'proposal', label: '方案报价' },
  { value: 'negotiation', label: '谈判中' },
  { value: 'closed', label: '已成交' },
  { value: 'lost', label: '已流失' }
]

const CUSTOMER_STAGE_LABELS = new Map(
  CUSTOMER_STAGE_FALLBACK_OPTIONS.map(option => [option.value, option.label])
)

/**
 * 选项枚举 store（单一真相源）。
 *
 * stage / level / relationType / source 等系统下拉的选项与标签统一从后端
 * `/enum/*` 接口获取（后端读 crm_custom_field.options）。组件挂载时调用对应
 * ensureXxx() 拉取一次并复用；设置页编辑选项后调用 refreshAll() 立即生效。
 */
export const useEnumStore = defineStore('enums', () => {
  const customerStage = ref<EnumOption[]>([])
  const customerLevel = ref<EnumOption[]>([])
  const relationType = ref<EnumOption[]>([])
  const relationSource = ref<EnumOption[]>([])

  const loaded: Record<string, boolean> = {}
  const inflight: Record<string, Promise<void> | null> = {}

  async function ensure(
    key: string,
    target: Ref<EnumOption[]>,
    fetcher: () => Promise<EnumOption[]>,
    normalize?: (options: EnumOption[]) => EnumOption[],
    force = false
  ): Promise<void> {
    if (loaded[key] && !force) return
    if (inflight[key]) return inflight[key] as Promise<void>
    const task = (async () => {
      try {
        const data = await fetcher()
        const options = Array.isArray(data) ? data : []
        target.value = normalize ? normalize(options) : options
        loaded[key] = true
      } catch {
        // 静默：失败时保持已有/空选项，避免阻塞页面
      } finally {
        inflight[key] = null
      }
    })()
    inflight[key] = task
    return task
  }

  function normalizeCustomerStageOptions(options: EnumOption[]): EnumOption[] {
    const normalized = options
      .map(option => {
        const value = String(option?.value || '').trim()
        if (!value) return null
        const fallbackLabel = CUSTOMER_STAGE_LABELS.get(value)
        const rawLabel = String(option?.label || '').trim()
        const rawLabelIsCode = rawLabel.toLowerCase() === value.toLowerCase()
        const label = fallbackLabel && (!rawLabel || rawLabelIsCode) ? fallbackLabel : (rawLabel || fallbackLabel || value)
        return {
          ...option,
          value,
          label
        }
      })
      .filter((option): option is EnumOption => Boolean(option))
    return normalized.length ? normalized : CUSTOMER_STAGE_FALLBACK_OPTIONS
  }

  const ensureCustomerStage = (force = false) =>
    ensure('customerStage', customerStage, getCustomerStageEnum, normalizeCustomerStageOptions, force)
  const ensureCustomerLevel = (force = false) =>
    ensure('customerLevel', customerLevel, getCustomerLevelEnum, undefined, force)
  const ensureRelationType = (force = false) =>
    ensure('relationType', relationType, getRelationTypeEnum, undefined, force)
  const ensureRelationSource = (force = false) =>
    ensure('relationSource', relationSource, getRelationSourceEnum, undefined, force)

  function labelOf(options: EnumOption[], value?: string | null): string {
    if (!value) return ''
    return options.find(o => o.value === value)?.label || value
  }

  const stageLabel = (value?: string | null) => labelOf(customerStage.value, value)
  const levelLabel = (value?: string | null) => labelOf(customerLevel.value, value)
  const relationTypeLabel = (value?: string | null) => labelOf(relationType.value, value)
  const sourceLabel = (value?: string | null) => labelOf(relationSource.value, value)

  /** 设置页编辑选项后强制刷新所有已用枚举 */
  async function refreshAll(): Promise<void> {
    await Promise.all([
      ensureCustomerStage(true),
      ensureCustomerLevel(true),
      ensureRelationType(true),
      ensureRelationSource(true)
    ])
  }

  return {
    customerStage,
    customerLevel,
    relationType,
    relationSource,
    ensureCustomerStage,
    ensureCustomerLevel,
    ensureRelationType,
    ensureRelationSource,
    stageLabel,
    levelLabel,
    relationTypeLabel,
    sourceLabel,
    refreshAll
  }
})
