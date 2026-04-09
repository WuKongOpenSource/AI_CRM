<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-40 bg-slate-900/40 backdrop-blur-sm"
        @click="handleClose"
      />
    </Transition>

    <Transition name="slide-right">
      <div
        v-if="modelValue"
        class="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col bg-white shadow-2xl"
      >
        <div class="flex items-center justify-between border-b border-slate-200 px-6 py-5">
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

        <div class="flex-1 overflow-y-auto px-6 py-5">
          <div v-if="step === 1" class="space-y-6">
            <section class="rounded-2xl border border-slate-200 bg-slate-50 p-5 text-center">
              <div class="mx-auto mb-4 flex size-20 items-center justify-center rounded-full"
                :class="isRecording ? 'bg-red-100 text-red-500' : 'bg-primary/10 text-primary'"
              >
                <span class="material-symbols-outlined text-4xl">
                  {{ isRecording ? 'mic' : 'graphic_eq' }}
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
                :class="isRecording ? 'bg-red-500 hover:bg-red-600' : 'bg-primary hover:bg-primary/90'"
                @click="isRecording ? handleStopRecording() : handleStartRecording()"
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
                <label class="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-dashed border-slate-300 px-3 py-2 text-sm text-slate-600 transition hover:border-primary/40 hover:bg-primary/5 hover:text-primary">
                  <span class="material-symbols-outlined text-base">attach_file</span>
                  <span>上传附件</span>
                  <input
                    type="file"
                    class="hidden"
                    multiple
                    accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.md,.csv"
                    @change="handleFileSelect"
                  />
                </label>

                <p class="text-[11px] text-slate-400">支持图片、PDF、Word、Excel、TXT、Markdown、CSV</p>
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
                :disabled="isProcessing || (!textInput.trim() && attachments.length === 0)"
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
                    class="w-full"
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

            <section
              v-if="parsedData.todos && parsedData.todos.length > 0"
              class="rounded-2xl border border-slate-200 bg-white p-4"
            >
              <p class="text-xs font-bold uppercase tracking-wider text-slate-500">待办事项</p>
              <div class="mt-3 space-y-2">
                <div v-for="(todo, index) in parsedData.todos" :key="index" class="flex items-start gap-2">
                  <span class="material-symbols-outlined text-base text-slate-400">radio_button_checked</span>
                  <span class="text-sm text-slate-700">{{ todo }}</span>
                </div>
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
import { onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { aiParseFollowUp, addFollowUp } from '@/api/followup'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import type { AiFollowUpParseVO } from '@/api/followup'
import type { Customer } from '@/types/customer'
import type { ChatAttachmentDTO } from '@/types/common'
import { isRequestErrorHandled } from '@/utils/requestError'

const props = defineProps<{
  modelValue: boolean
  customer: Customer | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

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

interface SpeechRecognitionAlternativeLike {
  transcript: string
}

interface SpeechRecognitionResultLike {
  length: number
  [index: number]: SpeechRecognitionAlternativeLike
}

interface SpeechRecognitionEventLike {
  results: ArrayLike<SpeechRecognitionResultLike>
}

interface SpeechRecognitionErrorEventLike {
  error: string
}

interface SpeechRecognitionLike {
  lang: string
  interimResults: boolean
  continuous: boolean
  onresult: ((event: SpeechRecognitionEventLike) => void) | null
  onerror: ((event: SpeechRecognitionErrorEventLike) => void) | null
  onend: (() => void) | null
  start: () => void
  stop: () => void
  abort: () => void
}

interface SpeechRecognitionConstructorLike {
  new (): SpeechRecognitionLike
}

const FOLLOW_UP_TYPES = new Set(['call', 'meeting', 'email', 'visit', 'other'])
const SUPPORTED_ATTACHMENT_EXTENSIONS = new Set(['txt', 'md', 'csv', 'pdf', 'doc', 'docx', 'xls', 'xlsx'])

const step = ref(1)
const textInput = ref('')
const isRecording = ref(false)
const isProcessing = ref(false)
const saving = ref(false)
const recordingDuration = ref(0)
const parsedData = ref<AiFollowUpParseVO | null>(null)
const attachments = ref<Attachment[]>([])
const parsedForm = reactive<ParsedFollowUpForm>({
  type: 'other',
  content: '',
  followTime: '',
  nextFollowTime: ''
})

let speechRecognition: SpeechRecognitionLike | null = null
let speechRecognitionStoppedManually = false
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
  step.value = 1
  textInput.value = ''
  parsedData.value = null
  clearAttachments()
  resetParsedForm()
  isRecording.value = false
  isProcessing.value = false
  saving.value = false
  recordingDuration.value = 0
  speechRecognitionStoppedManually = true
  speechRecognition?.abort()
  speechRecognition = null

  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
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

function buildEditableContent(result: AiFollowUpParseVO, fallbackContent: string): string {
  const blocks: string[] = []

  if (result.summary?.trim()) {
    blocks.push(result.summary.trim())
  }

  if (result.keyPoints?.length) {
    blocks.push(`关键要点:\n${result.keyPoints.map(point => `- ${point}`).join('\n')}`)
  }

  if (result.todos?.length) {
    blocks.push(`待办事项:\n${result.todos.map(todo => `- ${todo}`).join('\n')}`)
  }

  const content = blocks.join('\n\n').trim()
  return content || fallbackContent.trim()
}

function applyParsedResult(result: AiFollowUpParseVO, originalContent: string) {
  parsedData.value = result
  parsedForm.type = normalizeFollowUpType(result.type)
  parsedForm.content = buildEditableContent(result, originalContent)
  parsedForm.followTime = result.followTime || formatDateForApi()
  parsedForm.nextFollowTime = result.nextFollowTime || ''
  step.value = 2
}

function handleClose() {
  if (isRecording.value) {
    handleStopRecording()
  }
  emit('update:modelValue', false)
}

async function handleStartRecording() {
  const speechWindow = window as Window & {
    SpeechRecognition?: SpeechRecognitionConstructorLike
    webkitSpeechRecognition?: SpeechRecognitionConstructorLike
  }
  const recognitionCtor = speechWindow.SpeechRecognition || speechWindow.webkitSpeechRecognition

  if (!recognitionCtor) {
    ElMessage.warning('当前浏览器不支持语音识别，请改用文字输入')
    return
  }

  try {
    speechRecognitionStoppedManually = false
    speechInputBase = textInput.value.trim()

    speechRecognition = new recognitionCtor()
    speechRecognition.lang = 'zh-CN'
    speechRecognition.interimResults = true
    speechRecognition.continuous = true

    speechRecognition.onresult = (event) => {
      const transcript = Array.from(event.results)
        .map(result => result[0]?.transcript || '')
        .join('')
        .trim()

      if (!transcript) return

      textInput.value = speechInputBase
        ? `${speechInputBase}\n${transcript}`
        : transcript
    }

    speechRecognition.onerror = (event) => {
      if (event.error === 'not-allowed') {
        ElMessage.warning('未获得麦克风权限，请检查浏览器授权设置')
      } else if (event.error !== 'aborted') {
        ElMessage.warning('语音识别失败，请重试')
      }
      console.error('Speech recognition error:', event)
    }

    speechRecognition.onend = () => {
      isRecording.value = false
      speechRecognition = null

      if (recordingTimer) {
        clearInterval(recordingTimer)
        recordingTimer = null
      }

      if (!speechRecognitionStoppedManually && textInput.value.trim()) {
        ElMessage.success('语音已转成文字，可继续编辑后提交')
      }
    }

    speechRecognition.start()
    isRecording.value = true
    recordingDuration.value = 0
    recordingTimer = setInterval(() => {
      recordingDuration.value += 1
    }, 1000)
  } catch {
    ElMessage.warning('无法启动语音识别，请检查浏览器和麦克风权限')
  }
}

function handleStopRecording() {
  if (speechRecognition) {
    speechRecognitionStoppedManually = true
    speechRecognition.stop()
  }

  isRecording.value = false

  if (recordingTimer) {
    clearInterval(recordingTimer)
    recordingTimer = null
  }
}

function formatDuration(seconds: number): string {
  const minutes = Math.floor(seconds / 60).toString().padStart(2, '0')
  const remainSeconds = (seconds % 60).toString().padStart(2, '0')
  return `${minutes}:${remainSeconds}`
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

  const extension = file.name.includes('.')
    ? file.name.split('.').pop()?.toLowerCase() || ''
    : ''

  return SUPPORTED_ATTACHMENT_EXTENSIONS.has(extension)
}

function handleFile(file: File) {
  if (!isSupportedAttachment(file)) {
    ElMessage.warning('当前仅支持图片、PDF、Word、Excel、TXT、Markdown、CSV 附件')
    return
  }

  const id = Math.random().toString(36).slice(2, 11)
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
  attachments.value = attachments.value.filter(item => item.id !== id)
}

async function uploadAttachments(): Promise<ChatAttachmentDTO[]> {
  return Promise.all(
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
}

async function handleSubmitText() {
  if (isProcessing.value) return
  if (!textInput.value.trim() && attachments.value.length === 0) return

  isProcessing.value = true

  try {
    const attachmentDTOs = attachments.value.length > 0
      ? await uploadAttachments()
      : undefined

    const content = textInput.value.trim() || '请结合附件内容生成跟进记录。'
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
    await addFollowUp({
      customerId: props.customer.customerId,
      type: normalizeFollowUpType(parsedForm.type),
      content,
      followTime: parsedForm.followTime,
      nextFollowTime: parsedForm.nextFollowTime || undefined
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
  resetParsedForm()
}
</script>

<style scoped>
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
