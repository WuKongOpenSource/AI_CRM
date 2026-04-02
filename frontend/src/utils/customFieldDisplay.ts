import type { CustomField } from '@/types/customField'

function isBlankString(value: unknown): value is string {
  return typeof value === 'string' && value.trim() === ''
}

function parseArrayValue(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value
      .map(item => String(item).trim())
      .filter(Boolean)
  }

  if (typeof value !== 'string') return []

  const trimmed = value.trim()
  if (!trimmed) return []

  if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
    try {
      const parsed = JSON.parse(trimmed)
      if (Array.isArray(parsed)) {
        return parsed
          .map(item => String(item).trim())
          .filter(Boolean)
      }
    } catch {
      // Fall through to delimiter parsing.
    }
  }

  return trimmed
    .split(/[,，]/)
    .map(item => item.trim())
    .filter(Boolean)
}

function formatDateValue(value: unknown, includeTime = false): string {
  if (value === null || value === undefined || isBlankString(value)) return '-'

  const date = new Date(String(value))
  if (Number.isNaN(date.getTime())) return String(value)

  return includeTime
    ? date.toLocaleString('zh-CN')
    : date.toLocaleDateString('zh-CN')
}

function resolveOptionLabel(field: CustomField, value: unknown): string {
  const normalizedValue = String(value)
  return field.options?.find(option => option.value === normalizedValue)?.label || normalizedValue
}

export function getCustomFieldCheckboxState(value: unknown): boolean | null {
  if (value === null || value === undefined || isBlankString(value)) return null
  if (typeof value === 'boolean') return value
  if (typeof value === 'number') {
    if (value === 1) return true
    if (value === 0) return false
  }

  const normalized = String(value).trim().toLowerCase()

  if (['true', '1', 'yes', 'y', 'on', 'open', 'enabled', 'shi', '是', '开启', '已开启'].includes(normalized)) {
    return true
  }

  if (['false', '0', 'no', 'n', 'off', 'close', 'closed', 'disabled', 'fou', '否', '关闭', '已关闭'].includes(normalized)) {
    return false
  }

  return null
}

export function formatCustomFieldValue(field: CustomField, value: unknown): string {
  if (value === null || value === undefined || isBlankString(value)) return '-'

  switch (field.fieldType) {
    case 'checkbox': {
      const checked = getCustomFieldCheckboxState(value)
      return checked === null ? '-' : checked ? '开启' : '关闭'
    }
    case 'select':
      return resolveOptionLabel(field, value)
    case 'multiselect': {
      const selectedValues = parseArrayValue(value)
      if (selectedValues.length === 0) return '-'
      return selectedValues
        .map(item => resolveOptionLabel(field, item))
        .join(', ')
    }
    case 'date':
      return formatDateValue(value)
    case 'datetime':
      return formatDateValue(value, true)
    default:
      return Array.isArray(value)
        ? value.map(item => String(item)).join(', ')
        : String(value)
  }
}
