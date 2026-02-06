// Custom Field related types

export type FieldType = 'text' | 'textarea' | 'number' | 'date' | 'datetime' | 'select' | 'multiselect' | 'checkbox'
export type EntityType = 'customer' | 'contact'

export interface FieldOption {
  value: string
  label: string
}

export interface FieldValidation {
  minLength?: number
  maxLength?: number
  min?: number
  max?: number
  pattern?: string
  message?: string
}

export interface CustomField {
  fieldId: string
  entityType: EntityType
  fieldName: string
  fieldLabel: string
  fieldType: FieldType
  columnName: string
  defaultValue?: string
  placeholder?: string
  isRequired: boolean
  isSearchable: boolean
  isShowInList: boolean
  options?: FieldOption[]
  validation?: FieldValidation
  sortOrder: number
  status: number
}

export interface CustomFieldAddBO {
  entityType: EntityType
  fieldName: string
  fieldLabel: string
  fieldType: FieldType
  defaultValue?: string
  placeholder?: string
  isRequired?: boolean
  isSearchable?: boolean
  isShowInList?: boolean
  options?: FieldOption[]
  validation?: FieldValidation
  sortOrder?: number
}

export interface CustomFieldUpdateBO {
  fieldId: string
  fieldLabel?: string
  defaultValue?: string
  placeholder?: string
  isRequired?: boolean
  isSearchable?: boolean
  isShowInList?: boolean
  options?: FieldOption[]
  validation?: FieldValidation
  sortOrder?: number
}

export interface FieldSortBO {
  fieldId: string
  sortOrder: number
}

// Field type labels for display
export const FIELD_TYPE_LABELS: Record<FieldType, string> = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  date: '日期',
  datetime: '日期时间',
  select: '单选下拉',
  multiselect: '多选下拉',
  checkbox: '开关'
}

export const ENTITY_TYPE_LABELS: Record<EntityType, string> = {
  customer: '客户',
  contact: '联系人'
}
