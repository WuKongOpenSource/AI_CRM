<template>
  <div class="flex h-full flex-col gap-5 px-4 py-5 md:px-6">
    <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
      <div class="min-w-0">
        <h2 class="text-[22px] font-bold text-slate-900">人力</h2>
        <p class="mt-1 text-[13px] text-slate-500">候选人、简历和面试安排</p>
      </div>
      <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div class="relative flex items-center">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">search</span>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索姓名、岗位、公司、手机号或技能"
            class="h-10 w-full rounded-lg border border-slate-200 bg-white pl-10 pr-4 text-sm outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20 sm:w-80"
            @input="debouncedLoad"
            @keydown.enter="loadCandidates"
          />
        </div>
        <select
          v-model="stage"
          class="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-600 outline-none transition-all focus:border-primary focus:ring-2 focus:ring-primary/20"
          @change="loadCandidates"
        >
          <option value="">全部阶段</option>
          <option v-for="option in stageOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <button
          type="button"
          class="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-bold text-white shadow-sm transition-colors hover:bg-primary/90"
          @click="openCreateDialog"
        >
          <span class="material-symbols-outlined text-[18px] leading-none">person_add</span>
          新增候选人
        </button>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-lg border border-slate-200 bg-white" v-loading="loading">
      <el-table
        :data="candidates"
        height="100%"
        row-key="candidateId"
        table-layout="fixed"
        empty-text="暂无候选人"
        @row-click="handleCandidateRowClick"
      >
        <el-table-column label="候选人" min-width="220">
          <template #default="{ row }">
            <div class="flex min-w-0 items-center gap-3">
              <div class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-100 text-sm font-bold text-slate-500">
                <img
                  v-if="row.avatarUrl"
                  :src="row.avatarUrl"
                  :alt="row.name || '候选人头像'"
                  class="size-full object-cover"
                />
                <span v-else>{{ row.name?.charAt(0) || '?' }}</span>
              </div>
              <div class="min-w-0">
                <button
                  type="button"
                  class="block max-w-full truncate text-left text-sm font-semibold text-slate-900 transition-colors hover:text-primary"
                  @click.stop="openDetail(row)"
                >
                  {{ row.name }}
                </button>
                <p class="truncate text-xs text-slate-400">{{ row.phone || row.email || '-' }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="招聘岗位" min-width="170">
          <template #default="{ row }">
            <span class="block truncate text-sm text-slate-700">{{ row.recruitmentJobName || row.appliedPosition || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="阶段" width="120">
          <template #default="{ row }">
            <span class="inline-flex rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600">
              {{ stageLabel(row.stage, row.stageName) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="当前状态" min-width="200">
          <template #default="{ row }">
            <span class="block truncate text-sm text-slate-600">
              {{ [row.currentCompany, row.currentPosition].filter(Boolean).join(' · ') || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="简历/面试" width="128">
          <template #default="{ row }">
            <span class="text-sm text-slate-500">{{ row.resumeCount || 0 }} 份 / {{ row.interviewCount || 0 }} 场</span>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">
            <span class="truncate text-sm text-slate-500">{{ row.ownerName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="116" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1" @click.stop>
              <button class="candidate-icon-btn" title="对话" @click="openChat(row)">
                <span class="material-symbols-outlined text-[18px]">forum</span>
              </button>
              <button class="candidate-icon-btn" title="编辑" @click="openEditDialog(row)">
                <span class="material-symbols-outlined text-[18px]">edit</span>
              </button>
              <button class="candidate-icon-btn text-rose-500 hover:bg-rose-50" title="删除" @click="handleDelete(row)">
                <span class="material-symbols-outlined text-[18px]">delete</span>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="total > 0" class="flex shrink-0 items-center justify-between border-t border-slate-200 bg-slate-50/70 px-5 py-3 text-sm text-slate-500">
        <span>共 {{ total }} 位候选人</span>
        <el-pagination
          v-model:current-page="page"
          :page-size="limit"
          :total="total"
          layout="prev, pager, next"
          small
          @current-change="loadCandidates"
        />
      </div>
    </div>

    <CandidateUpsertDialog
      v-model="dialogVisible"
      :editing-candidate="editingCandidate"
      @saved="loadCandidates"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteCandidate, queryCandidateList } from '@/api/candidate'
import { useChatStore } from '@/stores/chat'
import { useEnumStore } from '@/stores/enums'
import type { CandidateListVO } from '@/types/candidate'
import { CANDIDATE_STAGE_OPTIONS } from '@/types/candidate'
import CandidateUpsertDialog from './CandidateUpsertDialog.vue'

const router = useRouter()
const chatStore = useChatStore()
const enumStore = useEnumStore()
const candidates = ref<CandidateListVO[]>([])
const loading = ref(false)
const keyword = ref('')
const stage = ref('')
const page = ref(1)
const limit = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const editingCandidate = ref<CandidateListVO | null>(null)
const stageOptions = computed(() => enumStore.candidateStage.length ? enumStore.candidateStage : CANDIDATE_STAGE_OPTIONS)
const ROW_INTERACTIVE_SELECTOR = 'button,a,input,textarea,select,.el-button,.el-select,.el-dropdown,.candidate-icon-btn'
let debounceTimer: number | undefined

onMounted(async () => {
  await enumStore.ensureCandidateStage()
  await loadCandidates()
})

function debouncedLoad() {
  window.clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(() => {
    page.value = 1
    void loadCandidates()
  }, 250)
}

async function loadCandidates() {
  loading.value = true
  try {
    const result = await queryCandidateList({
      page: page.value,
      limit: limit.value,
      keyword: keyword.value.trim() || undefined,
      stage: stage.value || undefined
    })
    candidates.value = result.list || []
    total.value = result.totalRow || candidates.value.length
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingCandidate.value = null
  dialogVisible.value = true
}

function openEditDialog(candidate: CandidateListVO) {
  editingCandidate.value = candidate
  dialogVisible.value = true
}

async function openChat(candidate: CandidateListVO) {
  const sessionId = await chatStore.openCandidateChat(candidate)
  await router.push({ path: '/chat', query: { sessionId, candidateId: String(candidate.candidateId) } })
}

function openDetail(candidate: CandidateListVO) {
  router.push({ path: `/candidate/${candidate.candidateId}` })
}

function handleCandidateRowClick(row: CandidateListVO, _column: unknown, event: Event) {
  const target = event.target
  if (target instanceof HTMLElement && target.closest(ROW_INTERACTIVE_SELECTOR)) return
  openDetail(row)
}

async function handleDelete(candidate: CandidateListVO) {
  await ElMessageBox.confirm(`确定删除候选人「${candidate.name}」吗？`, '删除候选人', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await deleteCandidate(String(candidate.candidateId))
  ElMessage.success('候选人已删除')
  await loadCandidates()
}

function stageLabel(value?: string, fallback?: string) {
  return fallback || enumStore.candidateStageLabel(value) || CANDIDATE_STAGE_OPTIONS.find(option => option.value === value)?.label || value || '新候选人'
}
</script>

<style scoped>
.candidate-icon-btn {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #64748b;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.candidate-icon-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}
</style>
