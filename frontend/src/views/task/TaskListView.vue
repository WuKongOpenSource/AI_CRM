<template>
  <div class="h-full flex bg-background-light overflow-hidden">
    <!-- Task List Section -->
    <div class="flex-1 overflow-y-auto p-4 md:p-8">
      <div class="max-w-4xl mx-auto space-y-6">
        <!-- Header -->
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h2 class="text-xl md:text-2xl font-bold text-slate-900">AI 优先行动中心</h2>
            <p class="text-sm text-slate-500 mt-1">基于客户价值与成交概率，AI 已为您自动排序今日任务。</p>
          </div>
          <div class="flex items-center gap-3">
            <!-- Segmented filter -->
            <div class="hidden md:flex bg-white p-1 rounded-xl border border-slate-200 shadow-sm">
              <button
                @click="valueFilter = 'all'"
                :class="[
                  'px-4 py-1.5 text-xs font-bold rounded-lg transition-all',
                  valueFilter === 'all' ? 'bg-primary text-white' : 'text-slate-500 hover:bg-slate-50'
                ]"
              >
                全部任务
              </button>
              <button
                @click="valueFilter = 'high-impact'"
                :class="[
                  'px-4 py-1.5 text-xs font-bold rounded-lg transition-all',
                  valueFilter === 'high-impact' ? 'bg-primary text-white' : 'text-slate-500 hover:bg-slate-50'
                ]"
              >
                高价值优先
              </button>
            </div>
            <!-- Add task button -->
            <button
              class="flex items-center gap-1.5 px-4 py-2 bg-primary text-white text-sm font-medium rounded-xl hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20"
              @click="handleAddTask"
            >
              <span class="material-symbols-outlined text-lg">add</span>
              <span>{{ isMobile ? '新建' : '新建任务' }}</span>
            </button>
          </div>
        </div>

        <!-- Status Filter Tabs -->
        <div class="flex gap-2 overflow-x-auto">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            @click="handleStatusFilter(tab.value)"
            :class="[
              'px-4 py-1.5 text-xs font-bold rounded-full transition-all whitespace-nowrap',
              currentStatus === tab.value
                ? 'bg-primary text-white'
                : 'bg-white text-slate-500 border border-slate-200 hover:bg-slate-50'
            ]"
          >
            {{ tab.label }} ({{ tab.count }})
          </button>
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
                    <span class="text-[8px] font-bold text-slate-400 uppercase tracking-tighter">AI 评分</span>
                  </template>
                </div>
                <div class="h-4 w-px bg-slate-100"></div>
                <div
                  :class="[
                    'size-2 rounded-full',
                    task.status === 'COMPLETED'
                      ? 'bg-slate-200'
                      : task.priority === 'HIGH' ? 'bg-red-500'
                      : task.priority === 'MEDIUM' ? 'bg-amber-500'
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
                      class="px-2 py-0.5 bg-red-50 text-red-600 text-[10px] font-bold rounded uppercase animate-pulse shrink-0"
                    >
                      已延期
                    </span>
                    <span
                      v-else-if="task.status === 'COMPLETED'"
                      class="px-2 py-0.5 bg-emerald-50 text-emerald-600 text-[10px] font-bold rounded uppercase shrink-0"
                    >
                      已完成
                    </span>
                    <span
                      v-else-if="task.status === 'IN_PROGRESS'"
                      class="px-2 py-0.5 bg-blue-50 text-blue-600 text-[10px] font-bold rounded uppercase shrink-0"
                    >
                      进行中
                    </span>
                    <span
                      v-else
                      class="px-2 py-0.5 bg-slate-100 text-slate-500 text-[10px] font-bold rounded uppercase shrink-0"
                    >
                      待处理
                    </span>
                  </div>
                  <span v-if="task.dueDate" class="text-[10px] font-bold text-slate-400 uppercase shrink-0 ml-2 hidden md:block">
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
                    class="text-[10px] font-bold px-2 py-0.5 rounded uppercase bg-blue-50 text-blue-600"
                  >
                    AI 生成
                  </span>
                  <span
                    v-else
                    class="text-[10px] font-bold px-2 py-0.5 rounded uppercase bg-slate-100 text-slate-600"
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
                  <span class="material-symbols-outlined text-primary text-sm mt-0.5">psychology</span>
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
        <div v-if="taskStore.totalCount > (taskStore.queryParams.limit || 10)" class="mt-6 flex justify-center">
          <div class="flex items-center gap-2">
            <button
              class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :disabled="(taskStore.queryParams.page || 1) <= 1"
              @click="handlePageChange((taskStore.queryParams.page || 1) - 1)"
            >
              <span class="material-symbols-outlined text-lg">chevron_left</span>
            </button>
            <button
              v-for="p in visiblePages"
              :key="p"
              @click="handlePageChange(p)"
              :class="[
                'size-8 flex items-center justify-center rounded-lg text-sm font-medium transition-colors',
                p === (taskStore.queryParams.page || 1)
                  ? 'bg-primary text-white'
                  : 'border border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
              ]"
            >
              {{ p }}
            </button>
            <button
              class="size-8 flex items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              :disabled="(taskStore.queryParams.page || 1) >= totalPages"
              @click="handlePageChange((taskStore.queryParams.page || 1) + 1)"
            >
              <span class="material-symbols-outlined text-lg">chevron_right</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <TaskDetailDrawer
      v-model="showTaskDetail"
      :task="selectedTask"
      :is-mobile="isMobile"
      :ai-insight="selectedTask ? getAiInsight(selectedTask) : ''"
      @edit="handleEditFromDetail"
      @toggle-complete="handleToggleCompleteFromDetail"
      @delete="handleDeleteFromDetail"
    />

    <TaskEditDialog
      v-model="showAddDialog"
      :is-mobile="isMobile"
      :editing-task="editingTask"
      :submitting="submitting"
      :ai-parsing="aiParsing"
      v-model:ai-parse-input="aiParseInput"
      :form-data="formData"
      v-model:selected-participants="selectedParticipants"
      :user-options="userOptions"
      :user-search-loading="userSearchLoading"
      :customer-options="customerOptions"
      :customer-search-loading="customerSearchLoading"
      :search-users="searchUsers"
      :search-customers="searchCustomers"
      @ai-parse="handleAiParse"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useTaskStore } from '@/stores/task'
import { useResponsive } from '@/composables/useResponsive'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiParseTask } from '@/api/task'
import { queryCustomerList } from '@/api/customer'
import { queryUserList } from '@/api/auth'
import type { Task, TaskAddBO, TaskStatus } from '@/types/common'
import TaskDetailDrawer from './components/TaskDetailDrawer.vue'
import TaskEditDialog from './components/TaskEditDialog.vue'

const taskStore = useTaskStore()
const { isMobile } = useResponsive()

const currentStatus = ref('all')
const valueFilter = ref<'all' | 'high-impact'>('all')
const showAddDialog = ref(false)
const editingTask = ref<Task | null>(null)
const selectedTask = ref<Task | null>(null)
const submitting = ref(false)
const aiParseInput = ref('')
const aiParsing = ref(false)

// Customer search
const customerOptions = ref<{ value: string; label: string }[]>([])
const customerSearchLoading = ref(false)

async function searchCustomers(query: string) {
  if (!query) {
    customerOptions.value = []
    return
  }
  customerSearchLoading.value = true
  try {
    const res = await queryCustomerList({ keyword: query, page: 1, limit: 20 })
    customerOptions.value = (res.list || []).map((c: any) => ({
      value: String(c.customerId),
      label: c.companyName
    }))
  } catch (e) {
    console.warn('客户搜索失败:', e)
    customerOptions.value = []
  } finally {
    customerSearchLoading.value = false
  }
}

// User search for participants
const userOptions = ref<{ value: string; label: string }[]>([])
const userSearchLoading = ref(false)

async function searchUsers(query: string) {
  if (!query) {
    userOptions.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const res = await queryUserList({ search: query })
    userOptions.value = (res.list || []).map((u: any) => ({
      value: u.realname || u.username,
      label: u.realname || u.username
    }))
  } catch (e) {
    console.warn('用户搜索失败:', e)
    userOptions.value = []
  } finally {
    userSearchLoading.value = false
  }
}

// Selected participant names as array for el-select multiple
const selectedParticipants = ref<string[]>([])

const formData = reactive<TaskAddBO & { status?: TaskStatus; assignedToName?: string }>({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: undefined,
  status: undefined,
  taskType: '',
  customerId: '',
  assignedToName: ''
})

// Computed properties
const statusTabs = computed(() => {
  const tasks = taskStore.taskList
  return [
    { value: 'all', label: '全部', count: taskStore.totalCount },
    { value: 'PENDING', label: '待处理', count: tasks.filter(t => t.status === 'PENDING').length },
    { value: 'IN_PROGRESS', label: '进行中', count: tasks.filter(t => t.status === 'IN_PROGRESS').length },
    { value: 'COMPLETED', label: '已完成', count: tasks.filter(t => t.status === 'COMPLETED').length }
  ]
})

const displayedTasks = computed(() => {
  if (valueFilter.value === 'high-impact') {
    return taskStore.taskList.filter(t => t.priority === 'HIGH')
  }
  return taskStore.taskList
})

const totalPages = computed(() => Math.ceil(taskStore.totalCount / (taskStore.queryParams.limit || 10)))

const visiblePages = computed(() => {
  const total = totalPages.value
  const current = taskStore.queryParams.page || 1
  const pages: number[] = []
  let start = Math.max(1, current - 2)
  const end = Math.min(total, start + 4)
  start = Math.max(1, end - 4)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const showTaskDetail = computed({
  get: () => !!selectedTask.value,
  set: (val: boolean) => {
    if (!val) selectedTask.value = null
  }
})

onMounted(() => {
  taskStore.fetchTaskList(true)
})



function handleStatusFilter(status: string) {
  currentStatus.value = status
  taskStore.queryParams.status = status === 'all' ? undefined : status as TaskStatus
  taskStore.queryParams.page = 1
  taskStore.fetchTaskList(false)
}

function handlePageChange(page: number) {
  if (taskStore.queryParams.page === page) return
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
  selectedTask.value = task
}

function handleEditFromDetail(task: Task) {
  handleEdit(task)
  if (isMobile.value) selectedTask.value = null
}

async function handleToggleCompleteFromDetail(task: Task) {
  await handleToggleComplete(task)
  if (isMobile.value) selectedTask.value = null
}

async function handleDeleteFromDetail(task: Task) {
  await handleDelete(task)
  if (isMobile.value) selectedTask.value = null
}

function handleAddTask() {
  resetForm()
  showAddDialog.value = true
}

function handleEdit(task: Task) {
  editingTask.value = task
  Object.assign(formData, {
    title: task.title,
    description: task.description || '',
    priority: task.priority,
    dueDate: task.dueDate ? formatDateTimeLocal(task.dueDate) : undefined,
    status: task.status,
    taskType: task.taskType || '',
    customerId: task.customerId || '',
    assignedToName: task.assignedToName || ''
  })
  // Populate customer select options for edit mode
  if (task.customerId && task.customerName) {
    customerOptions.value = [{ value: String(task.customerId), label: task.customerName }]
  }
  // Populate participants
  selectedParticipants.value = task.participantNames
    ? task.participantNames.split(/[,，]\s*/).filter(Boolean)
    : []
  // Populate user options so selected values display labels
  userOptions.value = selectedParticipants.value.map(name => ({ value: name, label: name }))
  showAddDialog.value = true
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

async function handleSubmit() {
  if (!formData.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  if (!formData.dueDate) {
    ElMessage.warning('请选择截止时间')
    return
  }

  submitting.value = true
  try {
    const submitData: any = {
      title: formData.title,
      description: formData.description,
      priority: formData.priority,
      dueDate: formData.dueDate,
      taskType: formData.taskType,
      participantNames: selectedParticipants.value.join(', '),
      customerId: formData.customerId || undefined
    }
    if (editingTask.value) {
      await taskStore.editTask({ ...submitData, taskId: editingTask.value.taskId, status: formData.status })
      ElMessage.success('更新成功')
    } else {
      await taskStore.createTask(submitData)
      ElMessage.success('创建成功')
    }
    showAddDialog.value = false
    resetForm()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  editingTask.value = null
  aiParseInput.value = ''
  selectedParticipants.value = []
  customerOptions.value = []
  userOptions.value = []
  Object.assign(formData, {
    title: '', description: '', priority: 'MEDIUM', dueDate: undefined, status: undefined,
    taskType: '', customerId: '', assignedToName: ''
  })
}

async function handleAiParse() {
  if (!aiParseInput.value.trim()) return
  aiParsing.value = true
  try {
    const result = await aiParseTask(aiParseInput.value)
    if (result.title) formData.title = result.title
    if (result.dueDate) formData.dueDate = result.dueDate
    if (result.priority) formData.priority = result.priority.toUpperCase() as any
    if (result.taskType) formData.taskType = result.taskType
    if (result.customerName) {
      // AI parsed a customer name - search and try to match
      const res = await queryCustomerList({ keyword: result.customerName, page: 1, limit: 5 })
      const list = res.list || []
      if (list.length > 0) {
        customerOptions.value = list.map((c: any) => ({ value: String(c.customerId), label: c.companyName }))
        formData.customerId = String(list[0].customerId)
      }
    }
    if (result.participantNames) {
      selectedParticipants.value = result.participantNames.split(/[,，]\s*/).filter(Boolean)
      userOptions.value = selectedParticipants.value.map(name => ({ value: name, label: name }))
    }
    if (result.description) formData.description = result.description
    if (result.assignedToName) formData.assignedToName = result.assignedToName
    ElMessage.success('AI 解析完成，请确认并补充信息')
  } catch {
    ElMessage.error('AI 解析失败，请手动填写')
  } finally {
    aiParsing.value = false
  }
}

// AI Score - deterministic based on priority + taskId
function getAiScore(task: Task): number {
  const base = task.priority === 'HIGH' ? 90 : task.priority === 'MEDIUM' ? 60 : 30
  const offset = Number(task.taskId) % 10
  return Math.min(99, base + offset)
}

// AI Insight - use description or generate from priority
function getAiInsight(task: Task): string {
  if (task.description) return task.description
  if (task.priority === 'HIGH') return '此任务优先级较高，建议尽快处理以推进业务进展。'
  if (task.priority === 'MEDIUM') return '常规跟进任务，按计划执行即可。'
  return '低优先级任务，可在空闲时间处理。'
}

// Check if task is overdue
function isOverdue(task: Task): boolean {
  if (!task.dueDate || task.status === 'COMPLETED') return false
  return new Date(task.dueDate) < new Date()
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function formatDateTimeLocal(dateStr: string): string {
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
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
/* (moved into reusable components) */
</style>
