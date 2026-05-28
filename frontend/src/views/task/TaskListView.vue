<template>
  <div class="h-full flex bg-background-light overflow-hidden">
    <!-- Task List Section -->
    <div class="flex-1 overflow-y-auto px-4 py-6 md:px-6">
      <div
        class="space-y-6 transition-[max-width]"
        :class="effectiveTaskViewMode === 'list' ? 'w-full' : 'mx-auto max-w-4xl'"
      >
        <!-- Header -->
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 class="text-xl md:text-[22px] font-bold text-slate-900">AI 优先行动中心</h2>
            <p class="text-[13px] text-slate-500 mt-1">基于客户价值与成交概率，AI 已为您自动排序今日任务。</p>
          </div>
          <!-- Add task button -->
          <button
            class="flex items-center gap-1.5 self-start px-4 py-2 bg-primary text-white text-sm font-medium rounded-xl hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20 md:self-auto"
            @click="handleAddTask"
          >
            <span class="material-symbols-outlined wk-plus-button-icon">add</span>
            <span>{{ isMobile ? '新建' : '新建任务' }}</span>
          </button>
        </div>

        <!-- Status Filter Tabs and list controls -->
        <div class="wk-task-list-toolbar flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div class="flex gap-2 overflow-x-auto">
            <button
              v-for="tab in statusTabs"
              :key="tab.value"
              @click="handleStatusFilter(tab.value)"
              :class="[
                'border border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] px-4 py-1.5 text-xs font-bold rounded-full transition-all whitespace-nowrap',
                currentStatus === tab.value
                  ? 'text-primary ring-1 ring-primary/15'
                  : 'text-slate-500 hover:border-[var(--wk-input-border-hover)] hover:text-slate-700'
              ]"
            >
              {{ tab.label }} ({{ tab.count }})
            </button>
          </div>
          <div class="flex items-center justify-end gap-3 overflow-x-auto overflow-y-hidden">
            <!-- Segmented filter -->
            <div class="flex h-9 max-w-full items-center overflow-x-auto overflow-y-hidden rounded-xl border border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] p-1 md:max-w-none">
              <button
                @click="handleValueFilter('all')"
                :class="[
                  'h-7 shrink-0 whitespace-nowrap px-4 text-xs font-bold rounded-lg transition-all',
                  valueFilter === 'all' ? 'bg-[var(--wk-bg-surface-hover)] text-primary' : 'text-slate-500 hover:text-slate-700'
                ]"
              >
                全部任务
              </button>
              <button
                @click="handleValueFilter('high-impact')"
                :class="[
                  'h-7 shrink-0 whitespace-nowrap px-4 text-xs font-bold rounded-lg transition-all',
                  valueFilter === 'high-impact' ? 'bg-[var(--wk-bg-surface-hover)] text-primary' : 'text-slate-500 hover:text-slate-700'
                ]"
              >
                高价值优先
              </button>
            </div>
            <div v-if="!isMobile" class="flex h-9 shrink-0 items-center rounded-lg border border-[var(--wk-input-border)] bg-[var(--wk-input-bg)] p-1">
              <button
                type="button"
                class="flex size-7 items-center justify-center rounded-md transition-all"
                :class="taskViewMode === 'list' ? 'bg-[var(--wk-bg-surface-hover)] text-primary' : 'text-slate-400 hover:text-slate-600'"
                title="列表视图"
                @click="taskViewMode = 'list'"
              >
                <span class="material-symbols-outlined text-[20px]">list</span>
              </button>
              <button
                type="button"
                class="flex size-7 items-center justify-center rounded-md transition-all"
                :class="taskViewMode === 'card' ? 'bg-[var(--wk-bg-surface-hover)] text-primary' : 'text-slate-400 hover:text-slate-600'"
                title="卡片视图"
                @click="taskViewMode = 'card'"
              >
                <span class="material-symbols-outlined text-[20px]">grid_view</span>
              </button>
            </div>
          </div>
        </div>

        <div
          v-if="taskStore.highValueFallbackActive"
          class="flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
        >
          <span class="material-symbols-outlined text-base leading-none mt-0.5">info</span>
          <p>
            当前没有达到高价值阈值的任务，已按 AI 评分为您展示前 {{ taskStore.highValueFallbackCount }} 条高分任务，方便优先处理。
          </p>
        </div>

        <!-- Loading -->
        <div v-if="taskStore.loading" class="text-center py-16">
          <span class="material-symbols-outlined text-4xl text-slate-300 animate-spin">progress_activity</span>
        </div>

        <!-- Empty State -->
        <div v-else-if="displayedTasks.length === 0" class="text-center py-16 text-slate-400">
          <span class="material-symbols-outlined text-5xl">task_alt</span>
          <p class="mt-4 text-sm">暂无任务</p>
        </div>

        <!-- Task List View -->
        <div v-else-if="effectiveTaskViewMode === 'list'" class="wk-task-list-table overflow-hidden rounded-2xl border shadow-sm">
          <div class="overflow-x-auto">
            <div class="wk-task-list-table__header task-list-grid min-w-[1180px] px-5 py-3 text-sm font-bold">
              <div>AI评分/任务标题</div>
              <div>关联客户</div>
              <div>优先级</div>
              <div>状态</div>
              <div>截止日期</div>
              <div>类别</div>
              <div>负责人</div>
              <div class="text-right">操作</div>
            </div>

            <div
              v-for="task in displayedTasks"
              :key="`list-${task.taskId}`"
              role="button"
              tabindex="0"
              :class="[
                'group wk-task-list-table__row task-list-grid min-w-[1180px] cursor-pointer items-center px-5 py-4 transition-colors last:border-b-0',
                selectedTask?.taskId === task.taskId ? 'is-selected ring-1 ring-inset ring-primary/20' : '',
                task.status === 'COMPLETED' ? 'opacity-75' : ''
              ]"
              @click="handleViewDetail(task)"
              @keyup.enter="handleViewDetail(task)"
            >
              <div class="flex min-w-0 items-center gap-3">
                <div
                  :class="[
                    'flex h-7 min-w-9 shrink-0 items-center justify-center rounded-lg border px-2 text-sm',
                    task.status === 'COMPLETED'
                      ? 'border-emerald-100 bg-emerald-50 text-emerald-500'
                      : 'border-primary/10 bg-primary/5 text-primary'
                  ]"
                >
                  <span v-if="task.status === 'COMPLETED'" class="material-symbols-outlined text-[18px] leading-none">check</span>
                  <span v-else>{{ getAiScore(task) }}</span>
                </div>
                <div class="min-w-0">
                  <h3
                    :class="[
                      'truncate text-sm font-bold leading-5 transition-colors group-hover:text-primary',
                      task.status === 'COMPLETED' ? 'text-slate-400 line-through' : 'text-slate-900'
                    ]"
                    :title="task.title"
                  >
                    {{ task.title }}
                  </h3>
                  <p class="mt-1 truncate text-xs leading-5 text-slate-400" :title="getAiInsight(task)">
                    {{ getAiInsight(task) }}
                  </p>
                </div>
              </div>

              <div class="min-w-0 truncate text-sm text-slate-900" :title="task.customerName || '-'">
                {{ task.customerName || '-' }}
              </div>

              <div>
                <span
                  class="inline-flex h-6 items-center rounded-full px-2.5 text-sm"
                  :class="getPriorityBadgeClass(task.priority)"
                >
                  {{ getPriorityLabel(task.priority) }}
                </span>
              </div>

              <div>
                <span
                  class="inline-flex h-6 items-center rounded-md px-2.5 text-sm"
                  :class="getStatusBadgeClass(task)"
                >
                  {{ getStatusLabel(task) }}
                </span>
              </div>

              <div class="whitespace-nowrap text-sm text-slate-500">
                {{ task.dueDate ? formatDateTime(task.dueDate) : '-' }}
              </div>

              <div>
                <span
                  class="inline-flex h-6 max-w-full items-center rounded-md px-2.5 text-sm"
                  :class="getTaskTypeBadgeClass(task.taskType)"
                  :title="getTaskTypeLabel(task.taskType)"
                >
                  <span class="truncate">{{ getTaskTypeLabel(task.taskType) }}</span>
                </span>
              </div>

              <div class="min-w-0 truncate text-sm text-slate-700" :title="getTaskOwnerName(task)">
                {{ getTaskOwnerName(task) }}
              </div>

              <div class="flex items-center justify-end gap-2" @click.stop @keyup.stop>
                <button
                  v-if="task.status === 'PENDING'"
                  type="button"
                  class="inline-flex h-8 items-center rounded-lg bg-blue-50 px-3 text-sm text-blue-600 transition-colors hover:bg-blue-100"
                  @click="handleStartTask(task)"
                >
                  开始处理
                </button>
                <button
                  v-if="task.status !== 'COMPLETED'"
                  type="button"
                  class="inline-flex h-8 items-center rounded-lg bg-emerald-50 px-3 text-sm text-emerald-600 transition-colors hover:bg-emerald-100"
                  @click="handleToggleComplete(task)"
                >
                  标记完成
                </button>
                <span v-else class="inline-flex h-8 items-center px-2 text-sm text-slate-400">已完成</span>
              </div>
            </div>

            <div
              v-if="showPagination"
              class="wk-task-list-table__footer flex min-w-[1180px] items-center justify-end border-t px-5 py-4"
            >
              <span class="text-sm text-[var(--wk-text-muted)]">
                共 {{ taskStore.totalCount }} 个任务
                <span class="hidden md:inline">（第 {{ currentPage }} / {{ totalPages }} 页）</span>
              </span>
              <div class="flex items-center gap-1">
                <button
                  class="flex size-8 items-center justify-center rounded border border-[var(--wk-border-subtle)] bg-[var(--wk-bg-surface)] text-[var(--wk-text-muted)] hover:bg-[var(--wk-bg-surface-hover)] disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="currentPage <= 1"
                  title="上一页"
                  aria-label="上一页"
                  @click="handlePageChange(currentPage - 1)"
                >
                  <span class="material-symbols-outlined text-lg leading-none">chevron_left</span>
                </button>
                <button
                  v-for="p in visiblePages"
                  :key="p"
                  class="flex size-8 items-center justify-center rounded border text-xs font-bold"
                  :class="p === currentPage
                    ? 'border-primary bg-primary text-white'
                    : 'border-[var(--wk-border-subtle)] bg-[var(--wk-bg-surface)] text-[var(--wk-text-muted)] hover:bg-[var(--wk-bg-surface-hover)]'"
                  :aria-current="p === currentPage ? 'page' : undefined"
                  :aria-label="`第 ${p} 页`"
                  @click="handlePageChange(p)"
                >
                  {{ p }}
                </button>
                <button
                  class="flex size-8 items-center justify-center rounded border border-[var(--wk-border-subtle)] bg-[var(--wk-bg-surface)] text-[var(--wk-text-muted)] hover:bg-[var(--wk-bg-surface-hover)] disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="currentPage >= totalPages"
                  title="下一页"
                  aria-label="下一页"
                  @click="handlePageChange(currentPage + 1)"
                >
                  <span class="material-symbols-outlined text-lg leading-none">chevron_right</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Task Cards -->
        <div v-else class="space-y-4">
          <div
            v-for="task in displayedTasks"
            :key="task.taskId"
            @click="handleViewDetail(task)"
            :class="[
              'group bg-white border rounded-2xl p-5 cursor-pointer transition-all hover:shadow-xl hover:shadow-slate-200/50',
              selectedTask?.taskId === task.taskId ? 'border-primary ring-1 ring-primary/20' : 'border-slate-200',
              task.status === 'COMPLETED' ? 'opacity-75' : ''
            ]"
          >
            <div class="flex items-start gap-4 md:gap-5">
              <!-- AI Score -->
              <div class="flex flex-col items-center gap-1 shrink-0">
                <div
                  :class="[
                    'size-12 rounded-xl flex flex-col items-center justify-center border',
                    task.status === 'COMPLETED'
                      ? 'bg-slate-50 border-slate-100'
                      : 'bg-primary/5 border-primary/10'
                  ]"
                >
                  <template v-if="task.status === 'COMPLETED'">
                    <span class="material-symbols-outlined text-emerald-500">check_circle</span>
                  </template>
                  <template v-else>
                    <span class="text-lg font-black text-primary leading-none">{{ getAiScore(task) }}</span>
                    <span class="text-xs font-bold text-slate-400 uppercase tracking-tighter">AI 评分</span>
                  </template>
                </div>
                <div class="h-4 w-px bg-slate-100"></div>
                <div
                  :class="[
                    'size-2 rounded-full',
                    task.status === 'COMPLETED'
                      ? 'bg-slate-200'
                      : task.valuePriorityTier === 'HIGH' ? 'bg-red-500'
                      : task.valuePriorityTier === 'MEDIUM' ? 'bg-amber-500'
                      : 'bg-slate-300'
                  ]"
                ></div>
              </div>

              <!-- Content -->
              <div class="flex-1 min-w-0">
                <!-- Title + Status + Date -->
                <div class="flex items-center justify-between mb-1">
                  <div class="flex items-center gap-2 min-w-0">
                    <h3
                      :class="[
                        'font-bold truncate group-hover:text-primary transition-colors',
                        task.status === 'COMPLETED' ? 'text-slate-400 line-through' : 'text-slate-900'
                      ]"
                    >
                      {{ task.title }}
                    </h3>
                    <span
                      v-if="isOverdue(task)"
                      class="px-2 py-0.5 bg-red-50 text-red-600 text-xs font-bold rounded uppercase animate-pulse shrink-0"
                    >
                      已延期
                    </span>
                    <span
                      v-else-if="task.status === 'COMPLETED'"
                      class="px-2 py-0.5 bg-emerald-50 text-emerald-600 text-xs font-bold rounded uppercase shrink-0"
                    >
                      已完成
                    </span>
                    <span
                      v-else-if="task.status === 'IN_PROGRESS'"
                      class="px-2 py-0.5 bg-blue-50 text-blue-600 text-xs font-bold rounded uppercase shrink-0"
                    >
                      进行中
                    </span>
                    <span
                      v-else
                      class="px-2 py-0.5 bg-slate-100 text-slate-500 text-xs font-bold rounded uppercase shrink-0"
                    >
                      待处理
                    </span>
                  </div>
                  <span v-if="task.dueDate" class="text-xs font-bold text-slate-400 uppercase shrink-0 ml-2 hidden md:block">
                    {{ formatDate(task.dueDate) }}
                    <span class="text-slate-300 ml-1">({{ getRelativeTime(task.dueDate) }})</span>
                  </span>
                </div>

                <!-- Customer + Category + Owner -->
                <div class="flex items-center gap-2 mb-3 flex-wrap">
                  <span v-if="task.customerName" class="text-xs font-medium text-slate-500 truncate max-w-[150px]">{{ task.customerName }}</span>
                  <span v-if="task.customerName" class="size-1 rounded-full bg-slate-200"></span>
                  <span
                    v-if="task.generatedByAi"
                    class="text-xs font-bold px-2 py-0.5 rounded uppercase bg-blue-50 text-blue-600"
                  >
                    AI 生成
                  </span>
                  <span
                    v-else
                    class="text-xs font-bold px-2 py-0.5 rounded uppercase bg-slate-100 text-slate-600"
                  >
                    手动创建
                  </span>
                  <template v-if="task.assignedToName">
                    <span class="size-1 rounded-full bg-slate-200"></span>
                    <div class="flex items-center gap-1 text-xs text-slate-400">
                      <span class="material-symbols-outlined text-[14px]">person</span>
                      <span>{{ task.assignedToName }}</span>
                    </div>
                  </template>
                </div>

                <!-- AI Insight -->
                <div class="p-3 bg-primary/5 rounded-xl border border-primary/10 flex items-start gap-2">
                  <WkIcon name="ai" class="text-primary text-sm mt-0.5" />
                  <p class="text-xs text-slate-600 leading-relaxed italic">"{{ getAiInsight(task) }}"</p>
                </div>

                <!-- Action Buttons -->
                <div class="mt-3 flex items-center gap-2" @click.stop>
                  <button
                    v-if="task.status === 'PENDING'"
                    class="px-3 py-1 text-xs font-medium text-primary bg-primary/5 rounded-lg hover:bg-primary/10 transition-colors"
                    @click="handleStartTask(task)"
                  >
                    开始处理
                  </button>
                  <button
                    v-if="task.status !== 'COMPLETED'"
                    class="px-3 py-1 text-xs font-medium text-emerald-600 bg-emerald-50 rounded-lg hover:bg-emerald-100 transition-colors"
                    @click="handleToggleComplete(task)"
                  >
                    标记完成
                  </button>
                  <div class="flex-1"></div>
                  <el-dropdown trigger="click">
                    <button class="size-8 flex items-center justify-center rounded-lg text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors">
                      <span class="material-symbols-outlined text-lg">more_horiz</span>
                    </button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item @click="handleEdit(task)">编辑</el-dropdown-item>
                        <el-dropdown-item divided @click="handleDelete(task)">
                          <span class="text-red-500">删除</span>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div v-if="showPagination && effectiveTaskViewMode !== 'list'" class="mt-6 flex justify-center">
          <div class="flex items-center gap-2">
            <button
              class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :disabled="currentPage <= 1"
              @click="handlePageChange(currentPage - 1)"
            >
              <span class="material-symbols-outlined text-lg">chevron_left</span>
            </button>
            <button
              v-for="p in visiblePages"
              :key="p"
              @click="handlePageChange(p)"
              :class="[
                'size-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors',
                p === currentPage
                  ? 'bg-primary text-white'
                  : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
              ]"
            >
              {{ p }}
            </button>
            <button
              class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :disabled="currentPage >= totalPages"
              @click="handlePageChange(currentPage + 1)"
            >
              <span class="material-symbols-outlined text-lg">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <TaskDetailDrawer
      v-model="showTaskDetail"
      :task="detailTask"
      :is-mobile="isMobile"
      @edit="handleEditFromDetail"
      @mutated="handleTaskDetailMutated"
    />

    <TaskEditDialog
      v-model="showAddDialog"
      :editing-task="editingTask"
      @saved="handleTaskSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTaskStore } from '@/stores/task'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Task, TaskPriority, TaskStatus } from '@/types/common'
import { normalizeTaskPriority } from '@/utils/taskPriority'
import TaskDetailDrawer from './components/TaskDetailDrawer.vue'
import TaskEditDialog from './components/TaskEditDialog.vue'

const taskStore = useTaskStore()
const route = useRoute()
const router = useRouter()
const { isMobile } = useResponsive()

type TaskStatusFilter = 'all' | TaskStatus | 'OVERDUE'

const currentStatus = ref<TaskStatusFilter>('all')
const valueFilter = ref<'all' | 'high-impact'>('all')
const taskViewMode = ref<'list' | 'card'>('list')
const showAddDialog = ref(false)
const editingTask = ref<Task | null>(null)
const showTaskDetail = ref(false)
const detailTask = ref<Task | null>(null)
const selectedTask = ref<Task | null>(null)

const effectiveTaskViewMode = computed(() => taskViewMode.value)

// Computed properties
const statusTabs = computed<Array<{ value: TaskStatusFilter; label: string; count: number }>>(() => {
  const counts = taskStore.statusCounts
  return [
    { value: 'all', label: '全部', count: counts.all },
    { value: 'PENDING', label: '待处理', count: counts.PENDING },
    { value: 'IN_PROGRESS', label: '进行中', count: counts.IN_PROGRESS },
    { value: 'COMPLETED', label: '已完成', count: counts.COMPLETED },
    // { value: 'OVERDUE', label: '已延期', count: counts.OVERDUE }
  ]
})

const displayedTasks = computed(() => taskStore.taskList)

const currentPage = computed(() => taskStore.queryParams.page || 1)
const pageSize = computed(() => taskStore.queryParams.limit || 10)
const totalPages = computed(() => Math.ceil(taskStore.totalCount / pageSize.value))
const showPagination = computed(() => taskStore.totalCount > 0)

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const pages: number[] = []
  let start = Math.max(1, current - 2)
  const end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

watch(showTaskDetail, open => {
  if (open) return
  detailTask.value = null
  if (!isMobile.value) {
    selectedTask.value = null
  }
})

watch(showAddDialog, visible => {
  if (!visible) editingTask.value = null
})

onMounted(() => {
  void initializeTaskList()
})

watch(
  () => route.query.openTaskId,
  (openTaskId, previousTaskId) => {
    if (typeof openTaskId !== 'string' || !openTaskId || openTaskId === previousTaskId) return
    void openTaskFromRouteQuery(openTaskId)
  }
)

async function initializeTaskList() {
  await taskStore.fetchTaskList(true)
  const openTaskId = typeof route.query.openTaskId === 'string' ? route.query.openTaskId : ''
  if (openTaskId) {
    await openTaskFromRouteQuery(openTaskId)
  }
}

async function openTaskFromRouteQuery(taskId: string) {
  const previousQuery = { ...taskStore.queryParams }

  Object.assign(taskStore.queryParams, {
    ...previousQuery,
    taskId,
    page: 1,
    limit: 1,
    sortMode: 'default',
    highValueOnly: false
  })
  await taskStore.fetchTaskList(false)

  const matchedTask = taskStore.taskList.find(task => task.taskId === taskId) || null
  if (matchedTask) {
    detailTask.value = matchedTask
    showTaskDetail.value = true
    if (!isMobile.value) {
      selectedTask.value = matchedTask
    }
  }

  const nextQuery = { ...route.query }
  delete nextQuery.openTaskId
  await router.replace({ path: route.path, query: nextQuery })

  Object.assign(taskStore.queryParams, {
    ...previousQuery,
    taskId: undefined
  })
  await taskStore.fetchTaskList(false)

  if (matchedTask) {
    const refreshed = taskStore.taskList.find(task => task.taskId === taskId) || matchedTask
    detailTask.value = refreshed
    if (!isMobile.value) {
      selectedTask.value = refreshed
    }
  }
}



async function handleValueFilter(filter: 'all' | 'high-impact') {
  valueFilter.value = filter
  taskStore.queryParams.taskId = undefined
  taskStore.queryParams.page = 1
  taskStore.queryParams.sortMode = filter === 'high-impact' ? 'value' : 'default'
  taskStore.queryParams.highValueOnly = filter === 'high-impact'
  await taskStore.fetchTaskList(false)
}

function handleStatusFilter(status: TaskStatusFilter) {
  currentStatus.value = status
  taskStore.queryParams.taskId = undefined
  if (status === 'OVERDUE') {
    taskStore.queryParams.status = undefined
    taskStore.queryParams.filter = 'overdue'
  } else {
    taskStore.queryParams.status = status === 'all' ? undefined : status
    taskStore.queryParams.filter = 'all'
  }
  taskStore.queryParams.page = 1
  taskStore.fetchTaskList(false)
}

function handlePageChange(page: number) {
  if (page < 1 || page > totalPages.value) return
  if (taskStore.queryParams.page === page) return
  taskStore.queryParams.taskId = undefined
  taskStore.queryParams.page = page
  taskStore.fetchTaskList(false)
}

async function handleToggleComplete(task: Task) {
  const newStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  await taskStore.changeTaskStatus(task.taskId, newStatus)
  await taskStore.fetchTaskList(false)
}

async function handleStartTask(task: Task) {
  if (task.status === 'PENDING') {
    await taskStore.changeTaskStatus(task.taskId, 'IN_PROGRESS')
    await taskStore.fetchTaskList(false)
    ElMessage.success('任务已开始处理')
  }
}

function handleViewDetail(task: Task) {
  detailTask.value = task
  showTaskDetail.value = true
  if (isMobile.value) {
    // 手机端点击详情时不走 selectedTask 选中逻辑，改用侧滑抽屉展示详情
    // selectedTask.value = task
    return
  }
  selectedTask.value = task
}

function handleEditFromDetail(task: Task) {
  handleEdit(task)
}

async function handleTaskDetailMutated() {
  await taskStore.fetchTaskList(false)
  const id = detailTask.value?.taskId
  if (id) {
    const refreshed = taskStore.taskList.find(t => t.taskId === id) ?? null
    detailTask.value = refreshed
    if (!isMobile.value) {
      selectedTask.value = refreshed
    }
  }
}

function handleAddTask() {
  editingTask.value = null
  showAddDialog.value = true
}

function handleEdit(task: Task) {
  editingTask.value = task
  showAddDialog.value = true
}

function handleTaskSaved() {
  editingTask.value = null
  const selectedTaskId = detailTask.value?.taskId
  if (selectedTaskId) {
    const refreshed = taskStore.taskList.find(task => task.taskId === selectedTaskId) || detailTask.value
    detailTask.value = refreshed
    if (!isMobile.value) {
      selectedTask.value = refreshed
    }
  }
}

async function handleDelete(task: Task) {
  try {
    await ElMessageBox.confirm(`确定要删除任务「${task.title}」吗？`, '提示', { type: 'warning' })
    await taskStore.removeTask(task.taskId)
    ElMessage.success('删除成功')
  } catch {
    // Cancelled
  }
}

// AI Score - deterministic based on priority + taskId
function getAiScore(task: Task): number {
  if (typeof task.valuePriorityScore === 'number') {
    return task.valuePriorityScore
  }
  const priority = normalizeTaskPriority(task.priority)
  const base = priority === 'HIGH' ? 90 : priority === 'MEDIUM' ? 60 : 30
  const offset = Number(task.taskId) % 10
  return Math.min(99, base + offset)
}

// AI Insight - use description or generate from priority
function getAiInsight(task: Task): string {
  if (task.valuePriorityReason) return task.valuePriorityReason
  if (task.description) return task.description
  const priority = normalizeTaskPriority(task.priority)
  if (priority === 'HIGH') return '此任务优先级较高，建议尽快处理以推进业务进展。'
  if (priority === 'MEDIUM') return '常规跟进任务，按计划执行即可。'
  return '低优先级任务，可在空闲时间处理。'
}

function getPriorityLabel(priority: TaskPriority): string {
  const normalized = normalizeTaskPriority(priority)
  if (normalized === 'HIGH') return '高级'
  if (normalized === 'MEDIUM') return '中级'
  return '低级'
}

function getPriorityBadgeClass(priority: TaskPriority): string {
  const normalized = normalizeTaskPriority(priority)
  if (normalized === 'HIGH') return 'bg-rose-50 text-rose-600'
  if (normalized === 'MEDIUM') return 'bg-amber-50 text-amber-600'
  return 'bg-slate-100 text-slate-600'
}

function getStatusLabel(task: Task): string {
  if (isOverdue(task)) return '已延期'
  if (task.status === 'COMPLETED') return '已完成'
  if (task.status === 'IN_PROGRESS') return '进行中'
  if (task.status === 'CANCELLED') return '已取消'
  return '待处理'
}

function getStatusBadgeClass(task: Task): string {
  if (isOverdue(task)) return 'bg-red-50 text-red-600'
  if (task.status === 'COMPLETED') return 'bg-emerald-50 text-emerald-600'
  if (task.status === 'IN_PROGRESS') return 'bg-blue-50 text-blue-600'
  if (task.status === 'CANCELLED') return 'bg-slate-100 text-slate-500'
  return 'bg-slate-100 text-slate-600'
}

function getTaskTypeLabel(taskType?: string): string {
  const raw = taskType?.trim()
  if (!raw) return '任务'
  const normalized = raw.toUpperCase().replace(/[\s-]+/g, '_')
  const labels: Record<string, string> = {
    DOCUMENT: '文档',
    DOC: '文档',
    FILE: '文档',
    RESEARCH: '调研',
    SURVEY: '调研',
    FOLLOW_UP: '跟进',
    FOLLOWUP: '跟进',
    VISIT: '拜访',
    MEETING: '会议',
    CALL: '电话',
    EMAIL: '邮件',
    CONTRACT: '合同',
    PROPOSAL: '方案'
  }
  return labels[normalized] || raw
}

function getTaskTypeBadgeClass(taskType?: string): string {
  const label = getTaskTypeLabel(taskType)
  if (label === '调研') return 'bg-violet-50 text-violet-600'
  if (label === '跟进') return 'bg-slate-100 text-slate-600'
  if (label === '会议') return 'bg-amber-50 text-amber-600'
  if (label === '电话' || label === '拜访') return 'bg-emerald-50 text-emerald-600'
  return 'bg-blue-50 text-blue-600'
}

function getTaskOwnerName(task: Task): string {
  return task.assignedToName || task.createUserName || '未分配'
}

// Check if task is overdue
function isOverdue(task: Task): boolean {
  if (typeof task.overdue === 'boolean') return task.overdue
  if (!task.dueDate || task.status === 'COMPLETED') return false
  return new Date(task.dueDate) < new Date()
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) return dateStr
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function getRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  date.setHours(0, 0, 0, 0)
  now.setHours(0, 0, 0, 0)
  const diff = Math.ceil((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))

  if (diff < 0) return `已逾期${-diff}天`
  if (diff === 0) return '今天到期'
  if (diff === 1) return '明天到期'
  return `${diff}天后到期`
}
</script>

<style scoped>
.wk-task-list-table {
  background: var(--wk-bg-surface);
  border-color: var(--wk-border-subtle);
  color: var(--wk-text-secondary);
}

.wk-task-list-table__header {
  background: var(--wk-bg-surface-subtle);
  color: var(--wk-text-muted);
  border-bottom: 1px solid var(--wk-border-muted);
}

.wk-task-list-table__row {
  background: var(--wk-bg-surface);
  border-bottom: 1px solid var(--wk-border-subtle);
}

.wk-task-list-table__footer {
  background: var(--wk-bg-surface-subtle);
  border-color: var(--wk-border-subtle);
}

.wk-task-list-table__row:hover,
.wk-task-list-table__row.is-selected {
  background: color-mix(in srgb, var(--wk-primary) 11%, var(--wk-bg-surface));
}

.task-list-grid {
  display: grid;
  grid-template-columns:
    minmax(300px, 2.25fr)
    minmax(170px, 1.35fr)
    minmax(82px, 0.6fr)
    minmax(92px, 0.65fr)
    minmax(150px, 1fr)
    minmax(74px, 0.55fr)
    minmax(90px, 0.65fr)
    minmax(150px, 1fr);
  gap: 18px;
}
</style>
