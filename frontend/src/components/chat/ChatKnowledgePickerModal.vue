<template>
  <el-dialog
    v-model="dialogVisible"
    width="880px"
    align-center
    append-to-body
    destroy-on-close
    class="wk-chat-knowledge-picker-dialog"
    :show-close="false"
    @closed="onDialogClosed"
  >
    <template #header>
      <div class="wk-chat-knowledge-picker__header">
        <div class="min-w-0 flex-1">
          <h2 class="wk-chat-knowledge-picker__title">选择知识库内容</h2>
          <p class="wk-chat-knowledge-picker__subtitle">选择文件或分类作为 AI 回复的参考背景</p>
        </div>
        <button
          type="button"
          class="wk-chat-knowledge-picker__close"
          aria-label="关闭"
          @click="dialogVisible = false"
        >
          <span class="material-symbols-outlined wk-chat-knowledge-picker__close-icon">close</span>
        </button>
      </div>
    </template>

    <div class="wk-chat-knowledge-picker__body">
      <aside class="wk-chat-knowledge-picker__sidebar">
        <div class="wk-chat-knowledge-picker__sidebar-inner">
          <button
            v-for="cat in categories"
            :key="cat.id"
            type="button"
            class="wk-chat-knowledge-picker__cat"
            :class="{ 'wk-chat-knowledge-picker__cat--active': activeCategory === cat.id }"
            @click="setCategory(cat.id)"
          >
            <span class="material-symbols-outlined wk-chat-knowledge-picker__cat-icon">{{ cat.icon }}</span>
            <span class="wk-chat-knowledge-picker__cat-label">{{ cat.label }}</span>
            <span
              class="material-symbols-outlined wk-chat-knowledge-picker__cat-check"
              :class="{ fill: activeCategory === cat.id }"
              aria-hidden="true"
            >
              {{ activeCategory === cat.id ? 'check_box' : 'check_box_outline_blank' }}
            </span>
          </button>
        </div>
      </aside>

      <div class="wk-chat-knowledge-picker__main">
        <div class="wk-chat-knowledge-picker__search">
          <span class="material-symbols-outlined wk-chat-knowledge-picker__search-icon">search</span>
          <input
            v-model="keywordInput"
            type="search"
            class="wk-chat-knowledge-picker__search-input"
            placeholder="搜索文档..."
            autocomplete="off"
          />
        </div>

        <div v-loading="listLoading" class="wk-chat-knowledge-picker__list-wrap">
          <div v-if="!listLoading && rows.length === 0" class="wk-chat-knowledge-picker__empty">
            暂无文档
          </div>
          <button
            v-for="row in rows"
            :key="row.knowledgeId"
            type="button"
            class="wk-chat-knowledge-picker__row"
            :class="{ 'wk-chat-knowledge-picker__row--selected': selectedIds.has(row.knowledgeId) }"
            @click="toggleRow(row.knowledgeId)"
          >
            <div class="wk-chat-knowledge-picker__row-type-wrap">
              <span class="material-symbols-outlined wk-chat-knowledge-picker__row-type-icon">
                {{ fileTypeIcon(row) }}
              </span>
            </div>
            <div class="min-w-0 flex-1 text-left">
              <div class="wk-chat-knowledge-picker__row-title">{{ row.name }}</div>
              <div class="wk-chat-knowledge-picker__row-meta">{{ knowledgeRowTag(row) }}</div>
            </div>
            <span
              class="material-symbols-outlined wk-chat-knowledge-picker__row-radio"
              :class="selectedIds.has(row.knowledgeId) ? 'wk-chat-knowledge-picker__row-radio--on' : ''"
            >
              {{ selectedIds.has(row.knowledgeId) ? 'check_circle' : 'radio_button_unchecked' }}
            </span>
          </button>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="wk-chat-knowledge-picker__footer">
        <div class="wk-chat-knowledge-picker__count">
          已选择
          <span class="wk-chat-knowledge-picker__count-pill">{{ selectedIds.size }} 项</span>
        </div>
        <div class="wk-chat-knowledge-picker__footer-actions">
          <button type="button" class="wk-chat-knowledge-picker__btn-cancel" @click="dialogVisible = false">
            取消
          </button>
          <button
            type="button"
            class="wk-chat-knowledge-picker__btn-confirm"
            :disabled="selectedIds.size === 0"
            @click="handleConfirm"
          >
            确定
          </button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { queryKnowledgeList } from '@/api/knowledge'
import type { Knowledge, KnowledgeQueryBO } from '@/types/common'
import { MAX_CHAT_ATTACHMENT_COUNT } from '@/utils/chatAttachment'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    /** 聊天框还可附加的条目上限（与已有附件/知识库选择合计 ≤ MAX） */
    remainingSlots?: number
  }>(),
  { remainingSlots: MAX_CHAT_ATTACHMENT_COUNT }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [items: Knowledge[]]
}>()

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const categories = [
  { id: 'all', label: '全部', icon: 'grid_view' },
  { id: 'document', label: '产品文档', icon: 'description' },
  { id: 'proposal', label: '方案资料', icon: 'lightbulb' },
  { id: 'meeting', label: '会议记录', icon: 'calendar_month' },
  { id: 'contract', label: '合同文件', icon: 'assignment' }
] as const

type CategoryId = (typeof categories)[number]['id']

const activeCategory = ref<CategoryId>('all')
const keywordInput = ref('')
const debouncedKeyword = ref('')
let keywordTimer: ReturnType<typeof setTimeout> | null = null

const rows = ref<Knowledge[]>([])
const listLoading = ref(false)
const selectedIds = ref<Set<string>>(new Set())
const idToRow = ref<Map<string, Knowledge>>(new Map())

const selectCap = computed(() => Math.max(0, props.remainingSlots))

watch(keywordInput, () => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => {
    keywordTimer = null
    debouncedKeyword.value = keywordInput.value.trim()
  }, 320)
})

watch(debouncedKeyword, () => {
  if (dialogVisible.value) void fetchList()
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      keywordInput.value = ''
      debouncedKeyword.value = ''
      activeCategory.value = 'all'
      selectedIds.value = new Set()
      void fetchList()
    }
  }
)

watch(activeCategory, () => {
  if (dialogVisible.value) void fetchList()
})

function onDialogClosed() {
  selectedIds.value = new Set()
  rows.value = []
  idToRow.value = new Map()
}

function setCategory(id: CategoryId) {
  activeCategory.value = id
}

function toggleRow(id: string) {
  const next = new Set(selectedIds.value)
  if (next.has(id)) next.delete(id)
  else {
    if (next.size >= selectCap.value) {
      ElMessage.warning(
        selectCap.value <= 0
          ? '附件数量已达上限'
          : `最多还可选择 ${selectCap.value} 个文件（合计不超过 ${MAX_CHAT_ATTACHMENT_COUNT} 个）`
      )
      return
    }
    next.add(id)
  }
  selectedIds.value = next
}

function typeBadge(raw: string | undefined): string {
  const t = (raw || '').toLowerCase()
  const map: Record<string, string> = {
    document: 'PRODUCT',
    proposal: 'SOLUTION',
    meeting: 'MEETING',
    contract: 'CONTRACT',
    email: 'EMAIL',
    recording: 'RECORDING'
  }
  return map[t] || (t ? t.toUpperCase() : 'DOC')
}

function knowledgeRowTag(row: Knowledge): string {
  return typeBadge(row.type)
}

function fileTypeIcon(row: Knowledge): string {
  const mime = (row.mimeType || '').toLowerCase()
  if (mime.includes('pdf')) return 'picture_as_pdf'
  if (mime.startsWith('video/')) return 'smart_display'
  if (mime.startsWith('image/')) return 'image'
  return 'description'
}

async function fetchList() {
  listLoading.value = true
  try {
    const query = {
      page: 1,
      limit: 80,
      keyword: debouncedKeyword.value || undefined,
      type: activeCategory.value === 'all' ? undefined : activeCategory.value
    } as KnowledgeQueryBO
    const result = await queryKnowledgeList(query)
    const list = result.list || []
    rows.value = list
    const m = new Map<string, Knowledge>()
    for (const k of list) m.set(k.knowledgeId, k)
    for (const id of selectedIds.value) {
      if (!m.has(id)) {
        const prev = idToRow.value.get(id)
        if (prev) m.set(id, prev)
      }
    }
    idToRow.value = m
  } catch {
    rows.value = []
    idToRow.value = new Map()
  } finally {
    listLoading.value = false
  }
}

function handleConfirm() {
  const ids = [...selectedIds.value]
  if (ids.length === 0) return
  if (ids.length > selectCap.value) {
    ElMessage.warning(`还可再选 ${selectCap.value} 个文件（合计不超过 ${MAX_CHAT_ATTACHMENT_COUNT} 个）`)
    return
  }
  const items = ids.map(id => idToRow.value.get(id)).filter((r): r is Knowledge => Boolean(r))
  if (items.length === 0) {
    ElMessage.warning('未能找到所选文档')
    return
  }
  emit('confirm', items)
  dialogVisible.value = false
}
</script>

<style scoped>
.wk-chat-knowledge-picker__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-right: 2px;
}

.wk-chat-knowledge-picker__title {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #141414;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.wk-chat-knowledge-picker__subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  font-weight: 400;
  color: #8c8c8c;
  line-height: 1.5;
}

.wk-chat-knowledge-picker__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin: -4px -6px 0 0;
  padding: 0;
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
}

.wk-chat-knowledge-picker__close-icon {
  font-size: 22px;
  line-height: 1;
  color: #8c8c8c;
  font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}

.wk-chat-knowledge-picker__close:hover {
  background: #f5f5f5;
}

.wk-chat-knowledge-picker__close:hover .wk-chat-knowledge-picker__close-icon {
  color: #595959;
}

.wk-chat-knowledge-picker__body {
  display: flex;
  gap: 0;
  min-height: 432px;
  max-height: min(568px, 72vh);
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  border: 0px solid #ebebeb;
}

.wk-chat-knowledge-picker__sidebar {
  width: 28%;
  min-width: 196px;
  max-width: 252px;
  flex-shrink: 0;
  border-right: 1px solid #ebebeb;
  background: #FCFDFE;
  overflow-y: auto;
}

.wk-chat-knowledge-picker__sidebar-inner {
  padding: 12px 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.wk-chat-knowledge-picker__cat {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 12px;
  border: none;
  border-radius: 10px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font-size: 14px;
  color: #262626;
  transition:
    background 0.18s ease,
    color 0.18s ease;
}

.wk-chat-knowledge-picker__cat:hover {
  background: rgba(0, 0, 0, 0.04);
}

.wk-chat-knowledge-picker__cat--active {
  background: #e8f1ff;
  color: #1677ff;
}

.wk-chat-knowledge-picker__cat--active:hover {
  background: #dceaff;
}

.wk-chat-knowledge-picker__cat--active .wk-chat-knowledge-picker__cat-label {
  color: #1677ff;
  font-weight: 600;
}

.wk-chat-knowledge-picker__cat-icon {
  font-size: 22px;
  line-height: 1;
  color: #8c8c8c;
  flex-shrink: 0;
  font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}

.wk-chat-knowledge-picker__cat--active .wk-chat-knowledge-picker__cat-icon {
  color: #1677ff;
}

.wk-chat-knowledge-picker__cat-label {
  flex: 1;
  min-width: 0;
  line-height: 1.35;
}

.wk-chat-knowledge-picker__cat-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
  color: #a8a8a8;
  font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}

.wk-chat-knowledge-picker__cat-check.fill {
  color: #1677ff;
}

.wk-chat-knowledge-picker__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.wk-chat-knowledge-picker__search {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 16px 20px 12px;
  padding: 0 16px;
  height: 44px;
  border-radius: 16px;
  background: #F7F9FC;
  border: 1px solid transparent;
  transition:
    border-color 0.2s ease,
    background 0.2s ease;
}

.wk-chat-knowledge-picker__search-icon {
  font-size: 20px;
  line-height: 1;
  color: #bfbfbf;
  flex-shrink: 0;
  font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}

.wk-chat-knowledge-picker__search:focus-within {
  border-color: rgba(22, 119, 255, 0.35);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.08);
}

.wk-chat-knowledge-picker__search-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #262626;
  outline: none;
}

.wk-chat-knowledge-picker__search-input::placeholder {
  color: #bfbfbf;
}

.wk-chat-knowledge-picker__list-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 4px 16px 18px;
  scrollbar-gutter: stable;
}

.wk-chat-knowledge-picker__empty {
  padding: 52px 16px;
  text-align: center;
  font-size: 14px;
  color: #8c8c8c;
}

.wk-chat-knowledge-picker__row {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  margin-bottom: 10px;
  border: 1px solid #F1F5F9;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.wk-chat-knowledge-picker__row:last-child {
  margin-bottom: 0;
}

.wk-chat-knowledge-picker__row:hover {
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.wk-chat-knowledge-picker__row--selected {
  border-color: rgba(22, 119, 255, 0.45);
  background: #f8fbff;
  box-shadow: 0 2px 10px rgba(22, 119, 255, 0.12);
}

.wk-chat-knowledge-picker__row-type-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 10px;
  background: #F8FAFC;
}

.wk-chat-knowledge-picker__row-type-icon {
  font-size: 26px;
  line-height: 1;
  color: #6b7280;
  font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}

.wk-chat-knowledge-picker__row-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wk-chat-knowledge-picker__row-meta {
  margin-top: 5px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #94a3b8;
  text-transform: uppercase;
}

.wk-chat-knowledge-picker__row-radio {
  font-size: 24px;
  line-height: 1;
  flex-shrink: 0;
  color: #d9d9d9;
  font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}

.wk-chat-knowledge-picker__row-radio--on {
  color: #1677ff;
  font-variation-settings:
    'FILL' 1,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}

.wk-chat-knowledge-picker__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.wk-chat-knowledge-picker__count {
  font-size: 14px;
  color: #595959;
}

.wk-chat-knowledge-picker__count-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  margin-left: 8px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  background: #e8f1ff;
  color: #1677ff;
}

.wk-chat-knowledge-picker__footer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.wk-chat-knowledge-picker__btn-cancel {
  border: none;
  background: transparent;
  font-size: 14px;
  color: #8c8c8c;
  cursor: pointer;
  padding: 10px 8px;
  border-radius: 8px;
}

.wk-chat-knowledge-picker__btn-cancel:hover {
  color: #434343;
  background: #f5f5f5;
}

.wk-chat-knowledge-picker__btn-confirm {
  border: none;
  border-radius: 10px;
  padding: 10px 28px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: #1677ff;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.35);
  transition:
    background 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.wk-chat-knowledge-picker__btn-confirm:hover:not(:disabled) {
  background: #4096ff;
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.4);
}

.wk-chat-knowledge-picker__btn-confirm:active:not(:disabled) {
  transform: translateY(1px);
}

.wk-chat-knowledge-picker__btn-confirm:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}
</style>

<style>
.wk-chat-knowledge-picker-dialog.el-dialog {
  padding: 0;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow:
    0 24px 48px rgba(0, 0, 0, 0.12),
    0 8px 16px rgba(0, 0, 0, 0.06);
}

.wk-chat-knowledge-picker-dialog .el-dialog__header {
  padding: 24px 24px 16px;
  margin: 0;
  border-bottom: 1px solid #f0f0f0;
}

.wk-chat-knowledge-picker-dialog .el-dialog__body {
  padding: 16px 24px 8px;
}

.wk-chat-knowledge-picker-dialog .el-dialog__footer {
  padding: 16px 24px 22px;
  border-top: 1px solid #f0f0f0;
}
</style>
