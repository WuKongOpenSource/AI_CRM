<template>
  <!-- Backdrop -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-40"
        @click="handleClose"
      />
    </Transition>
    <Transition name="slide-right">
      <div
        v-if="modelValue"
        class="fixed inset-y-0 right-0 w-full max-w-md bg-white shadow-2xl z-50 flex flex-col"
      >
        <!-- Header -->
        <div class="p-6 border-b border-slate-200 flex items-center justify-between shrink-0">
          <div class="flex items-center gap-2">
            <div class="size-8 bg-primary/10 text-primary rounded-lg flex items-center justify-center">
              <span class="material-symbols-outlined text-lg">smart_toy</span>
            </div>
            <h2 class="text-lg font-bold text-slate-900">AI 智能跟进录入</h2>
            <span v-if="customer" class="text-xs text-slate-500 font-medium px-2 py-0.5 bg-slate-100 rounded-full truncate max-w-[150px]">
              {{ customer.companyName }}
            </span>
          </div>
          <button @click="handleClose" class="text-slate-400 hover:text-slate-600 transition-colors">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <!-- Content -->
        <div class="flex-1 overflow-y-auto p-6">
          <!-- Step 1: Input -->
          <div v-if="step === 1" class="space-y-8">
            <!-- Voice Recording Section -->
            <div class="text-center space-y-4 py-8">
              <div
                class="size-24 mx-auto rounded-full flex items-center justify-center transition-all duration-500"
                :class="{
                  'bg-red-50 text-red-500 scale-110 shadow-lg shadow-red-200': isRecording,
                  'bg-primary/5 text-primary animate-pulse': isProcessing,
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
                  {{ isRecording ? '正在倾听您的跟进记录...' : isProcessing ? 'AI 正在分析内容...' : '点击开始录音' }}
                </h3>
                <p class="text-sm text-slate-500 mt-1">
                  您可以直接口述跟进内容，或在下方输入文字/粘贴图片，AI 将自动提取关键信息。
                </p>
              </div>
              <!-- Start Recording Button -->
              <button
                v-if="!isRecording && !isProcessing"
                class="px-8 py-3 bg-primary text-white rounded-full font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all"
                @click="handleStartRecording"
              >
                开始语音录入
              </button>
              <!-- Stop Recording Button -->
              <button
                v-if="isRecording"
                class="px-8 py-3 bg-red-500 text-white rounded-full font-bold shadow-lg shadow-red-200 hover:bg-red-600 transition-all"
                @click="handleStopRecording"
              >
                停止录音
              </button>
              <!-- Recording Visualizer -->
              <div v-if="isRecording" class="flex items-center justify-center gap-1 h-8">
                <div
                  v-for="i in 6"
                  :key="i"
                  class="w-1 bg-red-400 rounded-full animate-recording-bar"
                  :style="{ animationDelay: `${i * 0.1}s` }"
                />
              </div>
              <!-- Recording Duration -->
              <p v-if="isRecording" class="text-sm text-red-500 font-mono">{{ formatDuration(recordingDuration) }}</p>
            </div>

            <!-- Divider -->
            <div class="flex items-center gap-2">
              <div class="h-px bg-slate-200 flex-1" />
              <span class="text-xs font-bold text-slate-400 uppercase tracking-widest">或者</span>
              <div class="h-px bg-slate-200 flex-1" />
            </div>

            <!-- Text Input Section -->
            <div class="space-y-4">
              <div class="space-y-2 relative">
                <label class="text-xs font-bold text-slate-500 uppercase">手动输入或粘贴附件</label>
                <div class="relative">
                  <textarea
                    v-model="textInput"
                    class="w-full h-40 p-4 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-primary/50 outline-none transition-all resize-none pr-12"
                    placeholder="例如：今天和张总聊了关于 Q4 扩容的事情... (支持粘贴图片或文档)"
                    @paste="handlePaste"
                  />
                  <div class="absolute bottom-3 right-3 flex flex-col gap-2">
                    <label class="size-8 flex items-center justify-center text-slate-400 hover:text-primary hover:bg-primary/10 rounded-lg transition-all cursor-pointer">
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

                <!-- Attachments Preview -->
                <div v-if="attachments.length > 0" class="flex flex-wrap gap-2 mt-2">
                  <div v-for="attr in attachments" :key="attr.id" class="relative group">
                    <img
                      v-if="attr.preview"
                      :src="attr.preview"
                      class="size-16 rounded-lg object-cover border border-slate-200"
                      alt="preview"
                    />
                    <div v-else class="size-16 rounded-lg bg-slate-100 border border-slate-200 flex flex-col items-center justify-center p-1 text-center">
                      <span class="material-symbols-outlined text-slate-400 text-sm">description</span>
                      <span class="text-[8px] text-slate-500 truncate w-full">{{ attr.file.name }}</span>
                    </div>
                    <button
                      class="absolute -top-1.5 -right-1.5 size-5 bg-red-500 text-white rounded-full flex items-center justify-center shadow-sm opacity-0 group-hover:opacity-100 transition-opacity"
                      @click="removeAttachment(attr.id)"
                    >
                      <span class="material-symbols-outlined text-[12px]">close</span>
                    </button>
                  </div>
                </div>
              </div>

              <!-- Submit Button -->
              <button
                class="w-full py-3 bg-slate-900 text-white rounded-xl text-sm font-bold hover:bg-slate-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
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

          <!-- Step 2: AI Parsed Results -->
          <Transition name="fade-up">
            <div v-if="step === 2 && parsedData" class="space-y-6">
              <!-- Success Alert -->
              <div class="bg-emerald-50 border border-emerald-100 rounded-xl p-4 flex items-start gap-3">
                <span class="material-symbols-outlined text-emerald-600">check_circle</span>
                <div>
                  <p class="text-sm font-bold text-emerald-900">解析成功！</p>
                  <p class="text-xs text-emerald-700">AI 已从您的记录中提取了以下关键要素。</p>
                </div>
              </div>

              <div class="space-y-4">
                <!-- Summary & Time -->
                <div class="p-4 bg-white border border-slate-200 rounded-xl space-y-4">
                  <div>
                    <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-2">核心摘要</p>
                    <p class="text-sm text-slate-700 leading-relaxed">{{ parsedData.summary }}</p>
                  </div>

                  <div class="grid grid-cols-2 gap-4 pt-4 border-t border-slate-50">
                    <div>
                      <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">跟进时间</p>
                      <div class="flex items-center gap-1.5 text-slate-700">
                        <span class="material-symbols-outlined text-sm">event</span>
                        <span class="text-xs font-bold">{{ parsedData.followTime }}</span>
                      </div>
                    </div>
                    <div>
                      <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-1">预计下次联系</p>
                      <div class="flex items-center gap-1.5 text-primary">
                        <span class="material-symbols-outlined text-sm">event_repeat</span>
                        <span class="text-xs font-bold">{{ parsedData.nextFollowTime || '待定' }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Key Points -->
                <div v-if="parsedData.keyPoints && parsedData.keyPoints.length > 0" class="p-4 bg-white border border-slate-200 rounded-xl space-y-3">
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">关键要点</p>
                  <div class="space-y-2">
                    <div v-for="(point, idx) in parsedData.keyPoints" :key="idx" class="flex items-start gap-2">
                      <span class="material-symbols-outlined text-primary text-sm mt-0.5">check_circle</span>
                      <span class="text-sm text-slate-700">{{ point }}</span>
                    </div>
                  </div>
                </div>

                <!-- Auto-generated Todos -->
                <div v-if="parsedData.todos && parsedData.todos.length > 0" class="p-4 bg-white border border-slate-200 rounded-xl space-y-3">
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">自动生成的待办</p>
                  <div class="space-y-2">
                    <div v-for="(todo, idx) in parsedData.todos" :key="idx" class="flex items-center gap-3">
                      <input type="checkbox" checked class="rounded text-primary focus:ring-primary" />
                      <span class="text-sm text-slate-700">{{ todo }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Action Buttons -->
              <div class="pt-4 flex gap-3">
                <button
                  class="flex-1 py-3 border border-slate-200 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all"
                  @click="handleRetry"
                >
                  重新录入
                </button>
                <button
                  class="flex-1 py-3 bg-primary text-white rounded-xl text-sm font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all disabled:opacity-50"
                  :disabled="saving"
                  @click="handleConfirmSave"
                >
                  <span v-if="saving" class="material-symbols-outlined animate-spin text-sm mr-1">sync</span>
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
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { aiParseFollowUp, addFollowUp } from '@/api/followup'
import type { AiFollowUpParseVO } from '@/api/followup'
import type { Customer } from '@/types/customer'

const props = defineProps<{
  modelValue: boolean
  customer: Customer | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

// State
const step = ref(1)
const textInput = ref('')
const isRecording = ref(false)
const isProcessing = ref(false)
const saving = ref(false)
const recordingDuration = ref(0)
const parsedData = ref<AiFollowUpParseVO | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

// Recording
let mediaRecorder: MediaRecorder | null = null
let audioChunks: Blob[] = []
let recordingTimer: ReturnType<typeof setInterval> | null = null

// Attachments
interface Attachment {
  id: string
  file: File
  preview?: string
}
const attachments = ref<Attachment[]>([])

// Reset state when drawer opens/closes
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) {
    step.value = 1
    textInput.value = ''
    parsedData.value = null
    attachments.value = []
    isRecording.value = false
    isProcessing.value = false
    saving.value = false
    recordingDuration.value = 0
  }
})

function handleClose() {
  if (isRecording.value) {
    handleStopRecording()
  }
  emit('update:modelValue', false)
}

// ---- Voice Recording ----
async function handleStartRecording() {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
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
        const id = Math.random().toString(36).substring(2, 11)
        attachments.value.push({ id, file: audioFile })
      }
    }

    mediaRecorder.start()
    isRecording.value = true
    recordingDuration.value = 0
    recordingTimer = setInterval(() => {
      recordingDuration.value++
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
  const m = Math.floor(seconds / 60).toString().padStart(2, '0')
  const s = (seconds % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

// ---- File Handling ----
function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) {
    Array.from(input.files).forEach(handleFile)
  }
  input.value = ''
}

function handlePaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  for (let i = 0; i < items.length; i++) {
    if (items[i].kind === 'file') {
      const file = items[i].getAsFile()
      if (file) handleFile(file)
    }
  }
}

function handleFile(file: File) {
  const id = Math.random().toString(36).substring(2, 11)
  const isImage = file.type.startsWith('image/')
  attachments.value.push({
    id,
    file,
    preview: isImage ? URL.createObjectURL(file) : undefined
  })
}

function removeAttachment(id: string) {
  const idx = attachments.value.findIndex(a => a.id === id)
  if (idx >= 0) {
    const removed = attachments.value[idx]
    if (removed.preview) URL.revokeObjectURL(removed.preview)
    attachments.value.splice(idx, 1)
  }
}

// ---- AI Parse ----
async function handleSubmitText() {
  if (isProcessing.value) return
  if (!textInput.value.trim() && attachments.value.length === 0) return

  isProcessing.value = true
  try {
    // Build content: text + attachment file names for context
    let content = textInput.value.trim()
    if (attachments.value.length > 0) {
      const fileNames = attachments.value.map(a => a.file.name).join(', ')
      if (content) {
        content += `\n\n[附件: ${fileNames}]`
      } else {
        content = `[用户上传了文件: ${fileNames}]`
      }
    }

    const result = await aiParseFollowUp({
      content,
      customerName: props.customer?.companyName || '',
      customerId: props.customer?.customerId || ''
    })

    parsedData.value = result
    step.value = 2
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '未知错误'
    ElMessage.error('AI 解析失败: ' + message)
  } finally {
    isProcessing.value = false
  }
}

// ---- Save ----
async function handleConfirmSave() {
  if (!parsedData.value || !props.customer) return

  saving.value = true
  try {
    // Build the full content from AI summary + key points + original text
    const parts: string[] = []
    if (parsedData.value.summary) {
      parts.push(parsedData.value.summary)
    }
    if (parsedData.value.keyPoints && parsedData.value.keyPoints.length > 0) {
      parts.push('\n关键要点：\n' + parsedData.value.keyPoints.map(p => `• ${p}`).join('\n'))
    }
    if (parsedData.value.todos && parsedData.value.todos.length > 0) {
      parts.push('\n待办事项：\n' + parsedData.value.todos.map(t => `☐ ${t}`).join('\n'))
    }

    await addFollowUp({
      customerId: props.customer.customerId,
      type: parsedData.value.type || 'other',
      content: parts.join('\n'),
      followTime: parsedData.value.followTime || new Date().toISOString(),
      nextFollowTime: parsedData.value.nextFollowTime || undefined
    })

    ElMessage.success('跟进记录已保存')
    emit('saved')
    emit('update:modelValue', false)
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '未知错误'
    ElMessage.error('保存失败: ' + message)
  } finally {
    saving.value = false
  }
}

function handleRetry() {
  step.value = 1
  parsedData.value = null
}
</script>

<style scoped>
/* Drawer slide-in animation */
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

/* Backdrop fade */
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

/* Step 2 fade-up */
.fade-up-enter-active {
  transition: all 0.3s ease;
}
.fade-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

/* Recording bar animation */
@keyframes recording-bar {
  0%, 100% { height: 8px; }
  50% { height: 24px; }
}
.animate-recording-bar {
  animation: recording-bar 0.5s ease-in-out infinite;
  height: 8px;
}
</style>
