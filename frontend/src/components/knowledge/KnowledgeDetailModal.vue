<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[299] bg-slate-900/60 backdrop-blur-sm"
        @click="close"
      />
    </Transition>

    <Transition name="scale-fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[300] flex items-center justify-center p-4"
      >
        <div
          :class="[
            'flex w-full overflow-hidden bg-white shadow-2xl',
            isMobile ? 'h-full rounded-none' : 'h-[90vh] max-w-5xl rounded-[2.5rem]'
          ]"
          @click.stop
        >
          <div
            v-if="isMobile"
            class="absolute left-0 right-0 top-0 z-10 flex h-12 items-center border-b border-slate-200 bg-white px-4"
          >
            <div class="flex flex-1 gap-1">
              <button
                v-for="tab in mobileTabs"
                :key="tab.key"
                :class="[
                  'rounded-full px-4 py-1.5 text-xs font-medium transition-colors',
                  mobileTab === tab.key
                    ? 'bg-primary text-white'
                    : 'text-slate-500 hover:bg-slate-100'
                ]"
                @click="mobileTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>
            <button
              class="flex size-8 items-center justify-center rounded-full text-slate-400 hover:bg-slate-100"
              @click="close"
            >
              <span class="material-symbols-outlined text-lg">close</span>
            </button>
          </div>

          <div
            v-show="!isMobile || mobileTab === 'document'"
            :class="[
              'flex flex-1 flex-col overflow-hidden border-r border-slate-100',
              isMobile ? 'pt-12' : ''
            ]"
          >
            <div class="flex shrink-0 items-center justify-between border-b border-slate-100 p-6 pb-4">
              <div class="flex min-w-0 items-center gap-3">
                <div
                  :class="[
                    'flex size-10 shrink-0 items-center justify-center rounded-xl',
                    getTypeIconBg(knowledge?.type)
                  ]"
                >
                  <span
                    class="material-symbols-outlined text-lg"
                    :style="{ color: getTypeIconColor(knowledge?.type) }"
                  >
                    {{ getTypeIcon(knowledge?.type) }}
                  </span>
                </div>
                <div class="min-w-0">
                  <h3 class="truncate text-base font-bold text-slate-900">
                    {{ knowledge?.name || '加载中...' }}
                  </h3>
                  <p class="text-xs font-medium tracking-wide text-slate-400">
                    更新于 {{ formatDate(knowledge?.createTime) }}
                    <span v-if="knowledge?.fileSize" class="ml-2">
                      {{ formatFileSize(knowledge.fileSize) }}
                    </span>
                  </p>
                </div>
              </div>
              <div class="flex shrink-0 items-center gap-2">
                <button
                  v-if="knowledge"
                  class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100"
                  title="下载文件"
                  @click="handleDownload"
                >
                  <span class="material-symbols-outlined text-lg">download</span>
                </button>
                <button
                  v-if="!isMobile"
                  class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100"
                  @click="close"
                >
                  <span class="material-symbols-outlined text-lg">close</span>
                </button>
              </div>
            </div>

            <div class="relative flex-1 overflow-hidden">
              <div
                v-if="loadingDetail"
                class="absolute inset-0 flex items-center justify-center bg-white"
              >
                <span class="material-symbols-outlined animate-spin text-3xl text-slate-300">
                  progress_activity
                </span>
              </div>

              <div
                v-else-if="showImagePreview"
                class="media-preview-shell media-preview-shell--image h-full overflow-auto bg-slate-950"
              >
                <img
                  :src="mediaPreviewUrl || undefined"
                  :alt="knowledge?.name"
                  class="media-preview-image"
                >
              </div>

              <div
                v-else-if="showAudioPreview"
                class="media-preview-shell flex h-full items-center justify-center bg-slate-950 p-6"
              >
                <audio
                  :src="mediaPreviewUrl || undefined"
                  controls
                  preload="metadata"
                  class="w-full max-w-3xl"
                  @error="handleMediaPreviewError"
                />
              </div>

              <div
                v-else-if="showVideoPreview"
                class="media-preview-shell flex h-full items-center justify-center bg-slate-950 p-6"
              >
                <video
                  :src="mediaPreviewUrl || undefined"
                  controls
                  preload="metadata"
                  class="media-preview-video"
                  @error="handleMediaPreviewError"
                />
              </div>

              <div
                v-else-if="showPdfPreview"
                class="pdf-preview-shell h-full overflow-auto bg-white"
              >
                <KnowledgePdfPreview
                  :blob="previewBlob"
                  class="pdf-preview-viewer min-h-full"
                  @rendered="handlePreviewRendered"
                  @error="handlePreviewError"
                />
              </div>

              <div
                v-else-if="showOfficePreview"
                :class="[
                  'office-preview-shell h-full overflow-auto',
                  'bg-slate-100',
                  previewKind === 'pptx' ? 'office-preview-shell--pptx' : ''
                ]"
              >
                <component
                  :is="activePreviewComponent"
                  :src="previewSource"
                  :class="[
                    'office-preview-viewer min-h-full',
                    previewKind === 'pptx' ? 'office-preview-viewer--pptx' : ''
                  ]"
                  @rendered="handlePreviewRendered"
                  @error="handlePreviewError"
                />
              </div>

              <div
                v-else-if="showDocHtmlPreview"
                class="doc-html-preview-shell h-full overflow-auto bg-white"
              >
                <iframe
                  ref="docIframeRef"
                  sandbox="allow-same-origin"
                  class="h-full w-full border-0"
                  title="Document preview"
                />
              </div>

              <div v-else class="h-full overflow-y-auto p-6">
                <div
                  v-if="previewNotice || previewFailed"
                  class="mb-4 flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3"
                >
                  <span class="material-symbols-outlined text-sm text-amber-500">info</span>
                  <span class="text-xs text-amber-700">
                    {{ previewNotice || '预览不可用，已显示可读取的文本内容。' }}
                  </span>
                </div>

                <div
                  v-if="displayedText"
                  class="whitespace-pre-wrap text-sm leading-relaxed text-slate-700"
                >
                  {{ displayedText }}
                </div>

                <div
                  v-else
                  class="flex h-full flex-col items-center justify-center text-slate-400"
                >
                  <span class="material-symbols-outlined mb-2 text-4xl">description</span>
                  <p class="text-sm">当前文件暂无可直接展示的内容</p>
                  <button
                    v-if="knowledge"
                    class="mt-3 rounded-xl bg-primary px-4 py-2 text-xs text-white transition-colors hover:bg-primary/90"
                    @click="handleDownload"
                  >
                    下载文件查看
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div
            v-show="!isMobile || mobileTab === 'ai'"
            :class="[
              'flex flex-col bg-slate-50',
              isMobile ? 'flex-1 pt-12' : 'w-80'
            ]"
          >
            <div class="shrink-0 border-b border-slate-200 bg-white p-5">
              <div class="mb-1 flex items-center gap-2 text-primary">
                <WkIcon name="ai" class="text-sm" />
                <span class="text-xs font-bold uppercase tracking-widest">AI 智能助手</span>
              </div>
              <p class="text-xs font-medium text-slate-400">
                {{ analysisStatusText }}
              </p>
              <button
                v-if="knowledge"
                class="mt-3 rounded-full border border-primary/15 bg-primary/5 px-3 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-primary/10 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="loadingAnalysis"
                @click="handleAnalyze"
              >
                {{ analysis ? '重新分析' : '开始分析' }}
              </button>
              <p v-if="false" class="text-xs font-medium text-slate-400">
                {{ loadingAnalysis ? '正在为您分析文档内容...' : (isStreaming ? '正在思考中...' : '分析完成，可向 AI 提问') }}
              </p>
            </div>

            <div ref="scrollContainerRef" class="flex-1 space-y-6 overflow-y-auto p-5">
              <div v-if="loadingAnalysis" class="space-y-6">
                <div v-for="i in 3" :key="i" class="space-y-3">
                  <div class="h-3 w-16 animate-pulse rounded bg-slate-200" />
                  <div class="space-y-2 rounded-2xl border border-slate-200 bg-white p-4">
                    <div class="h-3 animate-pulse rounded bg-slate-100" />
                    <div class="h-3 w-3/4 animate-pulse rounded bg-slate-100" />
                  </div>
                </div>
              </div>

              <template v-else>
                <div
                  v-if="analysisError"
                  class="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs leading-relaxed text-amber-700"
                >
                  {{ analysisError }}
                </div>

                <section v-if="analysis">
                  <h4 class="mb-3 text-xs font-bold uppercase tracking-widest text-slate-400">
                    核心提炼
                  </h4>
                  <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                    <p class="text-xs italic leading-relaxed text-slate-600">
                      "{{ analysis?.coreHighlights || knowledge?.summary || '暂无摘要' }}"
                    </p>
                  </div>
                </section>

                <section
                  v-else
                  class="rounded-2xl border border-dashed border-slate-200 bg-white p-5 text-xs leading-relaxed text-slate-500"
                >
                  <div class="mb-3 flex items-center gap-2 text-slate-700">
                    <span class="material-symbols-outlined text-base text-primary">auto_awesome</span>
                    <span class="font-semibold">尚未生成 AI 分析</span>
                  </div>
                  <p>
                    打开文档后默认不会自动调用分析接口。需要时点击上方“开始分析”按钮，再生成核心提炼、推荐话术和关联商机。
                  </p>
                </section>

                <section v-if="analysis?.talkingPoints?.length">
                  <h4 class="mb-3 text-xs font-bold uppercase tracking-widest text-slate-400">
                    推荐话术
                  </h4>
                  <div class="space-y-2">
                    <div
                      v-for="(point, idx) in analysis.talkingPoints"
                      :key="idx"
                      class="rounded-xl border border-primary/10 bg-primary/5 p-3"
                    >
                      <p class="text-xs leading-relaxed text-slate-700">"{{ point }}"</p>
                    </div>
                  </div>
                </section>

                <section v-if="analysis?.relatedEntities?.length">
                  <h4 class="mb-3 text-xs font-bold uppercase tracking-widest text-slate-400">
                    关联商机
                  </h4>
                  <div class="space-y-2">
                    <div
                      v-for="(entity, idx) in analysis.relatedEntities"
                      :key="idx"
                      class="flex items-center gap-2 rounded-lg border border-slate-200 bg-white p-2"
                    >
                      <span class="material-symbols-outlined text-sm text-amber-500">trending_up</span>
                      <span class="text-xs font-medium text-slate-700">{{ entity.name }}</span>
                    </div>
                  </div>
                </section>

                <section v-if="chatMessages.length > 0" class="border-t border-slate-200 pt-4">
                  <h4 class="mb-3 text-xs font-bold uppercase tracking-widest text-slate-400">
                    对话详情
                  </h4>
                  <div class="space-y-3">
                    <div
                      v-for="(msg, idx) in chatMessages"
                      :key="idx"
                      :class="['flex flex-col', msg.role === 'user' ? 'items-end' : 'items-start']"
                    >
                      <div
                        :class="[
                          'max-w-[90%] rounded-2xl p-3 text-xs leading-relaxed',
                          msg.role === 'user'
                            ? 'rounded-tr-none bg-primary text-white'
                            : 'rounded-tl-none border border-slate-200 bg-white text-slate-700'
                        ]"
                      >
                        <span class="whitespace-pre-wrap">{{ msg.content }}</span>
                        <span
                          v-if="msg.isStreaming"
                          class="ml-0.5 inline-block h-3 w-1 animate-pulse bg-slate-400"
                        />
                      </div>
                    </div>
                  </div>
                </section>

                <div v-if="isStreaming && chatMessages.length === 0" class="flex items-start">
                  <div class="rounded-2xl rounded-tl-none border border-slate-200 bg-white p-3">
                    <div class="flex gap-1">
                      <div class="size-1.5 animate-bounce rounded-full bg-slate-300" />
                      <div class="size-1.5 animate-bounce rounded-full bg-slate-300" style="animation-delay: 0.2s" />
                      <div class="size-1.5 animate-bounce rounded-full bg-slate-300" style="animation-delay: 0.4s" />
                    </div>
                  </div>
                </div>
              </template>
            </div>

            <div class="shrink-0 border-t border-slate-200 bg-white p-4">
              <div class="relative">
                <input
                  v-model="chatInput"
                  type="text"
                  placeholder="向 AI 提问文档细节..."
                  class="w-full rounded-xl border border-slate-200 bg-slate-50 py-3 pl-4 pr-10 text-xs outline-none transition-colors focus:border-primary focus:ring-1 focus:ring-primary"
                  :disabled="isStreaming"
                  @keydown.enter="handleSendQuestion"
                />
                <button
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-primary transition-opacity disabled:opacity-30"
                  :disabled="isStreaming || !chatInput.trim()"
                  @click="handleSendQuestion"
                >
                  <span class="material-symbols-outlined text-sm">send</span>
                </button>
              </div>
              <button
                v-if="chatMessages.length > 0"
                class="mt-2 w-full text-center text-xs text-slate-400 transition-colors hover:text-primary"
                @click="chatMessages = []"
              >
                清空对话历史
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import DOMPurify from 'dompurify'
import { useResponsive } from '@/composables/useResponsive'
import {
  aiAnalyzeKnowledge,
  askKnowledgeQuestion,
  downloadKnowledge,
  getKnowledgeDetail,
  getKnowledgeFileBlob,
  getKnowledgePreviewHtml,
  getKnowledgePreviewToken
} from '@/api/knowledge'
import type { Knowledge, KnowledgeAiAnalyzeVO } from '@/types/common'
import { formatFileSize as formatFileSizeBytes, resolveKnowledgeFileSizeBytes } from '@/utils/formatFileSize'

type PreviewKind =
  | 'image'
  | 'audio'
  | 'video'
  | 'doc'
  | 'docx'
  | 'excel'
  | 'pdf'
  | 'pptx'
  | 'text'
  | 'unsupported'
  | 'none'

const VueOfficeDocx = defineAsyncComponent(async () => {
  await import('@vue-office/docx/lib/v3/index.css')
  const module = await import('@vue-office/docx/lib/v3/index.js')
  return module.default
})

const VueOfficeExcel = defineAsyncComponent(async () => {
  await import('@vue-office/excel/lib/v3/index.css')
  const module = await import('@vue-office/excel/lib/v3/index.js')
  return module.default
})

const KnowledgePdfPreview = defineAsyncComponent(async () => {
  const module = await import('@/components/knowledge/KnowledgePdfPreview.vue')
  return module.default
})

const VueOfficePptx = defineAsyncComponent(async () => {
  const module = await import('@vue-office/pptx/lib/v3/index.js')
  return module.default
})

const props = defineProps<{
  modelValue: boolean
  knowledgeId: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'summary-updated': [payload: { knowledgeId: string; summary: string }]
}>()

const { isMobile } = useResponsive()

const mobileTabs = [
  { key: 'document' as const, label: '文档内容' },
  { key: 'ai' as const, label: 'AI 助手' }
]

const knowledge = ref<Knowledge | null>(null)
const analysis = ref<KnowledgeAiAnalyzeVO | null>(null)
const previewBlob = ref<Blob | null>(null)
const officePreviewSource = ref<Blob | ArrayBuffer | null>(null)
const mediaPreviewUrl = ref('')
const mediaPreviewObjectUrl = ref(false)
const previewKind = ref<PreviewKind>('none')
const previewNotice = ref('')
const previewText = ref('')
const previewFailed = ref(false)
const loadingDetail = ref(false)
const loadingAnalysis = ref(false)
const analysisError = ref('')
const mobileTab = ref<'document' | 'ai'>('document')
const chatMessages = ref<Array<{ role: string; content: string; isStreaming?: boolean }>>([])
const chatInput = ref('')
const isStreaming = ref(false)
const scrollContainerRef = ref<HTMLElement | null>(null)
const docHtmlContent = ref('')
const docIframeRef = ref<HTMLIFrameElement | null>(null)

const activePreviewComponent = computed(() => {
  switch (previewKind.value) {
    case 'docx':
      return VueOfficeDocx
    case 'excel':
      return VueOfficeExcel
    case 'pptx':
      return VueOfficePptx
    default:
      return null
  }
})

const previewSource = computed(() => officePreviewSource.value ?? undefined)

const showImagePreview = computed(() => {
  return previewKind.value === 'image' && Boolean(mediaPreviewUrl.value) && !previewFailed.value
})

const showAudioPreview = computed(() => {
  return previewKind.value === 'audio' && Boolean(mediaPreviewUrl.value) && !previewFailed.value
})

const showVideoPreview = computed(() => {
  return previewKind.value === 'video' && Boolean(mediaPreviewUrl.value) && !previewFailed.value
})

const showPdfPreview = computed(() => {
  return previewKind.value === 'pdf' && Boolean(previewBlob.value) && !previewFailed.value
})

const showOfficePreview = computed(() => {
  return Boolean(activePreviewComponent.value && previewSource.value && !previewFailed.value)
})

const showDocHtmlPreview = computed(() => {
  return previewKind.value === 'doc' && Boolean(docHtmlContent.value) && !previewFailed.value
})

const displayedText = computed(() => {
  return previewText.value || knowledge.value?.contentText || ''
})

const analysisStatusText = computed(() => {
  if (loadingAnalysis.value) {
    return '正在为您分析文档内容...'
  }
  if (isStreaming.value) {
    return '正在思考中...'
  }
  if (analysis.value) {
    return '分析完成，可向 AI 提问'
  }
  return '默认不自动分析，可点击按钮按需生成'
})

function resetPreviewState() {
  previewBlob.value = null
  officePreviewSource.value = null
  previewKind.value = 'none'
  previewNotice.value = ''
  previewText.value = ''
  previewFailed.value = false
  docHtmlContent.value = ''

  if (mediaPreviewUrl.value && mediaPreviewObjectUrl.value) {
    URL.revokeObjectURL(mediaPreviewUrl.value)
  }
  mediaPreviewUrl.value = ''
  mediaPreviewObjectUrl.value = false
}

function close() {
  emit('update:modelValue', false)
}

function getFileExtension(filename?: string): string {
  const normalized = filename?.trim().toLowerCase() || ''
  const lastDotIndex = normalized.lastIndexOf('.')
  return lastDotIndex >= 0 ? normalized.slice(lastDotIndex + 1) : ''
}

function normalizeContentType(contentType?: string): string {
  return (contentType || '').split(';', 1)[0]?.trim().toLowerCase() || ''
}

function isImageType(extension: string, mimeType: string): boolean {
  return mimeType.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp', 'svg', 'avif'].includes(extension)
}

function isAudioType(extension: string, mimeType: string): boolean {
  return mimeType.startsWith('audio/') || ['mp3', 'wav', 'm4a', 'aac', 'ogg', 'oga', 'opus', 'flac', 'weba'].includes(extension)
}

function isVideoType(extension: string, mimeType: string): boolean {
  return mimeType.startsWith('video/') || ['mp4', 'webm', 'mov', 'm4v', 'avi', 'mkv', 'ogv', '3gp'].includes(extension)
}

function resolvePreviewKind(filename?: string, mimeType?: string): PreviewKind {
  const extension = getFileExtension(filename)
  const normalizedType = normalizeContentType(mimeType)

  if (isImageType(extension, normalizedType)) {
    return 'image'
  }
  if (isAudioType(extension, normalizedType)) {
    return 'audio'
  }
  if (isVideoType(extension, normalizedType)) {
    return 'video'
  }
  if (extension === 'pdf' || normalizedType === 'application/pdf') {
    return 'pdf'
  }
  if (
    extension === 'docx' ||
    normalizedType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ) {
    return 'docx'
  }
  if (
    extension === 'xlsx' ||
    extension === 'xls' ||
    normalizedType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
    normalizedType === 'application/vnd.ms-excel'
  ) {
    return 'excel'
  }
  if (
    extension === 'pptx' ||
    normalizedType === 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
  ) {
    return 'pptx'
  }
  if (
    ['txt', 'md', 'markdown', 'json', 'xml', 'html', 'htm'].includes(extension) ||
    normalizedType.startsWith('text/')
  ) {
    return 'text'
  }
  if (extension === 'doc') {
    return 'doc'
  }
  if (extension === 'ppt') {
    return 'unsupported'
  }
  return 'unsupported'
}

async function tryReadJsonBlob(blob: Blob): Promise<{ msg?: string } | null> {
  const contentType = normalizeContentType(blob.type)
  if (contentType !== 'application/json') {
    return null
  }

  try {
    return JSON.parse(await blob.text()) as { msg?: string }
  } catch (error) {
    console.warn('Failed to parse JSON blob response:', error)
    return null
  }
}

function getUnsupportedNotice(filename?: string): string {
  const extension = getFileExtension(filename)
  if (extension === 'ppt') {
    return '当前旧版 Office 格式暂不支持纯前端预览，请下载后查看。'
  }
  if (extension === 'xls') {
    return '当前 Excel 文件渲染兼容性有限，如预览异常请下载后查看。'
  }
  return '当前文件类型暂不支持纯前端预览，请下载后查看。'
}

function handlePreviewRendered() {
  previewFailed.value = false
}

function handlePreviewError(error: unknown) {
  console.error('Knowledge preview render failed:', error)
  previewBlob.value = null
  officePreviewSource.value = null
  previewFailed.value = true
  previewNotice.value = previewNotice.value || '文件渲染失败，已切换为文本内容。'
}

function handleMediaPreviewError(error: Event) {
  console.error('Knowledge media preview failed:', error)
  previewFailed.value = true
  previewNotice.value = '浏览器无法播放该音视频格式，请下载后查看。'
}

onBeforeUnmount(() => {
  resetPreviewState()
})

watch(
  () => [props.modelValue, props.knowledgeId],
  async ([visible, id]) => {
    if (visible && id) {
      await loadDocument(id as string)
    } else if (!visible) {
      knowledge.value = null
      analysis.value = null
      analysisError.value = ''
      resetPreviewState()
      chatMessages.value = []
      chatInput.value = ''
      mobileTab.value = 'document'
    }
  },
  { immediate: true }
)

watch(
  [showDocHtmlPreview, docHtmlContent],
  ([show, html]) => {
    if (show && html) {
      nextTick(() => {
        const iframe = docIframeRef.value
        if (!iframe) return
        const sanitized = DOMPurify.sanitize(html, {
          WHOLE_DOCUMENT: true,
          ADD_TAGS: ['style', 'link'],
          ADD_ATTR: ['style', 'class', 'color', 'face', 'size'],
          ALLOW_DATA_ATTR: false
        })
        const doc = iframe.contentDocument
        if (doc) {
          doc.open()
          doc.write(sanitized)
          doc.close()
          const baseStyle = doc.createElement('style')
          baseStyle.textContent =
            'body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; ' +
            'padding: 24px 32px; line-height: 1.6; max-width: 100%; overflow-wrap: break-word; }' +
            'img { max-width: 100%; height: auto; }' +
            'table { border-collapse: collapse; width: 100%; margin: 1em 0; }' +
            'td, th { border: 1px solid #e2e8f0; padding: 6px 10px; }'
          if (doc.head) {
            doc.head.insertBefore(baseStyle, doc.head.firstChild)
          } else {
            doc.documentElement?.prepend(baseStyle)
          }
        }
      })
    }
  },
  { immediate: true }
)

async function loadDocument(id: string) {
  loadingDetail.value = true
  loadingAnalysis.value = false
  analysis.value = null
  analysisError.value = ''
  resetPreviewState()

  try {
    const detail = await getKnowledgeDetail(id)
    knowledge.value = detail
    analysis.value = detail.aiAnalyzeResult ?? null

    const kind = resolvePreviewKind(detail.name, detail.mimeType)
    previewKind.value = kind

    if (kind === 'unsupported') {
      previewFailed.value = true
      previewNotice.value = getUnsupportedNotice(detail.name)
      return
    }

    if (kind === 'doc') {
      try {
        const html = await getKnowledgePreviewHtml(id)
        docHtmlContent.value = html
      } catch {
        previewFailed.value = true
        previewNotice.value = '文档转换失败，已显示可读取的文本内容。'
      }
      return
    }

    if (kind === 'audio' || kind === 'video') {
      const previewToken = await getKnowledgePreviewToken(id)
      if (!previewToken.url) {
        throw new Error('Missing knowledge media preview URL')
      }
      mediaPreviewUrl.value = previewToken.url
      mediaPreviewObjectUrl.value = false
      return
    }

    const fileBlob = await getKnowledgeFileBlob(id)
    const errorPayload = await tryReadJsonBlob(fileBlob)
    if (errorPayload) {
      previewFailed.value = true
      previewNotice.value = errorPayload.msg || '文件内容读取失败，无法加载预览。'
      return
    }

    if (kind === 'text') {
      previewText.value = await fileBlob.text()
      if (!previewText.value && !detail.contentText) {
        previewFailed.value = true
        previewNotice.value = '当前文本文件暂无可展示的内容。'
      }
      return
    }

    if (kind === 'image') {
      mediaPreviewUrl.value = URL.createObjectURL(fileBlob)
      mediaPreviewObjectUrl.value = true
      return
    }

    previewBlob.value = kind === 'pdf' ? fileBlob : null
    officePreviewSource.value = kind === 'pdf'
      ? null
      : kind === 'pptx'
        ? await fileBlob.arrayBuffer()
        : fileBlob
  } catch (error) {
    console.error('Failed to load knowledge detail:', error)
    previewFailed.value = true
    previewNotice.value = '文件预览加载失败，请稍后重试。'
  } finally {
    loadingDetail.value = false
  }

}

async function handleAnalyze() {
  if (!props.knowledgeId || loadingAnalysis.value) return

  loadingAnalysis.value = true
  analysisError.value = ''

  try {
    analysis.value = await aiAnalyzeKnowledge(props.knowledgeId, Boolean(analysis.value))
    const summary = analysis.value?.coreHighlights?.trim()
    if (summary) {
      if (knowledge.value) {
        knowledge.value.summary = summary
      }
      emit('summary-updated', {
        knowledgeId: props.knowledgeId,
        summary
      })
    }
  } catch (error) {
    console.error('AI analysis failed:', error)
    analysisError.value = 'AI 分析失败，请稍后重试。'
  } finally {
    loadingAnalysis.value = false
  }
}

async function handleSendQuestion() {
  const question = chatInput.value.trim()
  if (!question || isStreaming.value || !props.knowledgeId) return

  chatInput.value = ''
  isStreaming.value = true

  chatMessages.value.push({ role: 'user', content: question })

  const assistantIdx = chatMessages.value.length
  chatMessages.value.push({ role: 'assistant', content: '', isStreaming: true })

  await nextTick()
  scrollToBottom()

  const history = chatMessages.value
    .slice(0, -1)
    .map(message => ({ role: message.role, content: message.content }))

  try {
    await askKnowledgeQuestion(
      props.knowledgeId,
      question,
      history,
      chunk => {
        if (chatMessages.value[assistantIdx]) {
          chatMessages.value[assistantIdx].content += chunk
          scrollToBottom()
        }
      },
      () => {
        if (chatMessages.value[assistantIdx]) {
          chatMessages.value[assistantIdx].isStreaming = false
        }
        isStreaming.value = false
      },
      error => {
        console.error('Ask document question failed:', error)
        if (chatMessages.value[assistantIdx]) {
          chatMessages.value[assistantIdx].content = '抱歉，处理您的请求时发生错误，请稍后重试。'
          chatMessages.value[assistantIdx].isStreaming = false
        }
        isStreaming.value = false
      }
    )
  } catch {
    isStreaming.value = false
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (scrollContainerRef.value) {
      scrollContainerRef.value.scrollTop = scrollContainerRef.value.scrollHeight
    }
  })
}

function handleDownload() {
  if (knowledge.value) {
    downloadKnowledge(knowledge.value.knowledgeId, knowledge.value.name)
  }
}

function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function formatFileSize(bytes?: number | string | null): string {
  if (resolveKnowledgeFileSizeBytes(bytes) <= 0) return ''
  return formatFileSizeBytes(bytes)
}

function getTypeIcon(type?: string): string {
  const icons: Record<string, string> = {
    meeting: 'groups',
    email: 'mail',
    recording: 'mic',
    document: 'description',
    proposal: 'slideshow',
    contract: 'gavel'
  }
  return icons[type || ''] || 'description'
}

function getTypeIconBg(type?: string): string {
  const backgrounds: Record<string, string> = {
    meeting: 'bg-blue-50',
    email: 'bg-green-50',
    recording: 'bg-purple-50',
    document: 'bg-slate-50',
    proposal: 'bg-orange-50',
    contract: 'bg-red-50'
  }
  return backgrounds[type || ''] || 'bg-slate-50'
}

function getTypeIconColor(type?: string): string {
  const colors: Record<string, string> = {
    meeting: '#3b82f6',
    email: '#22c55e',
    recording: '#a855f7',
    document: '#64748b',
    proposal: '#f97316',
    contract: '#ef4444'
  }
  return colors[type || ''] || '#64748b'
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.scale-fade-enter-active {
  transition: all 0.3s ease-out;
}

.scale-fade-leave-active {
  transition: all 0.2s ease-in;
}

.scale-fade-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.scale-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

.media-preview-shell {
  overscroll-behavior: contain;
}

.media-preview-shell--image {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.media-preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 24px;
}

.media-preview-video {
  max-width: 100%;
  max-height: 100%;
  border-radius: 16px;
  background: #020617;
}

.office-preview-shell {
  overscroll-behavior: contain;
}

.office-preview-shell--pptx {
  background: #edf3fb;
}

:deep(.office-preview-viewer) {
  min-height: 100%;
}

.office-preview-shell--pptx :deep(.office-preview-viewer--pptx) {
  min-height: auto;
}

.office-preview-shell--pptx :deep(.vue-office-pptx) {
  min-height: 100%;
  padding: 12px 0 16px;
}

.office-preview-shell--pptx :deep(.vue-office-pptx-main) {
  height: auto !important;
  min-height: 100%;
}

.office-preview-shell--pptx :deep(.pptx-preview-wrapper) {
  background: transparent !important;
  width: 100% !important;
  height: auto !important;
  overflow: visible !important;
  padding: 0 12px 16px;
  box-sizing: border-box;
}

.office-preview-shell--pptx :deep(.pptx-preview-slide-wrapper) {
  margin: 0 auto 16px !important;
  max-width: 100%;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
}

.office-preview-shell--pptx :deep(.pptx-preview-slide-wrapper:last-child) {
  margin-bottom: 0 !important;
}

.pdf-preview-shell {
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.doc-html-preview-shell {
  overscroll-behavior: contain;
}
</style>
