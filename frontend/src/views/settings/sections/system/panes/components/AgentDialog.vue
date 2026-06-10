<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingAgent ? '编辑智能体' : '添加智能体'"
    :width="isMobile ? 'calc(100vw - 2rem)' : '460px'"
    append-to-body
    class="wk-dialog--flush wk-crm-el-field-scope wk-mobile-inset-dialog wk-agent-dialog"
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

<style>
.el-dialog.wk-agent-dialog {
  display: flex;
  flex-direction: column;
  max-height: 88vh;
  overscroll-behavior: none;
}

.el-dialog.wk-agent-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.el-dialog.wk-agent-dialog .el-dialog__header,
.el-dialog.wk-agent-dialog .el-dialog__footer {
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .el-dialog.wk-agent-dialog.wk-mobile-inset-dialog {
    --wk-agent-dialog-top: max(96px, calc(var(--wk-safe-top) + 72px));
    --wk-agent-dialog-bottom: max(16px, var(--wk-safe-bottom));

    max-height: calc(100dvh - var(--wk-agent-dialog-top) - var(--wk-agent-dialog-bottom));
    margin: var(--wk-agent-dialog-top) auto var(--wk-agent-dialog-bottom) !important;
  }
}
</style>
