<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingDept ? '编辑部门' : '新增部门'"
    :width="isMobile ? '95%' : '420px'"
    :fullscreen="isMobile"
    class="wk-dialog--flush wk-crm-el-field-scope wk-dept-dialog"
  >
    <el-form :model="deptForm" label-position="top">
      <el-form-item label="部门名称" required>
        <el-input v-model="deptForm.deptName" placeholder="请输入部门名称" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item label="上级部门">
        <el-select
          v-model="deptForm.parentId"
          placeholder="请选择上级部门"
          filterable
          class="w-full wk-crm-el-field-select"
          size="large"
        >
          <el-option label="无（一级部门）" value="0" />
          <el-option
            v-for="option in parentDeptOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          >
            <span
              class="wk-dept-dialog__option-label"
              :style="{ paddingLeft: `${option.depth * 18}px` }"
            >
              {{ option.label }}
            </span>
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="flex gap-3 w-full">
        <el-button class="flex-1" size="large" @click="dialogVisible = false">取消</el-button>
        <el-button class="flex-1" size="large" type="primary" :loading="submitting" @click="$emit('save')">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DeptVO } from '@/types/dept'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  editingDept: DeptVO | null
  deptForm: {
    deptName: string
    parentId: string
    sortOrder: number
  }
  parentDeptOptions: Array<{
    label: string
    value: string
    depth: number
  }>
  submitting: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'save'): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
})
</script>

<style>
.el-dialog.wk-dept-dialog .el-dialog__footer {
  padding-top: 8px;
}

.wk-dept-dialog__option-label {
  display: block;
}
</style>
