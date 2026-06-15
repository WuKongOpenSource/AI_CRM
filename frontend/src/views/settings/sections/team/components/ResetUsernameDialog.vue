<template>
  <el-dialog
    v-model="dialogVisible"
    title="重置用户名"
    :width="isMobile ? '95%' : '460px'"
    :fullscreen="isMobile"
    class="wk-dialog--flush wk-crm-el-field-scope"
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

      <el-form :model="form" label-position="top">
        <el-form-item label="新用户名" required>
          <el-input
            v-model="form.username"
            placeholder="请输入新的登录用户名"
            class="w-full wk-crm-el-field-input"
            size="large"
            clearable
            @keydown.enter.prevent="$emit('save')"
          />
        </el-form-item>

        <el-form-item v-if="requiresPassword" label="当前登录密码" required>
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
  requiresPassword: boolean
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
