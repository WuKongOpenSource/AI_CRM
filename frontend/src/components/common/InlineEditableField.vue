<template>
  <div
    class="inline-editable-field"
    :class="{ 'is-editing': editing, 'is-disabled': !editable }"
  >
    <template v-if="editing">
      <div
        class="inline-editable-field__editor"
        :class="{ 'is-block': resolvedFieldType === 'textarea' }"
        data-row-action="true"
        @click.stop
      >
        <el-select
          v-if="resolvedFieldType === 'select'"
          ref="editorRef"
          v-model="draftValue"
          :placeholder="resolvedPlaceholder"
          clearable
          filterable
          size="small"
          class="inline-editable-field__control"
          @keyup.enter="commit"
          @keyup.esc="cancel"
        >
          <el-option
            v-for="option in resolvedOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <el-select
          v-else-if="resolvedFieldType === 'multiselect'"
          ref="editorRef"
          v-model="draftValue"
          :placeholder="resolvedPlaceholder"
          multiple
          clearable
          filterable
          collapse-tags
          collapse-tags-tooltip
          size="small"
          class="inline-editable-field__control"
          @keyup.enter="commit"
          @keyup.esc="cancel"
        >
          <el-option
            v-for="option in resolvedOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <el-date-picker
          v-else-if="resolvedFieldType === 'date'"
          ref="editorRef"
          v-model="draftValue"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="resolvedPlaceholder"
          size="small"
          class="inline-editable-field__control"
          @keyup.enter="commit"
          @keyup.esc="cancel"
        />

        <el-date-picker
          v-else-if="resolvedFieldType === 'datetime'"
          ref="editorRef"
          v-model="draftValue"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          :placeholder="resolvedPlaceholder"
          size="small"
          class="inline-editable-field__control"
          @keyup.enter="commit"
          @keyup.esc="cancel"
        />

        <el-switch
          v-else-if="resolvedFieldType === 'checkbox'"
          v-model="draftValue"
          size="small"
          class="inline-editable-field__switch"
          @keyup.enter="commit"
          @keyup.esc="cancel"
        />

        <el-input
          v-else-if="resolvedFieldType === 'textarea'"
          ref="editorRef"
          v-model="draftValue"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 4 }"
          :placeholder="resolvedPlaceholder"
          class="inline-editable-field__control"
          @keyup.enter.ctrl="commit"
          @keyup.esc="cancel"
        />

        <el-input
          v-else
          ref="editorRef"
          v-model="draftValue"
          :type="resolvedFieldType === 'number' ? 'number' : 'text'"
          :placeholder="resolvedPlaceholder"
          size="small"
          class="inline-editable-field__control"
          @keyup.enter="commit"
          @keyup.esc="cancel"
        />

        <button
          type="button"
          class="inline-editable-field__action is-save"
          :disabled="saving"
          title="保存"
          @click="commit"
        >
          <span v-if="saving" class="inline-editable-field__spinner"></span>
          <span v-else class="material-symbols-outlined">check</span>
        </button>
        <button
          type="button"
          class="inline-editable-field__action"
          :disabled="saving"
          title="取消"
          @click="cancel"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </template>

    <template v-else>
      <div class="inline-editable-field__display">
        <div class="inline-editable-field__content">
          <slot :value="modelValue" :display-value="resolvedDisplayValue">
            <span class="inline-editable-field__fallback" :title="resolvedDisplayValue">
              {{ resolvedDisplayValue }}
            </span>
          </slot>
        </div>
        <button
          v-if="editable"
          type="button"
          class="inline-editable-field__edit"
          :title="`编辑${resolvedFieldLabel}`"
          data-row-action="true"
          @click.stop="startEdit"
        >
          <span class="material-symbols-outlined">edit</span>
        </button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { CustomField, FieldOption, FieldType } from '@/types/customField'

type SaveHandler = (value: any) => void | Promise<void>

const props = withDefaults(defineProps<{
  modelValue: any
  field?: Partial<CustomField> | null
  fieldName?: string
  fieldLabel?: string
  fieldType?: FieldType
  options?: FieldOption[]
  displayValue?: string
  placeholder?: string
  emptyText?: string
  editable?: boolean
  required?: boolean
  saveHandler?: SaveHandler
}>(), {
  field: null,
  options: () => [],
  displayValue: '',
  placeholder: '',
  emptyText: '-',
  editable: true,
  required: false,
  saveHandler: undefined
})

const emit = defineEmits<{
  (e: 'saved', value: any): void
  (e: 'cancel'): void
}>()

const editing = ref(false)
const saving = ref(false)
const draftValue = ref<any>(null)
const editorRef = ref<any>(null)

const resolvedFieldName = computed(() => props.fieldName || props.field?.fieldName || '')
const resolvedFieldLabel = computed(() => props.fieldLabel || props.field?.fieldLabel || '字段')
const resolvedFieldType = computed<FieldType>(() => props.fieldType || props.field?.fieldType || 'text')
const resolvedOptions = computed<FieldOption[]>(() => props.options?.length ? props.options : props.field?.options || [])
const resolvedPlaceholder = computed(() => props.placeholder || props.field?.placeholder || `请输入${resolvedFieldLabel.value}`)
const isRequired = computed(() => props.required || Boolean(props.field?.isRequired))

const resolvedDisplayValue = computed(() => {
  if (props.displayValue) return props.displayValue
  if (props.modelValue === null || props.modelValue === undefined || props.modelValue === '') return props.emptyText
  if (Array.isArray(props.modelValue)) return props.modelValue.join(', ') || props.emptyText
  if (typeof props.modelValue === 'boolean') return props.modelValue ? '开启' : '关闭'
  return String(props.modelValue)
})

function normalizeMultiselectValue(value: unknown): string[] {
  if (value === null || value === undefined || value === '') return []
  if (Array.isArray(value)) return value.map(item => String(item)).filter(Boolean)
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) return []
    if (trimmed.startsWith('[') && trimmed.endsWith(']')) {
      try {
        const parsed = JSON.parse(trimmed)
        if (Array.isArray(parsed)) return parsed.map(item => String(item)).filter(Boolean)
      } catch {
        // Fall back to comma parsing.
      }
    }
    return trimmed.split(',').map(item => item.trim()).filter(Boolean)
  }
  return [String(value)]
}

function normalizeCheckboxValue(value: unknown): boolean {
  if (value === true || value === 1) return true
  if (value === false || value === 0 || value === null || value === undefined || value === '') return false
  return ['true', '1', 'yes', 'y', 'on', 'enabled'].includes(String(value).trim().toLowerCase())
}

function createDraftValue(value: unknown): any {
  switch (resolvedFieldType.value) {
    case 'multiselect':
      return normalizeMultiselectValue(value)
    case 'checkbox':
      return normalizeCheckboxValue(value)
    case 'number':
      return value === null || value === undefined ? '' : value
    case 'select':
      return value === null || value === undefined || value === '' ? '' : String(value)
    default:
      return value === null || value === undefined ? '' : value
  }
}

function buildSubmitValue(): any {
  if (resolvedFieldType.value === 'number') {
    if (draftValue.value === '' || draftValue.value === null || draftValue.value === undefined) return null
    const numericValue = Number(draftValue.value)
    return Number.isNaN(numericValue) ? draftValue.value : numericValue
  }
  if (resolvedFieldType.value === 'checkbox') {
    return Boolean(draftValue.value)
  }
  if (resolvedFieldType.value === 'multiselect') {
    return Array.isArray(draftValue.value) ? draftValue.value : normalizeMultiselectValue(draftValue.value)
  }
  if (draftValue.value === '') return null
  return draftValue.value
}

async function startEdit() {
  if (!props.editable || saving.value) return
  draftValue.value = createDraftValue(props.modelValue)
  editing.value = true
  await nextTick()
  const editor = editorRef.value
  if (editor?.focus) editor.focus()
  else if (editor?.$el?.querySelector) editor.$el.querySelector('input, textarea')?.focus()
}

function cancel() {
  if (saving.value) return
  editing.value = false
  emit('cancel')
}

async function commit() {
  if (saving.value) return
  const submitValue = buildSubmitValue()
  if (isRequired.value && (submitValue === null || submitValue === undefined || submitValue === '' || (Array.isArray(submitValue) && submitValue.length === 0))) {
    ElMessage.warning(`请填写${resolvedFieldLabel.value}`)
    return
  }

  saving.value = true
  try {
    await props.saveHandler?.(submitValue)
    editing.value = false
    emit('saved', submitValue)
  } finally {
    saving.value = false
  }
}

watch(() => [props.modelValue, resolvedFieldName.value], () => {
  if (!editing.value) {
    draftValue.value = createDraftValue(props.modelValue)
  }
})
</script>

<style scoped>
.inline-editable-field {
  min-width: 0;
  width: 100%;
}

.inline-editable-field__display {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  max-width: 100%;
}

.inline-editable-field__content {
  min-width: 0;
  max-width: 100%;
  flex: 1 1 auto;
}

.inline-editable-field__fallback {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-editable-field__edit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: 6px;
  color: #0b84ff;
  background: #e8f3ff;
  opacity: 0;
  transform: translateX(-2px);
  transition: opacity 0.15s ease, transform 0.15s ease, background 0.15s ease;
  flex: 0 0 auto;
}

.inline-editable-field__edit:hover {
  background: #d6ebff;
}

.inline-editable-field:hover .inline-editable-field__edit,
.inline-editable-field__edit:focus-visible {
  opacity: 1;
  transform: translateX(0);
}

.inline-editable-field__edit .material-symbols-outlined,
.inline-editable-field__action .material-symbols-outlined {
  font-size: 17px;
  line-height: 1;
}

.inline-editable-field__editor {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-width: 160px;
}

.inline-editable-field__editor.is-block {
  align-items: flex-start;
}

.inline-editable-field__control {
  min-width: 0;
  flex: 1 1 auto;
}

.inline-editable-field__switch {
  flex: 0 0 auto;
}

.inline-editable-field__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 7px;
  color: #64748b;
  background: #f1f5f9;
  transition: background 0.15s ease, color 0.15s ease;
  flex: 0 0 auto;
}

.inline-editable-field__action:hover:not(:disabled) {
  background: #e2e8f0;
  color: #334155;
}

.inline-editable-field__action.is-save {
  color: #fff;
  background: #0b84ff;
}

.inline-editable-field__action.is-save:hover:not(:disabled) {
  background: #0875e3;
}

.inline-editable-field__action:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.inline-editable-field__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgb(255 255 255 / 45%);
  border-top-color: #fff;
  border-radius: 999px;
  animation: inline-editable-spin 0.8s linear infinite;
}

@keyframes inline-editable-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
