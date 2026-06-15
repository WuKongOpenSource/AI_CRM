<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[160] bg-slate-900/55 backdrop-blur-sm"
        @click="close"
      />
    </Transition>

    <Transition name="scale-fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[161] flex items-center justify-center p-4"
      >
        <div
          :class="[
            'flex w-full flex-col overflow-hidden bg-white shadow-2xl',
            isMobile ? 'h-full rounded-none' : 'max-h-[90vh] max-w-4xl rounded-[2rem]'
          ]"
          @click.stop
        >
          <div class="flex shrink-0 items-center justify-between border-b border-slate-100 px-6 py-5 md:px-8">
            <div>
              <h2 class="text-2xl font-black tracking-tight text-slate-950">AI 话术 / SOP 生成</h2>
              <p class="mt-1 text-sm text-slate-400">
                {{ result ? result.subtitle : '从知识库资料中提炼更贴近客户场景的销售话术' }}
              </p>
            </div>
            <button
              type="button"
              class="flex size-10 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
              @click="close"
            >
              <span class="material-symbols-outlined text-2xl">close</span>
            </button>
          </div>

          <div v-if="!result" class="flex-1 overflow-y-auto px-6 py-6 md:px-8 md:py-7">
            <section class="space-y-4">
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p class="text-sm font-bold text-slate-700">1. 选择参考文档</p>
                  <p class="mt-1 text-xs text-slate-400">最多选择 4 份资料，作为本次话术的依据</p>
                </div>
                <div class="relative w-full sm:w-64">
                  <span class="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-300">
                    search
                  </span>
                  <input
                    v-model="documentKeyword"
                    type="text"
                    placeholder="搜索文档..."
                    class="w-full rounded-full border border-slate-200 bg-slate-50 py-2 pl-10 pr-4 text-sm text-slate-700 outline-none transition-colors placeholder:text-slate-400 focus:border-primary focus:bg-white"
                  />
                </div>
              </div>

              <div v-if="selectedDocuments.length" class="flex flex-wrap items-center gap-2 rounded-2xl bg-slate-50 p-3">
                <span class="text-xs font-semibold text-slate-500">已选 {{ selectedDocuments.length }}/4</span>
                <button
                  v-for="item in selectedDocuments"
                  :key="`selected-${item.knowledgeId}`"
                  type="button"
                  class="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-600 shadow-sm"
                  @click="toggleDocument(item)"
                >
                  <span class="truncate max-w-40">{{ item.name }}</span>
                  <span class="material-symbols-outlined text-sm text-slate-300">close</span>
                </button>
              </div>

              <div v-if="documentLoading" class="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div
                  v-for="index in 4"
                  :key="`skeleton-${index}`"
                  class="rounded-2xl border border-slate-200 bg-white p-5"
                >
                  <div class="mb-4 flex items-center justify-between">
                    <div class="h-9 w-9 animate-pulse rounded-xl bg-slate-100" />
                    <div class="h-4 w-20 animate-pulse rounded-full bg-slate-100" />
                  </div>
                  <div class="space-y-3">
                    <div class="h-5 animate-pulse rounded-full bg-slate-100" />
                    <div class="h-4 w-2/3 animate-pulse rounded-full bg-slate-100" />
                  </div>
                </div>
              </div>

              <div
                v-else-if="documents.length === 0"
                class="rounded-[1.5rem] border border-dashed border-slate-200 bg-slate-50 px-6 py-12 text-center"
              >
                <p class="text-sm font-medium text-slate-500">没有找到匹配的文档</p>
                <p class="mt-1 text-xs text-slate-400">换个关键词试试，或先上传知识库文档</p>
              </div>

              <div v-else class="grid grid-cols-1 gap-4 md:grid-cols-2">
                <button
                  v-for="item in documents"
                  :key="item.knowledgeId"
                  type="button"
                  :class="[
                    'group rounded-2xl border bg-white p-5 text-left transition-all',
                    isDocumentSelected(item)
                      ? 'border-primary bg-primary/5 shadow-[0_12px_30px_rgba(59,130,246,0.12)]'
                      : 'border-slate-200 hover:-translate-y-0.5 hover:border-primary/25 hover:shadow-md'
                  ]"
                  @click="toggleDocument(item)"
                >
                  <div class="mb-4 flex items-start justify-between gap-3">
                    <div
                      :class="[
                        'flex size-10 shrink-0 items-center justify-center rounded-xl',
                        getTypeIconBg(item.type)
                      ]"
                    >
                      <span class="material-symbols-outlined text-lg">{{ getTypeIcon(item.type) }}</span>
                    </div>
                    <div
                      :class="[
                        'flex size-6 shrink-0 items-center justify-center rounded-full border transition-colors',
                        isDocumentSelected(item)
                          ? 'border-primary bg-primary text-white'
                          : 'border-slate-200 bg-white text-transparent group-hover:text-slate-300'
                      ]"
                    >
                      <span class="material-symbols-outlined text-sm">check</span>
                    </div>
                  </div>

                  <h3 class="line-clamp-2 text-lg font-bold leading-snug text-slate-900">{{ item.name }}</h3>

                  <div class="mt-4 flex flex-wrap items-center gap-2 text-xs text-slate-400">
                    <span class="rounded-md bg-slate-50 px-2 py-1 font-semibold text-slate-500">
                      {{ getTypeLabel(item.type) }}
                    </span>
                    <span>{{ formatDate(item.createTime) }}</span>
                    <span v-if="item.customerName" class="truncate">· {{ item.customerName }}</span>
                  </div>
                </button>
              </div>
            </section>

            <section class="mt-8 space-y-4">
              <div>
                <p class="text-sm font-bold text-slate-700">2. 选择目标客户</p>
                <p class="mt-1 text-xs text-slate-400">支持搜索客户名称，生成更有针对性的销售话术</p>
              </div>

              <el-select
                v-model="selectedCustomerId"
                class="w-full"
                filterable
                remote
                clearable
                reserve-keyword
                placeholder="搜索或选择客户..."
                :remote-method="handleCustomerSearch"
                :loading="customerLoading"
              >
                <el-option
                  v-for="item in customerOptions"
                  :key="item.customerId"
                  :label="item.companyName"
                  :value="item.customerId"
                >
                  <div class="flex items-center justify-between gap-3">
                    <span class="truncate text-sm text-slate-700">{{ item.companyName }}</span>
                    <span class="shrink-0 text-xs text-slate-400">
                      {{ item.industry || item.stage || '客户' }}
                    </span>
                  </div>
                </el-option>
              </el-select>
            </section>
          </div>

          <div v-else class="flex-1 overflow-hidden bg-[linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)]">
            <div class="flex h-full flex-col overflow-hidden">
              <div class="shrink-0 px-6 pb-4 pt-6 md:px-8">
                <div class="flex flex-wrap items-start justify-between gap-4 rounded-[1.75rem] border border-[#d8e6ff] bg-white/80 p-5 shadow-[0_18px_45px_rgba(59,130,246,0.08)]">
                  <div class="flex min-w-0 items-start gap-4">
                    <div class="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                      <WkIcon name="ai" class="text-xl" />
                    </div>
                    <div class="min-w-0">
                      <h3 class="truncate text-2xl font-black tracking-tight text-slate-950">{{ result.title }}</h3>
                      <p class="mt-1 truncate text-sm text-slate-500">{{ result.subtitle }}</p>
                    </div>
                  </div>
                  <div class="flex items-center gap-2">
                    <button
                      type="button"
                      class="flex size-10 items-center justify-center rounded-xl bg-slate-50 text-slate-500 transition-colors hover:bg-slate-100 hover:text-primary"
                      title="分享"
                      @click="handleShare"
                    >
                      <span class="material-symbols-outlined text-lg">share</span>
                    </button>
                    <button
                      type="button"
                      class="flex size-10 items-center justify-center rounded-xl bg-slate-50 text-slate-500 transition-colors hover:bg-slate-100 hover:text-primary"
                      title="下载"
                      @click="handleDownloadResult"
                    >
                      <span class="material-symbols-outlined text-lg">download</span>
                    </button>
                  </div>
                </div>
              </div>

              <div class="min-h-0 flex-1 overflow-y-auto px-6 pb-6 md:px-8 md:pb-8">
                <section class="overflow-hidden rounded-[1.75rem] border border-slate-200 bg-white shadow-sm">
                  <div class="flex items-center justify-between border-b border-slate-100 px-5 py-4">
                    <div>
                      <p class="text-sm font-bold text-slate-700">话术详情</p>
                      <p class="mt-1 text-xs text-slate-400">可直接复制给销售或作为电话 / 面访 SOP 使用</p>
                    </div>
                    <button
                      type="button"
                      class="text-sm font-semibold text-primary transition-colors hover:text-primary/80"
                      @click="handleCopyResult"
                    >
                      复制全文
                    </button>
                  </div>

                  <div
                    class="wk-markdown max-h-[52vh] overflow-y-auto px-5 py-6 text-[15px] leading-8 text-slate-800"
                    v-html="renderedResult"
                  />
                </section>
              </div>
            </div>
          </div>

          <div
            v-if="!result"
            class="shrink-0 border-t border-slate-100 px-6 py-5 md:px-8"
          >
            <button
              type="button"
              class="flex w-full items-center justify-center gap-2 rounded-2xl bg-primary px-6 py-4 text-base font-bold text-white transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500"
              :disabled="generateDisabled"
              @click="handleGenerate"
            >
              <span
                v-if="generating"
                class="inline-block size-4 rounded-full border-2 border-white/70 border-t-transparent animate-spin"
              />
              <WkIcon v-else name="ai" class="text-base" />
              {{ generating ? '生成中...' : '生成针对性话术' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { queryKnowledgeList, streamKnowledgeTargetedScript } from '@/api/knowledge'
import { queryCustomerList } from '@/api/customer'
import type { Knowledge } from '@/types/common'
import type { CustomerListVO } from '@/types/customer'
import { renderMarkdown } from '@/utils/markdown'

interface TargetedScriptResult {
  title: string
  subtitle: string
  content: string
  customerId: string
  customerName: string
  knowledgeIds: string[]
  knowledgeNames: string[]
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { isMobile } = useResponsive()

const documentKeyword = ref('')
const documentLoading = ref(false)
const documents = ref<Knowledge[]>([])
const documentCache = ref<Record<string, Knowledge>>({})
const selectedDocumentIds = ref<string[]>([])

const selectedCustomerId = ref('')
const customerOptions = ref<CustomerListVO[]>([])
const customerLoading = ref(false)

const generating = ref(false)
const result = ref<TargetedScriptResult | null>(null)

let documentSearchTimer: ReturnType<typeof setTimeout> | null = null

const selectedDocuments = computed(() =>
  selectedDocumentIds.value
    .map(id => documentCache.value[id])
    .filter((item): item is Knowledge => Boolean(item))
)

const generateDisabled = computed(() => {
  return generating.value
    || selectedDocumentIds.value.length === 0
    || !selectedCustomerId.value
})

const renderedResult = computed(() => renderMarkdown(result.value?.content || ''))

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      await initializeDialog()
      return
    }
    resetState()
  }
)

watch(documentKeyword, () => {
  if (!props.modelValue) return
  if (documentSearchTimer) {
    clearTimeout(documentSearchTimer)
  }
  documentSearchTimer = setTimeout(() => {
    void fetchDocuments()
  }, 250)
})

onBeforeUnmount(() => {
  if (documentSearchTimer) {
    clearTimeout(documentSearchTimer)
  }
})

async function initializeDialog() {
  result.value = null
  generating.value = false
  selectedDocumentIds.value = []
  selectedCustomerId.value = ''
  customerOptions.value = []
  documentKeyword.value = ''
  await Promise.all([fetchDocuments(), handleCustomerSearch('')])
}

function resetState() {
  result.value = null
  generating.value = false
}

function close() {
  emit('update:modelValue', false)
}

async function fetchDocuments() {
  documentLoading.value = true
  try {
    const response = await queryKnowledgeList({
      page: 1,
      limit: 12,
      keyword: documentKeyword.value.trim() || undefined
    })
    documents.value = response.list
    const nextCache = { ...documentCache.value }
    response.list.forEach(item => {
      nextCache[item.knowledgeId] = item
    })
    documentCache.value = nextCache
  } finally {
    documentLoading.value = false
  }
}

function isDocumentSelected(item: Knowledge): boolean {
  return selectedDocumentIds.value.includes(item.knowledgeId)
}

function toggleDocument(item: Knowledge) {
  const currentIndex = selectedDocumentIds.value.indexOf(item.knowledgeId)
  if (currentIndex >= 0) {
    selectedDocumentIds.value.splice(currentIndex, 1)
    return
  }

  if (selectedDocumentIds.value.length >= 4) {
    ElMessage.warning('最多只能选择 4 份参考文档')
    return
  }

  selectedDocumentIds.value.push(item.knowledgeId)
  documentCache.value = {
    ...documentCache.value,
    [item.knowledgeId]: item
  }
}

async function handleCustomerSearch(keyword: string) {
  customerLoading.value = true
  try {
    const response = await queryCustomerList({
      keyword: keyword.trim() || undefined,
      page: 1,
      limit: 20
    })
    customerOptions.value = response.list
  } finally {
    customerLoading.value = false
  }
}

async function handleGenerate() {
  if (generateDisabled.value) {
    if (selectedDocumentIds.value.length === 0) {
      ElMessage.warning('请先选择参考文档')
    } else if (!selectedCustomerId.value) {
      ElMessage.warning('请选择目标客户')
    }
    return
  }

  generating.value = true
  try {
    result.value = buildStreamingResult()
    await streamKnowledgeTargetedScript({
      knowledgeIds: selectedDocumentIds.value,
      customerId: selectedCustomerId.value
    }, (chunk) => {
      if (result.value) {
        result.value.content += chunk
      }
    }, () => {
      if (result.value) {
        result.value.title = '针对性销售话术已生成'
      }
    }, () => {
      ElMessage.error('生成失败，请稍后重试')
    })
  } catch {
    if (result.value && !result.value.content.trim()) {
      result.value = null
    } else if (result.value) {
      result.value.title = '生成中断，请重试'
    }
  } finally {
    generating.value = false
  }
}

function buildStreamingResult(): TargetedScriptResult {
  const customer = customerOptions.value.find(item => item.customerId === selectedCustomerId.value)
  const docs = selectedDocuments.value
  const customerName = customer?.companyName || '目标客户'
  return {
    title: 'AI 正在生成话术...',
    subtitle: buildClientSubtitle(customerName, docs),
    content: '',
    customerId: selectedCustomerId.value,
    customerName,
    knowledgeIds: selectedDocumentIds.value,
    knowledgeNames: docs.map(item => item.name)
  }
}

function buildClientSubtitle(customerName: string, docs: Knowledge[]): string {
  const docNames = docs.map(item => item.name).filter(Boolean).slice(0, 2)
  let docLabel = docNames.length > 0 ? docNames.join('、') : '所选参考资料'
  if (docs.length > docNames.length) {
    docLabel += ` 等 ${docs.length} 份资料`
  }
  return `${customerName} · 基于 ${docLabel}`
}

async function handleCopyResult() {
  if (!result.value?.content) return
  await navigator.clipboard.writeText(result.value.content)
  ElMessage.success('已复制到剪贴板')
}

async function handleShare() {
  if (!result.value) return
  if (navigator.share) {
    try {
      await navigator.share({
        title: result.value.title,
        text: result.value.content
      })
      return
    } catch {
      // Ignore cancelled share and fallback to copy.
    }
  }
  await handleCopyResult()
}

function handleDownloadResult() {
  if (!result.value?.content) return
  const blob = new Blob([result.value.content], { type: 'text/markdown;charset=utf-8' })
  const link = document.createElement('a')
  const date = new Date().toISOString().slice(0, 10)
  link.href = URL.createObjectURL(blob)
  link.download = `${result.value.customerName || '目标客户'}-销售话术-${date}.md`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(link.href)
}

function getTypeIcon(type?: string): string {
  const icons: Record<string, string> = {
    meeting: 'groups',
    email: 'mail',
    recording: 'mic',
    document: 'description',
    proposal: 'slideshow',
    contract: 'description'
  }
  return icons[(type || '').toLowerCase()] || 'description'
}

function getTypeLabel(type?: string): string {
  const labels: Record<string, string> = {
    meeting: '会议记录',
    email: '邮件往来',
    recording: '录音文件',
    document: '产品文档',
    proposal: '方案资料',
    contract: '合同文件'
  }
  return labels[(type || '').toLowerCase()] || '文档'
}

function getTypeIconBg(type?: string): string {
  const colors: Record<string, string> = {
    meeting: 'bg-blue-50 text-blue-500',
    email: 'bg-emerald-50 text-emerald-500',
    recording: 'bg-purple-50 text-purple-500',
    document: 'bg-sky-50 text-sky-500',
    proposal: 'bg-amber-50 text-amber-500',
    contract: 'bg-slate-100 text-slate-500'
  }
  return colors[(type || '').toLowerCase()] || 'bg-slate-100 text-slate-500'
}

function formatDate(value?: string): string {
  if (!value) return '未知时间'
  return new Date(value).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.line-clamp-2 {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wk-markdown :deep(p) {
  margin: 0 0 0.75rem;
}

.wk-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.wk-markdown :deep(ul),
.wk-markdown :deep(ol) {
  margin: 0.5rem 0 0.75rem;
  padding-left: 1.25rem;
}

.wk-markdown :deep(ul) {
  list-style: disc;
}

.wk-markdown :deep(ol) {
  list-style: decimal;
}

.wk-markdown :deep(strong) {
  color: #0f172a;
  font-weight: 700;
}

.fade-enter-active,
.fade-leave-active,
.scale-fade-enter-active,
.scale-fade-leave-active {
  transition: all 0.18s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.scale-fade-enter-from,
.scale-fade-leave-to {
  opacity: 0;
  transform: scale(0.98);
}
</style>
