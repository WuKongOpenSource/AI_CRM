<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingAgent ? '编辑智能体' : '添加智能体'"
    :width="isMobile ? '95%' : '460px'"
    :fullscreen="isMobile"
    class="wk-dialog--flush wk-crm-el-field-scope"
  >
    <el-form :model="agentForm" label-position="top">
      <el-form-item label="名称">
        <el-input v-model="agentForm.label" placeholder="智能体名称" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item label="提示词">
        <el-input
          v-model="agentForm.prompt"
          type="textarea"
          :rows="4"
          resize="none"
          placeholder="AI 提示词或快捷指令"
          class="w-full wk-crm-el-field-input"
        />
      </el-form-item>
      <el-form-item label="图标">
        <el-input v-model="agentForm.iconName" placeholder="图标名称（如 Promotion）" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="agentForm.enabled" />
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
import type { AiAgent } from '@/types/common'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  editingAgent: AiAgent | null
  agentForm: {
    label: string
    prompt: string
    iconName: string
    enabled: boolean
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
