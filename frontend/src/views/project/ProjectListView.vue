<template>
  <div class="flex h-full flex-col gap-4 overflow-hidden bg-slate-50/30 px-4 py-6 md:px-6">
    <header class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div class="min-w-0">
        <h2 class="text-[22px] font-bold text-slate-900">项目列表</h2>
      </div>
      <div
        class="wk-native-input-shell flex h-10 w-full min-w-0 items-center rounded-xl border border-[#dbe8f8] bg-[#f8fbff] px-1.5 transition-all focus-within:border-primary/60 sm:ml-auto sm:w-[360px]"
      >
        <span class="material-symbols-outlined shrink-0 pl-2 pr-1 text-[22px] leading-none text-[#8fa6c5]">search</span>
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索项目名称、描述或客户"
          class="min-w-0 flex-1 border-none bg-transparent px-1 text-[13px] leading-5 text-slate-900 outline-none placeholder:text-[#8fa6c5] focus:ring-0"
          @input="debouncedLoadProjects"
          @keydown.enter="handleSearch"
        />
      </div>
      <button
        type="button"
        class="inline-flex h-10 shrink-0 items-center gap-2 self-start rounded-xl bg-primary px-4 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 sm:self-auto"
        @click="openCreateDialog"
      >
        <span class="material-symbols-outlined text-[20px] leading-none">add</span>
        <span>新建项目</span>
      </button>
      <el-dropdown trigger="click" placement="bottom-end">
        <button
          type="button"
          class="inline-flex size-10 shrink-0 items-center justify-center rounded-xl border border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] text-[#284462] transition-all hover:border-[var(--wk-input-border-hover)] hover:text-primary sm:self-auto"
          title="更多项目设置"
          aria-label="更多项目设置"
        >
          <span class="material-symbols-outlined text-[22px] leading-none">more_horiz</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="showRolePermissionDialog = true">
              <span class="inline-flex items-center gap-2">
                <span class="material-symbols-outlined text-[16px]">admin_panel_settings</span>
                <span>项目角色权限设置</span>
              </span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <div class="flex min-w-0 flex-wrap items-center justify-between gap-3">
      <div class="flex min-w-0 flex-wrap items-center gap-2">
        <button
          v-for="filter in statusFilters"
          :key="filter.value"
          type="button"
          :class="[
            'inline-flex h-8 shrink-0 items-center gap-1.5 rounded-full border px-3 text-[13px] transition-all',
            statusFilter === filter.value
              ? 'border-primary/25 bg-primary/10 text-primary'
              : 'border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] text-[#284462] hover:border-[var(--wk-input-border-hover)] hover:text-primary'
          ]"
          @click="handleStatusFilter(filter.value)"
        >
          <span class="material-symbols-outlined text-[17px] leading-none">{{ filter.icon }}</span>
          {{ filter.label }}（{{ filter.count }}）
        </button>
      </div>

      <div class="flex shrink-0 items-center gap-2">
        <div class="inline-flex h-9 shrink-0 items-center rounded-lg border border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] p-1">
        <button
          type="button"
          class="inline-flex size-7 items-center justify-center rounded-md transition-all"
          :class="viewMode === 'card' ? 'bg-[var(--wk-bg-surface-hover)] text-primary' : 'text-[#8aa1c2] hover:text-primary'"
          title="卡片视图"
          @click="viewMode = 'card'"
        >
          <span class="material-symbols-outlined text-[20px] leading-none">grid_view</span>
        </button>
        <button
          type="button"
          class="inline-flex size-7 items-center justify-center rounded-md transition-all"
          :class="viewMode === 'table' ? 'bg-[var(--wk-bg-surface-hover)] text-primary' : 'text-[#8aa1c2] hover:text-primary'"
          title="表格视图"
          @click="viewMode = 'table'"
        >
          <span class="material-symbols-outlined text-[20px] leading-none">list</span>
        </button>
        </div>
      </div>
    </div>

    <main class="min-h-0 flex-1 overflow-y-auto" v-loading="loading">
      <div v-if="projects.length === 0" class="flex h-full min-h-[280px] flex-col items-center justify-center text-center text-slate-400">
        <span class="material-symbols-outlined text-5xl">folder_open</span>
        <p class="mt-4 text-sm">{{ keyword.trim() ? '没有匹配的项目' : '还没有项目，先创建一个项目开始管理任务吧。' }}</p>
      </div>

      <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 gap-4 pb-2 xl:grid-cols-2">
        <article
          v-for="project in projects"
          :key="project.projectId"
          class="group rounded-[8px] border border-[var(--wk-input-border)] bg-white p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-primary/30 hover:shadow-lg hover:shadow-slate-200/60"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 flex-1 cursor-pointer" @click="goToProject(project.projectId)">
              <div class="flex min-w-0 items-center gap-2">
                <h3 class="truncate text-base font-bold text-slate-900 transition-colors group-hover:text-primary">
                  {{ project.name }}
                </h3>
                <span class="inline-flex shrink-0 rounded-full px-2.5 py-1 text-xs font-bold" :class="projectStatusClass(project.status)">
                  {{ projectStatusLabel(project.status) }}
                </span>
              </div>
              <p class="mt-2 line-clamp-2 text-sm leading-6 text-slate-500">
                {{ project.description || '暂无项目描述' }}
              </p>
            </div>
            <button
              v-if="canEditProject(project)"
              type="button"
              class="flex size-9 shrink-0 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-50 hover:text-primary"
              title="编辑项目"
              @click="openEditDialog(project)"
            >
              <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
            </button>
          </div>

          <div class="mt-5 grid grid-cols-2 gap-x-5 gap-y-3 border-t border-slate-100 pt-4 text-sm">
            <div class="min-w-0">
              <p class="text-xs text-slate-400">负责人</p>
              <p class="mt-1 truncate font-semibold text-slate-900">{{ project.ownerName || '未指定' }}</p>
            </div>
            <div class="min-w-0">
              <p class="text-xs text-slate-400">关联客户</p>
              <p class="mt-1 truncate font-semibold text-slate-900">{{ project.customerName || '未关联' }}</p>
            </div>
            <div class="min-w-0">
              <p class="text-xs text-slate-400">最近更新</p>
              <p class="mt-1 truncate font-semibold text-slate-900">{{ formatDateTime(project.updateTime) }}</p>
            </div>
            <div class="min-w-0">
              <p class="text-xs text-slate-400">项目任务</p>
              <p class="mt-1 truncate font-semibold text-slate-900">
                {{ project.taskCount || 0 }} 个
                <span class="text-slate-400">/ 未完成 {{ project.incompleteTaskCount || 0 }} 个</span>
              </p>
            </div>
          </div>

          <div class="mt-5 flex items-center justify-end">
            <button
              type="button"
              class="inline-flex items-center gap-1 rounded-lg bg-slate-900 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-slate-700"
              @click="goToProject(project.projectId)"
            >
              进入项目
              <span class="material-symbols-outlined text-[16px] leading-none">arrow_forward</span>
            </button>
          </div>
        </article>
      </div>

      <div v-else class="wk-project-list-table-shell overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <div class="overflow-x-auto">
          <table class="wk-project-list-table min-w-[920px] text-sm">
            <thead class="text-left">
              <tr>
                <th class="px-5 py-3 font-semibold">项目名称</th>
                <th class="px-5 py-3 font-semibold">状态</th>
                <th class="px-5 py-3 font-semibold">负责人</th>
                <th class="px-5 py-3 font-semibold">关联客户</th>
                <th class="px-5 py-3 font-semibold">任务</th>
                <th class="px-5 py-3 font-semibold">更新时间</th>
                <th class="px-5 py-3 font-semibold">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="project in projects"
                :key="project.projectId"
                class="cursor-pointer transition-colors"
                @click="goToProject(project.projectId)"
              >
                <td class="max-w-[280px] px-5 py-4">
                  <p class="truncate font-semibold text-slate-900 transition-colors hover:text-primary hover:underline hover:decoration-primary underline-offset-2">{{ project.name }}</p>
                  <p v-if="project.description" class="mt-1 line-clamp-1 text-xs text-slate-400">{{ project.description }}</p>
                </td>
                <td class="px-5 py-4">
                  <span class="inline-flex rounded-full px-2.5 py-1 text-xs font-bold" :class="projectStatusClass(project.status)">
                    {{ projectStatusLabel(project.status) }}
                  </span>
                </td>
                <td class="px-5 py-4 text-slate-600">{{ project.ownerName || '未指定' }}</td>
                <td class="px-5 py-4 text-slate-600">{{ project.customerName || '未关联' }}</td>
                <td class="px-5 py-4 text-slate-600">
                  {{ project.taskCount || 0 }} / 未完成 {{ project.incompleteTaskCount || 0 }}
                </td>
                <td class="px-5 py-4 text-slate-600">{{ formatDateTime(project.updateTime) }}</td>
                <td class="px-5 py-4">
                  <div class="flex items-center justify-start gap-1" @click.stop>
                    <button
                      type="button"
                      class="flex size-8 items-center justify-center rounded-lg text-[var(--wk-text-muted)] transition-colors hover:bg-[var(--wk-bg-surface-hover)] hover:text-primary"
                      title="进入项目"
                      @click="goToProject(project.projectId)"
                    >
                      <span class="material-symbols-outlined text-[18px] leading-none">open_in_new</span>
                    </button>
                    <button
                      v-if="canEditProject(project)"
                      type="button"
                      class="flex size-8 items-center justify-center rounded-lg text-[var(--wk-text-muted)] transition-colors hover:bg-[var(--wk-bg-surface-hover)] hover:text-primary"
                      title="编辑项目"
                      @click="openEditDialog(project)"
                    >
                      <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>

    <div v-if="total > 0" class="flex shrink-0 items-center justify-between border-t border-slate-200 bg-slate-50/50 px-1 py-4 text-sm text-slate-500 md:px-0">
      <span>共 {{ total }} 个<span class="hidden md:inline">项目</span></span>
      <div class="flex items-center gap-1">
        <button class="flex size-8 items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50" :disabled="page <= 1" @click="changePage(page - 1)">
          <span class="material-symbols-outlined text-lg leading-none">chevron_left</span>
        </button>
        <button
          v-for="pageNum in visiblePages"
          :key="pageNum"
          class="flex size-8 items-center justify-center rounded border text-xs font-bold"
          :class="pageNum === page
            ? 'border-primary bg-primary text-white'
            : 'border-slate-200 bg-white text-slate-500 hover:bg-slate-50'"
          @click="changePage(pageNum)"
        >
          {{ pageNum }}
        </button>
        <span v-if="totalPages > 5" class="px-1 text-xs text-slate-400">...</span>
        <button class="flex size-8 items-center justify-center rounded border border-slate-200 bg-white text-slate-500 disabled:opacity-50" :disabled="page >= totalPages" @click="changePage(page + 1)">
          <span class="material-symbols-outlined text-lg leading-none">chevron_right</span>
        </button>
      </div>
    </div>

    <ProjectUpsertDialog
      v-model="showDialog"
      :editing-project="editingProject"
      @submit="handleSubmitProject"
    />
    <ProjectRolePermissionDialog v-model="showRolePermissionDialog" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { queryProjectPageList } from '@/api/project'
import ProjectRolePermissionDialog from '@/views/project/components/ProjectRolePermissionDialog.vue'
import ProjectUpsertDialog from '@/views/project/components/ProjectUpsertDialog.vue'
import { useProjectStore } from '@/stores/project'
import type {
  ProjectEntity,
  ProjectListStats,
  ProjectListStatusFilter,
  ProjectListViewMode
} from '@/types/project'
import {
  formatDateTime,
  projectStatusClass,
  projectStatusLabel
} from '@/utils/project'

const router = useRouter()
const projectStore = useProjectStore()

const projects = ref<ProjectEntity[]>([])
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<ProjectListStatusFilter>('all')
const viewMode = ref<ProjectListViewMode>('card')
const page = ref(1)
const limit = ref(10)
const total = ref(0)
const stats = ref<ProjectListStats>({
  all: 0,
  inProgress: 0,
  completed: 0
})
const showDialog = ref(false)
const showRolePermissionDialog = ref(false)
const editingProject = ref<ProjectEntity | null>(null)
let searchTimer: number | null = null

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / limit.value)))
const visiblePages = computed(() => {
  const totalPageCount = totalPages.value
  const current = page.value
  const pages: number[] = []
  const maxVisible = 5
  const start = Math.max(1, Math.min(current - 2, totalPageCount - maxVisible + 1))
  const end = Math.min(totalPageCount, start + maxVisible - 1)
  for (let i = start; i <= end; i += 1) {
    pages.push(i)
  }
  return pages
})

const statusFilters = computed(() => [
  { value: 'all' as const, label: '全部', icon: 'folder', count: stats.value.all },
  { value: 'IN_PROGRESS' as const, label: '进行中', icon: 'play_circle', count: stats.value.inProgress },
  { value: 'COMPLETED' as const, label: '已完成', icon: 'check_circle', count: stats.value.completed }
])

onMounted(() => {
  void loadProjects()
})

onBeforeUnmount(() => {
  if (searchTimer) window.clearTimeout(searchTimer)
})

async function loadProjects() {
  loading.value = true
  try {
    const result = await queryProjectPageList({
      keyword: keyword.value,
      status: statusFilter.value,
      page: page.value,
      limit: limit.value
    })
    projects.value = result.list
    total.value = Number(result.totalRow || 0)
    stats.value = normalizeStats(result.extraData)

    const lastPage = Math.max(1, Math.ceil(total.value / limit.value))
    if (page.value > lastPage) {
      page.value = lastPage
      await loadProjects()
    }
  } finally {
    loading.value = false
  }
}

function normalizeStats(extraData: unknown): ProjectListStats {
  const raw = (extraData && typeof extraData === 'object' ? extraData : {}) as Partial<Record<keyof ProjectListStats, unknown>>
  return {
    all: Number(raw.all ?? 0),
    inProgress: Number(raw.inProgress ?? 0),
    completed: Number(raw.completed ?? 0)
  }
}

function debouncedLoadProjects() {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    page.value = 1
    void loadProjects()
  }, 300)
}

function handleSearch() {
  if (searchTimer) window.clearTimeout(searchTimer)
  page.value = 1
  void loadProjects()
}

function handleStatusFilter(value: ProjectListStatusFilter) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  page.value = 1
  void loadProjects()
}

function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  void loadProjects()
}

function openCreateDialog() {
  editingProject.value = null
  showDialog.value = true
}

function openEditDialog(project: ProjectEntity) {
  editingProject.value = project
  showDialog.value = true
}

async function handleSubmitProject(payload: {
  name: string
  description?: string
  customerId?: string
  customerName?: string
  ownerId?: string
  ownerName?: string
  startDate?: string
  dueDate?: string
  status: ProjectEntity['status']
}) {
  if (editingProject.value) {
    await projectStore.updateProject({
      projectId: editingProject.value.projectId,
      ...payload
    })
    ElMessage.success('项目信息已更新')
  } else {
    await projectStore.createProject(payload)
    ElMessage.success('项目创建成功')
  }
  showDialog.value = false
  await loadProjects()
}

function canEditProject(project: ProjectEntity) {
  return Boolean(
    project.systemAdmin
    || project.currentUserPermissions?.includes('EDIT_PROJECT')
    || project.currentUserRole === 'OWNER'
    || project.currentUserRole === 'ADMIN'
  )
}

function goToProject(projectId: string) {
  router.push({ name: 'ProjectDetail', params: { id: projectId }, query: { view: 'board' } })
}
</script>

<style scoped>
.wk-project-list-table-shell {
  background: var(--wk-bg-surface);
}

.wk-project-list-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  color: var(--wk-text-secondary);
  background: var(--wk-bg-surface);
}

.wk-project-list-table th {
  border-bottom: 1px solid var(--wk-border-muted);
  background: var(--wk-bg-surface-subtle);
  color: var(--wk-text-muted);
  padding: 16px 20px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  white-space: nowrap;
}

.wk-project-list-table td {
  border-bottom: 1px solid var(--wk-border-subtle);
  padding: 16px 20px;
  color: var(--wk-text-secondary);
  vertical-align: middle;
}

.wk-project-list-table tbody tr:last-child td {
  border-bottom: 0;
}

.wk-project-list-table tbody tr:hover td {
  background: color-mix(in srgb, var(--wk-primary) 11%, var(--wk-bg-surface));
}
</style>
