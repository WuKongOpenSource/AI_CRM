<template>
  <el-drawer
    v-model="open"
    title="联系人详情"
    :size="isMobile ? '100%' : '400px'"
    destroy-on-close
  >
    <template v-if="contact">
      <div class="space-y-4">
        <div class="mb-6 flex items-center gap-3">
          <div class="flex size-14 items-center justify-center rounded-xl bg-primary/10 text-xl font-bold text-primary">
            {{ contact.name?.charAt(0) }}
          </div>
          <div>
            <h3 class="text-lg font-bold text-slate-900">{{ contact.name }}</h3>
            <p v-if="contact.position" class="text-sm text-slate-500">{{ contact.position }}</p>
            <span v-if="contact.isPrimary" class="rounded bg-emerald-50 px-2 py-0.5 text-xs font-bold text-emerald-600">主联系人</span>
          </div>
        </div>

        <div class="space-y-3">
          <div class="rounded-xl bg-slate-50 p-3">
            <p class="mb-1 text-xs font-bold uppercase text-slate-400">电话</p>
            <p class="text-sm text-slate-900">{{ contact.phone || '-' }}</p>
          </div>
          <div class="rounded-xl bg-slate-50 p-3">
            <p class="mb-1 text-xs font-bold uppercase text-slate-400">邮箱</p>
            <p class="text-sm text-slate-900">{{ contact.email || '-' }}</p>
          </div>
          <div class="rounded-xl bg-slate-50 p-3">
            <p class="mb-1 text-xs font-bold uppercase text-slate-400">微信</p>
            <p class="text-sm text-slate-900">{{ contact.wechat || '-' }}</p>
          </div>
          <div class="rounded-xl bg-slate-50 p-3">
            <p class="mb-1 text-xs font-bold uppercase text-slate-400">备注</p>
            <p class="text-sm text-slate-900">{{ contact.notes || '-' }}</p>
          </div>
          <div class="rounded-xl bg-slate-50 p-3">
            <p class="mb-1 text-xs font-bold uppercase text-slate-400">创建时间</p>
            <p class="text-sm text-slate-900">{{ contact.createTime ? formatDateTime(contact.createTime) : '-' }}</p>
          </div>
        </div>

        <div class="mt-6 flex gap-2">
          <button
            v-if="canEdit"
            type="button"
            class="rounded-xl bg-primary px-4 py-2 text-xs font-bold text-white transition-all hover:bg-primary/90"
            @click="handleEdit"
          >
            编辑
          </button>
          <button
            v-if="contact && !contact.isPrimary && canSetPrimary"
            type="button"
            class="rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-bold text-slate-700 transition-all hover:bg-slate-50"
            @click="handleSetPrimary"
          >
            设为主联系人
          </button>
          <el-popconfirm
            v-if="canDelete"
            title="确定删除该联系人吗？"
            confirm-button-text="删除"
            cancel-button-text="取消"
            @confirm="handleDelete"
          >
            <template #reference>
              <button type="button" class="rounded-xl border border-red-200 bg-white px-4 py-2 text-xs font-bold text-red-500 transition-all hover:bg-red-50">
                删除
              </button>
            </template>
          </el-popconfirm>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useResponsive } from '@/composables/useResponsive'
import type { Contact } from '@/types/customer'

const props = withDefaults(defineProps<{
  modelValue: boolean
  contact?: Contact | null
  canEdit?: boolean
  canDelete?: boolean
  canSetPrimary?: boolean
}>(), {
  contact: null,
  canEdit: false,
  canDelete: false,
  canSetPrimary: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'edit', contact: Contact): void
  (e: 'delete', contactId: string): void
  (e: 'set-primary', contactId: string): void
}>()

const { isMobile } = useResponsive()

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

function handleEdit() {
  if (!props.contact) return
  emit('edit', props.contact)
  open.value = false
}

function handleSetPrimary() {
  if (!props.contact) return
  emit('set-primary', props.contact.contactId)
  open.value = false
}

function handleDelete() {
  if (!props.contact) return
  emit('delete', props.contact.contactId)
  open.value = false
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>
