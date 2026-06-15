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
        <div class="flex shrink-0 items-center justify-between border-b border-slate-200 p-6">
          <div class="flex items-center gap-2">
            <div class="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <WkIcon name="ai" class="text-lg" />
            </div>
            <h2 class="text-lg font-bold text-slate-900">AI 智能跟进录入</h2>
            <span
              v-if="customer"
              class="max-w-[150px] truncate rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-500"
            >
              {{ customer.companyName }}
            </span>
          </div>

          <button
            class="text-slate-400 transition-colors hover:text-slate-600"
            @click="handleClose"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-6">
          <div v-if="step === 1" class="space-y-8">
            <div class="space-y-4 py-8 text-center">
              <div
                class="mx-auto flex size-24 items-center justify-center rounded-full transition-all duration-500"
                :class="{
                  'scale-110 bg-red-50 text-red-500 shadow-lg shadow-red-200': isRecording,
                  'animate-pulse bg-primary/5 text-primary': isProcessing,
                  'bg-slate-50 text-slate-400': !isRecording && !isProcessing
                }"
              >
                <span
                  class="material-symbols-outlined text-4xl"
                  :class="{ 'animate-pulse': isRecording }"
                >
                  {{ isProcessing ? 'sync' : 'mic' }}
                </span>
              </div>

              <div>
                <h3 class="text-lg font-bold text-slate-900">
                  {{
                    isRecording
                      ? '正在倾听您的跟进记录...'
                      : isProcessing
                        ? 'AI 正在分析内容...'
                        : '点击开始录音'
                  }}
                </h3>
                <p class="mt-1 text-sm text-slate-500">
                  您可以直接口述跟进内容，或在下方输入文字、粘贴图片，AI 将自动提取关键要素。
                </p>
              </div>

              <button
                v-if="!isRecording && !isProcessing"
                class="rounded-full bg-primary px-8 py-3 font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90"
                @click="handleStartRecording"
              >
                开始语音录入
              </button>

              <button
                v-if="isRecording"
                class="rounded-full bg-red-500 px-8 py-3 font-bold text-white shadow-lg shadow-red-200 transition-all hover:bg-red-600"
                @click="handleStopRecording"
              >
                停止录音
              </button>

              <div v-if="isRecording" class="flex h-8 items-center justify-center gap-1">
                <div
                  v-for="i in 6"
                  :key="i"
                  class="animate-recording-bar w-1 rounded-full bg-red-400"
                  :style="{ animationDelay: `${i * 0.1}s` }"
                />
              </div>

              <p v-if="isRecording" class="font-mono text-sm text-red-500">
                {{ formatDuration(recordingDuration) }}
              </p>
            </div>

            <div class="flex items-center gap-2">
              <div class="h-px flex-1 bg-slate-200" />
              <span class="text-xs font-bold uppercase tracking-widest text-slate-400">或者</span>
              <div class="h-px flex-1 bg-slate-200" />
            </div>

            <div class="space-y-4">
              <div class="relative space-y-2">
                <label class="text-xs font-bold uppercase text-slate-500">手动输入或粘贴附件</label>

                <div class="relative">
                  <textarea
                    v-model="textInput"
                    class="h-40 w-full resize-none rounded-xl border border-slate-200 bg-slate-50 p-4 pr-12 text-sm outline-none transition-all focus:ring-2 focus:ring-primary/50"
                    placeholder="例如：今天和张总沟通了 Q4 扩容计划，客户表示预算已基本确认，本周五继续对接。"
                    @paste="handlePaste"
                  />

                  <div class="absolute bottom-3 right-3 flex flex-col gap-2">
                    <label class="flex size-8 cursor-pointer items-center justify-center rounded-lg text-slate-400 transition-all hover:bg-primary/10 hover:text-primary">
                      <span class="material-symbols-outlined text-xl">attach_file</span>
                      <input
                        ref="fileInput"
                        type="file"
                        class="hidden"
                        multiple
                        accept="audio/*,image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt"
                        @change="handleFileSelect"
                      />
                    </label>
                  </div>
                </div>

                <div v-if="attachments.length > 0" class="mt-2 flex flex-wrap gap-2">
                  <div v-for="attr in attachments" :key="attr.id" class="group relative">
                    <img
                      v-if="attr.preview"
                      :src="attr.preview"
                      class="size-16 rounded-lg border border-slate-200 object-cover"
                      alt="preview"
                    />

                    <div
                      v-else
                      class="flex size-16 flex-col items-center justify-center rounded-lg border border-slate-200 bg-slate-100 p-1 text-center"
                    >
                      <span class="material-symbols-outlined text-sm text-slate-400">description</span>
                      <span class="w-full truncate text-xs text-slate-500">{{ attr.file.name }}</span>
                    </div>

                    <button
                      class="absolute -right-1.5 -top-1.5 flex size-5 items-center justify-center rounded-full bg-red-500 text-white opacity-0 shadow-sm transition-opacity group-hover:opacity-100"
                      @click="removeAttachment(attr.id)"
                    >
                      <span class="material-symbols-outlined text-[12px]">close</span>
                    </button>
                  </div>
                </div>
              </div>

              <button
                class="flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 py-3 text-sm font-bold text-white transition-all hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="isProcessing || (!textInput.trim() && attachments.length === 0)"
                @click="handleSubmitText"
              >
                <template v-if="isProcessing">
                  <span class="material-symbols-outlined animate-spin text-sm">sync</span>
                  AI 正在解析内容...
                </template>
                <template v-else>
                  提交并由 AI 解析
                </template>
              </button>
            </div>
          </div>

          <Transition name="fade-up">
            <div v-if="step === 2 && parsedData" class="space-y-6">
              <div class="flex items-start gap-3 rounded-xl border border-emerald-100 bg-emerald-50 p-4">
                <span class="material-symbols-outlined text-emerald-600">check_circle</span>
                <div>
                  <p class="text-sm font-bold text-emerald-900">解析成功</p>
                  <p class="text-xs text-emerald-700">AI 已完成初稿整理，您可以直接修改跟进内容和时间后再保存。</p>
                </div>
              </div>

              <div class="space-y-4">
                <div class="space-y-4 rounded-xl border border-slate-200 bg-white p-4">
                  <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                    <div class="space-y-1">
                      <p class="text-xs font-bold uppercase tracking-widest text-slate-400">跟进内容</p>
                      <p class="text-xs text-slate-500">保存前可直接修改内容、类型和时间，最终将按您当前填写的内容入库。</p>
                    </div>

                    <div class="sm:w-32">
                      <p class="mb-2 text-xs font-bold uppercase tracking-widest text-slate-400">跟进类型</p>
                      <el-select v-model="parsedForm.type" class="w-full">
                        <el-option label="电话" value="call" />
                        <el-option label="会议" value="meeting" />
                        <el-option label="邮件" value="email" />
                        <el-option label="拜访" value="visit" />
                        <el-option label="其他" value="other" />
                      </el-select>
                    </div>
                  </div>

                  <textarea
                    v-model="parsedForm.content"
                    class="min-h-[140px] w-full resize-y rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm leading-relaxed outline-none transition-all focus:ring-2 focus:ring-primary/50"
                    placeholder="请根据实际情况修改跟进内容"
                  />

                  <div class="grid grid-cols-1 gap-4 border-t border-slate-50 pt-4 sm:grid-cols-2">
                    <div>
                      <p class="mb-2 text-xs font-bold uppercase tracking-widest text-slate-400">跟进时间</p>
                      <el-date-picker
                        v-model="parsedForm.followTime"
                        type="datetime"
                        class="w-full"
                        placeholder="选择跟进时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                      />
                    </div>

                    <div>
                      <p class="mb-2 text-xs font-bold uppercase tracking-widest text-slate-400">预计下次联系</p>
                      <el-date-picker
                        v-model="parsedForm.nextFollowTime"
                        type="datetime"
                        class="w-full"
                        placeholder="选择下次联系时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        clearable
                      />
                    </div>
                  </div>
                </div>

                <div
                  v-if="attachments.length > 0"
                  class="space-y-3 rounded-xl border border-slate-200 bg-white p-4"
                >
                  <p class="text-xs font-bold uppercase tracking-widest text-slate-400">已上传附件</p>
                  <div class="flex flex-wrap gap-3">
                    <div
                      v-for="item in attachments"
                      :key="item.id"
                      class="flex max-w-full items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-3 py-3"
                    >
                      <img
                        v-if="item.preview"
                        :src="item.preview"
                        :alt="item.file.name"
                        class="size-12 rounded-lg object-cover"
                      />
                      <span v-else class="material-symbols-outlined text-slate-400">description</span>
                      <div class="min-w-0">
                        <p class="truncate text-sm font-medium text-slate-700">{{ item.file.name }}</p>
                        <p class="mt-1 text-xs text-slate-400">{{ formatAttachmentSize(item.file.size) }}</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div
                  v-if="parsedData.summary"
                  class="space-y-2 rounded-xl border border-slate-200 bg-slate-50 p-4"
                >
                  <p class="text-xs font-bold uppercase tracking-widest text-slate-400">AI 摘要参考</p>
                  <p class="text-sm leading-relaxed text-slate-700">{{ parsedData.summary }}</p>
                </div>

                <div
                  v-if="parsedData.keyPoints && parsedData.keyPoints.length > 0"
                  class="space-y-3 rounded-xl border border-slate-200 bg-white p-4"
                >
                  <p class="text-xs font-bold uppercase tracking-widest text-slate-400">关键要点</p>
                  <div class="space-y-2">
                    <div
                      v-for="(point, idx) in parsedData.keyPoints"
                      :key="idx"
                      class="flex items-start gap-2"
                    >
                      <span class="material-symbols-outlined mt-0.5 text-sm text-primary">check_circle</span>
                      <span class="text-sm text-slate-700">{{ point }}</span>
                    </div>
                  </div>
                </div>

                <div
                  v-if="parsedData.todos && parsedData.todos.length > 0"
                  class="space-y-3 rounded-xl border border-slate-200 bg-white p-4"
                >
                  <p class="text-xs font-bold uppercase tracking-widest text-slate-400">自动生成的待办</p>
                  <div class="space-y-2">
                    <div
                      v-for="(todo, idx) in parsedData.todos"
                      :key="idx"
                      class="flex items-center gap-3"
                    >
                      <input type="checkbox" checked class="rounded text-primary focus:ring-primary" />
                      <span class="text-sm text-slate-700">{{ todo }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="flex gap-3 pt-4">
                <button
                  class="flex-1 rounded-xl border border-slate-200 py-3 text-sm font-bold text-slate-600 transition-all hover:bg-slate-50"
                  @click="handleRetry"
                >
                  重新录入
                </button>

                <button
                  class="flex-1 rounded-xl bg-primary py-3 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 disabled:opacity-50"
                  :disabled="saving"
                  @click="handleConfirmSave"
                >
                  <span v-if="saving" class="material-symbols-outlined mr-1 animate-spin text-sm">sync</span>
                  确认并保存
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { aiParseFollowUp, addFollowUp, uploadFollowUpAttachment } from '@/api/followup'
import type { AiFollowUpParseVO } from '@/api/followup'
import type { FollowUpAttachmentDraft } from '@/types/customer'
import type { Customer } from '@/types/customer'
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

const FOLLOW_UP_TYPES = new Set(['call', 'meeting', 'email', 'visit', 'other'])

const step = ref(1)
const textInput = ref('')
const isRecording = ref(false)
const isProcessing = ref(false)
const saving = ref(false)
const recordingDuration = ref(0)
const parsedData = ref<AiFollowUpParseVO | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const attachments = ref<Attachment[]>([])
const parsedForm = reactive<ParsedFollowUpForm>({
  type: 'other',
  content: '',
  followTime: '',
  nextFollowTime: ''
})

let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let recordingTimer: ReturnType<typeof setInterval> | null = null

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      resetDrawerState()
    }
  }
)

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

function formatDateForApi(date: Date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

function normalizeFollowUpType(type?: string): string {
  return type && FOLLOW_UP_TYPES.has(type) ? type : 'other'
}

function buildEditableContent(result: AiFollowUpParseVO, fallbackContent: string): string {
  const blocks: string[] = []

  if (result.summary?.trim()) {
    blocks.push(result.summary.trim())
  }

  if (result.keyPoints?.length) {
    blocks.push(`关键要点：\n${result.keyPoints.map(point => `- ${point}`).join('\n')}`)
  }

  if (result.todos?.length) {
    blocks.push(`待办事项：\n${result.todos.map(todo => `- ${todo}`).join('\n')}`)
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
  const hasAudioInput = hasMobileAudioInputSupport()
  const canUseFileCapture = canCaptureMobileAudioFile()
    && (!hasAudioInput || typeof MediaRecorder === 'undefined')
    && typeof window !== 'undefined'
    && (window.innerWidth < 768 || window.matchMedia?.('(pointer: coarse)').matches)

  if (canUseFileCapture) {
    const capturedFile = await captureMobileAudioFile()
    if (capturedFile) {
      handleFile(capturedFile)
    }
    return
  }

  if (!hasAudioInput || typeof MediaRecorder === 'undefined') {
    ElMessage.warning('当前浏览器不支持录音，请改用文字输入')
    return
  }

  try {
    const stream = await requestMobileAudioStream({ audio: true })
    audioChunks = []
    mediaRecorder = new MediaRecorder(stream)

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }

    mediaRecorder.onstop = () => {
      stream.getTracks().forEach(track => track.stop())
      if (audioChunks.length > 0) {
        const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
        const audioFile = new File([audioBlob], `recording-${Date.now()}.webm`, { type: 'audio/webm' })
        const id = Math.random().toString(36).slice(2, 11)
        attachments.value.push({ id, file: audioFile })
      }
    }

    mediaRecorder.start()
    isRecording.value = true
    recordingDuration.value = 0
    recordingTimer = setInterval(() => {
      recordingDuration.value += 1
    }, 1000)
  } catch {
    ElMessage.warning('无法访问麦克风，请检查浏览器权限设置')
  }
}

function handleStopRecording() {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
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

function handleFile(file: File) {
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

async function handleSubmitText() {
  if (isProcessing.value) return
  if (!textInput.value.trim() && attachments.value.length === 0) return

  isProcessing.value = true

  try {
    let content = textInput.value.trim()

    if (attachments.value.length > 0) {
      const fileNames = attachments.value.map(item => item.file.name).join(', ')
      content = content
        ? `${content}\n\n[附件: ${fileNames}]`
        : `[用户上传了附件: ${fileNames}]`
    }

    const result = await aiParseFollowUp({
      content,
      customerName: props.customer?.companyName || '',
      customerId: props.customer?.customerId || ''
    })

    applyParsedResult(result, content)
  } catch (err: unknown) {
    console.error('AI parse follow-up failed:', err)
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
    const uploadedAttachments: FollowUpAttachmentDraft[] = []
    for (const attachment of attachments.value) {
      uploadedAttachments.push(await uploadFollowUpAttachment(attachment.file))
    }

    await addFollowUp({
      customerId: props.customer.customerId,
      type: normalizeFollowUpType(parsedForm.type),
      content,
      followTime: parsedForm.followTime,
      nextFollowTime: parsedForm.nextFollowTime || undefined,
      summary: parsedData.value?.summary || undefined,
      aiGenerated: parsedData.value ? 1 : 0,
      attachments: uploadedAttachments
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

.fade-up-enter-active {
  transition: all 0.3s ease;
}

.fade-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

@keyframes recording-bar {
  0%,
  100% {
    height: 8px;
  }

  50% {
    height: 24px;
  }
}

.animate-recording-bar {
  height: 8px;
  animation: recording-bar 0.5s ease-in-out infinite;
}
</style>
