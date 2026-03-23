<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingField ? '编辑自定义字段' : '添加自定义字段'"
    :width="isMobile ? '95%' : '460px'"
    :fullscreen="isMobile"
    class="wk-dialog--flush wk-crm-el-field-scope"
  >
    <el-form :model="fieldForm" label-position="top">
      <el-form-item label="字段标签" required>
        <el-input v-model="fieldForm.fieldLabel" placeholder="显示名称，如：合同类型" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item v-if="!editingField" label="字段标识" required>
        <el-input v-model="fieldForm.fieldName" placeholder="英文标识，如：contractType" class="w-full wk-crm-el-field-input" size="large" />
        <div class="text-xs text-slate-400 mt-1">只能包含字母、数字、下划线，以字母开头</div>
      </el-form-item>
      <el-form-item v-if="!editingField" label="字段类型" required>
        <el-select
          v-model="fieldForm.fieldType"
          class="w-full wk-crm-el-field-select"
          size="large"
          @change="$emit('field-type-change', $event)"
        >
          <el-option label="单行文本" value="text" />
          <el-option label="多行文本" value="textarea" />
          <el-option label="数字" value="number" />
          <el-option label="日期" value="date" />
          <el-option label="日期时间" value="datetime" />
          <el-option label="单选下拉" value="select" />
          <el-option label="多选下拉" value="multiselect" />
          <el-option label="开关" value="checkbox" />
        </el-select>
      </el-form-item>
      <el-form-item label="占位提示">
        <el-input v-model="fieldForm.placeholder" placeholder="输入框提示文字" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item v-if="!['multiselect', 'checkbox'].includes(fieldForm.fieldType)" label="默认值">
        <el-input-number
          v-if="fieldForm.fieldType === 'number'"
          v-model="fieldForm.defaultValue"
          placeholder="字段默认值"
          :controls="false"
          class="w-full"
          size="large"
        />
        <el-date-picker
          v-else-if="fieldForm.fieldType === 'date'"
          v-model="fieldForm.defaultValue"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择默认日期"
          class="w-full wk-crm-el-field-date"
          size="large"
        />
        <el-date-picker
          v-else-if="fieldForm.fieldType === 'datetime'"
          v-model="fieldForm.defaultValue"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择默认日期时间"
          class="w-full wk-crm-el-field-date"
          size="large"
        />
        <el-select
          v-else-if="fieldForm.fieldType === 'select'"
          v-model="fieldForm.defaultValue"
          placeholder="选择默认值"
          class="w-full wk-crm-el-field-select"
          size="large"
          clearable
        >
          <el-option
            v-for="option in fieldForm.options.filter((item) => item.value && item.label)"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-input v-else v-model="fieldForm.defaultValue" placeholder="字段默认值" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item v-if="fieldForm.fieldType === 'checkbox'" label="默认值">
        <el-switch v-model="fieldForm.defaultValue" />
      </el-form-item>

      <el-form-item v-if="['select', 'multiselect'].includes(fieldForm.fieldType)" label="选项配置">
        <div class="w-full space-y-2">
          <div v-for="(option, index) in fieldForm.options" :key="index" class="flex gap-2">
            <el-input v-model="option.value" placeholder="值" class="w-1/3 min-w-0 wk-crm-el-field-input" size="large" />
            <el-input v-model="option.label" placeholder="显示文字" class="min-w-0 flex-1 wk-crm-el-field-input" size="large" />
            <el-button text type="danger" @click="fieldForm.options.splice(index, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button text @click="fieldForm.options.push({ value: '', label: '' })">
            <span class="inline-flex items-center gap-1.5">
              <span class="wk-plus-button-mark" aria-hidden="true">+</span>
              <span>添加选项</span>
            </span>
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="必填">
        <el-switch v-model="fieldForm.isRequired" />
      </el-form-item>
      <el-form-item label="可搜索">
        <el-switch v-model="fieldForm.isSearchable" />
      </el-form-item>
      <el-form-item label="列表显示">
        <el-switch v-model="fieldForm.isShowInList" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="$emit('save')">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import type { CustomField, FieldOption, FieldType } from '@/types/customField'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  editingField: CustomField | null
  fieldForm: {
    fieldLabel: string
    fieldName: string
    fieldType: FieldType
    placeholder: string
    defaultValue: any
    isRequired: boolean
    isSearchable: boolean
    isShowInList: boolean
    options: FieldOption[]
  }
  submitting: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save'): void
  (e: 'field-type-change', value: FieldType): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})
</script>
