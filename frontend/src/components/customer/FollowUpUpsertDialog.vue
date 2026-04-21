<template>
  <Teleport to="body">
    <Transition name="wk-follow-up-modal">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
        role="dialog"
        aria-modal="true"
        @keydown.esc="handleClose"
      >
        <div
          class="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
          aria-hidden="true"
          @click="handleClose"
        />
        <div
          class="wk-follow-up-panel relative flex max-h-[90vh] w-full max-w-lg flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
          @click.stop
        >
          <div class="flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50/50 px-6 py-4">
            <div class="flex min-w-0 items-center gap-3">
              <div
                class="flex size-8 shrink-0 items-center justify-center rounded-lg text-white shadow-sm"
                :class="headerIconBgClass"
              >
                <span class="material-symbols-outlined text-sm">{{ headerIconName }}</span>
              </div>
              <div class="min-w-0">
                <h3 class="text-sm font-bold text-slate-900">{{ dialogTitle }}</h3>
                <p class="text-[10px] font-bold uppercase tracking-widest text-slate-400">{{ headerSubtitle }}</p>
              </div>
            </div>
            <button
              type="button"
              class="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200"
              aria-label="关闭"
              @click="handleClose"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="min-h-0 flex-1 space-y-6 overflow-y-auto p-6">
            <div class="flex flex-col gap-1.5">
              <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-400">记录内容</label>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="6"
                resize="none"
                placeholder="输入跟进内容..."
                class="follow-up-content-input w-full"
              />
            </div>

            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div class="flex flex-col gap-1.5">
                <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-400">跟进类型</label>
                <el-select v-model="form.type" class="follow-up-control block w-full" placeholder="选择类型">
                  <el-option label="电话" value="call" />
                  <el-option label="会议" value="meeting" />
                  <el-option label="邮件" value="email" />
                  <el-option label="拜访" value="visit" />
                  <el-option label="其他" value="other" />
                </el-select>
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-400">跟进时间</label>
                <el-date-picker
                  v-model="form.followTime"
                  type="datetime"
                  class="follow-up-control block w-full"
                  placeholder="选择跟进时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                />
              </div>
            </div>

            <div class="flex w-full flex-col gap-1.5">
              <label class="block text-[10px] font-bold uppercase tracking-wider text-slate-400">下次联系时间</label>
              <el-date-picker
                v-model="form.nextFollowTime"
                type="datetime"
                class="follow-up-control block w-full"
                placeholder="选择下次联系时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                clearable
              />
            </div>

            <!-- <div class="space-y-3">
              <div class="flex items-center justify-between">
                <label class="text-[10px] font-bold uppercase tracking-wider text-slate-400">附件管理</label>
                <button
                  type="button"
                  class="inline-flex items-center gap-1 text-[10px] font-bold text-primary/50"
                  disabled
                  title="即将支持"
                >
                  <span class="material-symbols-outlined text-xs">add</span>
                  添加附件
                </button>
              </div>
              <div class="rounded-xl border-2 border-dashed border-slate-100 py-4 text-center">
                <p class="text-[10px] font-medium text-slate-400">暂无附件</p>
              </div>
            </div> -->
          </div>

          <div class="flex shrink-0 gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-4">
            <button
              type="button"
              class="flex-1 rounded-xl border border-slate-200 py-2.5 text-sm font-bold text-slate-600 transition-all hover:bg-white"
              @click="handleClose"
            >
              取消
            </button>
            <el-button
              type="primary"
              class="!m-0 flex-1 !h-auto !rounded-xl !py-2.5 !text-sm !font-bold shadow-lg shadow-primary/20"
              :loading="submitting"
              @click="handleSubmit"
            >
              {{ primaryActionLabel }}
            </el-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FollowUp, FollowUpType } from '@/types/customer'

export type FollowUpUpsertSubmitPayload = {
  mode: 'add' | 'edit'
  followUpId?: string
  customerId: string
  type: FollowUpType
  content: string
  followTime: string
  nextFollowTime?: string
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    customerId: string
    editingFollowUp: FollowUp | null
    submitting?: boolean
  }>(),
  { submitting: false }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: FollowUpUpsertSubmitPayload]
}>()

const FOLLOW_UP_TYPES = new Set<FollowUpType>(['call', 'meeting', 'email', 'visit', 'other'])

function formatDateForApi(date: Date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

function normalizeFollowUpType(type?: string): FollowUpType {
  return type && FOLLOW_UP_TYPES.has(type as FollowUpType) ? (type as FollowUpType) : 'call'
}

function normalizeDateTimeValue(value?: string): string {
  if (!value) return ''
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(value)) {
    return value
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : formatDateForApi(parsed)
}

function getFollowUpTypeLabel(type: string): string {
  const labels: Record<string, string> = {
    call: '电话',
    meeting: '会议',
    email: '邮件',
    visit: '拜访',
    other: '其他'
  }
  return labels[type] || type
}

function getFollowUpIconName(type: FollowUpType): string {
  const icons: Record<FollowUpType, string> = {
    call: 'call',
    meeting: 'groups',
    email: 'mail',
    visit: 'location_on',
    other: 'edit_note'
  }
  return icons[type] || 'edit_note'
}

const form = reactive({
  type: 'call' as FollowUpType,
  content: '',
  followTime: '',
  nextFollowTime: ''
})

const isEditing = computed(() => !!props.editingFollowUp)

const dialogTitle = computed(() => (isEditing.value ? '编辑跟进记录' : '添加跟进记录'))

const primaryActionLabel = computed(() => (isEditing.value ? '保存修改' : '添加'))

const isAiFollowUp = computed(() => (props.editingFollowUp?.aiGenerated ?? 0) > 0)

const headerIconBgClass = computed(() => (isAiFollowUp.value ? 'bg-primary' : 'bg-slate-400'))

const headerIconName = computed(() => {
  if (isAiFollowUp.value) return 'auto_awesome'
  return getFollowUpIconName(form.type)
})

const headerSubtitle = computed(() => getFollowUpTypeLabel(form.type))

function resetForm() {
  form.type = 'call'
  form.content = ''
  form.followTime = formatDateForApi()
  form.nextFollowTime = ''
}

watch(
  () => [props.modelValue, props.editingFollowUp] as const,
  ([visible, edit]) => {
    if (!visible) return
    if (edit) {
      form.type = normalizeFollowUpType(edit.type)
      form.content = edit.content || ''
      form.followTime = normalizeDateTimeValue(edit.followTime)
      form.nextFollowTime = normalizeDateTimeValue(edit.nextFollowTime || '')
    } else {
      resetForm()
    }
  },
  { flush: 'post' }
)

function handleClose() {
  emit('update:modelValue', false)
}

function handleSubmit() {
  if (!form.content.trim()) {
    ElMessage.warning('请输入跟进内容')
    return
  }
  if (!form.followTime) {
    ElMessage.warning('请选择跟进时间')
    return
  }
  const payload: FollowUpUpsertSubmitPayload = {
    mode: isEditing.value ? 'edit' : 'add',
    followUpId: props.editingFollowUp?.followUpId,
    customerId: props.customerId,
    type: form.type,
    content: form.content.trim(),
    followTime: form.followTime,
    nextFollowTime: form.nextFollowTime || undefined
  }
  emit('submit', payload)
}
</script>

<style scoped>
.wk-follow-up-modal-enter-active,
.wk-follow-up-modal-leave-active {
  transition: opacity 0.2s ease;
}

.wk-follow-up-modal-enter-active .wk-follow-up-panel,
.wk-follow-up-modal-leave-active .wk-follow-up-panel {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.wk-follow-up-modal-enter-from,
.wk-follow-up-modal-leave-to {
  opacity: 0;
}

.wk-follow-up-modal-enter-from .wk-follow-up-panel,
.wk-follow-up-modal-leave-to .wk-follow-up-panel {
  opacity: 0;
  transform: scale(0.95) translateY(12px);
}

.follow-up-content-input :deep(.el-textarea__inner) {
  min-height: 150px;
  border-radius: 0.75rem;
  border-color: rgb(226 232 240);
  background-color: rgb(248 250 252);
  padding: 1rem;
  font-size: 0.875rem;
  line-height: 1.5;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.follow-up-content-input :deep(.el-textarea__inner:hover) {
  border-color: rgb(203 213 225);
}

.follow-up-content-input :deep(.el-textarea__inner:focus) {
  border-color: var(--el-color-primary);
  background-color: #fff;
  box-shadow: 0 0 0 2px var(--el-color-primary-light-8);
}

/* 避免 label（默认 inline）与 date-picker 根节点同一行并排 */
.follow-up-control :deep(.el-date-editor) {
  display: block;
  width: 100%;
  max-width: 100%;
}

.follow-up-control :deep(.el-input__wrapper),
.follow-up-control :deep(.el-select__wrapper) {
  border-radius: 0.75rem;
  background-color: rgb(248 250 252);
  box-shadow: 0 0 0 1px rgb(226 232 240) inset;
}

.follow-up-control :deep(.el-input__wrapper:hover),
.follow-up-control :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px rgb(203 213 225) inset;
}

.follow-up-control :deep(.el-input__wrapper.is-focus),
.follow-up-control :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}
</style>
