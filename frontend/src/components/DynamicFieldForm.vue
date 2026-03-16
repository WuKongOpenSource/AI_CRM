<template>
  <div v-if="fields.length > 0" class="dynamic-field-form">
    <!-- Native mode: renders fields matching the plain HTML style of basic info -->
    <template v-if="nativeStyle">
      <div
        v-for="field in fields"
        :key="field.fieldId"
        :class="field.fieldType === 'textarea' ? 'md:col-span-2' : ''"
        class="space-y-1.5"
      >
        <label class="text-[10px] font-bold text-slate-500 uppercase ml-1">
          {{ field.fieldLabel }}
          <span v-if="field.isRequired" class="text-red-400">*</span>
        </label>

        <!-- Text -->
        <input
          v-if="field.fieldType === 'text'"
          v-model="localValues[field.fieldName]"
          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
          @input="emitChange"
        />

        <!-- Textarea -->
        <textarea
          v-else-if="field.fieldType === 'textarea'"
          v-model="localValues[field.fieldName]"
          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
          rows="3"
          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all resize-none"
          @input="emitChange"
        />

        <!-- Number -->
        <input
          v-else-if="field.fieldType === 'number'"
          v-model="localValues[field.fieldName]"
          type="number"
          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
          @input="emitChange"
        />

        <!-- Date / DateTime -->
        <input
          v-else-if="field.fieldType === 'date'"
          v-model="localValues[field.fieldName]"
          type="date"
          :placeholder="field.placeholder || '选择日期'"
          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
          @input="emitChange"
        />

        <input
          v-else-if="field.fieldType === 'datetime'"
          v-model="localValues[field.fieldName]"
          type="datetime-local"
          :placeholder="field.placeholder || '选择日期时间'"
          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all"
          @input="emitChange"
        />

        <!-- Select -->
        <select
          v-else-if="field.fieldType === 'select'"
          v-model="localValues[field.fieldName]"
          class="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-lg text-sm focus:ring-2 focus:ring-primary/50 focus:border-primary outline-none transition-all appearance-none"
          @change="emitChange"
        >
          <option value="" disabled>{{ field.placeholder || '请选择' }}</option>
          <option v-for="opt in field.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>

        <!-- MultiSelect (fallback to el-select for multi) -->
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
        <div v-else-if="field.fieldType === 'checkbox'" class="pt-1">
          <el-switch v-model="localValues[field.fieldName]" @change="emitChange" />
        </div>
      </div>
    </template>

    <!-- Legacy mode: el-form-item + el-divider -->
    <template v-else>
      <el-divider v-if="showDivider" content-position="left">{{ title }}</el-divider>
      <el-form-item
        v-for="field in fields"
        :key="field.fieldId"
        :label="field.fieldLabel"
        :required="field.isRequired"
      >
        <el-input
          v-if="field.fieldType === 'text'"
          v-model="localValues[field.fieldName]"
          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
          @change="emitChange"
        />
        <el-input
          v-else-if="field.fieldType === 'textarea'"
          v-model="localValues[field.fieldName]"
          type="textarea"
          :rows="3"
          :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
          @change="emitChange"
        />
        <el-input-number
          v-else-if="field.fieldType === 'number'"
          v-model="localValues[field.fieldName]"
          :placeholder="field.placeholder"
          class="w-full"
          @change="emitChange"
        />
        <el-date-picker
          v-else-if="field.fieldType === 'date'"
          v-model="localValues[field.fieldName]"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="field.placeholder || '选择日期'"
          class="w-full"
          @change="emitChange"
        />
        <el-date-picker
          v-else-if="field.fieldType === 'datetime'"
          v-model="localValues[field.fieldName]"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          :placeholder="field.placeholder || '选择日期时间'"
          class="w-full"
          @change="emitChange"
        />
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
        <el-switch
          v-else-if="field.fieldType === 'checkbox'"
          v-model="localValues[field.fieldName]"
          @change="emitChange"
        />
      </el-form-item>
    </template>
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
  nativeStyle?: boolean
}>(), {
  modelValue: () => ({}),
  showDivider: true,
  title: '扩展信息',
  nativeStyle: false
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
