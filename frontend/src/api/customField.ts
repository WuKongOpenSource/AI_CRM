import { get, post } from '@/utils/request'
import type {
  CustomField,
  CustomFieldAddBO,
  CustomFieldUpdateBO,
  FieldSortBO,
  EntityType
} from '@/types/customField'

/**
 * Add custom field
 */
export function addCustomField(data: CustomFieldAddBO): Promise<string> {
  return post('/custom-field/add', data)
}

/**
 * Update custom field
 */
export function updateCustomField(data: CustomFieldUpdateBO): Promise<void> {
  return post('/custom-field/update', data)
}

/**
 * Disable custom field
 */
export function disableCustomField(fieldId: string): Promise<void> {
  return post(`/custom-field/disable/${fieldId}`)
}

/**
 * Enable custom field
 */
export function enableCustomField(fieldId: string): Promise<void> {
  return post(`/custom-field/enable/${fieldId}`)
}

/**
 * Delete custom field
 */
export function deleteCustomField(fieldId: string): Promise<void> {
  return post(`/custom-field/delete/${fieldId}`)
}

/**
 * Get all fields by entity type
 */
export function getFieldsByEntity(entityType: EntityType): Promise<CustomField[]> {
  return get(`/custom-field/list/${entityType}`)
}

/**
 * Get enabled fields by entity type
 */
export function getEnabledFieldsByEntity(entityType: EntityType): Promise<CustomField[]> {
  return get(`/custom-field/enabled/${entityType}`)
}

/**
 * Update fields sort order
 */
export function updateFieldSort(sortList: FieldSortBO[]): Promise<void> {
  return post('/custom-field/sort', sortList)
}
