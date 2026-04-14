<template>
  <div class="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow">
    <div class="flex items-start justify-between gap-4">
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-3">
          <div class="size-10 rounded-xl flex items-center justify-center text-white shadow-sm bg-primary">
            <span class="material-symbols-outlined text-base">{{ getFollowUpIcon(item.type) }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-2">
              <h4 class="font-bold text-slate-900 text-base leading-tight">
                {{ item.summary || getFollowUpTypeLabel(item.type) }}
              </h4>
              <span
                v-if="item.aiGenerated"
                class="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-bold text-primary"
              >
                AI 智能生成
              </span>
              <span
                v-if="item.sceneType"
                class="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-500"
              >
                {{ item.sceneType }}
              </span>
              <span class="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-500">
                {{ getFollowUpTypeLabel(item.type) }}
              </span>
              <span
                v-if="item.createUserName"
                class="inline-flex items-center rounded-full border border-slate-200 px-2 py-0.5 text-[11px] font-medium text-slate-500"
              >
                {{ item.createUserName }}
              </span>
            </div>
          </div>
        </div>

        <p class="mt-4 text-sm leading-7 text-slate-700 whitespace-pre-line">{{ item.content }}</p>

        <div class="mt-4 flex flex-wrap items-center gap-3 text-xs">
          <span class="inline-flex items-center gap-1.5 rounded-xl bg-slate-50 px-3 py-2 text-slate-500">
            <span class="material-symbols-outlined text-sm">schedule</span>
            跟进时间 {{ formatDateTime(item.followTime || item.createTime) }}
          </span>
          <span
            v-if="item.nextFollowTime"
            class="inline-flex items-center gap-1.5 rounded-xl bg-amber-50 px-3 py-2 text-amber-700"
          >
            <span class="material-symbols-outlined text-sm">event_repeat</span>
            下次联系 {{ formatDateTime(item.nextFollowTime) }}
          </span>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <button
          v-if="canEdit"
          type="button"
          class="rounded-lg p-2 text-slate-300 transition hover:bg-slate-50 hover:text-primary"
          @click="$emit('edit', item)"
        >
          <span class="material-symbols-outlined text-base">edit</span>
        </button>
        <button
          v-if="canDelete"
          type="button"
          class="rounded-lg p-2 text-slate-300 transition hover:bg-slate-50 hover:text-red-500"
          @click="$emit('delete', item.followUpId)"
        >
          <span class="material-symbols-outlined text-base">delete</span>
        </button>
      </div>
    </div>

    <div v-if="attachments.length > 0" class="mt-5 rounded-2xl border border-slate-200 overflow-hidden">
      <div
        v-for="attachment in attachments"
        :key="attachment.attachmentId"
        class="border-b border-slate-100 last:border-b-0"
      >
        <div class="flex items-center gap-3 px-4 py-4">
          <div class="size-11 rounded-xl bg-slate-100 flex items-center justify-center text-primary shrink-0">
            <span class="material-symbols-outlined text-xl">{{ getAttachmentIcon(attachment) }}</span>
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate text-sm font-semibold text-slate-900">{{ attachment.fileName }}</p>
            <p class="mt-1 text-xs text-slate-400">{{ formatFileSize(attachment.fileSize) }}</p>
          </div>
          <button
            type="button"
            class="rounded-lg p-2 text-slate-300 transition hover:bg-slate-50 hover:text-primary"
            @click="handleDownload(attachment)"
          >
            <span class="material-symbols-outlined text-base">download</span>
          </button>
        </div>

        <div v-if="previewUrls[attachment.attachmentId]" class="px-4 pb-4">
          <img
            :src="previewUrls[attachment.attachmentId]"
            :alt="attachment.fileName"
            class="h-48 w-full rounded-2xl object-cover border border-slate-100"
          />
        </div>

        <div class="bg-slate-50/80 px-4 py-3">
          <template v-if="attachment.analysisStatus === 'completed' && attachment.analysisContent">
            <div class="flex items-center gap-2 text-primary">
              <span class="material-symbols-outlined text-base">auto_awesome</span>
              <span class="text-sm font-semibold">AI 深度洞察</span>
            </div>
            <p class="mt-2 text-sm leading-6 text-slate-600">{{ attachment.analysisContent }}</p>
          </template>

          <template v-else>
            <button
              type="button"
              class="flex w-full items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-500 transition hover:border-primary/30 hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="analyzingId === attachment.attachmentId"
              @click="handleAnalyze(attachment)"
            >
              <span
                class="material-symbols-outlined text-base"
                :class="analyzingId === attachment.attachmentId ? 'animate-spin' : ''"
              >
                {{ analyzingId === attachment.attachmentId ? 'sync' : 'neurology' }}
              </span>
              {{ attachment.analysisStatus === 'failed' ? '重新分析' : 'AI 深度分析' }}
            </button>
            <p v-if="attachment.analysisStatus === 'failed' && attachment.analysisContent" class="mt-2 text-xs leading-5 text-amber-600">
              {{ attachment.analysisContent }}
            </p>
          </template>
        </div>
      </div>
    </div>

    <div v-if="tasks.length > 0" class="mt-5 rounded-2xl border border-slate-200 p-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-2">
          <span class="material-symbols-outlined text-primary">task_alt</span>
          <span class="text-sm font-semibold text-slate-900">AI 智能行动建议</span>
        </div>
        <span class="inline-flex items-center rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
          {{ pendingTaskCount }} 个待办
        </span>
      </div>

      <div class="mt-3 space-y-3">
        <div
          v-for="task in tasks"
          :key="task.taskId"
          class="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-3"
        >
          <div class="min-w-0 flex items-center gap-3">
            <span
              class="size-5 rounded border flex items-center justify-center shrink-0"
              :class="isTaskCompleted(task) ? 'border-primary bg-primary text-white' : 'border-slate-300 bg-white text-transparent'"
            >
              <span class="material-symbols-outlined text-[14px]">check</span>
            </span>
            <div class="min-w-0">
              <p class="truncate text-sm font-medium" :class="isTaskCompleted(task) ? 'text-slate-400 line-through' : 'text-slate-700'">
                {{ task.title }}
              </p>
              <p v-if="task.description" class="mt-1 text-xs text-slate-400 truncate">
                {{ task.description }}
              </p>
            </div>
          </div>
          <span
            v-if="task.dueDate"
            class="shrink-0 rounded-lg px-2 py-1 text-xs font-medium"
            :class="isTaskCompleted(task) ? 'bg-slate-200 text-slate-500' : 'bg-white text-slate-500 border border-slate-200'"
          >
            {{ formatDueBadge(task.dueDate) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  aiAnalyzeFollowUpAttachment,
  downloadFollowUpAttachment,
  getFollowUpAttachmentBlob
} from '@/api/followup'
import type { FollowUp, FollowUpAttachment, FollowUpTask } from '@/types/customer'

const props = defineProps<{
  item: FollowUp
  canEdit?: boolean
  canDelete?: boolean
}>()

defineEmits<{
  (e: 'edit', value: FollowUp): void
  (e: 'delete', value: string): void
}>()

const attachments = ref<FollowUpAttachment[]>([])
const previewUrls = ref<Record<string, string>>({})
const analyzingId = ref('')

const tasks = computed(() => props.item.tasks || [])
const pendingTaskCount = computed(() => tasks.value.filter(task => !isTaskCompleted(task)).length)

watch(
  () => props.item.attachments,
  (value) => {
    attachments.value = (value || []).map(item => ({ ...item }))
    void loadImagePreviews()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearPreviewUrls()
})

async function loadImagePreviews() {
  clearPreviewUrls()
  for (const attachment of attachments.value) {
    if (!isImageAttachment(attachment)) continue
    try {
      const blob = await getFollowUpAttachmentBlob(attachment.attachmentId)
      previewUrls.value[attachment.attachmentId] = URL.createObjectURL(blob)
    } catch (error) {
      console.error('Load follow-up attachment preview failed:', error)
    }
  }
}

function clearPreviewUrls() {
  Object.values(previewUrls.value).forEach(url => URL.revokeObjectURL(url))
  previewUrls.value = {}
}

async function handleAnalyze(attachment: FollowUpAttachment) {
  analyzingId.value = attachment.attachmentId
  try {
    const result = await aiAnalyzeFollowUpAttachment(attachment.attachmentId)
    attachments.value = attachments.value.map(item => (item.attachmentId === result.attachmentId ? result : item))
    ElMessage.success('附件 AI 分析已完成')
  } finally {
    analyzingId.value = ''
  }
}

async function handleDownload(attachment: FollowUpAttachment) {
  await downloadFollowUpAttachment(attachment.attachmentId, attachment.fileName)
}

function isImageAttachment(attachment: FollowUpAttachment): boolean {
  return String(attachment.mimeType || '').startsWith('image/')
}

function getFollowUpTypeLabel(type?: string): string {
  switch (type) {
    case 'call':
      return '电话跟进'
    case 'meeting':
      return '会议跟进'
    case 'email':
      return '邮件跟进'
    case 'visit':
      return '拜访跟进'
    default:
      return '其他跟进'
  }
}

function getFollowUpIcon(type?: string): string {
  switch (type) {
    case 'call':
      return 'call'
    case 'meeting':
      return 'groups'
    case 'email':
      return 'mail'
    case 'visit':
      return 'location_on'
    default:
      return 'event_note'
  }
}

function getAttachmentIcon(attachment: FollowUpAttachment): string {
  if (isImageAttachment(attachment)) return 'image'
  const mime = String(attachment.mimeType || '')
  const fileName = String(attachment.fileName || '').toLowerCase()
  if (mime.startsWith('audio/') || /\.(mp3|wav|m4a|aac|webm|ogg)$/.test(fileName)) return 'music_note'
  if (mime.includes('pdf') || fileName.endsWith('.pdf')) return 'picture_as_pdf'
  if (mime.includes('word') || /\.(doc|docx)$/.test(fileName)) return 'description'
  if (mime.includes('excel') || /\.(xls|xlsx|csv)$/.test(fileName)) return 'table_chart'
  return 'attach_file'
}

function isTaskCompleted(task: FollowUpTask): boolean {
  return String(task.status || '').toLowerCase() === 'completed'
}

function formatDateTime(value?: string): string {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 16)
}

function formatFileSize(size?: number): string {
  if (!size || size <= 0) return '--'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function formatDueBadge(value: string): string {
  const target = new Date(value.replace(' ', 'T'))
  if (Number.isNaN(target.getTime())) {
    return formatDateTime(value)
  }

  const today = new Date()
  const startOfToday = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  const startOfTarget = new Date(target.getFullYear(), target.getMonth(), target.getDate())
  const diffDays = Math.round((startOfTarget.getTime() - startOfToday.getTime()) / (24 * 60 * 60 * 1000))

  if (diffDays === 0) return '今天'
  if (diffDays === 1) return '明天'
  if (diffDays === 2) return '后天'
  return value.slice(0, 10)
}
</script>
