<template>
  <el-dialog
    v-model="dialogVisible"
    title="重置用户名"
    :width="isMobile ? 'calc(100vw - 2rem)' : '460px'"
    append-to-body
    class="wk-dialog--flush wk-crm-el-field-scope wk-reset-username-dialog wk-mobile-inset-dialog"
  >
    <div class="space-y-5">
      <div class="flex items-center gap-3">
        <div class="size-11 rounded-xl flex items-center justify-center text-white text-sm font-bold bg-slate-900">
          {{ (member?.realname || member?.username || '?').charAt(0) }}
        </div>
        <div class="min-w-0">
          <p class="text-sm font-bold text-slate-900 truncate">{{ member?.realname || member?.username || '-' }}</p>
          <p class="text-xs text-slate-400 truncate">{{ member?.username || '-' }}</p>
        </div>
      </div>

      <el-alert
        v-if="member?.tenantCreator"
        type="warning"
        :closable="false"
        show-icon
        title="该账号为企业创建者，重置后会同步企业联系人邮箱。"
      />

      <el-form :model="form" label-position="top">
        <el-form-item label="新用户名" required>
          <el-input
            v-model="form.username"
            :placeholder="member?.tenantCreator ? '请输入新的邮箱用户名' : '请输入新的登录用户名'"
            class="w-full wk-crm-el-field-input"
            size="large"
            clearable
            @keydown.enter.prevent="$emit('save')"
          />
        </el-form-item>

        <el-form-item v-if="member?.tenantCreator" label="当前登录密码" required>
          <el-input
            v-model="form.currentPassword"
            type="password"
            show-password
            autocomplete="current-password"
            placeholder="请输入当前登录密码"
            class="w-full wk-crm-el-field-input"
            size="large"
            @keydown.enter.prevent="$emit('save')"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <div class="flex gap-3 w-full">
        <el-button class="flex-1" size="large" @click="dialogVisible = false">取消</el-button>
        <el-button class="flex-1" size="large" type="primary" :loading="submitting" @click="$emit('save')">
          确认重置
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  visible: boolean
  isMobile: boolean
  member: any
  form: {
    username: string
    currentPassword: string
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
.el-dialog.wk-reset-username-dialog {
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  margin-top: 5vh !important;
}

.el-dialog.wk-reset-username-dialog.is-fullscreen {
  max-height: none;
  height: 100%;
  margin: 0 !important;
}

.el-dialog.wk-reset-username-dialog .el-dialog__body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.el-dialog.wk-reset-username-dialog .el-dialog__footer {
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .el-dialog.wk-reset-username-dialog.wk-mobile-inset-dialog {
    --wk-reset-username-dialog-top: max(96px, calc(var(--wk-safe-top) + 72px));
    --wk-reset-username-dialog-bottom: max(16px, var(--wk-safe-bottom));

    max-height: calc(100dvh - var(--wk-reset-username-dialog-top) - var(--wk-reset-username-dialog-bottom));
    margin: var(--wk-reset-username-dialog-top) auto var(--wk-reset-username-dialog-bottom) !important;
  }
}
</style>
