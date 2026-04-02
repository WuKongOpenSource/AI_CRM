<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingDept ? '编辑部门' : '添加部门'"
    :width="isMobile ? '95%' : '460px'"
    :fullscreen="isMobile"
    class="wk-dialog--flush wk-crm-el-field-scope"
  >
    <el-form :model="deptForm" label-position="top">
      <el-form-item label="部门名称" required>
        <el-input v-model="deptForm.deptName" placeholder="请输入部门名称" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item label="排序号">
        <el-input-number v-model="deptForm.sortOrder" :min="0" :max="999" class="w-full" size="large" />
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
import type { DeptVO } from '@/types/dept'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  editingDept: DeptVO | null
  deptForm: {
    deptName: string
    sortOrder: number
  }
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
