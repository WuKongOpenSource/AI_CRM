<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingRole ? '编辑角色' : '创建角色'"
    :width="isMobile ? 'calc(100vw - 2rem)' : '460px'"
    append-to-body
    class="wk-dialog--flush wk-crm-el-field-scope wk-mobile-inset-dialog wk-role-dialog"
  >
    <el-form :model="roleForm" label-position="top">
      <el-form-item label="角色名称" required>
        <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" class="w-full wk-crm-el-field-input" size="large" />
      </el-form-item>
      <el-form-item label="角色描述">
        <el-input
          v-model="roleForm.description"
          type="textarea"
          :rows="4"
          resize="none"
          placeholder="请输入角色描述"
          class="w-full wk-crm-el-field-input"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="wk-role-dialog__footer">
        <el-button size="large" @click="dialogVisible = false">取消</el-button>
        <el-button size="large" type="primary" :loading="savingRole" @click="$emit('save')">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RoleVO } from '@/types/role'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  editingRole: RoleVO | null
  roleForm: {
    roleName: string
    description: string
  }
  savingRole: boolean
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
.el-dialog.wk-role-dialog {
  display: flex;
  flex-direction: column;
  max-height: 88vh;
  overscroll-behavior: none;
}

.el-dialog.wk-role-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.el-dialog.wk-role-dialog .el-dialog__header,
.el-dialog.wk-role-dialog .el-dialog__footer {
  flex-shrink: 0;
}

.wk-role-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}

@media (max-width: 767px) {
  .el-dialog.wk-role-dialog.wk-mobile-inset-dialog {
    --wk-role-dialog-top: max(96px, calc(var(--wk-safe-top) + 72px));
    --wk-role-dialog-bottom: max(16px, var(--wk-safe-bottom));

    max-height: calc(100dvh - var(--wk-role-dialog-top) - var(--wk-role-dialog-bottom));
    margin: var(--wk-role-dialog-top) auto var(--wk-role-dialog-bottom) !important;
  }

  .wk-role-dialog__footer .el-button {
    flex: 1;
  }
}
</style>
