<template>
  <el-dialog
    v-model="dialogVisible"
    width="820px"
    append-to-body
    destroy-on-close
    class="chat-knowledge-picker"
    @closed="handleClosed"
  >
    <template #header>
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="text-lg font-semibold text-slate-900">选择知识库文件</h2>
          <p class="mt-1 text-sm text-slate-500">AI 将仅基于选中的文件范围优先回答。</p>
        </div>
        <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500">
          已选 {{ selectedIds.size }} / {{ selectCap }}
        </span>
      </div>
    </template>

    <div class="space-y-4">
      <div class="flex flex-col gap-3 md:flex-row md:items-center">
        <div class="relative flex-1">
          <span class="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-slate-400">
            search
          </span>
          <input
            v-model="keywordInput"
            type="search"
            class="h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-3 text-sm text-slate-900 outline-none transition focus:border-primary"
            placeholder="搜索知识库文件"
          />
        </div>
        <div class="flex gap-2 overflow-x-auto pb-1 md:pb-0">
          <button
            v-for="cat in categories"
            :key="cat.id"
            type="button"
            class="shrink-0 rounded-full border px-3 py-2 text-sm transition"
            :class="activeCategory === cat.id
              ? 'border-primary bg-primary/10 text-primary'
              : 'border-slate-200 bg-white text-slate-500 hover:border-slate-300 hover:text-slate-700'"
            @click="setCategory(cat.id)"
          >
            {{ cat.label }}
          </button>
        </div>
      </div>

      <div v-loading="loading" class="min-h-[320px] max-h-[54vh] overflow-y-auto rounded-2xl border border-slate-100 bg-slate-50/70 p-3">
        <div v-if="!loading && rows.length === 0" class="flex h-[300px] items-center justify-center text-sm text-slate-400">
          暂无可选文件
        </div>

        <button
          v-for="row in rows"
          :key="row.knowledgeId"
          type="button"
          class="mb-2 flex w-full items-center gap-3 rounded-xl border bg-white p-3 text-left transition last:mb-0 hover:border-primary/40"
          :class="selectedIds.has(row.knowledgeId)
            ? 'border-primary/40 shadow-sm shadow-primary/10'
            : 'border-slate-100'"
          @click="toggle(row)"
        >
          <span
            class="material-symbols-outlined flex size-10 shrink-0 items-center justify-center rounded-xl text-[22px]"
            :class="resolveIconClass(row)"
          >
            {{ resolveIcon(row) }}
          </span>
          <span class="min-w-0 flex-1">
            <span class="block truncate text-sm font-semibold text-slate-900">{{ row.name }}</span>
            <span class="mt-1 block truncate text-xs text-slate-400">
              {{ resolveMeta(row) }}
            </span>
          </span>
          <span
            class="material-symbols-outlined text-[22px]"
            :class="selectedIds.has(row.knowledgeId) ? 'text-primary' : 'text-slate-300'"
          >
            {{ selectedIds.has(row.knowledgeId) ? 'check_circle' : 'radio_button_unchecked' }}
          </span>
        </button>
      </div>
    </div>

    <template #footer>
      <div class="flex items-center justify-end gap-3">
        <button
          type="button"
          class="rounded-xl px-4 py-2 text-sm text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
          @click="dialogVisible = false"
        >
          取消
        </button>
        <button
          type="button"
          class="rounded-xl bg-primary px-5 py-2 text-sm font-medium text-white transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="selectedIds.size === 0"
          @click="confirm"
        >
          确定
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { queryKnowledgeList } from '@/api/knowledge'
import type { Knowledge, KnowledgeQueryBO } from '@/types/common'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    remainingSlots?: number
  }>(),
  { remainingSlots: 5 }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [items: Knowledge[]]
}>()

const categories = [
  { id: 'all', label: '全部' },
  { id: 'document', label: '文档' },
  { id: 'proposal', label: '方案' },
  { id: 'meeting', label: '会议' },
  { id: 'contract', label: '合同' },
  { id: 'email', label: '邮件' }
] as const

type CategoryId = (typeof categories)[number]['id']

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const activeCategory = ref<CategoryId>('all')
const keywordInput = ref('')
const debouncedKeyword = ref('')
const loading = ref(false)
const rows = ref<Knowledge[]>([])
const selectedIds = ref<Set<string>>(new Set())
const selectedRows = ref<Map<string, Knowledge>>(new Map())
let keywordTimer: ReturnType<typeof setTimeout> | null = null

const selectCap = computed(() => Math.max(0, props.remainingSlots))

watch(keywordInput, () => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    keywordTimer = null
    debouncedKeyword.value = keywordInput.value.trim()
  }, 260)
})

watch(debouncedKeyword, () => {
  if (dialogVisible.value) void fetchList()
})

watch(activeCategory, () => {
  if (dialogVisible.value) void fetchList()
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      activeCategory.value = 'all'
      keywordInput.value = ''
      debouncedKeyword.value = ''
      selectedIds.value = new Set()
      selectedRows.value = new Map()
      void fetchList()
    }
  }
)

function setCategory(id: CategoryId) {
  activeCategory.value = id
}

async function fetchList() {
  loading.value = true
  try {
    const query: KnowledgeQueryBO = {
      page: 1,
      limit: 80,
      keyword: debouncedKeyword.value || undefined,
      type: activeCategory.value === 'all' ? undefined : activeCategory.value
    }
    const result = await queryKnowledgeList(query)
    rows.value = result.list || []
  } finally {
    loading.value = false
  }
}

function toggle(row: Knowledge) {
  const nextIds = new Set(selectedIds.value)
  const nextRows = new Map(selectedRows.value)
  if (nextIds.has(row.knowledgeId)) {
    nextIds.delete(row.knowledgeId)
    nextRows.delete(row.knowledgeId)
  } else {
    if (nextIds.size >= selectCap.value) {
      ElMessage.warning(selectCap.value <= 0 ? '附件数量已达上限' : `最多还可选择 ${selectCap.value} 个文件`)
      return
    }
    nextIds.add(row.knowledgeId)
    nextRows.set(row.knowledgeId, row)
  }
  selectedIds.value = nextIds
  selectedRows.value = nextRows
}

function confirm() {
  const items = [...selectedIds.value]
    .map(id => selectedRows.value.get(id))
    .filter((item): item is Knowledge => Boolean(item))
  if (items.length === 0) {
    ElMessage.warning('请选择知识库文件')
    return
  }
  emit('confirm', items)
  dialogVisible.value = false
}

function handleClosed() {
  rows.value = []
  selectedIds.value = new Set()
  selectedRows.value = new Map()
}

function resolveIcon(row: Knowledge): string {
  const type = (row.type || '').toLowerCase()
  const mime = (row.mimeType || '').toLowerCase()
  if (mime.includes('pdf')) return 'picture_as_pdf'
  if (mime.startsWith('image/')) return 'image'
  if (type === 'meeting') return 'forum'
  if (type === 'contract') return 'assignment'
  if (type === 'email') return 'mail'
  return 'description'
}

function resolveIconClass(row: Knowledge): string {
  const mime = (row.mimeType || '').toLowerCase()
  if (mime.includes('pdf')) return 'bg-red-50 text-red-500'
  if (mime.startsWith('image/')) return 'bg-blue-50 text-blue-500'
  return 'bg-slate-100 text-slate-500'
}

function resolveMeta(row: Knowledge): string {
  const labels: string[] = []
  if (row.typeName) labels.push(row.typeName)
  else if (row.type) labels.push(row.type.toUpperCase())
  if (row.fileSizeFormatted) labels.push(row.fileSizeFormatted)
  if (row.weKnoraParseStatus && row.weKnoraParseStatus !== 'completed') {
    labels.push(`解析状态: ${row.weKnoraParseStatus}`)
  }
  return labels.join(' · ') || '知识库文件'
}
</script>
