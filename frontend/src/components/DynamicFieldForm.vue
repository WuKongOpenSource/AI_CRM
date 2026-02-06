<template>
  <div v-if="fields.length > 0" class="dynamic-field-form">
    <el-divider v-if="showDivider" content-position="left">{{ title }}</el-divider>
    <el-form-item
      v-for="field in fields"
      :key="field.fieldId"
      :label="field.fieldLabel"
      :required="field.isRequired"
    >
      <!-- Text -->
      <el-input
        v-if="field.fieldType === 'text'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        @change="emitChange"
      />

      <!-- Textarea -->
      <el-input
        v-else-if="field.fieldType === 'textarea'"
        v-model="localValues[field.fieldName]"
        type="textarea"
        :rows="3"
        :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
        @change="emitChange"
      />

      <!-- Number -->
      <el-input-number
        v-else-if="field.fieldType === 'number'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder"
        class="w-full"
        @change="emitChange"
      />

      <!-- Date -->
      <el-date-picker
        v-else-if="field.fieldType === 'date'"
        v-model="localValues[field.fieldName]"
        type="date"
        value-format="YYYY-MM-DD"
        :placeholder="field.placeholder || '选择日期'"
        class="w-full"
        @change="emitChange"
      />

      <!-- DateTime -->
      <el-date-picker
        v-else-if="field.fieldType === 'datetime'"
        v-model="localValues[field.fieldName]"
        type="datetime"
        value-format="YYYY-MM-DD HH:mm:ss"
        :placeholder="field.placeholder || '选择日期时间'"
        class="w-full"
        @change="emitChange"
      />

      <!-- Select -->
      <el-select
        v-else-if="field.fieldType === 'select'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || '请选择'"
        class="w-full"
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

      <!-- MultiSelect -->
      <el-select
        v-else-if="field.fieldType === 'multiselect'"
        v-model="localValues[field.fieldName]"
        :placeholder="field.placeholder || '请选择'"
        class="w-full"
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

      <!-- Checkbox/Switch -->
      <el-switch
        v-else-if="field.fieldType === 'checkbox'"
        v-model="localValues[field.fieldName]"
        @change="emitChange"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { CustomField, EntityType } from '@/types/customField'
import { getEnabledFieldsByEntity } from '@/api/customField'

const props = withDefaults(defineProps<{
  entityType: EntityType
  modelValue?: Record<string, any>
  showDivider?: boolean
  title?: string
}>(), {
  modelValue: () => ({}),
  showDivider: true,
  title: '扩展信息'
})

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
  'fieldsLoaded': [fields: CustomField[]]
}>()

const fields = ref<CustomField[]>([])
const localValues = ref<Record<string, any>>({})

// Load custom fields
async function loadFields() {
  try {
    fields.value = await getEnabledFieldsByEntity(props.entityType)
    // Initialize default values
    fields.value.forEach(field => {
      if (localValues.value[field.fieldName] === undefined) {
        if (field.fieldType === 'multiselect') {
          localValues.value[field.fieldName] = []
        } else if (field.fieldType === 'checkbox') {
          localValues.value[field.fieldName] = false
        } else if (field.defaultValue) {
          localValues.value[field.fieldName] = field.defaultValue
        } else {
          localValues.value[field.fieldName] = null
        }
      }
    })
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
  if (newVal) {
    Object.assign(localValues.value, newVal)
  }
}, { deep: true, immediate: true })

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
