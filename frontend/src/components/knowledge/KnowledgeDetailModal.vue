<template>
  <Teleport to="body">
    <!-- Backdrop -->
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-[150]"
        @click="close"
      />
    </Transition>

    <!-- Modal -->
    <Transition name="scale-fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[151] flex items-center justify-center p-4"
      >
        <div
          :class="[
            'w-full bg-white shadow-2xl overflow-hidden flex',
            isMobile ? 'h-full rounded-none' : 'max-w-5xl h-[90vh] rounded-[2.5rem]'
          ]"
          @click.stop
        >
          <!-- Mobile Tab Bar -->
          <div v-if="isMobile" class="absolute top-0 left-0 right-0 z-10 flex items-center bg-white border-b border-slate-200 px-4 h-12">
            <div class="flex gap-1 flex-1">
              <button
                v-for="tab in [{ key: 'document', label: '文档内容' }, { key: 'ai', label: 'AI 助手' }]"
                :key="tab.key"
                :class="[
                  'px-4 py-1.5 rounded-full text-xs font-medium transition-colors',
                  mobileTab === tab.key
                    ? 'bg-primary text-white'
                    : 'text-slate-500 hover:bg-slate-100'
                ]"
                @click="mobileTab = tab.key as 'document' | 'ai'"
              >
                {{ tab.label }}
              </button>
            </div>
            <button
              class="size-8 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400"
              @click="close"
            >
              <span class="material-symbols-outlined text-lg">close</span>
            </button>
          </div>

          <!-- Left Panel: Document Preview -->
          <div
            v-show="!isMobile || mobileTab === 'document'"
            :class="[
              'flex flex-col overflow-hidden border-r border-slate-100',
              isMobile ? 'flex-1 pt-12' : 'flex-1'
            ]"
          >
            <!-- Header -->
            <div class="flex items-center justify-between p-6 pb-4 border-b border-slate-100 shrink-0">
              <div class="flex items-center gap-3 min-w-0">
                <div
                  :class="[
                    'size-10 rounded-xl flex items-center justify-center shrink-0',
                    getTypeIconBg(knowledge?.type)
                  ]"
                >
                  <span class="material-symbols-outlined text-lg" :style="{ color: getTypeIconColor(knowledge?.type) }">
                    {{ getTypeIcon(knowledge?.type) }}
                  </span>
                </div>
                <div class="min-w-0">
                  <h3 class="text-base font-bold text-slate-900 truncate">{{ knowledge?.name || '加载中...' }}</h3>
                  <p class="text-[10px] text-slate-400 font-medium tracking-wide">
                    更新于 {{ formatDate(knowledge?.createTime) }}
                    <span v-if="knowledge?.fileSize" class="ml-2">{{ formatFileSize(knowledge.fileSize) }}</span>
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-2 shrink-0">
                <button
                  v-if="knowledge"
                  class="size-8 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 transition-colors"
                  title="下载文件"
                  @click="handleDownload"
                >
                  <span class="material-symbols-outlined text-lg">download</span>
                </button>
                <button
                  v-if="!isMobile"
                  class="size-8 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 transition-colors"
                  @click="close"
                >
                  <span class="material-symbols-outlined text-lg">close</span>
                </button>
              </div>
            </div>

            <!-- Document Content -->
            <div class="flex-1 overflow-hidden relative">
              <!-- Loading -->
              <div v-if="loadingDetail" class="absolute inset-0 flex items-center justify-center bg-white">
                <span class="material-symbols-outlined text-3xl text-slate-300 animate-spin">progress_activity</span>
              </div>

              <!-- kkFileView iframe preview -->
              <iframe
                v-else-if="previewUrl && !previewFailed"
                :src="previewUrl"
                class="w-full h-full border-0"
                @error="previewFailed = true"
                @load="onIframeLoad"
              />

              <!-- Fallback: plain text -->
              <div v-else class="h-full overflow-y-auto p-6">
                <div v-if="previewFailed" class="mb-4 p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-center gap-2">
                  <span class="material-symbols-outlined text-amber-500 text-sm">info</span>
                  <span class="text-xs text-amber-700">预览服务不可用，显示文本内容</span>
                </div>
                <div
                  v-if="knowledge?.contentText"
                  class="text-slate-700 leading-relaxed whitespace-pre-wrap text-sm"
                >{{ knowledge.contentText }}</div>
                <div v-else class="flex flex-col items-center justify-center h-full text-slate-400">
                  <span class="material-symbols-outlined text-4xl mb-2">description</span>
                  <p class="text-sm">暂无可预览的文本内容</p>
                  <button
                    v-if="knowledge"
                    class="mt-3 px-4 py-2 bg-primary text-white rounded-xl text-xs hover:bg-primary/90 transition-colors"
                    @click="handleDownload"
                  >
                    下载文件查看
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Right Panel: AI Assistant -->
          <div
            v-show="!isMobile || mobileTab === 'ai'"
            :class="[
              'bg-slate-50 flex flex-col',
              isMobile ? 'flex-1 pt-12' : 'w-80'
            ]"
          >
            <!-- AI Header -->
            <div class="p-5 border-b border-slate-200 bg-white shrink-0">
              <div class="flex items-center gap-2 text-primary mb-1">
                <span class="material-symbols-outlined text-sm">auto_awesome</span>
                <span class="text-xs font-bold uppercase tracking-widest">AI 智能助手</span>
              </div>
              <p class="text-[10px] text-slate-400 font-medium">
                {{ loadingAnalysis ? '正在为您分析文档内容...' : (isStreaming ? '正在思考中...' : '分析完成，可向AI提问') }}
              </p>
            </div>

            <!-- Scrollable Analysis + Chat -->
            <div ref="scrollContainerRef" class="flex-1 overflow-y-auto p-5 space-y-6">
              <!-- Loading skeleton -->
              <div v-if="loadingAnalysis" class="space-y-6">
                <div v-for="i in 3" :key="i" class="space-y-3">
                  <div class="h-3 w-16 bg-slate-200 rounded animate-pulse"></div>
                  <div class="p-4 bg-white border border-slate-200 rounded-2xl space-y-2">
                    <div class="h-3 bg-slate-100 rounded animate-pulse"></div>
                    <div class="h-3 bg-slate-100 rounded animate-pulse w-3/4"></div>
                  </div>
                </div>
              </div>

              <template v-else>
                <!-- Core Highlights -->
                <section>
                  <h4 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">核心提炼</h4>
                  <div class="p-4 bg-white border border-slate-200 rounded-2xl shadow-sm">
                    <p class="text-xs text-slate-600 leading-relaxed italic">
                      "{{ analysis?.coreHighlights || knowledge?.summary || '暂无摘要' }}"
                    </p>
                  </div>
                </section>

                <!-- Talking Points -->
                <section v-if="analysis?.talkingPoints?.length">
                  <h4 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">推荐话术</h4>
                  <div class="space-y-2">
                    <div
                      v-for="(point, idx) in analysis.talkingPoints"
                      :key="idx"
                      class="p-3 bg-primary/5 border border-primary/10 rounded-xl"
                    >
                      <p class="text-[11px] text-slate-700 leading-relaxed">"{{ point }}"</p>
                    </div>
                  </div>
                </section>

                <!-- Related Entities -->
                <section v-if="analysis?.relatedEntities?.length">
                  <h4 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">关联商机</h4>
                  <div class="space-y-2">
                    <div
                      v-for="(entity, idx) in analysis.relatedEntities"
                      :key="idx"
                      class="flex items-center gap-2 p-2 bg-white border border-slate-200 rounded-lg"
                    >
                      <span class="material-symbols-outlined text-amber-500 text-sm">trending_up</span>
                      <span class="text-[11px] font-medium text-slate-700">{{ entity.name }}</span>
                    </div>
                  </div>
                </section>

                <!-- Chat History -->
                <section v-if="chatMessages.length > 0" class="pt-4 border-t border-slate-200">
                  <h4 class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">对话详情</h4>
                  <div class="space-y-3">
                    <div
                      v-for="(msg, idx) in chatMessages"
                      :key="idx"
                      :class="['flex flex-col', msg.role === 'user' ? 'items-end' : 'items-start']"
                    >
                      <div
                        :class="[
                          'max-w-[90%] p-3 rounded-2xl text-xs leading-relaxed',
                          msg.role === 'user'
                            ? 'bg-primary text-white rounded-tr-none'
                            : 'bg-white border border-slate-200 text-slate-700 rounded-tl-none'
                        ]"
                      >
                        <span class="whitespace-pre-wrap">{{ msg.content }}</span>
                        <span v-if="msg.isStreaming" class="inline-block w-1 h-3 bg-slate-400 ml-0.5 animate-pulse"></span>
                      </div>
                    </div>
                  </div>
                </section>

                <!-- Streaming indicator -->
                <div v-if="isStreaming && chatMessages.length === 0" class="flex items-start">
                  <div class="bg-white border border-slate-200 p-3 rounded-2xl rounded-tl-none">
                    <div class="flex gap-1">
                      <div class="size-1.5 bg-slate-300 rounded-full animate-bounce"></div>
                      <div class="size-1.5 bg-slate-300 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
                      <div class="size-1.5 bg-slate-300 rounded-full animate-bounce" style="animation-delay: 0.4s"></div>
                    </div>
                  </div>
                </div>
              </template>
            </div>

            <!-- Chat Input -->
            <div class="p-4 border-t border-slate-200 bg-white shrink-0">
              <div class="relative">
                <input
                  v-model="chatInput"
                  type="text"
                  placeholder="向 AI 提问文档细节..."
                  class="w-full pl-4 pr-10 py-3 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-1 focus:ring-primary focus:border-primary outline-none transition-colors"
                  :disabled="isStreaming"
                  @keydown.enter="handleSendQuestion"
                />
                <button
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-primary disabled:opacity-30 transition-opacity"
                  :disabled="isStreaming || !chatInput.trim()"
                  @click="handleSendQuestion"
                >
                  <span class="material-symbols-outlined text-sm">send</span>
                </button>
              </div>
              <button
                v-if="chatMessages.length > 0"
                class="w-full mt-2 text-[10px] text-slate-400 hover:text-primary transition-colors text-center"
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
import { ref, watch, nextTick } from 'vue'
import { useResponsive } from '@/composables/useResponsive'
import { getKnowledgeDetail, getKnowledgeFileUrl, aiAnalyzeKnowledge, askKnowledgeQuestion, downloadKnowledge } from '@/api/knowledge'
import type { Knowledge, KnowledgeAiAnalyzeVO } from '@/types/common'

const props = defineProps<{
  modelValue: boolean
  knowledgeId: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { isMobile } = useResponsive()

// State
const knowledge = ref<Knowledge | null>(null)
const analysis = ref<KnowledgeAiAnalyzeVO | null>(null)
const previewUrl = ref('')
const previewFailed = ref(false)
const loadingDetail = ref(false)
const loadingAnalysis = ref(false)
const mobileTab = ref<'document' | 'ai'>('document')
const chatMessages = ref<Array<{ role: string; content: string; isStreaming?: boolean }>>([])
const chatInput = ref('')
const isStreaming = ref(false)
const scrollContainerRef = ref<HTMLElement | null>(null)

// kkFileView base URL (proxied through nginx)
function getKkfileviewUrl(): string {
  return `${window.location.origin}/kkfileview`
}

function close() {
  emit('update:modelValue', false)
}

// Watch for modal open
watch(
  () => [props.modelValue, props.knowledgeId],
  async ([visible, id]) => {
    if (visible && id) {
      await loadDocument(id as string)
    } else if (!visible) {
      // Reset state on close
      knowledge.value = null
      analysis.value = null
      previewUrl.value = ''
      previewFailed.value = false
      chatMessages.value = []
      chatInput.value = ''
      mobileTab.value = 'document'
    }
  },
  { immediate: true }
)

async function loadDocument(id: string) {
  loadingDetail.value = true
  loadingAnalysis.value = true
  previewFailed.value = false

  try {
    // Load detail and file URL in parallel
    const [detail, fileUrl] = await Promise.all([
      getKnowledgeDetail(id),
      getKnowledgeFileUrl(id).catch(() => null)
    ])

    knowledge.value = detail

    // Construct kkFileView preview URL
    if (fileUrl) {
      try {
        const encoded = encodeURIComponent(btoa(fileUrl))
        previewUrl.value = `${getKkfileviewUrl()}/onlinePreview?url=${encoded}`
      } catch {
        previewFailed.value = true
      }
    } else {
      previewFailed.value = true
    }
  } catch (error) {
    console.error('Failed to load knowledge detail:', error)
    previewFailed.value = true
  } finally {
    loadingDetail.value = false
  }

  // Load AI analysis (non-blocking)
  try {
    analysis.value = await aiAnalyzeKnowledge(id)
  } catch (error) {
    console.error('AI analysis failed:', error)
    // Fallback: use existing summary
    if (knowledge.value) {
      analysis.value = {
        coreHighlights: knowledge.value.summary || '暂无摘要',
        talkingPoints: [],
        relatedEntities: []
      }
    }
  } finally {
    loadingAnalysis.value = false
  }
}

function onIframeLoad(event: Event) {
  // Check if iframe loaded successfully by trying to detect error pages
  const iframe = event.target as HTMLIFrameElement
  try {
    // If kkFileView is not available, the iframe might show an error
    // We use a timeout to detect non-loading
    setTimeout(() => {
      if (!iframe.contentDocument && !iframe.contentWindow) {
        previewFailed.value = true
      }
    }, 10000)
  } catch {
    // Cross-origin - kkFileView loaded fine
  }
}

async function handleSendQuestion() {
  const question = chatInput.value.trim()
  if (!question || isStreaming.value || !props.knowledgeId) return

  chatInput.value = ''
  isStreaming.value = true

  // Add user message
  chatMessages.value.push({ role: 'user', content: question })

  // Add placeholder for assistant response
  const assistantIdx = chatMessages.value.length
  chatMessages.value.push({ role: 'assistant', content: '', isStreaming: true })

  // Scroll to bottom
  await nextTick()
  scrollToBottom()

  // Build history (exclude current streaming message)
  const history = chatMessages.value
    .slice(0, -1) // exclude current streaming placeholder
    .map(m => ({ role: m.role, content: m.content }))

  try {
    await askKnowledgeQuestion(
      props.knowledgeId,
      question,
      history,
      (chunk) => {
        // Append chunk to assistant message
        if (chatMessages.value[assistantIdx]) {
          chatMessages.value[assistantIdx].content += chunk
          scrollToBottom()
        }
      },
      () => {
        // Complete
        if (chatMessages.value[assistantIdx]) {
          chatMessages.value[assistantIdx].isStreaming = false
        }
        isStreaming.value = false
      },
      (error) => {
        console.error('Ask document question failed:', error)
        if (chatMessages.value[assistantIdx]) {
          chatMessages.value[assistantIdx].content = '抱歉，处理您的请求时发生错误。请稍后重试。'
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

// Helpers
function formatDate(dateStr?: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function formatFileSize(bytes?: number): string {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
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
  const bgs: Record<string, string> = {
    meeting: 'bg-blue-50',
    email: 'bg-green-50',
    recording: 'bg-purple-50',
    document: 'bg-slate-50',
    proposal: 'bg-orange-50',
    contract: 'bg-red-50'
  }
  return bgs[type || ''] || 'bg-slate-50'
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
</style>
