<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 backdrop-blur-sm"
        :class="isMobile ? 'z-[3450] bg-slate-900/45' : 'z-[298] bg-slate-900/40'"
        @click="handleClose"
      />
    </Transition>

    <Transition :name="drawerTransitionName">
      <div
        v-if="modelValue"
        :class="[
          'wk-ai-follow-up-drawer fixed flex w-full flex-col bg-white shadow-2xl',
          isMobile
            ? 'wk-ai-follow-up-drawer--mobile inset-x-0 bottom-0 z-[3451]'
            : 'inset-y-0 right-0 z-[299] max-w-md'
        ]"
      >
        <span v-if="isMobile" class="wk-ai-follow-up-drawer__handle" aria-hidden="true"></span>

        <div
          class="flex items-center justify-between border-b border-slate-200"
          :class="isMobile ? 'px-5 pb-4 pt-2' : 'px-6 py-5'"
        >
          <div class="min-w-0">
            <h2 class="truncate text-lg font-bold text-slate-900">AI 智能跟进录入</h2>
            <p v-if="customer" class="mt-1 truncate text-xs text-slate-500">{{ customer.companyName }}</p>
          </div>
          <button
            type="button"
            class="rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
            @click="handleClose"
          >
            <span class="material-symbols-outlined text-lg">close</span>
          </button>
        </div>

        <div
          class="wk-ai-follow-up-drawer__body flex-1 overflow-y-auto"
          :class="isMobile ? 'px-5 pt-4' : 'px-6 py-5'"
        >
          <div v-if="step === 1" class="space-y-6">
            <section class="md:block rounded-2xl border border-slate-200 bg-slate-50 p-5 text-center">
              <div class="mx-auto mb-4 flex size-20 items-center justify-center rounded-full"
                :class="isRecording ? 'bg-red-100 text-red-500' : isTranscribing ? 'bg-amber-100 text-amber-500' : 'bg-primary/10 text-primary'"
              >
                <span class="material-symbols-outlined text-4xl">
                  {{ isRecording ? 'mic' : isTranscribing ? 'hourglass_top' : 'graphic_eq' }}
                </span>
              </div>

              <p class="text-sm font-semibold text-slate-900">
                {{ isRecording ? '正在语音录入，请直接说话' : '可直接说话，内容会自动转成文字' }}
              </p>
              <p class="mt-2 text-xs text-slate-500">
                {{ isRecording ? formatDuration(recordingDuration) : '也可以手动输入或上传图片、文档附件' }}
              </p>

              <button
                type="button"
                class="mt-4 inline-flex items-center justify-center rounded-full px-6 py-3 text-sm font-bold text-white transition"
                :class="isRecording ? 'bg-red-500 hover:bg-red-600' : isTranscribing ? 'bg-amber-500' : 'bg-primary hover:bg-primary/90'"
                :disabled="isTranscribing"
                @click="isRecording ? handleStopAudioRecording() : handleStartAudioRecording()"
              >
                {{ isRecording ? '停止语音录入' : '开始语音录入' }}
              </button>
            </section>

            <section class="space-y-3">
              <label class="text-xs font-bold uppercase tracking-wider text-slate-500">跟进内容</label>
              <textarea
                v-model="textInput"
                class="h-44 w-full resize-none rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 outline-none transition focus:border-primary/40 focus:ring-2 focus:ring-primary/20"
                placeholder="例如：今天和客户沟通了扩容计划，对方预算基本确认，下周二继续推进。"
                @paste="handlePaste"
              />

              <div class="flex items-center justify-between gap-3">
                <label class="inline-flex shrink-0 cursor-pointer items-center gap-2 whitespace-nowrap rounded-xl border border-dashed border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:border-primary/40 hover:bg-primary/5 hover:text-primary">
                  <span class="material-symbols-outlined shrink-0 text-base">attach_file</span>
                  <span>上传附件</span>
                  <input
                    type="file"
                    class="hidden"
                    multiple
                    accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.md,.csv,.mp3,.wav,.m4a,.aac,.webm"
                    @change="handleFileSelect"
                  />
                </label>

                <p class="text-[11px] text-slate-400">支持图片、PDF、Word、Excel、TXT、Markdown、CSV、音频</p>
              </div>

              <div v-if="attachments.length > 0" class="flex flex-wrap gap-2">
                <div
                  v-for="item in attachments"
                  :key="item.id"
                  class="group relative flex max-w-full items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 py-2"
                >
                  <img
                    v-if="item.preview"
                    :src="item.preview"
                    alt="preview"
                    class="size-10 rounded-lg object-cover"
                  />
                  <span v-else class="material-symbols-outlined text-slate-400">description</span>
                  <span class="max-w-[180px] truncate text-xs text-slate-600">{{ item.file.name }}</span>
                  <button
                    type="button"
                    class="rounded-full p-0.5 text-slate-400 transition hover:bg-slate-100 hover:text-red-500"
                    @click="removeAttachment(item.id)"
                  >
                    <span class="material-symbols-outlined text-sm">close</span>
                  </button>
                </div>
              </div>

              <button
                type="button"
                class="flex w-full items-center justify-center gap-2 rounded-2xl bg-slate-900 py-3 text-sm font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="isProcessing || isTranscribing || (!textInput.trim() && attachments.length === 0)"
                @click="handleSubmitText"
              >
                <span v-if="isProcessing" class="material-symbols-outlined animate-spin text-base">sync</span>
                <span>{{ isProcessing ? 'AI 正在解析内容' : '提交并由 AI 解析' }}</span>
              </button>
            </section>
          </div>

          <div v-else-if="parsedData" class="space-y-5">
            <div class="rounded-2xl border border-emerald-100 bg-emerald-50 p-4">
              <p class="text-sm font-bold text-emerald-800">解析完成</p>
              <p class="mt-1 text-xs text-emerald-700">请确认内容、类型和时间后再保存。</p>
            </div>

            <section class="space-y-4 rounded-2xl border border-slate-200 bg-white p-4">
              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <p class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">跟进类型</p>
                  <el-select v-model="parsedForm.type" class="w-full">
                    <el-option label="电话" value="call" />
                    <el-option label="会议" value="meeting" />
                    <el-option label="邮件" value="email" />
                    <el-option label="拜访" value="visit" />
                    <el-option label="其他" value="other" />
                  </el-select>
                </div>

                <div>
                  <p class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">跟进时间</p>
                  <el-date-picker
                    v-model="parsedForm.followTime"
                    type="datetime"
                    style="width: 100%;"
                    placeholder="选择跟进时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </div>
              </div>

              <div>
                <p class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">跟进内容</p>
                <textarea
                  v-model="parsedForm.content"
                  class="min-h-[180px] w-full resize-y rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 outline-none transition focus:border-primary/40 focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <div>
                <p class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-500">下次跟进时间</p>
                <el-date-picker
                  v-model="parsedForm.nextFollowTime"
                  type="datetime"
                  class="w-full"
                  placeholder="选择下次跟进时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  clearable
                />
              </div>
            </section>

            <section class="rounded-2xl border border-slate-200 bg-white p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-500">已上传附件</p>
                  <p class="mt-1 text-[11px] text-slate-400">
                    {{ attachments.length > 0 ? `共 ${attachments.length} 个附件` : '当前暂无附件，可继续上传新的附件' }}
                  </p>
                </div>

                <label class="inline-flex shrink-0 cursor-pointer items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-2 text-xs font-medium text-primary transition hover:border-primary/30 hover:bg-primary/5">
                  <span class="material-symbols-outlined text-base">add</span>
                  <span>新增</span>
                  <input
                    type="file"
                    class="hidden"
                    multiple
                    accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.md,.csv,.mp3,.wav,.m4a,.aac,.webm"
                    @change="handleFileSelect"
                  />
                </label>
              </div>

              <div v-if="attachments.length > 0" class="mt-3 flex flex-wrap gap-3">
                <div
                  v-for="item in attachments"
                  :key="item.id"
                  class="group relative flex max-w-full items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-3 py-3 pr-10"
                >
                  <img
                    v-if="item.preview"
                    :src="item.preview"
                    :alt="item.file.name"
                    class="size-12 rounded-xl object-cover"
                  />
                  <span v-else class="material-symbols-outlined text-slate-400">description</span>
                  <div class="min-w-0">
                    <p class="truncate text-sm font-medium text-slate-700">{{ item.file.name }}</p>
                    <p class="mt-1 text-xs text-slate-400">{{ formatAttachmentSize(item.file.size) }}</p>
                  </div>
                  <button
                    type="button"
                    class="absolute right-2 top-2 rounded-full bg-white p-1 text-slate-400 shadow-sm transition hover:text-red-500"
                    @click="removeAttachment(item.id)"
                  >
                    <span class="material-symbols-outlined text-sm">close</span>
                  </button>
                </div>
              </div>

              <div
                v-else
                class="mt-3 flex min-h-[88px] items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 text-center text-xs text-slate-400"
              >
                暂无附件，点击右上角“新增”可继续上传
              </div>

              <p v-if="attachmentsChangedAfterParse" class="mt-3 text-xs text-amber-600">
                附件已变更，如需让 AI 结合最新附件重新生成内容，可点击“重新录入”再次解析。
              </p>
            </section>

            <section v-if="parsedData.summary" class="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p class="text-xs font-bold uppercase tracking-wider text-slate-500">AI 摘要</p>
              <p class="mt-2 text-sm leading-6 text-slate-700">{{ parsedData.summary }}</p>
            </section>

            <section
              v-if="parsedData.keyPoints && parsedData.keyPoints.length > 0"
              class="rounded-2xl border border-slate-200 bg-white p-4"
            >
              <p class="text-xs font-bold uppercase tracking-wider text-slate-500">关键要点</p>
              <div class="mt-3 space-y-2">
                <div v-for="(point, index) in parsedData.keyPoints" :key="index" class="flex items-start gap-2">
                  <span class="material-symbols-outlined text-base text-primary">check_circle</span>
                  <span class="text-sm text-slate-700">{{ point }}</span>
                </div>
              </div>
            </section>

            <section class="rounded-2xl border border-slate-200 bg-white p-4">
              <div class="flex items-start justify-between gap-3">
                <div class="min-w-0">
                  <p class="text-xs font-bold uppercase tracking-wider text-slate-500">待办事项</p>
                  <p class="mt-1 text-[11px] leading-5 text-slate-400">
                    保存后会为当前客户创建任务，可在跟进记录中查看详情并标记完成。
                  </p>
                </div>

                <button
                  type="button"
                  class="inline-flex shrink-0 items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-2 text-xs font-medium text-primary transition hover:border-primary/30 hover:bg-primary/5"
                  @click="addSuggestedTask"
                >
                  <span class="material-symbols-outlined text-base">add</span>
                  <span>新增</span>
                </button>
              </div>

              <div v-if="suggestedTasks.length > 0" class="mt-3 space-y-2.5">
                <div
                  v-for="item in suggestedTasks"
                  :key="item.id"
                  class="rounded-2xl border border-slate-200 bg-slate-50 p-3 transition"
                  :class="item.enabled ? 'opacity-100' : 'opacity-70'"
                >
                  <div class="flex items-start gap-3">
                    <button
                      type="button"
                      class="mt-1 inline-flex size-5 shrink-0 items-center justify-center rounded-md border transition"
                      :class="item.enabled ? 'border-primary bg-primary text-white' : 'border-slate-300 bg-white text-transparent'"
                      :aria-label="item.enabled ? '取消创建该任务' : '创建该任务'"
                      @click="toggleSuggestedTask(item.id)"
                    >
                      <span class="material-symbols-outlined text-[14px] leading-none">check</span>
                    </button>

                    <div class="min-w-0 flex-1">
                      <el-input
                        v-model="item.title"
                        maxlength="100"
                        placeholder="输入待办标题"
                      />
                      <p class="mt-2 text-[11px] leading-5 text-slate-400">
                        {{ item.enabled ? '将按当前下次跟进时间生成任务截止时间。' : '已跳过，保存时不会创建这条任务。' }}
                      </p>
                    </div>

                    <button
                      type="button"
                      class="inline-flex size-8 shrink-0 items-center justify-center rounded-lg text-slate-300 transition hover:bg-white hover:text-red-500"
                      aria-label="删除待办"
                      @click="removeSuggestedTask(item.id)"
                    >
                      <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
                    </button>
                  </div>
                </div>
              </div>

              <div
                v-else
                class="mt-3 flex min-h-[88px] items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 text-center text-xs text-slate-400"
              >
                暂无待办事项，点击右上角“新增”可补充任务。
              </div>
            </section>

            <div class="flex gap-3 pt-2">
              <button
                type="button"
                class="flex-1 rounded-2xl border border-slate-200 py-3 text-sm font-bold text-slate-600 transition hover:bg-slate-50"
                @click="handleRetry"
              >
                重新录入
              </button>
              <button
                type="button"
                class="flex-1 rounded-2xl bg-primary py-3 text-sm font-bold text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="saving"
                @click="handleConfirmSave"
              >
                {{ saving ? '保存中...' : '确认并保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { aiParseFollowUp, addFollowUp, transcribeFollowUpAudio } from '@/api/followup'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { getAiConfig } from '@/api/systemConfig'
import { useResponsive } from '@/composables/useResponsive'
import type { AiFollowUpParseVO } from '@/api/followup'
import type { Customer } from '@/types/customer'
import type { ChatAttachmentDTO } from '@/types/common'
import { isRequestErrorHandled } from '@/utils/requestError'
import {
  canCaptureMobileAudioFile,
  captureMobileAudioFile,
  hasMobileAudioInputSupport,
  requestMobileAudioStream
} from '@/utils/mobileAudioRecording'

const props = defineProps<{
  modelValue: boolean
  customer: Customer | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const { isMobile } = useResponsive()
const drawerTransitionName = computed(() => isMobile.value ? 'slide-up' : 'slide-right')

interface Attachment {
  id: string
  file: File
  preview?: string
}

interface ParsedFollowUpForm {
  type: string
  content: string
  followTime: string
  nextFollowTime: string
}

interface SuggestedTaskDraft {
  id: string
  title: string
  enabled: boolean
}


const FOLLOW_UP_TYPES = new Set(['call', 'meeting', 'email', 'visit', 'other'])
const DEFAULT_ATTACHMENT_ONLY_CONTENT = '请结合附件内容生成跟进记录。'
const SUPPORTED_ATTACHMENT_EXTENSIONS = new Set(['txt', 'md', 'csv', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'mp3', 'wav', 'm4a', 'aac', 'webm'])
const CHINESE_RELATIVE_TIME_REGEX = /(明天|后天|明早|明晚|今天)\s*(凌晨|早上|早晨|上午|中午|下午|傍晚|晚上|今晚)?\s*(\d{1,2})(?:\s*[:点时]\s*(\d{1,2}))?(半)?\s*(?:分)?/g
const ENGLISH_RELATIVE_TIME_REGEX = /\b(today|tomorrow|day after tomorrow)\b(?:\s+at)?(?:\s+(morning|afternoon|evening))?\s*(\d{1,2})(?::(\d{1,2}))?\s*(am|pm)?/ig

const step = ref(1)
const textInput = ref('')
const isRecording = ref(false)
const isTranscribing = ref(false)
const isProcessing = ref(false)
const saving = ref(false)
const recordingDuration = ref(0)
const parsedData = ref<AiFollowUpParseVO | null>(null)
const attachments = ref<Attachment[]>([])
const uploadedAttachments = ref<ChatAttachmentDTO[]>([])
const attachmentsChangedAfterParse = ref(false)
const suggestedTasks = ref<SuggestedTaskDraft[]>([])
const parsedForm = reactive<ParsedFollowUpForm>({
  type: 'other',
  content: '',
  followTime: '',
  nextFollowTime: ''
})

let mediaRecorder: MediaRecorder | null = null
let mediaStream: MediaStream | null = null
let recordedChunks: Blob[] = []
let recordedMimeType = ''
let skipNextTranscription = false
let transcriptionToken = 0
let speechInputBase = ''
let recordingTimer: ReturnType<typeof setInterval> | null = null

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      resetDrawerState()
    }
  }
)

onBeforeUnmount(() => {
  resetDrawerState()
})

function resetDrawerState() {
  transcriptionToken += 1
  step.value = 1
  textInput.value = ''
  parsedData.value = null
  attachmentsChangedAfterParse.value = false
  suggestedTasks.value = []
  clearAttachments()
  resetParsedForm()
  isRecording.value = false
  isTranscribing.value = false
  isProcessing.value = false
  saving.value = false
  recordingDuration.value = 0
  stopRecordingTimer()
  abortRecording()
}

function resetParsedForm() {
  parsedForm.type = 'other'
  parsedForm.content = ''
  parsedForm.followTime = ''
  parsedForm.nextFollowTime = ''
}

function clearAttachments() {
  attachments.value.forEach(item => {
    if (item.preview) {
      URL.revokeObjectURL(item.preview)
    }
  })
  attachments.value = []
  uploadedAttachments.value = []
}

function stopRecordingTimer() {
  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
}

function releaseMediaStream() {
  mediaStream?.getTracks().forEach(track => track.stop())
  mediaStream = null
}

function abortRecording() {
  skipNextTranscription = true

  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }

  mediaRecorder = null
  releaseMediaStream()
  recordedChunks = []
  recordedMimeType = ''
}

function normalizeFollowUpType(type?: string): string {
  return type && FOLLOW_UP_TYPES.has(type) ? type : 'other'
}

function formatDateForApi(date: Date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

function parseApiDateTime(value?: string): Date | null {
  const trimmed = String(value || '').trim()
  if (!trimmed) return null

  const parsed = new Date(trimmed.replace(' ', 'T'))
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function resolveChineseDayOffset(token: string): number {
  if (token.startsWith('后天')) return 2
  if (['明天', '明早', '明晚'].includes(token)) return 1
  return 0
}

function resolveEnglishDayOffset(token: string): number {
  const normalized = token.toLowerCase()
  if (normalized.includes('day after tomorrow')) return 2
  if (normalized.includes('tomorrow')) return 1
  return 0
}

function deriveChinesePeriodToken(token: string): string {
  if (token === '明早') return '上午'
  if (token === '明晚') return '晚上'
  return ''
}

function parseMinuteToken(value?: string, halfToken?: string): number | null {
  if (value) {
    const minute = Number.parseInt(value, 10)
    return minute >= 0 && minute <= 59 ? minute : null
  }
  return halfToken ? 30 : 0
}

function normalizeHour(hour: number, periodToken?: string): number | null {
  if (!Number.isInteger(hour) || hour < 0 || hour > 23) return null

  const normalizedPeriod = String(periodToken || '').toLowerCase()
  let normalizedHour = hour

  if (
    normalizedPeriod.includes('下午')
    || normalizedPeriod.includes('傍晚')
    || normalizedPeriod.includes('晚上')
    || normalizedPeriod.includes('今晚')
    || normalizedPeriod.includes('afternoon')
    || normalizedPeriod.includes('evening')
    || normalizedPeriod === 'pm'
  ) {
    if (normalizedHour < 12) normalizedHour += 12
  } else if (normalizedPeriod.includes('中午')) {
    if (normalizedHour < 11) normalizedHour += 12
  } else if (
    normalizedPeriod.includes('凌晨')
    || normalizedPeriod.includes('早上')
    || normalizedPeriod.includes('早晨')
    || normalizedPeriod.includes('上午')
    || normalizedPeriod.includes('morning')
    || normalizedPeriod === 'am'
  ) {
    if (normalizedHour === 12) normalizedHour = 0
  }

  return normalizedHour >= 0 && normalizedHour <= 23 ? normalizedHour : null
}

function buildRelativeDateTime(now: Date, dayOffset: number, hour: number, minute: number): Date {
  const candidate = new Date(now)
  candidate.setDate(candidate.getDate() + dayOffset)
  candidate.setHours(hour, minute, 0, 0)
  return candidate
}

function inferNextFollowTimeFromContent(content: string, now: Date): string {
  for (const match of content.matchAll(CHINESE_RELATIVE_TIME_REGEX)) {
    const [, dayToken, rawPeriodToken, hourToken, minuteToken, halfToken] = match
    const hour = Number.parseInt(hourToken, 10)
    const minute = parseMinuteToken(minuteToken, halfToken)
    const normalizedHour = normalizeHour(hour, rawPeriodToken || deriveChinesePeriodToken(dayToken))
    if (minute === null || normalizedHour === null) continue

    const candidate = buildRelativeDateTime(now, resolveChineseDayOffset(dayToken), normalizedHour, minute)
    if (candidate.getTime() > now.getTime()) {
      return formatDateForApi(candidate)
    }
  }

  for (const match of content.matchAll(ENGLISH_RELATIVE_TIME_REGEX)) {
    const [, dayToken, rawPeriodToken, hourToken, minuteToken, meridiemToken] = match
    const hour = Number.parseInt(hourToken, 10)
    const minute = parseMinuteToken(minuteToken)
    const normalizedHour = normalizeHour(hour, meridiemToken || rawPeriodToken)
    if (minute === null || normalizedHour === null) continue

    const candidate = buildRelativeDateTime(now, resolveEnglishDayOffset(dayToken), normalizedHour, minute)
    if (candidate.getTime() > now.getTime()) {
      return formatDateForApi(candidate)
    }
  }

  return ''
}

function normalizeParsedTimes(result: AiFollowUpParseVO, originalContent: string) {
  const now = new Date()
  const normalizedNow = formatDateForApi(now)
  let followTime = result.followTime || normalizedNow
  let nextFollowTime = result.nextFollowTime || ''

  const parsedFollowTime = parseApiDateTime(followTime)
  let parsedNextFollowTime = parseApiDateTime(nextFollowTime)
  const inferredNextFollowTime = inferNextFollowTimeFromContent(originalContent, now)
  const parsedInferredNextFollowTime = parseApiDateTime(inferredNextFollowTime)

  if (parsedFollowTime && parsedFollowTime.getTime() > now.getTime() + 5 * 60 * 1000) {
    nextFollowTime = inferredNextFollowTime || (parsedNextFollowTime ? nextFollowTime : followTime)
    followTime = normalizedNow
    parsedNextFollowTime = parseApiDateTime(nextFollowTime)
  }

  if (
    parsedInferredNextFollowTime
    && parsedInferredNextFollowTime.getTime() > now.getTime() + 5 * 60 * 1000
    && (!parsedNextFollowTime || parsedNextFollowTime.getTime() <= now.getTime() + 5 * 60 * 1000)
  ) {
    nextFollowTime = inferredNextFollowTime
  }

  return {
    followTime,
    nextFollowTime
  }
}

function buildEditableContent(result: AiFollowUpParseVO, fallbackContent: string): string {
  const trimmedFallback = fallbackContent.trim()
  if (trimmedFallback && trimmedFallback !== DEFAULT_ATTACHMENT_ONLY_CONTENT) {
    return trimmedFallback
  }

  const blocks: string[] = []

  if (result.summary?.trim()) {
    blocks.push(result.summary.trim())
  }

  if (result.keyPoints?.length) {
    blocks.push(`关键要点:\n${result.keyPoints.map(point => `- ${point}`).join('\n')}`)
  }

  if (false && result.todos?.length) {
    blocks.push(`待办事项:\n${result.todos.map(todo => `- ${todo}`).join('\n')}`)
  }

  const content = blocks.join('\n\n').trim()
  return content || trimmedFallback
}

function createSuggestedTaskDraft(title = ''): SuggestedTaskDraft {
  return {
    id: Math.random().toString(36).slice(2, 11),
    title,
    enabled: true
  }
}

function hydrateSuggestedTasks(todos?: string[]) {
  suggestedTasks.value = (todos || [])
    .map(todo => String(todo || '').trim())
    .filter(Boolean)
    .map(todo => createSuggestedTaskDraft(todo))
}

function applyParsedResult(result: AiFollowUpParseVO, originalContent: string) {
  const normalizedTimes = normalizeParsedTimes(result, originalContent)
  parsedData.value = result
  attachmentsChangedAfterParse.value = false
  hydrateSuggestedTasks(result.todos)
  parsedForm.type = normalizeFollowUpType(result.type)
  parsedForm.content = buildEditableContent(result, originalContent)
  parsedForm.followTime = normalizedTimes.followTime
  parsedForm.nextFollowTime = normalizedTimes.nextFollowTime
  step.value = 2
}

function addSuggestedTask() {
  suggestedTasks.value.push(createSuggestedTaskDraft())
}

function toggleSuggestedTask(id: string) {
  const target = suggestedTasks.value.find(item => item.id === id)
  if (!target) return
  target.enabled = !target.enabled
}

function removeSuggestedTask(id: string) {
  suggestedTasks.value = suggestedTasks.value.filter(item => item.id !== id)
}

function buildSuggestedTaskPayload(content: string) {
  const payload = suggestedTasks.value
    .map(item => ({
      title: item.title.trim(),
      enabled: item.enabled
    }))
    .filter(item => item.enabled && item.title)
    .map(item => ({
      title: item.title,
      description: parsedData.value?.summary || content,
      dueDate: parsedForm.nextFollowTime || undefined,
      taskType: '跟进'
    }))

  return payload.length > 0 ? payload : undefined
}

function handleClose() {
  if (isRecording.value) {
    abortRecording()
  }
  transcriptionToken += 1
  isTranscribing.value = false
  emit('update:modelValue', false)
}

async function ensureAudioTranscriptionSupported(): Promise<boolean> {
  try {
    const aiConfig = await getAiConfig()
    if (aiConfig.capabilities?.supportsAudioTranscription) {
      return true
    }

    ElMessage.warning('当前模型不支持语音识别，请配置支持的模型')
    return false
  } catch (err: unknown) {
    console.error('Load AI config failed:', err)
    if (!isRequestErrorHandled(err)) {
      ElMessage.warning('暂时无法获取语音识别能力，请稍后再试')
    }
    return false
  }
}

function resolveRecordingMimeType(): string {
  const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']

  if (typeof MediaRecorder === 'undefined' || typeof MediaRecorder.isTypeSupported !== 'function') {
    return ''
  }

  return candidates.find(type => MediaRecorder.isTypeSupported(type)) || ''
}

function resolveAudioExtension(mimeType: string): string {
  if (mimeType.includes('mp4')) return 'm4a'
  if (mimeType.includes('mpeg')) return 'mp3'
  if (mimeType.includes('wav')) return 'wav'
  return 'webm'
}

function buildRecordedAudioFile(): File | null {
  if (recordedChunks.length === 0) {
    return null
  }

  const mimeType = recordedMimeType || recordedChunks[0]?.type || 'audio/webm'
  const blob = new Blob(recordedChunks, { type: mimeType })
  return new File([blob], `followup-recording.${resolveAudioExtension(mimeType)}`, {
    type: mimeType
  })
}

async function transcribeRecordedAudio(file: File | null) {
  if (!file) {
    ElMessage.warning('未采集到有效录音，请重试')
    return
  }

  const currentToken = ++transcriptionToken
  isTranscribing.value = true

  try {
    const transcript = (await transcribeFollowUpAudio(file)).trim()
    if (currentToken !== transcriptionToken || !props.modelValue) {
      return
    }

    if (!transcript) {
      ElMessage.warning('未识别到有效语音内容，请重试')
      return
    }

    textInput.value = speechInputBase
      ? `${speechInputBase}\n${transcript}`
      : transcript
    ElMessage.success('语音已转成文字，可继续编辑后提交')
  } catch (err: unknown) {
    console.error('Audio transcription failed:', err)
    if (!isRequestErrorHandled(err)) {
      ElMessage.warning('语音识别失败，请稍后重试')
    }
  } finally {
    if (currentToken === transcriptionToken) {
      isTranscribing.value = false
    }
  }
}

async function handleRecordedAudioStop() {
  const shouldSkip = skipNextTranscription
  skipNextTranscription = false
  stopRecordingTimer()
  isRecording.value = false

  const file = shouldSkip ? null : buildRecordedAudioFile()
  mediaRecorder = null
  releaseMediaStream()
  recordedChunks = []
  recordedMimeType = ''

  if (shouldSkip) {
    return
  }

  await transcribeRecordedAudio(file)
}

async function handleStartAudioRecording() {
  if (isRecording.value || isTranscribing.value) return

  const useMobileAudioApi = isMobile.value
  const hasAudioInput = useMobileAudioApi
    ? hasMobileAudioInputSupport()
    : Boolean(navigator.mediaDevices?.getUserMedia)
  const useMobileAudioFileCapture = useMobileAudioApi
    && canCaptureMobileAudioFile()
    && (!hasAudioInput || typeof MediaRecorder === 'undefined')

  if (useMobileAudioFileCapture) {
    speechInputBase = textInput.value.trim()
    const capturedFile = await captureMobileAudioFile()
    if (!capturedFile) return
    if (!(await ensureAudioTranscriptionSupported())) return
    await transcribeRecordedAudio(capturedFile)
    return
  }

  if (!(await ensureAudioTranscriptionSupported())) return
  if (!hasAudioInput || typeof MediaRecorder === 'undefined') {
    ElMessage.warning('当前浏览器不支持录音，请改用文字输入')
    return
  }

  try {
    speechInputBase = textInput.value.trim()
    skipNextTranscription = false
    recordedChunks = []

    mediaStream = useMobileAudioApi
      ? await requestMobileAudioStream({ audio: true })
      : await navigator.mediaDevices.getUserMedia({ audio: true })
    const mimeType = resolveRecordingMimeType()
    mediaRecorder = mimeType
      ? new MediaRecorder(mediaStream, { mimeType })
      : new MediaRecorder(mediaStream)
    recordedMimeType = mediaRecorder.mimeType || mimeType || 'audio/webm'

    mediaRecorder.ondataavailable = (event: BlobEvent) => {
      if (event.data && event.data.size > 0) {
        recordedChunks.push(event.data)
      }
    }
    mediaRecorder.onstop = () => {
      void handleRecordedAudioStop()
    }
    mediaRecorder.onerror = (event: Event) => {
      console.error('MediaRecorder error:', event)
      ElMessage.warning('录音失败，请检查麦克风权限后重试')
    }

    mediaRecorder.start()
    isRecording.value = true
    recordingDuration.value = 0
    stopRecordingTimer()
    recordingTimer = setInterval(() => {
      recordingDuration.value += 1
    }, 1000)
  } catch (err) {
    console.error('Start recording failed:', err)
    abortRecording()
    ElMessage.warning('无法启动录音，请检查浏览器和麦克风权限')
  }
}

function handleStopAudioRecording() {
  if (!mediaRecorder) {
    return
  }

  skipNextTranscription = false

  if (mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }

  isRecording.value = false
  stopRecordingTimer()
}

function formatDuration(seconds: number): string {
  const minutes = Math.floor(seconds / 60).toString().padStart(2, '0')
  const remainSeconds = (seconds % 60).toString().padStart(2, '0')
  return `${minutes}:${remainSeconds}`
}

function formatAttachmentSize(size: number): string {
  if (!size || size <= 0) return '--'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files) {
    Array.from(input.files).forEach(handleFile)
  }
  input.value = ''
}

function handlePaste(event: ClipboardEvent) {
  const items = event.clipboardData?.items
  if (!items) return

  for (let i = 0; i < items.length; i += 1) {
    if (items[i].kind === 'file') {
      const file = items[i].getAsFile()
      if (file) {
        handleFile(file)
      }
    }
  }
}

function isSupportedAttachment(file: File): boolean {
  if (file.type.startsWith('image/')) {
    return true
  }

  if (file.type.startsWith('audio/')) {
    return true
  }

  const extension = file.name.includes('.')
    ? file.name.split('.').pop()?.toLowerCase() || ''
    : ''

  return SUPPORTED_ATTACHMENT_EXTENSIONS.has(extension)
}

function handleFile(file: File) {
  if (!isSupportedAttachment(file)) {
    ElMessage.warning('当前仅支持图片、PDF、Word、Excel、TXT、Markdown、CSV、音频附件')
    return
  }

  const id = Math.random().toString(36).slice(2, 11)
  uploadedAttachments.value = []
  if (step.value === 2) {
    attachmentsChangedAfterParse.value = true
  }
  attachments.value.push({
    id,
    file,
    preview: file.type.startsWith('image/') ? URL.createObjectURL(file) : undefined
  })
}

function removeAttachment(id: string) {
  const target = attachments.value.find(item => item.id === id)
  if (target?.preview) {
    URL.revokeObjectURL(target.preview)
  }
  uploadedAttachments.value = []
  if (step.value === 2) {
    attachmentsChangedAfterParse.value = true
  }
  attachments.value = attachments.value.filter(item => item.id !== id)
}

async function uploadAttachments(): Promise<ChatAttachmentDTO[]> {
  const result = await Promise.all(
    attachments.value.map(async ({ file }) => {
      const presigned = await getPresignedUploadUrl(file.name, file.type)
      await uploadToMinIO(file, presigned.uploadUrl)
      return {
        fileName: file.name,
        filePath: presigned.objectKey,
        fileSize: file.size,
        mimeType: file.type || 'application/octet-stream'
      }
    })
  )
  uploadedAttachments.value = result
  return result
}

async function ensureUploadedAttachments(): Promise<ChatAttachmentDTO[] | undefined> {
  if (attachments.value.length === 0) {
    uploadedAttachments.value = []
    return undefined
  }

  if (uploadedAttachments.value.length === attachments.value.length) {
    return uploadedAttachments.value
  }

  return uploadAttachments()
}

async function handleSubmitText() {
  if (isProcessing.value) return
  if (!textInput.value.trim() && attachments.value.length === 0) return

  isProcessing.value = true

  try {
    const attachmentDTOs = await ensureUploadedAttachments()
    const content = textInput.value.trim() || DEFAULT_ATTACHMENT_ONLY_CONTENT
    const result = await aiParseFollowUp({
      content,
      customerName: props.customer?.companyName || '',
      customerId: props.customer?.customerId || '',
      attachments: attachmentDTOs
    })

    applyParsedResult(result, content)
  } catch (err: unknown) {
    console.error('AI parse follow-up failed:', err)
    if (!isRequestErrorHandled(err)) {
      const message = err instanceof Error && /upload|上传/i.test(err.message)
        ? '附件上传失败，请重试'
        : 'AI 解析失败，请稍后重试'
      ElMessage.error(message)
    }
  } finally {
    isProcessing.value = false
  }
}

async function handleConfirmSave() {
  if (!props.customer) return

  const content = parsedForm.content.trim()
  if (!content) {
    ElMessage.warning('请输入跟进内容')
    return
  }

  if (!parsedForm.followTime) {
    ElMessage.warning('请选择跟进时间')
    return
  }

  saving.value = true

  try {
    const attachmentDTOs = await ensureUploadedAttachments()
    const suggestedTaskPayload = buildSuggestedTaskPayload(content)

    await addFollowUp({
      customerId: props.customer.customerId,
      type: normalizeFollowUpType(parsedForm.type),
      content,
      summary: parsedData.value?.summary?.trim() || undefined,
      sceneType: parsedData.value?.sceneType?.trim() || undefined,
      aiGenerated: parsedData.value ? 1 : 0,
      followTime: parsedForm.followTime,
      nextFollowTime: parsedForm.nextFollowTime || undefined,
      attachments: attachmentDTOs,
      suggestedTasks: suggestedTaskPayload
    })

    ElMessage.success('跟进记录已保存')
    emit('saved')
    emit('update:modelValue', false)
  } catch (err: unknown) {
    console.error('Save follow-up failed:', err)
  } finally {
    saving.value = false
  }
}

function handleRetry() {
  step.value = 1
  parsedData.value = null
  suggestedTasks.value = []
  resetParsedForm()
}
</script>

<style scoped>
.wk-ai-follow-up-drawer--mobile {
  max-height: min(92vh, calc(100dvh - 24px));
  overflow: hidden;
  border-radius: 28px 28px 0 0;
}

.wk-ai-follow-up-drawer__handle {
  width: 42px;
  height: 4px;
  margin: 10px auto 2px;
  flex-shrink: 0;
  border-radius: 9999px;
  background: rgb(148 163 184 / 0.35);
}

.wk-ai-follow-up-drawer--mobile .wk-ai-follow-up-drawer__body {
  padding-bottom: max(24px, calc(env(safe-area-inset-bottom) + 20px));
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
}

.slide-right-enter-active {
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-right-leave-active {
  transition: transform 0.25s ease-in;
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

.slide-up-enter-active {
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.slide-up-leave-active {
  transition: transform 0.22s ease-in;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

.fade-enter-active {
  transition: opacity 0.3s ease;
}

.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
