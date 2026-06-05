<template>
  <div
    ref="anchorRef"
    class="inline-editable-field"
    :class="{
      'is-editing': editing,
      'is-disabled': !editable,
      'is-click-reveal': revealEditOnClick,
      'is-edit-action-visible': editActionVisible
    }"
  >
    <template v-if="editing">
      <!-- 与展示态同结构占位，避免行高被 min-height 撑变；不可见且不响应事件 -->
      <div class="inline-editable-field__ghost" aria-hidden="true">
        <div class="inline-editable-field__display">
          <div class="inline-editable-field__content">
            <slot :value="modelValue" :display-value="resolvedDisplayValue">
              <span class="inline-editable-field__fallback">{{ resolvedDisplayValue }}</span>
            </slot>
          </div>
        </div>
      </div>
      <Teleport to="body">
        <div
          ref="floatPanelRef"
          class="inline-editable-field__float"
          :style="floatStyle"
          data-row-action="true"
          @click.stop
        >
          <div
            class="inline-editable-field__editor"
            :class="{ 'is-block': resolvedFieldType === 'textarea' }"
          >
        <div class="inline-editable-field__editor-main">
          <el-select
            v-if="resolvedFieldType === 'select'"
            ref="editorRef"
            v-model="draftValue"
            :placeholder="resolvedPlaceholder"
            clearable
            filterable
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
            class="inline-editable-field__control"
            @keyup.enter="commit"
            @keyup.esc="cancel"
          />

          <el-switch
            v-else-if="resolvedFieldType === 'checkbox'"
            v-model="draftValue"
            class="inline-editable-field__switch"
            @keyup.enter="commit"
            @keyup.esc="cancel"
          />

          <el-input
            v-else-if="resolvedFieldType === 'textarea'"
            ref="editorRef"
            v-model="draftValue"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
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
            class="inline-editable-field__control"
            @keyup.enter="commit"
            @keyup.esc="cancel"
          />
        </div>

        <div class="inline-editable-field__editor-actions">
          <button
            type="button"
            class="inline-editable-field__btn inline-editable-field__btn--secondary"
            :disabled="saving"
            @click="cancel"
          >
            取消
          </button>
          <button
            type="button"
            class="inline-editable-field__btn inline-editable-field__btn--primary"
            :disabled="saving"
            @click="commit"
          >
            <span v-if="saving" class="inline-editable-field__spinner inline-editable-field__spinner--btn"></span>
            <span v-else>保存</span>
          </button>
        </div>
          </div>
        </div>
      </Teleport>
    </template>

    <template v-else>
      <div class="inline-editable-field__display" @click="handleDisplayClick">
        <div class="inline-editable-field__content">
          <slot :value="modelValue" :display-value="resolvedDisplayValue">
            <span class="inline-editable-field__fallback" :title="resolvedDisplayValue">
              {{ resolvedDisplayValue }}
            </span>
          </slot>
        </div>
        <button
          v-if="editable && (!revealEditOnClick || editActionVisible)"
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
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
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
  revealEditOnClick?: boolean
  required?: boolean
  saveHandler?: SaveHandler
}>(), {
  field: null,
  options: () => [],
  displayValue: '',
  placeholder: '',
  emptyText: '-',
  editable: true,
  revealEditOnClick: false,
  required: false,
  saveHandler: undefined
})

const emit = defineEmits<{
  (e: 'saved', value: any): void
  (e: 'cancel'): void
}>()

const editing = ref(false)
const editActionVisible = ref(false)
const saving = ref(false)
const draftValue = ref<any>(null)
const editorRef = ref<any>(null)
const anchorRef = ref<HTMLElement | null>(null)
const floatPanelRef = ref<HTMLElement | null>(null)
const floatStyle = ref<Record<string, string>>({})

const FLOAT_MARGIN = 12
/* 浮动面板宽度约为原设计的 3/4，避免在表格上占得过宽 */
const FLOAT_MIN_WIDTH = 216
const FLOAT_MAX_WIDTH = 300
/** 相对单元格底边向上偏移，使面板略盖住原内容（像素） */
const FLOAT_OVERLAP_UP = 28
/** 普通页面内联编辑时的基准层；在抽屉/对话框内会按遮罩层抬高 */
const FLOAT_Z_INDEX_BASE = 1900

let floatListenersBound = false
let tableScrollEl: HTMLElement | null = null
let floatResizeObserver: ResizeObserver | null = null

function onFloatReposition() {
  requestAnimationFrame(() => updateFloatPosition())
}

/** 锚点在 ElDrawer / ElDialog 等 Teleport 遮罩内时，浮动层也在 body 上，必须高于 .el-overlay 的 z-index，否则会整层被挡住看起来像「点了没反应」 */
function resolveFloatZIndex(anchor: HTMLElement): number {
  const overlay = anchor.closest?.('.el-overlay') as HTMLElement | null
  if (!overlay) return FLOAT_Z_INDEX_BASE
  const raw = window.getComputedStyle(overlay).zIndex
  const parsed = raw === 'auto' ? NaN : Number.parseInt(raw, 10)
  if (!Number.isFinite(parsed) || parsed <= 0) return FLOAT_Z_INDEX_BASE
  return Math.max(FLOAT_Z_INDEX_BASE, parsed + 1)
}

function updateFloatPosition() {
  const anchor = anchorRef.value
  const panel = floatPanelRef.value
  if (!anchor || !panel) return

  const ar = anchor.getBoundingClientRect()
  const vw = window.innerWidth
  const vh = window.innerHeight
  const targetW = Math.min(
    FLOAT_MAX_WIDTH,
    Math.max(FLOAT_MIN_WIDTH, vw - FLOAT_MARGIN * 2)
  )
  /* 左缘与锚点一致，表单/抽屉里与上方字段标题对齐；窄视口则贴边夹紧 */
  let left = ar.left
  left = Math.max(FLOAT_MARGIN, Math.min(left, vw - targetW - FLOAT_MARGIN))

  const gap = 6
  const ph = panel.getBoundingClientRect().height
  // 默认在锚点下方但整体上移，盖住单元格文案；空间不足时改到锚点上方
  let top = ar.bottom + gap - FLOAT_OVERLAP_UP
  if (top + ph > vh - FLOAT_MARGIN) {
    top = ar.top - ph - gap
  }
  if (top < FLOAT_MARGIN) {
    top = FLOAT_MARGIN
  }

  floatStyle.value = {
    position: 'fixed',
    left: `${Math.round(left)}px`,
    top: `${Math.round(top)}px`,
    width: `${Math.round(targetW)}px`,
    /* 在遮罩（抽屉/对话框）内时高于遮罩；普通场景保持 1900，下拉/日期打开时会再申请更高 z-index */
    zIndex: String(resolveFloatZIndex(anchor))
  }
}

function bindFloatListeners() {
  if (floatListenersBound) return
  floatListenersBound = true
  window.addEventListener('scroll', onFloatReposition, true)
  window.addEventListener('resize', onFloatReposition)
  tableScrollEl = anchorRef.value?.closest?.('.el-table__body-wrapper') ?? null
  tableScrollEl?.addEventListener('scroll', onFloatReposition, true)
  const el = floatPanelRef.value
  if (el && typeof ResizeObserver !== 'undefined') {
    floatResizeObserver = new ResizeObserver(onFloatReposition)
    floatResizeObserver.observe(el)
  }
}

function teardownFloatListeners() {
  if (!floatListenersBound) return
  floatListenersBound = false
  window.removeEventListener('scroll', onFloatReposition, true)
  window.removeEventListener('resize', onFloatReposition)
  tableScrollEl?.removeEventListener('scroll', onFloatReposition, true)
  tableScrollEl = null
  floatResizeObserver?.disconnect()
  floatResizeObserver = null
}

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

const revealEditOnClick = computed(() => props.revealEditOnClick)

function handleDisplayClick() {
  if (!props.editable || editing.value || !props.revealEditOnClick) return
  editActionVisible.value = true
}

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
  editActionVisible.value = false
  draftValue.value = createDraftValue(props.modelValue)
  editing.value = true
  await nextTick()
  await nextTick()
  requestAnimationFrame(() => {
    updateFloatPosition()
    bindFloatListeners()
    requestAnimationFrame(() => {
      updateFloatPosition()
      const editor = editorRef.value
      if (editor?.focus) editor.focus()
      else if (editor?.$el?.querySelector) editor.$el.querySelector('input, textarea')?.focus()
    })
  })
}

function cancel() {
  if (saving.value) return
  teardownFloatListeners()
  floatStyle.value = {}
  editing.value = false
  editActionVisible.value = false
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
    teardownFloatListeners()
    floatStyle.value = {}
    editing.value = false
    editActionVisible.value = false
    emit('saved', submitValue)
  } finally {
    saving.value = false
  }
}

watch(() => [props.modelValue, resolvedFieldName.value], () => {
  if (!editing.value) {
    draftValue.value = createDraftValue(props.modelValue)
    editActionVisible.value = false
  }
})

watch(() => props.revealEditOnClick, () => {
  editActionVisible.value = false
})

watch(editing, (v) => {
  if (!v) {
    teardownFloatListeners()
    floatStyle.value = {}
  }
})

onUnmounted(() => {
  teardownFloatListeners()
})
</script>

<style scoped>
.inline-editable-field {
  min-width: 0;
  width: 100%;
}

.inline-editable-field.is-editing {
  position: relative;
}

.inline-editable-field__ghost {
  visibility: hidden;
  pointer-events: none;
  user-select: none;
  width: 100%;
}

.inline-editable-field__ghost .inline-editable-field__display {
  width: 100%;
}

.inline-editable-field__float {
  box-sizing: border-box;
  pointer-events: auto;
}

.inline-editable-field__float .inline-editable-field__editor {
  overflow-x: visible;
  padding: 12px 14px;
  box-shadow:
    0 12px 36px rgba(15, 23, 42, 0.14),
    0 0 0 1px rgba(15, 23, 42, 0.06);
}

.inline-editable-field__float .inline-editable-field__editor-main {
  overflow-x: visible;
}

.inline-editable-field__float .inline-editable-field__editor-actions {
  justify-content: flex-end;
  flex-wrap: nowrap;
}

.inline-editable-field__float .inline-editable-field__btn {
  flex: 0 0 auto;
  min-width: 72px;
  padding: 0 16px;
  font-size: 13px;
}

.inline-editable-field__display {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0;
  min-width: 0;
  width: 100%;
  max-width: 100%;
}

.inline-editable-field.is-click-reveal:not(.is-disabled):not(.is-editing) .inline-editable-field__display {
  cursor: pointer;
}

.inline-editable-field__content {
  min-width: 0;
  max-width: 100%;
  flex: 1 1 auto;
  padding-right: 0;
  box-sizing: border-box;
}

/* 悬停/聚焦编辑钮时预留宽度，避免绝对定位按钮盖住省略号文案 */
.inline-editable-field:not(.is-click-reveal):has(.inline-editable-field__edit) .inline-editable-field__display:hover .inline-editable-field__content,
.inline-editable-field:has(.inline-editable-field__edit:focus-visible) .inline-editable-field__display .inline-editable-field__content,
.inline-editable-field.is-edit-action-visible .inline-editable-field__content {
  padding-right: 32px;
}

.inline-editable-field__fallback {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-editable-field__edit {
  position: absolute;
  top: 50%;
  right: 0;
  z-index: 1;
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
  transform: translateY(-50%);
  transition: background 0.15s ease;
  pointer-events: none;
}

.inline-editable-field__edit:hover {
  background: #d6ebff;
}

.inline-editable-field:not(.is-click-reveal) .inline-editable-field__display:hover .inline-editable-field__edit,
.inline-editable-field__edit:focus-visible,
.inline-editable-field.is-edit-action-visible .inline-editable-field__edit {
  opacity: 1;
  pointer-events: auto;
}

.inline-editable-field__edit .material-symbols-outlined {
  font-size: 17px;
  line-height: 1;
}

/* 无 hover 的触屏设备：编辑钮不能长期 pointer-events:none，否则无法点开 */
@media (hover: none) {
  .inline-editable-field:not(.is-click-reveal):has(.inline-editable-field__edit) .inline-editable-field__content,
  .inline-editable-field.is-edit-action-visible .inline-editable-field__content {
    padding-right: 32px;
  }

  .inline-editable-field:not(.is-click-reveal) .inline-editable-field__edit,
  .inline-editable-field.is-edit-action-visible .inline-editable-field__edit {
    opacity: 0.92;
    pointer-events: auto;
  }
}

.inline-editable-field__editor {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
  padding: 8px 8px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow:
    0 4px 18px rgba(15, 23, 42, 0.08),
    0 0 0 1px rgba(15, 23, 42, 0.04);
  overflow-x: hidden;
}

.inline-editable-field__editor.is-block .inline-editable-field__editor-main {
  align-items: stretch;
}

.inline-editable-field__editor-main {
  display: flex;
  align-items: center;
  min-width: 0;
  width: 100%;
  max-width: 100%;
  flex: 1 1 auto;
  overflow-x: hidden;
}

/* 窄列：flex-end + nowrap 会把按钮整体顶到右侧，多出的宽度向左溢出盖住左列；改为均分铺满当前单元格 */
.inline-editable-field__editor-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  gap: 6px;
  width: 100%;
  min-width: 0;
  flex-shrink: 0;
}

.inline-editable-field__control {
  min-width: 0;
  width: 100%;
  flex: 1 1 auto;
}

.inline-editable-field__editor :deep(.el-select),
.inline-editable-field__editor :deep(.el-input),
.inline-editable-field__editor :deep(.el-date-editor) {
  width: 100% !important;
  max-width: 100%;
  min-width: 0;
}

.inline-editable-field__editor :deep(.el-select .el-select__wrapper),
.inline-editable-field__editor :deep(.el-input__wrapper) {
  min-width: 0 !important;
}

.inline-editable-field__switch {
  flex: 0 0 auto;
}

.inline-editable-field__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 1 1 0;
  min-width: 0;
  min-height: 32px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  border-radius: 8px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease, opacity 0.15s ease;
  white-space: nowrap;
}

.inline-editable-field__btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.inline-editable-field__btn--secondary {
  color: #475569;
  background: #f8fafc;
  border-color: #e2e8f0;
}

.inline-editable-field__btn--secondary:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #cbd5e1;
  color: #334155;
}

.inline-editable-field__btn--primary {
  color: #fff;
  background: #0b84ff;
  border-color: #0b84ff;
}

.inline-editable-field__btn--primary:hover:not(:disabled) {
  background: #0875e3;
  border-color: #0875e3;
}

.inline-editable-field__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgb(255 255 255 / 45%);
  border-top-color: #fff;
  border-radius: 999px;
  animation: inline-editable-spin 0.8s linear infinite;
}

.inline-editable-field__spinner--btn {
  display: block;
}

@keyframes inline-editable-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
