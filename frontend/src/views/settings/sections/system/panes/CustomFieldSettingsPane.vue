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
        class="flex items-center justify-between p-4 bg-white rounded-lg border border-slate-200"
      >
        <div class="flex items-center flex-1">
          <div class="mr-4">
            <div class="font-medium">
              {{ field.fieldLabel }}
              <el-tag v-if="field.isRequired" size="small" type="danger" class="ml-2">必填</el-tag>
            </div>
            <div class="text-sm text-slate-500 mt-1">
              <el-tag size="small">{{ getFieldTypeLabel(field.fieldType) }}</el-tag>
              <span v-if="field.options && field.options.length > 0" class="ml-2">
                选项: {{ field.options.map((item) => item.label).join(', ') }}
              </span>
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
          <el-button text type="danger" @click="confirmDeleteField(field)">
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
  deleteCustomField,
  enableCustomField,
  disableCustomField
} from '@/api/customField'
import type { CustomField, EntityType, FieldOption, FieldType } from '@/types/customField'
import { SYSTEM_FIELD_TYPE_LABELS } from '../constants'
import CustomFieldDialog from './components/CustomFieldDialog.vue'

const { isMobile } = useResponsive()

const showFieldDialog = ref(false)
const activeEntityType = ref<EntityType>('customer')
const customFields = ref<CustomField[]>([])
const loadingFields = ref(false)
const editingField = ref<CustomField | null>(null)
const submitting = ref(false)

const fieldForm = reactive({
  fieldLabel: '',
  fieldType: 'text' as FieldType,
  placeholder: '',
  defaultValue: '',
  isRequired: false,
  isSearchable: false,
  isShowInList: true,
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
  if (!fieldForm.fieldLabel.trim()) {
    ElMessage.warning('请输入字段标签')
    return
  }

  if (['select', 'multiselect'].includes(fieldForm.fieldType)) {
    const validOptions = fieldForm.options.filter((option) => option.value.trim() && option.label.trim())
    if (validOptions.length === 0) {
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
        options: fieldForm.options.length > 0 ? fieldForm.options : undefined
      })
      ElMessage.success('字段添加成功')
    }
    showFieldDialog.value = false
    resetFieldForm()
    await loadCustomFields()
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
