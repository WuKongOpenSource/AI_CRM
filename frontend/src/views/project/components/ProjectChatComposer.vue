<template>
  <div class="mx-auto w-[calc(100%-20px)] max-w-4xl md:w-full">
    <div class="relative group min-w-0 w-[768px] max-w-full mx-auto">
      <div class="wk-chat-composer relative flex min-w-0 items-center rounded-[28px] p-[6px]">
        <div class="w-full min-w-0">
          <textarea
            ref="textareaRef"
            :value="modelValue"
            rows="1"
            class="wk-project-shared-composer-textarea"
            :placeholder="placeholder"
            :disabled="disabled"
            @input="handleInput"
            @keydown.enter.exact.prevent="handleSend"
          ></textarea>

          <div class="flex min-w-0 items-center justify-between w-full px-1 pb-1 select-none mt-1">
            <div class="flex min-w-0 items-center gap-2">
              <el-popover
                v-model:visible="uploadMenuVisible"
                trigger="click"
                placement="top-start"
                width="200"
                :show-arrow="false"
                :disabled="disabled"
                :teleported="true"
                transition="el-zoom-in-bottom"
                popper-class="wk-chat-upload-menu-popper"
              >
                <template #reference>
                  <button
                    type="button"
                    class="group/project-upload-trigger relative flex size-8 items-center justify-center rounded-full text-[#0d0d0d] transition-colors hover:bg-[#F1F1F1] disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="disabled"
                    aria-label="添加文件等"
                  >
                    <WkIcon name="add-1" :box-size="16" class="shrink-0" />
                    <span class="wk-project-chat-tooltip" role="tooltip">添加文件等</span>
                  </button>
                </template>
                <div
                  class="wk-chat-upload-menu"
                  @mouseenter="clearUploadMenuLeaveTimer"
                  @mouseleave="handleUploadMenuMouseLeave"
                >
                  <button type="button" class="wk-chat-upload-menu__item" @click="handleUnsupportedAttachment">
                    <WkIcon name="file" :box-size="18" class="shrink-0" />
                    <span class="wk-chat-upload-menu__label">上传图片和文件</span>
                  </button>
                  <button type="button" class="wk-chat-upload-menu__item" @click="handleUnsupportedAttachment">
                    <WkIcon name="knowledge-1" :size="18" class="shrink-0" />
                    <span class="wk-chat-upload-menu__label">选择知识库文件</span>
                  </button>
                  <el-popover
                    v-model:visible="uploadSubmenuVisible"
                    trigger="hover"
                    placement="right-end"
                    :show-arrow="false"
                    :disabled="disabled"
                    :teleported="false"
                    :offset="8"
                    :hide-after="220"
                    width="200"
                    popper-class="wk-chat-upload-menu-popper wk-chat-upload-submenu-popper"
                  >
                    <template #reference>
                      <div
                        class="wk-chat-upload-menu__apps-ref"
                        role="button"
                        tabindex="0"
                      >
                        <WkIcon name="application" :size="18" class="shrink-0" />
                        <span class="wk-chat-upload-menu__label">悟空技能</span>
                        <span class="wk-chat-upload-menu__chevron material-symbols-outlined">chevron_right</span>
                      </div>
                    </template>
                    <div
                      class="wk-chat-upload-submenu"
                      @mouseenter="clearUploadMenuLeaveTimer"
                      @mouseleave="handleUploadMenuMouseLeave"
                    >
                      <button
                        v-for="app in appChoices"
                        :key="app.code"
                        type="button"
                        class="wk-chat-upload-menu__item wk-chat-upload-submenu__btn"
                        @click="handleSelectApp(app.code)"
                      >
                        <WkIcon
                          :name="app.icon"
                          :size="18"
                          class="shrink-0"
                          :class="chatStore.selectedAppCode === app.code ? '!text-[var(--wk-text-primary)]' : ''"
                        />
                        <span
                          class="wk-chat-upload-menu__label wk-chat-upload-submenu__label"
                          :class="chatStore.selectedAppCode === app.code ? 'text-[var(--wk-text-primary)]' : 'text-[#0d0d0d]'"
                        >
                          {{ app.label }}
                        </span>
                        <span
                          v-if="chatStore.selectedAppCode === app.code"
                          class="wk-chat-upload-menu__check material-symbols-outlined fill-1 text-primary"
                        >check</span>
                      </button>
                    </div>
                  </el-popover>
                </div>
              </el-popover>

              <button
                v-if="selectedChatAppLabel"
                type="button"
                class="group/project-app-toolbar h-[36px] rounded-full pl-1 pr-3.5 text-sm text-[var(--wk-text-primary)] transition-all hover:bg-[var(--wk-bg-surface-hover)]"
                aria-pressed="true"
                :title="`已启用 ${selectedChatAppLabel}，点击关闭`"
                @click="chatStore.setSelectedAppCode('general')"
              >
                <span class="flex items-center gap-1.5">
                  <span class="relative flex size-[22px] shrink-0 items-center justify-center">
                    <span class="flex size-full items-center justify-center transition-opacity duration-150 group-hover/project-app-toolbar:pointer-events-none group-hover/project-app-toolbar:opacity-0">
                      <WkIcon :name="selectedChatAppIcon" :size="18" class="shrink-0" />
                    </span>
                    <span class="pointer-events-none absolute inset-0 flex items-center justify-center rounded-full bg-[var(--wk-bg-surface-active)] text-[var(--wk-text-primary)] opacity-0 transition-opacity duration-150 group-hover/project-app-toolbar:opacity-100" aria-hidden="true">
                      <span class="material-symbols-outlined text-[14px] leading-none">close</span>
                    </span>
                  </span>
                  <span>{{ selectedChatAppLabel }}</span>
                </span>
              </button>
            </div>

            <div class="flex shrink-0 items-center gap-2 pr-1">
              <el-popover
                v-model:visible="modelPopoverVisible"
                trigger="click"
                placement="top-end"
                width="340"
                :show-arrow="false"
                :teleported="true"
                :disabled="disabled || chatStore.modelOptionsLoading"
                transition="el-zoom-in-bottom"
                popper-class="wk-chat-model-popper"
              >
                <template #reference>
                  <button
                    type="button"
                    class="inline-flex h-9 max-w-[260px] shrink-0 items-center gap-1.5 rounded-[18px] border border-[#ececec] bg-[#f5f5f5] pl-2 pr-2 text-left text-[13px] text-[#0d0d0d] transition-colors hover:bg-[#ececec] disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="disabled || chatStore.modelOptionsLoading"
                    :title="`当前模型：${composerModelLabel}`"
                  >
                    <span class="relative flex h-[20px] w-[20px] shrink-0 items-center justify-center overflow-hidden rounded-md bg-[#ececec]" aria-hidden="true">
                      <template v-if="chatStore.selectedModel">
                        <img
                          v-if="modelShowImage(chatStore.selectedModel)"
                          :src="modelIconSrc(chatStore.selectedModel)"
                          alt=""
                          class="size-full object-fill"
                          @error="onModelImageError($event)"
                        />
                        <span
                          v-else
                          class="flex size-full items-center justify-center text-[11px] font-semibold text-[#909090]"
                        >
                          {{ modelOptionLabel(chatStore.selectedModel).slice(0, 1) }}
                        </span>
                      </template>
                      <span
                        v-else
                        class="flex size-full items-center justify-center text-[11px] font-semibold text-[#909090]"
                      >
                        {{ selectedModelInitial }}
                      </span>
                    </span>
                    <span class="min-w-0 flex-1 truncate">{{ composerModelLabel }}</span>
                    <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-[#8f8f8f]">expand_more</span>
                  </button>
                </template>
                <div class="wk-chat-model-menu">
                  <template v-for="group in modelOptionGroups" :key="group.source">
                    <div v-if="modelOptionGroups.length > 1" class="wk-chat-model-menu__group-label">
                      {{ group.label }}
                    </div>
                    <button
                      v-for="option in group.options"
                      :key="chatStore.toModelKey(option)"
                      type="button"
                      class="wk-chat-model-menu__item"
                      @click="handleModelChange(chatStore.toModelKey(option))"
                    >
                      <span
                        class="relative flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-lg"
                        aria-hidden="true"
                      >
                        <img
                          v-if="modelShowImage(option)"
                          :src="modelIconSrc(option)"
                          alt=""
                          class="size-5 object-fill"
                          @error="onModelImageError($event)"
                        />
                        <span
                          v-else
                          class="flex size-full items-center justify-center text-[12px] font-semibold text-[#909090]"
                        >
                          {{ modelOptionLabel(option).slice(0, 1) }}
                        </span>
                      </span>
                      <div class="min-w-0 flex-1">
                        <div class="flex items-center justify-start gap-2">
                          <div class="min-w-0 text-[14px] leading-tight text-[#0d0d0d]">
                            {{ modelOptionLabel(option) }}
                          </div>
                          <span
                            v-if="shouldShowModelMultiplier(option)"
                            class="shrink-0 text-xs text-slate-400"
                          >
                            {{ formatModelMultiplier(option.creditMultiplier) }}
                          </span>
                        </div>
                      </div>
                      <span
                        class="material-symbols-outlined flex size-5 shrink-0 items-center justify-center text-[20px] leading-none"
                        :class="chatStore.selectedModelKey === chatStore.toModelKey(option) ? 'text-primary' : 'invisible'"
                        aria-hidden="true"
                      >
                        check
                      </span>
                    </button>
                    <button
                      v-if="group.source === 'custom' && canManageAiConfig"
                      type="button"
                      class="wk-chat-model-menu__more"
                      @click="handleOpenMoreModels"
                    >
                      <span class="material-symbols-outlined text-[18px] leading-none" aria-hidden="true">
                        add_circle
                      </span>
                      <span class="min-w-0 flex-1 truncate">更多模型</span>
                      <span class="material-symbols-outlined text-[18px] leading-none text-[#8f8f8f]" aria-hidden="true">
                        chevron_right
                      </span>
                    </button>
                  </template>
                </div>
              </el-popover>

              <button
                type="button"
                class="group/project-send relative flex h-[36px] w-[36px] shrink-0 items-center justify-center rounded-full bg-[#000] text-white transition-colors hover:bg-[#575757] disabled:cursor-not-allowed disabled:opacity-35"
                :class="sendActionButtonClass"
                :disabled="sendActionDisabled"
                :aria-label="sendActionTitle"
                @click="handleSendAction"
              >
                <span v-if="isTranscribing" class="material-symbols-outlined text-[20px] leading-none animate-spin">progress_activity</span>
                <span v-else-if="isRecording" class="wk-recording-indicator" aria-hidden="true">
                  <span class="material-symbols-outlined wk-recording-indicator__stop">stop</span>
                  <span class="wk-recording-indicator__bars">
                    <span />
                    <span />
                    <span />
                    <span />
                  </span>
                </span>
                <WkIcon
                  v-else-if="isComposerInputEmpty"
                  name="voice"
                  :box-size="20"
                  class="text-[20px] leading-none"
                />
                <span v-else class="material-symbols-outlined text-[20px] leading-none">arrow_upward</span>
                <span class="wk-project-chat-tooltip" role="tooltip">{{ sendActionTitle }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import WkIcon from '@/components/common/WkIcon.vue'
import { transcribeFollowUpAudio } from '@/api/followup'
import { getAiConfig } from '@/api/systemConfig'
import { useResponsive } from '@/composables/useResponsive'
import { useChatStore } from '@/stores/chat'
import type { ChatModelOption } from '@/types/common'
import { useAiQuota } from '@/composables/useAiQuota'
import {
  canCaptureMobileAudioFile,
  captureMobileAudioFile,
  hasMobileAudioInputSupport,
  requestMobileAudioStream
} from '@/utils/mobileAudioRecording'
import { isRequestErrorHandled } from '@/utils/requestError'
import dashscopeBrandUrl from '@/assets/model-provider-brands/dashscope.svg?url'
import openaiBrandUrl from '@/assets/model-provider-brands/openai.svg?url'
import deepseekBrandUrl from '@/assets/model-provider-brands/deepseek.svg?url'
import moonshotBrandUrl from '@/assets/model-provider-brands/moonshot.svg?url'
import arkBrandUrl from '@/assets/model-provider-brands/ark.svg?url'
import hunyuanBrandUrl from '@/assets/model-provider-brands/hunyuan.svg?url'
import minimaxBrandUrl from '@/assets/model-provider-brands/minimax.svg?url'
import zhipuBrandUrl from '@/assets/model-provider-brands/zhipu.svg?url'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  disabled?: boolean
}>(), {
  placeholder: '发消息...',
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'send'): void
}>()

const chatStore = useChatStore()
const { canManageAiConfig, goToAiSettings } = useAiQuota()
const { isMobile } = useResponsive()
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const uploadMenuVisible = ref(false)
const uploadSubmenuVisible = ref(false)
const modelPopoverVisible = ref(false)
const modelImageLoadFailed = ref<Record<string, boolean>>({})
const isRecording = ref(false)
const isTranscribing = ref(false)
let uploadMenuLeaveTimer: ReturnType<typeof setTimeout> | null = null
let mediaRecorder: MediaRecorder | null = null
let mediaStream: MediaStream | null = null
let recordedChunks: Blob[] = []
let recordedMimeType = ''
let skipNextTranscription = false
let transcriptionToken = 0
let speechInputBase = ''

const appChoices = [
  { code: 'crm', label: 'CRM管理', icon: 'customer' },
  { code: 'knowledge', label: '知识库检索', icon: 'knowledge-1' },
  { code: 'address_book', label: '通讯录', icon: 'customer' },
  { code: 'relation', label: '关系', icon: 'customer' }
] as const

const modelOptionGroups = computed(() => {
  const customOptions = chatStore.modelOptions.filter(option => option.modelSource === 'custom')
  const systemOptions = chatStore.modelOptions.filter(option => option.modelSource !== 'custom')
  return [
    { source: 'custom', label: '自定义模型', options: customOptions },
    { source: 'system', label: '系统模型', options: systemOptions }
  ].filter(group => group.options.length > 0)
})

const composerModelLabel = computed(() => {
  if (chatStore.modelOptionsLoading) return '加载模型...'
  const model = chatStore.selectedModel
  return model ? modelOptionLabel(model) : '选择模型'
})

const selectedModelInitial = computed(() => composerModelLabel.value.slice(0, 1) || '?')

const selectedChatAppLabel = computed(() => {
  if (chatStore.selectedAppCode === 'general') return ''
  return appChoices.find(app => app.code === chatStore.selectedAppCode)?.label || chatStore.selectedApp?.label || ''
})

const selectedChatAppIcon = computed(() =>
  appChoices.find(app => app.code === chatStore.selectedAppCode)?.icon || 'application'
)

const isComposerInputEmpty = computed(() => !props.modelValue.trim())

const sendActionButtonClass = computed(() => {
  if (isRecording.value) return '!bg-red-500 text-white hover:!bg-red-600'
  if (isTranscribing.value) return '!bg-[#e5e5e5] !text-[#909090]'
  return ''
})

const sendActionDisabled = computed(() => props.disabled || isTranscribing.value)

const sendActionTitle = computed(() => {
  if (isTranscribing.value) return '语音识别中…'
  if (isRecording.value) return '点击结束录音'
  if (isComposerInputEmpty.value) return '使用语音功能'
  return '发送'
})

onMounted(() => {
  if (chatStore.modelOptions.length === 0) void chatStore.fetchModelOptions()
  if (chatStore.appOptions.length === 0) void chatStore.fetchAppOptions()
  void nextTick(resizeTextarea)
})

watch(() => props.modelValue, () => {
  void nextTick(resizeTextarea)
})

watch(
  () => chatStore.modelOptions,
  () => {
    modelImageLoadFailed.value = {}
  },
  { deep: true }
)

watch(uploadMenuVisible, (visible) => {
  if (!visible) {
    uploadSubmenuVisible.value = false
    clearUploadMenuLeaveTimer()
  }
})

onBeforeUnmount(() => {
  clearUploadMenuLeaveTimer()
  abortVoiceRecording()
})

function modelOptionLabel(option: ChatModelOption): string {
  return option.displayName || option.modelName
}

const MODEL_PROVIDER_BRAND_URL: Record<string, string> = {
  dashscope: dashscopeBrandUrl,
  openai: openaiBrandUrl,
  deepseek: deepseekBrandUrl,
  moonshot: moonshotBrandUrl,
  ark: arkBrandUrl,
  arkl: arkBrandUrl,
  hunyuan: hunyuanBrandUrl,
  minimax: minimaxBrandUrl,
  zhipu: zhipuBrandUrl,
}

function providerBrandAssetUrl(provider: string): string | undefined {
  const id = provider?.trim().toLowerCase()
  if (!id || !/^[-a-z0-9._]+$/.test(id)) return undefined
  return MODEL_PROVIDER_BRAND_URL[id]
}

function modelIconSrc(option: ChatModelOption): string | undefined {
  const fromApi = option.icon?.trim()
  if (fromApi) return fromApi
  return providerBrandAssetUrl(option.provider)
}

function modelShowImage(option: ChatModelOption): boolean {
  const src = modelIconSrc(option)
  if (!src) return false
  return !modelImageLoadFailed.value[src]
}

function onModelImageError(event: Event) {
  const target = event.target as HTMLImageElement | null
  const src = target?.currentSrc || target?.src
  if (!src) return
  modelImageLoadFailed.value = { ...modelImageLoadFailed.value, [src]: true }
}

function shouldShowModelMultiplier(option: ChatModelOption): boolean {
  return Number(option.creditMultiplier || 1) !== 1
}

function formatModelMultiplier(value?: number | null) {
  const multiplier = Number(value || 1)
  return `${multiplier.toFixed(multiplier % 1 === 0 ? 0 : 2)}x 积分`
}

function resizeTextarea() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 220)}px`
}

function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement | null
  emit('update:modelValue', target?.value || '')
  void nextTick(resizeTextarea)
}

function handleSend() {
  if (props.disabled || !props.modelValue.trim()) return
  emit('send')
}

function handleSendAction() {
  if (props.disabled || isTranscribing.value) return
  if (isRecording.value) {
    handleStopAudioRecording()
    return
  }
  if (!isComposerInputEmpty.value) {
    handleSend()
    return
  }
  void handleStartAudioRecording()
}

function handleModelChange(modelKey: string) {
  chatStore.setSelectedModelKey(modelKey)
  modelPopoverVisible.value = false
  void nextTick(() => textareaRef.value?.focus())
}

function handleOpenMoreModels() {
  modelPopoverVisible.value = false
  goToAiSettings()
}

function handleSelectApp(appCode: string) {
  chatStore.setSelectedAppCode(chatStore.selectedAppCode === appCode ? 'general' : appCode)
}

function handleUnsupportedAttachment() {
  uploadMenuVisible.value = false
  ElMessage.info('项目/任务对话暂未接入附件上传，当前可使用技能和模型选择')
}

function clearUploadMenuLeaveTimer() {
  if (uploadMenuLeaveTimer != null) {
    clearTimeout(uploadMenuLeaveTimer)
    uploadMenuLeaveTimer = null
  }
}

function isInsideUploadMenuPopover(target: EventTarget | null): boolean {
  if (!(target instanceof Node)) return false
  const el = target instanceof Element ? target : target.parentElement
  return Boolean(el?.closest('.wk-chat-upload-menu-popper'))
}

function handleUploadMenuMouseLeave(event: MouseEvent) {
  if (isInsideUploadMenuPopover(event.relatedTarget)) {
    clearUploadMenuLeaveTimer()
    return
  }
  clearUploadMenuLeaveTimer()
  uploadMenuLeaveTimer = setTimeout(() => {
    uploadMenuLeaveTimer = null
    uploadMenuVisible.value = false
    uploadSubmenuVisible.value = false
  }, 120)
}

function releaseMediaStream() {
  mediaStream?.getTracks().forEach(track => track.stop())
  mediaStream = null
}

function abortVoiceRecording() {
  skipNextTranscription = true
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  mediaRecorder = null
  releaseMediaStream()
  recordedChunks = []
  recordedMimeType = ''
  isRecording.value = false
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
  if (recordedChunks.length === 0) return null
  const mimeType = recordedMimeType || recordedChunks[0]?.type || 'audio/webm'
  const blob = new Blob(recordedChunks, { type: mimeType })
  return new File([blob], `project-chat-recording.${resolveAudioExtension(mimeType)}`, { type: mimeType })
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
    if (currentToken !== transcriptionToken) return
    if (!transcript) {
      ElMessage.warning('未识别到有效语音内容，请重试')
      return
    }
    emit('update:modelValue', speechInputBase ? `${speechInputBase}\n${transcript}` : transcript)
    void nextTick(() => {
      resizeTextarea()
      textareaRef.value?.focus()
    })
    ElMessage.success('语音已转成文字，可继续编辑后发送')
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
  isRecording.value = false
  const file = shouldSkip ? null : buildRecordedAudioFile()
  mediaRecorder = null
  releaseMediaStream()
  recordedChunks = []
  recordedMimeType = ''
  if (shouldSkip) return
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
    speechInputBase = props.modelValue.trim()
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
    speechInputBase = props.modelValue.trim()
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
  } catch (err) {
    console.error('Start recording failed:', err)
    abortVoiceRecording()
    ElMessage.warning('无法启动录音，请检查浏览器和麦克风权限')
  }
}

function handleStopAudioRecording() {
  if (!mediaRecorder) return
  skipNextTranscription = false
  if (mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  isRecording.value = false
}
</script>

<style scoped>
.wk-chat-composer {
  border: 1px solid var(--wk-border-subtle);
  background: var(--wk-bg-surface);
  box-shadow:
    0 20px 70px rgb(var(--wk-shadow-color) / 0.08),
    0 2px 8px rgb(var(--wk-shadow-color) / 0.05);
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.wk-chat-composer:focus-within {
  border-color: var(--wk-border-muted);
  box-shadow:
    0 22px 78px rgb(var(--wk-shadow-color) / 0.11),
    0 0 0 1px rgb(var(--wk-primary-rgb) / 0.12);
}

.wk-project-shared-composer-textarea {
  min-height: 90px;
  max-height: 220px;
  width: 100%;
  min-width: 0;
  resize: none;
  overflow-x: hidden;
  overflow-y: auto;
  border: 0;
  background: transparent;
  padding: 0.75rem;
  color: #0d0d0d;
  font-size: 16px;
  line-height: 26px;
  outline: none;
}

.wk-project-shared-composer-textarea::placeholder {
  color: #909090;
}

.wk-chat-upload-menu {
  padding: 10px;
}

.wk-chat-upload-menu__item,
.wk-chat-upload-menu__apps-ref {
  width: 100%;
  display: flex;
  height: 36px;
  align-items: center;
  gap: 10px;
  border-radius: 8px;
  padding: 10px;
  color: var(--wk-text-primary);
  transition: background-color 150ms ease;
}

.wk-chat-upload-menu__item:hover,
.wk-chat-upload-menu__apps-ref:hover {
  background: var(--wk-bg-surface-hover);
}

.wk-chat-upload-menu__item:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.wk-chat-upload-menu__label,
.wk-chat-upload-submenu__label {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wk-chat-upload-menu__apps-ref {
  cursor: default;
  outline: none;
}

.wk-chat-upload-menu__chevron {
  margin-left: auto;
  color: var(--wk-text-muted);
  font-size: 18px;
}

.wk-chat-upload-submenu {
  padding: 10px;
}

.wk-chat-upload-submenu__btn {
  justify-content: flex-start;
}

.wk-chat-upload-submenu__label {
  text-align: left;
}

.wk-chat-upload-menu__check {
  margin-left: auto;
  font-size: 18px;
  line-height: 1;
}

.wk-chat-model-menu__group-label {
  padding: 8px 10px 4px;
  color: var(--wk-text-muted);
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
}

.wk-chat-model-menu {
  padding: 6px;
  max-height: min(52vh, 360px);
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-gutter: stable;
}

.wk-chat-model-menu__item {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 5px;
  border: none;
  border-radius: 8px;
  background: transparent;
  padding: 2px 10px;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.12s ease;
}

.wk-chat-model-menu__item:hover {
  background: var(--wk-bg-surface-hover);
}

.wk-chat-model-menu__more {
  display: flex;
  width: calc(100% - 4px);
  align-items: center;
  gap: 8px;
  margin: 4px 2px 6px;
  border: 0px solid var(--wk-border-subtle);
  border-radius: 8px;
  background: var(--wk-bg-surface-subtle);
  padding: 8px 10px;
  color: var(--wk-primary);
  font-size: 13px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.12s ease, border-color 0.12s ease;
}

.wk-chat-model-menu__more:hover {
  border-color: color-mix(in srgb, var(--wk-primary) 24%, var(--wk-border-subtle));
  background: color-mix(in srgb, var(--wk-primary) 8%, var(--wk-bg-surface));
}

.wk-project-chat-tooltip {
  pointer-events: none;
  position: absolute;
  left: 50%;
  top: 100%;
  z-index: 200;
  margin-top: 8px;
  transform: translateX(-50%);
  white-space: nowrap;
  border-radius: 8px;
  background: #000;
  padding: 6px 12px;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  opacity: 0;
  box-shadow: 0 8px 20px rgb(15 23 42 / 0.18);
  transition: opacity 150ms ease;
}

.group\/project-upload-trigger:hover .wk-project-chat-tooltip,
.group\/project-send:hover .wk-project-chat-tooltip {
  opacity: 1;
}

.wk-recording-indicator {
  position: relative;
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
}

.wk-recording-indicator::before {
  position: absolute;
  inset: -4px;
  border: 1px solid rgb(255 255 255 / 0.55);
  border-radius: 9999px;
  content: "";
  transition: opacity 0.14s ease;
  animation: wk-recording-pulse 1.2s ease-out infinite;
}

.wk-recording-indicator__stop {
  position: relative;
  font-size: 20px;
  line-height: 1;
  opacity: 0;
  transform: scale(0.84);
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.wk-recording-indicator__bars {
  position: absolute;
  inset: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transition: opacity 0.14s ease, transform 0.14s ease;
}

.wk-recording-indicator__bars span {
  display: block;
  width: 2px;
  border-radius: 9999px;
  background: #fff;
  animation: wk-recording-bar 0.72s ease-in-out infinite;
}

.wk-recording-indicator__bars span:nth-child(1) {
  height: 7px;
}

.wk-recording-indicator__bars span:nth-child(2) {
  height: 13px;
  animation-delay: 0.12s;
}

.wk-recording-indicator__bars span:nth-child(3) {
  height: 10px;
  animation-delay: 0.24s;
}

.wk-recording-indicator__bars span:nth-child(4) {
  height: 15px;
  animation-delay: 0.36s;
}

.group\/project-send:hover .wk-recording-indicator::before {
  opacity: 0;
  animation-play-state: paused;
}

.group\/project-send:hover .wk-recording-indicator__stop {
  opacity: 1;
  transform: scale(1);
}

.group\/project-send:hover .wk-recording-indicator__bars {
  opacity: 0;
  transform: scaleX(0.7);
}

.group\/project-send:hover .wk-recording-indicator__bars span {
  animation-play-state: paused;
}

@keyframes wk-recording-pulse {
  0% {
    opacity: 0.8;
    transform: scale(0.85);
  }

  100% {
    opacity: 0;
    transform: scale(1.18);
  }
}

@keyframes wk-recording-bar {
  0%,
  100% {
    transform: scaleY(0.55);
  }

  50% {
    transform: scaleY(1);
  }
}

:global(.wk-chat-upload-menu-popper.el-popper) {
  overflow: hidden;
  border: 1px solid var(--wk-border-subtle) !important;
  border-radius: 16px !important;
  background: var(--wk-bg-surface) !important;
  padding: 0 !important;
  box-shadow: 0 12px 36px rgb(var(--wk-shadow-color) / 0.28) !important;
}

:global(.wk-chat-upload-menu-popper.el-popper:not(.wk-chat-upload-submenu-popper)) {
  z-index: 3000 !important;
  overflow: visible !important;
}

:global(.wk-chat-upload-submenu-popper.el-popper) {
  z-index: 3100 !important;
}

:global(.wk-chat-upload-menu-popper .el-popper__arrow),
:global(.wk-chat-upload-menu-popper .el-popper__arrow::before) {
  display: none !important;
}

:global(.wk-chat-model-popper.el-popper) {
  z-index: 3000 !important;
  overflow: hidden;
  border: 1px solid var(--wk-border-subtle) !important;
  border-radius: 8px !important;
  background: var(--wk-bg-surface) !important;
  padding: 0 !important;
  box-shadow: 0 12px 36px rgb(var(--wk-shadow-color) / 0.28) !important;
}

:global(.wk-chat-model-popper .el-popper__arrow),
:global(.wk-chat-model-popper .el-popper__arrow::before) {
  display: none !important;
}
</style>
