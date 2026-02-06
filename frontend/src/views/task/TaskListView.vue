<template>
  <div class="h-full flex flex-col bg-gray-50">
    <!-- Page Header -->
    <div class="px-6 py-4 bg-white border-b border-gray-200">
      <div class="flex items-start justify-between">
        <div>
          <h1 class="text-lg font-semibold">待办任务</h1>
          <p class="text-sm text-gray-500 mt-1">AI自动生成的跟进任务和待办事项</p>
        </div>
        <div class="text-sm text-gray-500">
          您的权限: <span class="text-gray-700">{{ userStore.realname || '用户' }}，完整权限</span>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 overflow-auto p-6">
      <!-- Section Header -->
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-medium">待办任务</h2>
        <div class="flex items-center gap-4">
          <div class="flex items-center text-primary-500">
            <el-icon class="mr-1"><MagicStick /></el-icon>
            <span>AI已自动生成 {{ aiGeneratedCount }} 个任务</span>
          </div>
          <el-button type="primary" @click="handleAddTask">
            <el-icon class="mr-1"><Plus /></el-icon>
            新建任务
          </el-button>
        </div>
      </div>

      <!-- Status Filter Tabs -->
      <div class="flex gap-3 mb-6">
        <el-button
          v-for="tab in statusTabs"
          :key="tab.value"
          :type="currentStatus === tab.value ? 'primary' : 'default'"
          :class="currentStatus !== tab.value ? '!bg-white' : ''"
          round
          @click="handleStatusFilter(tab.value)"
        >
          {{ tab.label }} ({{ tab.count }})
        </el-button>
      </div>

      <!-- Task List -->
      <div v-if="taskStore.loading" class="text-center py-16">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      </div>
      <div v-else-if="taskStore.taskList.length === 0" class="text-center py-16 text-gray-400">
        <el-icon :size="48"><List /></el-icon>
        <p class="mt-4">暂无任务</p>
      </div>
      <div v-else class="space-y-4">
        <div
          v-for="task in taskStore.taskList"
          :key="task.taskId"
          class="bg-white rounded-lg border border-gray-200 p-5 hover:shadow-md transition-shadow"
        >
          <div class="flex items-start">
            <el-checkbox
              :model-value="task.status === 'COMPLETED'"
              class="mt-1"
              @change="handleToggleComplete(task)"
            />
            <div class="ml-3 flex-1">
              <!-- Title + AI Tag -->
              <div class="flex items-center gap-2">
                <span
                  :class="[
                    'font-medium text-base',
                    task.status === 'COMPLETED' ? 'line-through text-gray-400' : 'text-gray-800'
                  ]"
                >
                  {{ task.title }}
                </span>
                <span
                  v-if="task.generatedByAi"
                  class="inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-blue-50 text-blue-600 border border-blue-200"
                >
                  <el-icon class="mr-0.5" :size="10"><MagicStick /></el-icon>
                  AI
                </span>
              </div>

              <!-- Description -->
              <p v-if="task.description" class="mt-2 text-sm text-gray-600">
                {{ task.description }}
              </p>

              <!-- Priority + Status -->
              <div class="mt-3 flex items-center gap-3">
                <span :class="['flex items-center text-sm', getPriorityColor(task.priority)]">
                  <span class="w-2 h-2 rounded-full mr-1.5" :class="getPriorityDotColor(task.priority)"></span>
                  {{ getPriorityLabel(task.priority) }}
                </span>
                <el-tag :type="getStatusType(task.status)" size="small" round>
                  {{ getStatusLabel(task.status) }}
                </el-tag>
              </div>

              <!-- Date + Assignee + Customer -->
              <div class="mt-3 flex items-center text-sm text-gray-500">
                <template v-if="task.dueDate">
                  <el-icon class="mr-1"><Calendar /></el-icon>
                  <span>{{ formatDate(task.dueDate) }}</span>
                  <span class="ml-1 text-gray-400">({{ getRelativeTime(task.dueDate) }})</span>
                  <span class="mx-2">•</span>
                </template>
                <template v-if="task.assignedToName">
                  <el-icon class="mr-1"><User /></el-icon>
                  <span>{{ task.assignedToName }}</span>
                </template>
                <template v-if="task.customerName">
                  <span class="mx-2">•</span>
                  <span>{{ task.customerName }}</span>
                </template>
              </div>

              <!-- Action Buttons -->
              <div class="mt-4 flex items-center gap-3">
                <el-button
                  v-if="task.status !== 'COMPLETED'"
                  size="small"
                  @click="handleStartTask(task)"
                >
                  {{ task.status === 'IN_PROGRESS' ? '继续处理' : '开始处理' }}
                </el-button>
                <el-button size="small" text @click="handleViewDetail(task)">查看详情</el-button>
                <div class="flex-1"></div>
                <el-dropdown trigger="click">
                  <el-button text size="small">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
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
        <el-pagination
          v-model:current-page="taskStore.queryParams.page"
          v-model:page-size="taskStore.queryParams.limit"
          :total="taskStore.totalCount"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- Add/Edit Dialog -->
    <el-dialog v-model="showAddDialog" :title="editingTask ? '编辑任务' : '新建任务'" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="formData.priority" class="w-full">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止日期">
              <el-date-picker v-model="formData.dueDate" type="date" class="w-full" placeholder="选择日期" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="editingTask" label="状态">
          <el-select v-model="formData.status" class="w-full">
            <el-option label="待处理" value="PENDING" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ editingTask ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Task Detail Dialog -->
    <el-dialog v-model="showDetailDialog" title="任务详情" width="600px">
      <template v-if="selectedTask">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="任务标题" :span="2">
            {{ selectedTask.title }}
            <el-tag v-if="selectedTask.generatedByAi" size="small" class="ml-2">AI生成</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedTask.status)">{{ getStatusLabel(selectedTask.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <span :class="getPriorityColor(selectedTask.priority)">{{ getPriorityLabel(selectedTask.priority) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="截止日期">
            {{ selectedTask.dueDate ? formatDate(selectedTask.dueDate) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="负责人">
            {{ selectedTask.assignedToName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ selectedTask.createUserName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="关联客户" :span="2">
            {{ selectedTask.customerName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="任务描述" :span="2">
            {{ selectedTask.description || '无' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
        <el-button v-if="selectedTask?.status !== 'COMPLETED'" type="primary" @click="handleStartFromDetail">
          开始处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useTaskStore } from '@/stores/task'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { Loading, List, MoreFilled, MagicStick, Calendar, User, Plus } from '@element-plus/icons-vue'
import type { Task, TaskAddBO, TaskStatus } from '@/types/common'

const taskStore = useTaskStore()
const userStore = useUserStore()

const currentStatus = ref('all')
const showAddDialog = ref(false)
const showDetailDialog = ref(false)
const editingTask = ref<Task | null>(null)
const selectedTask = ref<Task | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<TaskAddBO & { status?: TaskStatus }>({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: undefined,
  status: undefined
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }]
}

// Computed properties
const aiGeneratedCount = computed(() =>
  taskStore.taskList.filter(t => t.generatedByAi).length
)

const statusTabs = computed(() => {
  const tasks = taskStore.taskList
  return [
    { value: 'all', label: '全部', count: taskStore.totalCount },
    { value: 'PENDING', label: '待处理', count: tasks.filter(t => t.status === 'PENDING').length },
    { value: 'IN_PROGRESS', label: '进行中', count: tasks.filter(t => t.status === 'IN_PROGRESS').length },
    { value: 'COMPLETED', label: '已完成', count: tasks.filter(t => t.status === 'COMPLETED').length }
  ]
})

onMounted(() => {
  taskStore.fetchTaskList(true)
})

function handleStatusFilter(status: string) {
  currentStatus.value = status
  // Use backend filtering for status
  taskStore.queryParams.status = status === 'all' ? undefined : status as TaskStatus
  taskStore.queryParams.page = 1
  taskStore.fetchTaskList(false)
}

function handlePageChange(page: number) {
  if (taskStore.queryParams.page === page) {
    return
  }
  taskStore.queryParams.page = page
  taskStore.fetchTaskList(false)
}

function handleSizeChange(size: number) {
  taskStore.queryParams.limit = size
  taskStore.queryParams.page = 1
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
  showDetailDialog.value = true
}

async function handleStartFromDetail() {
  if (selectedTask.value && selectedTask.value.status === 'PENDING') {
    await taskStore.changeTaskStatus(selectedTask.value.taskId, 'IN_PROGRESS')
    await taskStore.fetchTaskList(false)
    showDetailDialog.value = false
    ElMessage.success('任务已开始处理')
  }
}

function handleAddTask() {
  resetForm()
  showAddDialog.value = true
}

function handleEdit(task: Task) {
  editingTask.value = task
  Object.assign(formData, {
    title: task.title,
    description: task.description,
    priority: task.priority,
    dueDate: task.dueDate,
    status: task.status
  })
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
  if (!formRef.value) return
  await formRef.value.validate()

  submitting.value = true
  try {
    if (editingTask.value) {
      await taskStore.editTask({ ...formData, taskId: editingTask.value.taskId })
      ElMessage.success('更新成功')
    } else {
      await taskStore.createTask(formData)
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
  Object.assign(formData, { title: '', description: '', priority: 'MEDIUM', dueDate: undefined, status: undefined })
}

function getPriorityColor(priority: string): string {
  switch (priority) {
    case 'HIGH': return 'text-red-500'
    case 'MEDIUM': return 'text-orange-500'
    default: return 'text-green-500'
  }
}

function getPriorityDotColor(priority: string): string {
  switch (priority) {
    case 'HIGH': return 'bg-red-500'
    case 'MEDIUM': return 'bg-orange-500'
    default: return 'bg-green-500'
  }
}

function getPriorityLabel(priority: string): string {
  return { HIGH: '高优先级', MEDIUM: '中优先级', LOW: '低优先级' }[priority] || priority
}

function getStatusType(status: string): 'info' | 'primary' | 'success' {
  switch (status) {
    case 'PENDING': return 'info'
    case 'IN_PROGRESS': return 'primary'
    case 'COMPLETED': return 'success'
    default: return 'info'
  }
}

function getStatusLabel(status: string): string {
  return { PENDING: '待处理', IN_PROGRESS: '进行中', COMPLETED: '已完成' }[status] || status
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function getRelativeTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  // Reset time to compare dates only
  date.setHours(0, 0, 0, 0)
  now.setHours(0, 0, 0, 0)
  const diff = Math.ceil((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))

  if (diff < 0) return `已逾期${-diff}天`
  if (diff === 0) return '今天到期'
  if (diff === 1) return '明天到期'
  return `${diff}天后到期`
}
</script>
