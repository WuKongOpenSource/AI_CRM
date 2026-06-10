import { defineStore } from 'pinia'
import { ref, type Ref } from 'vue'
import {
  getCustomerStageEnum,
  getCustomerLevelEnum,
  getRelationTypeEnum,
  getRelationSourceEnum,
  type EnumOption
} from '@/api/enum'

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
    force = false
  ): Promise<void> {
    if (loaded[key] && !force) return
    if (inflight[key]) return inflight[key] as Promise<void>
    const task = (async () => {
      try {
        const data = await fetcher()
        target.value = Array.isArray(data) ? data : []
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

  const ensureCustomerStage = (force = false) =>
    ensure('customerStage', customerStage, getCustomerStageEnum, force)
  const ensureCustomerLevel = (force = false) =>
    ensure('customerLevel', customerLevel, getCustomerLevelEnum, force)
  const ensureRelationType = (force = false) =>
    ensure('relationType', relationType, getRelationTypeEnum, force)
  const ensureRelationSource = (force = false) =>
    ensure('relationSource', relationSource, getRelationSourceEnum, force)

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
