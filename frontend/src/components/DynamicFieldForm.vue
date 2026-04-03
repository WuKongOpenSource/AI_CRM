<template>
  <div v-if="fields.length > 0" class="dynamic-field-form">
    <div
      v-for="field in fields"
      :key="field.fieldId"
      :class="getFieldWrapperClass(field)"
      class="space-y-1.5"
    >
      <label class="text-xs font-bold text-slate-500 ml-1">
        {{ field.fieldLabel }}
        <span v-if="field.isRequired" class="text-red-400">*</span>
      </label>

      <el-input
        v-if="field.fieldType === 'text'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        size="large"
        class="w-full wk-crm-el-field-input"
        @input="emitChange"
      />
      <el-input
        v-else-if="field.fieldType === 'textarea'"
        v-model="localValues[field.fieldName]"
        type="textarea"
        :rows="3"
        resize="none"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        class="w-full wk-crm-el-field-input"
        @input="emitChange"
      />
      <el-input
        v-else-if="field.fieldType === 'number'"
        v-model="localValues[field.fieldName]"
        type="number"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        size="large"
        class="w-full wk-crm-el-field-input"
        @input="emitChange"
      />
      <el-date-picker
        v-else-if="field.fieldType === 'date'"
        v-model="localValues[field.fieldName]"
        type="date"
        value-format="YYYY-MM-DD"
        :placeholder="field.placeholder || '选择日期'"
        size="large"
        class="w-full wk-crm-el-field-date"
        @change="emitChange"
      />
      <el-date-picker
        v-else-if="field.fieldType === 'datetime'"
        v-model="localValues[field.fieldName]"
        type="datetime"
        value-format="YYYY-MM-DD HH:mm:ss"
        :placeholder="field.placeholder || '选择日期时间'"
        size="large"
        class="w-full wk-crm-el-field-date"
        @change="emitChange"
      />
      <el-select
        v-else-if="field.fieldType === 'select'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || '请选择'"
        size="large"
        class="w-full wk-crm-el-field-select"
        clearable
        @change="emitChange"
      >
        <el-option
          v-for="opt in field.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <el-select
        v-else-if="field.fieldType === 'multiselect'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || '请选择'"
        size="large"
        class="w-full wk-crm-el-field-select"
        multiple
        clearable
        @change="emitChange"
      >
        <el-option
          v-for="opt in field.options"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div v-else-if="field.fieldType === 'checkbox'" class="pt-1">
        <el-switch v-model="localValues[field.fieldName]" @change="emitChange" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import type { CustomField, EntityType } from '@/types/customField'
import { getEnabledFieldsByEntity, getFormFieldsByEntity } from '@/api/customField'

// 与新建任务一致：父级需带 wk-crm-el-field-scope 才应用 wk-crm-el-field-skin.css

const props = withDefaults(defineProps<{
  entityType: EntityType
  modelValue?: Record<string, any>
  fields?: CustomField[] | null
  mode?: 'custom' | 'form'
  fullSpanFieldNames?: string[]
}>(), {
  modelValue: () => ({}),
  fields: null,
  mode: 'custom',
  fullSpanFieldNames: () => []
})

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
  'fieldsLoaded': [fields: CustomField[]]
}>()

const fields = ref<CustomField[]>([])
const localValues = ref<Record<string, any>>({})

function getFieldWrapperClass(field: CustomField): string {
  if (field.fieldType === 'textarea' || props.fullSpanFieldNames.includes(field.fieldName)) {
    return 'md:col-span-2'
  }
  return ''
}

function normalizeMultiselectValue(value: unknown): string[] {
  if (value === null || value === undefined || value === '') return []

  if (Array.isArray(value)) {
    return value
      .map(item => String(item).trim())
      .filter(Boolean)
  }

  if (typeof value === 'string') {
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
        // Fall back to delimiter parsing.
      }
    }

    return trimmed
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
  }

  return [String(value)]
}

function normalizeCheckboxValue(value: unknown): boolean {
  if (value === true || value === 1) return true
  if (value === false || value === 0 || value === null || value === undefined || value === '') return false

  const normalized = String(value).trim().toLowerCase()
  return ['true', '1', 'yes', 'y', 'on', 'enabled'].includes(normalized)
}

function normalizeFieldValue(field: CustomField, value: unknown): any {
  if (value === undefined) return undefined

  switch (field.fieldType) {
    case 'multiselect':
      return normalizeMultiselectValue(value)
    case 'checkbox':
      return normalizeCheckboxValue(value)
    case 'number':
      if (value === null || value === '') return null
      return Number.isNaN(Number(value)) ? value : Number(value)
    case 'select':
      return value === null || value === '' ? null : String(value)
    default:
      return value
  }
}

function applyModelValue(modelValue?: Record<string, any>) {
  const sourceValues = modelValue || {}

  if (fields.value.length === 0) {
    localValues.value = { ...sourceValues }
    return
  }

  const nextValues: Record<string, any> = {}
  const fieldMap = new Map(fields.value.map(field => [field.fieldName, field]))

  for (const [fieldName, rawValue] of Object.entries(sourceValues)) {
    const field = fieldMap.get(fieldName)
    nextValues[fieldName] = field ? normalizeFieldValue(field, rawValue) : rawValue
  }

  localValues.value = nextValues
}

// Parse and validate default value based on field type
function parseDefaultValue(field: CustomField): any {
  const val: unknown = (field as any).defaultValue
  if (val === undefined || val === null || val === '') return null

  switch (field.fieldType) {
    case 'number': {
      const num = Number(val)
      return Number.isNaN(num) ? null : num
    }
    case 'date':
      return /^\d{4}-\d{2}-\d{2}$/.test(String(val)) ? val : null
    case 'datetime':
      return /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?$/.test(String(val)) ? val : null
    case 'checkbox':
      return normalizeCheckboxValue(val)
    case 'select':
      if (field.options?.some(opt => opt.value === String(val))) return String(val)
      return null
    case 'multiselect':
      return normalizeMultiselectValue(val)
    default:
      return val
  }
}

function applyFieldDefaults() {
  fields.value.forEach(field => {
    if (localValues.value[field.fieldName] === undefined || localValues.value[field.fieldName] === null) {
      if (field.fieldType === 'multiselect') {
        localValues.value[field.fieldName] = []
      } else if (field.fieldType === 'checkbox') {
        const hasDefault = (field as any).defaultValue !== undefined && (field as any).defaultValue !== null && (field as any).defaultValue !== ''
        localValues.value[field.fieldName] = hasDefault ? parseDefaultValue(field) : false
      } else if ((field as any).defaultValue !== undefined && (field as any).defaultValue !== null && (field as any).defaultValue !== '') {
        localValues.value[field.fieldName] = parseDefaultValue(field)
      } else {
        localValues.value[field.fieldName] = null
      }
    }
  })
}

// Load custom fields
async function loadFields() {
  try {
    if (props.fields && props.fields.length > 0) {
      fields.value = props.fields
    } else {
      fields.value = props.mode === 'form'
        ? await getFormFieldsByEntity(props.entityType)
        : await getEnabledFieldsByEntity(props.entityType)
    }
    // 先按字段类型归一化编辑态已有值，再补默认值
    applyModelValue(props.modelValue)
    applyFieldDefaults()
    emit('fieldsLoaded', fields.value)
  } catch {
    // Error handled by interceptor
  }
}

function emitChange() {
  emit('update:modelValue', { ...localValues.value })
}

// Watch for external value changes
watch(() => props.modelValue, (newVal) => {
  applyModelValue(newVal)
  applyFieldDefaults()
}, { deep: true, immediate: true })

watch(() => props.fields, (newFields) => {
  if (newFields && newFields.length > 0) {
    fields.value = newFields
    applyModelValue(props.modelValue)
    applyFieldDefaults()
    emit('fieldsLoaded', fields.value)
  }
}, { deep: true })

onMounted(() => {
  loadFields()
})

// Expose validation method
function validate(): boolean {
  for (const field of fields.value) {
    if (field.isRequired) {
      const value = localValues.value[field.fieldName]
      if (value === null || value === undefined || value === '' ||
          (Array.isArray(value) && value.length === 0)) {
        return false
      }
    }
  }
  return true
}

function getRequiredFieldLabels(): string[] {
  const missing: string[] = []
  for (const field of fields.value) {
    if (field.isRequired) {
      const value = localValues.value[field.fieldName]
      if (value === null || value === undefined || value === '' ||
          (Array.isArray(value) && value.length === 0)) {
        missing.push(field.fieldLabel)
      }
    }
  }
  return missing
}

defineExpose({ validate, getRequiredFieldLabels, fields, localValues })
</script>
