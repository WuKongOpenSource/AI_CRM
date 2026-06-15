import type { RelationType } from '@/types/relation'
import type { EnumOption } from '@/api/enum'

export const relationTypeOptions: Array<{ value: RelationType; label: string }> = [
  { value: 'friend', label: '朋友' },
  { value: 'family', label: '家人' },
  { value: 'relative', label: '亲戚' },
  { value: 'partner', label: '合作伙伴' },
  { value: 'customer_contact', label: '客户联系人' },
  { value: 'supplier', label: '供应商' },
  { value: 'investor', label: '投资人' },
  { value: 'other', label: '其他' }
]

const relationTypeLabelMap = new Map<string, string>(
  relationTypeOptions.map(option => [option.value, option.label])
)

export function normalizeRelationTypeOptions(options?: Array<Partial<EnumOption> | null>): EnumOption[] {
  const normalized: EnumOption[] = []
  for (const option of options || []) {
    const value = String(option?.value || '').trim()
    if (!value) continue
    const fallbackLabel = relationTypeLabelMap.get(value)
    const rawLabel = String(option?.label || '').trim()
    const label = fallbackLabel && (!rawLabel || rawLabel === value) ? fallbackLabel : (rawLabel || fallbackLabel || value)
    normalized.push({
      value,
      label,
      description: option?.description ?? null,
      color: option?.color
    })
  }

  return normalized.length ? normalized : relationTypeOptions
}

export function resolveRelationTypeLabel(
  type?: string | null,
  fallback?: string | null,
  options?: Array<Partial<EnumOption> | null>
) {
  const value = String(type || '').trim()
  if (!value) return fallback || '其他'
  const normalizedOptions = normalizeRelationTypeOptions(options)
  const matched = normalizedOptions.find(option => option.value === value)
  if (matched?.label) return matched.label
  if (fallback && fallback !== value) return fallback
  return relationTypeLabelMap.get(value) || value
}
