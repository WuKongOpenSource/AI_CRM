<template>
  <div>
    <div class="flex items-center justify-between mb-4">
      <h3 class="font-medium">自定义字段管理</h3>
      <el-button type="primary" @click="handleOpenFieldDialog">
        <span class="inline-flex items-center gap-1.5">
          <span class="wk-plus-button-mark" aria-hidden="true">+</span>
          <span>添加字段</span>
        </span>
      </el-button>
    </div>

    <el-tabs v-model="activeEntityType" class="mb-4" @tab-change="loadCustomFields">
      <el-tab-pane label="客户字段" name="customer" />
      <el-tab-pane label="联系人字段" name="contact" />
      <el-tab-pane label="关系字段" name="relation" />
      <el-tab-pane label="产品字段" name="product" />
      <el-tab-pane label="候选人字段" name="candidate" />
    </el-tabs>

    <div v-if="loadingFields" class="text-center py-8">
      <span class="material-symbols-outlined text-slate-300 text-3xl animate-spin">progress_activity</span>
    </div>
    <div v-else-if="customFields.length === 0" class="text-center py-8 text-slate-400">
      暂无自定义字段
    </div>
    <div v-else class="space-y-3">
      <div
        v-for="field in customFields"
        :key="field.fieldId"
        class="flex items-center justify-between p-4 bg-white rounded-lg border border-slate-200 transition-colors"
        :class="{
          'border-primary/50 bg-primary/5': dragOverFieldId === field.fieldId,
          'opacity-70': sortingFields && draggedFieldId === field.fieldId
        }"
        draggable="true"
        @dragstart="handleDragStart($event, field)"
        @dragenter.prevent="handleDragOver($event, field)"
        @dragover.prevent="handleDragOver($event, field)"
        @drop.prevent="handleDrop($event, field)"
        @dragend="handleDragEnd"
      >
        <div class="flex items-center flex-1">
          <button
            v-if="false"
            type="button"
            class="mr-3 text-slate-400 hover:text-slate-600 cursor-grab active:cursor-grabbing"
            title="拖动排序"
          >
            <span class="material-symbols-outlined text-lg">drag_indicator</span>
          </button>
          <div class="mr-4">
            <div class="font-medium">
              {{ field.fieldLabel }}
              <el-tag v-if="field.isRequired" size="small" type="danger" class="ml-2">必填</el-tag>
              <el-tag v-if="field.isUnique" size="small" type="warning" class="ml-2">唯一</el-tag>
            </div>
            <div class="text-sm text-slate-500 mt-1">
              <el-tag size="small">{{ getFieldTypeLabel(field.fieldType) }}</el-tag>
              <el-tag
                size="small"
                :type="field.fieldSource === 'system' ? 'warning' : 'success'"
                class="ml-2"
              >
                {{ field.fieldSource === 'system' ? '系统字段' : '自定义字段' }}
              </el-tag>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <el-switch
            :model-value="field.status === 1"
            @change="(value: boolean) => handleToggleFieldStatus(field, value)"
          />
          <el-button text @click="handleEditField(field)">
            <span class="material-symbols-outlined text-base">edit</span>
          </el-button>
          <el-button v-if="field.fieldSource !== 'system'" text type="danger" @click="confirmDeleteField(field)">
            <span class="material-symbols-outlined text-base">delete</span>
          </el-button>
        </div>
      </div>
    </div>

    <CustomFieldDialog
      :visible="showFieldDialog"
      :is-mobile="isMobile"
      :editing-field="editingField"
      :field-form="fieldForm"
      :submitting="submitting"
      @update:visible="showFieldDialog = $event"
      @field-type-change="handleFieldTypeChange"
      @save="handleSaveField"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import {
  getFieldsByEntity,
  addCustomField,
  updateCustomField,
  updateFieldSort,
  deleteCustomField,
  enableCustomField,
  disableCustomField
} from '@/api/customField'
import type { CustomField, EntityType, FieldOption, FieldType } from '@/types/customField'
import { useEnumStore } from '@/stores/enums'
import { SYSTEM_FIELD_TYPE_LABELS } from '../constants'
import CustomFieldDialog from './components/CustomFieldDialog.vue'

const { isMobile } = useResponsive()
const enumStore = useEnumStore()

const showFieldDialog = ref(false)
const activeEntityType = ref<EntityType>('customer')
const customFields = ref<CustomField[]>([])
const loadingFields = ref(false)
const editingField = ref<CustomField | null>(null)
const submitting = ref(false)
const sortingFields = ref(false)
const draggedFieldId = ref<string | null>(null)
const dragOverFieldId = ref<string | null>(null)

const fieldForm = reactive({
  fieldLabel: '',
  fieldType: 'text' as FieldType,
  placeholder: '',
  defaultValue: '',
  isRequired: false,
  isSearchable: false,
  isShowInList: true,
  isUnique: false,
  options: [] as FieldOption[]
})

onMounted(async () => {
  await loadCustomFields()
})

function getFieldTypeLabel(type: FieldType): string {
  return SYSTEM_FIELD_TYPE_LABELS[type] || type
}

async function loadCustomFields() {
  loadingFields.value = true
  try {
    customFields.value = await getFieldsByEntity(activeEntityType.value)
  } catch {
    // Error handled by interceptor
  } finally {
    loadingFields.value = false
  }
}

function handleDragStart(event: DragEvent, field: CustomField) {
  if (sortingFields.value) return
  draggedFieldId.value = field.fieldId
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.dropEffect = 'move'
    event.dataTransfer.setData('text/plain', field.fieldId)
  }
}

function handleDragOver(event: DragEvent, field: CustomField) {
  if (!draggedFieldId.value || draggedFieldId.value === field.fieldId) return
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
  dragOverFieldId.value = field.fieldId
}

function handleDragEnd() {
  draggedFieldId.value = null
  dragOverFieldId.value = null
}

async function handleDrop(event: DragEvent, targetField: CustomField) {
  const sourceFieldId = draggedFieldId.value || event.dataTransfer?.getData('text/plain') || null
  handleDragEnd()

  if (!sourceFieldId || sourceFieldId === targetField.fieldId || sortingFields.value) {
    return
  }

  const sourceIndex = customFields.value.findIndex(field => field.fieldId === sourceFieldId)
  const targetIndex = customFields.value.findIndex(field => field.fieldId === targetField.fieldId)
  if (sourceIndex < 0 || targetIndex < 0 || sourceIndex === targetIndex) {
    return
  }

  const nextFields = [...customFields.value]
  const [movedField] = nextFields.splice(sourceIndex, 1)
  nextFields.splice(targetIndex, 0, movedField)
  nextFields.forEach((field, index) => {
    field.sortOrder = (index + 1) * 10
  })
  customFields.value = nextFields

  sortingFields.value = true
  try {
    await updateFieldSort(nextFields.map(field => ({
      fieldId: field.fieldId,
      sortOrder: field.sortOrder
    })))
    ElMessage.success('字段排序已更新')
  } catch {
    await loadCustomFields()
  } finally {
    sortingFields.value = false
  }
}

function resetFieldForm() {
  editingField.value = null
  Object.assign(fieldForm, {
    fieldLabel: '',
    fieldType: 'text',
    placeholder: '',
    defaultValue: '',
    isRequired: false,
    isSearchable: false,
    isShowInList: true,
    isUnique: false,
    options: []
  })
}

function handleOpenFieldDialog() {
  editingField.value = null
  resetFieldForm()
  showFieldDialog.value = true
}

function handleEditField(field: CustomField) {
  editingField.value = field
  Object.assign(fieldForm, {
    fieldLabel: field.fieldLabel,
    fieldType: field.fieldType,
    placeholder: field.placeholder || '',
    defaultValue: field.defaultValue || '',
    isRequired: field.isRequired,
    isSearchable: field.isSearchable,
    isShowInList: field.isShowInList,
    isUnique: field.isUnique ?? false,
    options: field.options ? [...field.options] : []
  })
  showFieldDialog.value = true
}

function handleFieldTypeChange(newType: FieldType) {
  if (newType === 'checkbox') {
    fieldForm.defaultValue = false as any
  } else {
    fieldForm.defaultValue = ''
  }
  if (!['select', 'multiselect'].includes(newType)) {
    fieldForm.options = []
  } else if (fieldForm.options.length === 0) {
    fieldForm.options = [{ value: '', label: '' }]
  }
}

async function handleSaveField() {
  const isEditingSystemField = editingField.value?.fieldSource === 'system'

  if (!fieldForm.fieldLabel.trim()) {
    ElMessage.warning('请输入字段标签')
    return
  }

  if (['select', 'multiselect'].includes(fieldForm.fieldType)) {
    const validOptions = fieldForm.options.filter((option) => option.value.trim() && option.label.trim())
    // 系统字段内置选项由后端校验保留；自定义字段要求至少一个有效选项
    if (!isEditingSystemField && validOptions.length === 0) {
      ElMessage.warning('请至少添加一个有效选项')
      return
    }
    fieldForm.options = validOptions
  }

  submitting.value = true
  try {
    if (editingField.value) {
      await updateCustomField({
        fieldId: editingField.value.fieldId,
        fieldLabel: fieldForm.fieldLabel,
        placeholder: fieldForm.placeholder || undefined,
        defaultValue: fieldForm.defaultValue || undefined,
        isRequired: fieldForm.isRequired,
        isSearchable: fieldForm.isSearchable,
        isShowInList: fieldForm.isShowInList,
        isUnique: fieldForm.isUnique,
        options: fieldForm.options.length > 0 ? fieldForm.options : undefined
      })
      ElMessage.success('字段更新成功')
    } else {
      await addCustomField({
        entityType: activeEntityType.value,
        fieldLabel: fieldForm.fieldLabel,
        fieldType: fieldForm.fieldType,
        placeholder: fieldForm.placeholder || undefined,
        defaultValue: fieldForm.defaultValue || undefined,
        isRequired: fieldForm.isRequired,
        isSearchable: fieldForm.isSearchable,
        isShowInList: fieldForm.isShowInList,
        isUnique: fieldForm.isUnique,
        options: fieldForm.options.length > 0 ? fieldForm.options : undefined
      })
      ElMessage.success('字段添加成功')
    }
    showFieldDialog.value = false
    resetFieldForm()
    await loadCustomFields()
    // 选项可能变化（stage/level/relation 等），刷新枚举真相源缓存让各页面立即生效
    enumStore.refreshAll()
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleToggleFieldStatus(field: CustomField, enabled: boolean) {
  try {
    if (enabled) {
      await enableCustomField(field.fieldId)
    } else {
      await disableCustomField(field.fieldId)
    }
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  }
}

async function confirmDeleteField(field: CustomField) {
  if (field.fieldSource === 'system') {
    ElMessage.warning('系统字段不支持删除')
    return
  }

  try {
    await ElMessageBox.confirm('删除字段后该字段数据将不再展示，确定继续吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await handleDeleteField(field)
}

async function handleDeleteField(field: CustomField) {
  try {
    await deleteCustomField(field.fieldId)
    ElMessage.success('字段删除成功')
    await loadCustomFields()
  } catch {
    // Error handled by interceptor
  }
}
</script>
