<template>
  <div class="flex h-full flex-col gap-5 bg-slate-50 px-4 py-5 md:px-8">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
      <div class="min-w-0">
        <h1 class="text-xl font-bold text-slate-900">项目管理</h1>
        <p class="mt-1 text-sm text-slate-500">共 {{ total }} 个项目</p>
      </div>

      <div class="flex flex-col gap-3 md:flex-row md:items-center">
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">search</span>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索项目、客户或描述"
            class="h-10 w-full rounded-lg border border-slate-200 bg-white pl-10 pr-4 text-sm outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20 md:w-80"
            @input="debouncedLoadProjects"
            @keydown.enter="loadProjects"
          />
        </div>
        <select
          v-model="status"
          class="h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-600 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/20"
          @change="applyFilters"
        >
          <option value="">全部状态</option>
          <option value="NOT_STARTED">未开始</option>
          <option value="IN_PROGRESS">进行中</option>
          <option value="COMPLETED">已完成</option>
          <option value="PAUSED">已暂停</option>
          <option value="ARCHIVED">已归档</option>
        </select>
        <button
          type="button"
          class="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-bold text-white shadow-sm transition hover:bg-primary/90"
          @click="openCreateDialog"
        >
          <span class="material-symbols-outlined text-[18px] leading-none">add_task</span>
          新建项目
        </button>
      </div>
    </div>

    <div class="min-h-0 flex-1 overflow-hidden rounded-lg border border-slate-200 bg-white" v-loading="loading">
      <el-table
        v-if="!isMobile"
        :data="projects"
        height="100%"
        row-key="projectId"
        table-layout="fixed"
        empty-text="暂无项目"
        @row-click="openDetail"
      >
        <el-table-column label="项目" min-width="260">
          <template #default="{ row }">
            <div class="min-w-0">
              <p class="truncate text-sm font-semibold text-slate-900">{{ row.name }}</p>
              <p class="mt-1 truncate text-xs text-slate-400">{{ row.description || '暂无描述' }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <span class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium" :class="statusClass(row.status)">
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="客户" min-width="170">
          <template #default="{ row }">
            <span class="block truncate text-sm text-slate-600">{{ row.customerName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="130">
          <template #default="{ row }">
            <span class="block truncate text-sm text-slate-600">{{ row.ownerName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="任务" width="120">
          <template #default="{ row }">
            <span class="text-sm text-slate-600">{{ row.incompleteTaskCount || 0 }} / {{ row.taskCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="150">
          <template #default="{ row }">
            <span class="text-sm text-slate-500">{{ formatDate(row.dueDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1" @click.stop>
              <button class="project-icon-button" title="编辑" type="button" @click="openEditDialog(row)">
                <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
              </button>
              <button class="project-icon-button" :title="row.status === 'ARCHIVED' ? '恢复' : '归档'" type="button" @click="toggleArchive(row)">
                <span class="material-symbols-outlined text-[18px] leading-none">{{ row.status === 'ARCHIVED' ? 'unarchive' : 'archive' }}</span>
              </button>
              <button class="project-icon-button text-rose-500 hover:bg-rose-50" title="删除" type="button" @click="handleDeleteProject(row)">
                <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
              </button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="h-full overflow-y-auto px-3 py-3">
        <div v-if="projects.length === 0" class="py-16 text-center text-slate-400">
          <span class="material-symbols-outlined text-5xl">view_kanban</span>
          <p class="mt-3 text-sm">{{ keyword.trim() ? '未找到匹配项目' : '暂无项目' }}</p>
        </div>
        <div v-else class="flex flex-col gap-3">
          <button
            v-for="project in projects"
            :key="project.projectId"
            type="button"
            class="w-full rounded-lg border border-slate-200 bg-white px-4 py-3 text-left transition active:bg-slate-100"
            @click="openDetail(project)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0 flex-1">
                <div class="truncate text-sm font-bold text-slate-900">{{ project.name }}</div>
                <p class="mt-1 truncate text-xs text-slate-400">{{ project.customerName || '未关联客户' }}</p>
              </div>
              <span class="inline-flex shrink-0 rounded-full px-2 py-0.5 text-xs font-medium" :class="statusClass(project.status)">
                {{ statusLabel(project.status) }}
              </span>
            </div>
            <div class="mt-3 text-sm text-slate-600">任务 {{ project.incompleteTaskCount || 0 }} / {{ project.taskCount || 0 }}</div>
          </button>
        </div>
      </div>
    </div>

    <div v-if="total > 0" class="flex items-center justify-between text-sm text-slate-500">
      <span>第 {{ page }} / {{ totalPages }} 页</span>
      <div class="flex items-center gap-2">
        <button class="rounded-lg border border-slate-200 bg-white px-3 py-2 disabled:opacity-40" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <button class="rounded-lg border border-slate-200 bg-white px-3 py-2 disabled:opacity-40" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>

    <el-dialog
      v-model="projectDialogVisible"
      :fullscreen="isMobile"
      :width="isMobile ? '100%' : '560px'"
      :title="editingProject ? '编辑项目' : '新建项目'"
      destroy-on-close
    >
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="项目名称" required>
          <el-input v-model="projectForm.name" maxlength="255" placeholder="请输入项目名称" />
        </el-form-item>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <el-form-item label="客户名称">
            <el-input v-model="projectForm.customerName" maxlength="255" placeholder="选填" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="projectForm.status" class="w-full">
              <el-option label="未开始" value="NOT_STARTED" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已暂停" value="PAUSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="projectForm.startDate" class="w-full" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker v-model="projectForm.dueDate" class="w-full" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </div>
        <el-form-item label="描述">
          <el-input v-model="projectForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="项目目标、范围或交付说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-2">
          <button type="button" class="rounded-lg px-4 py-2 text-sm font-medium text-slate-500 hover:bg-slate-100" @click="projectDialogVisible = false">取消</button>
          <button type="button" class="rounded-lg bg-primary px-4 py-2 text-sm font-bold text-white disabled:opacity-50" :disabled="savingProject" @click="saveProject">
            {{ savingProject ? '保存中...' : '保存' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :size="isMobile ? '100%' : '880px'" title="项目看板" append-to-body>
      <div v-loading="detailLoading" class="flex h-full min-h-0 flex-col gap-4">
        <template v-if="currentProject">
          <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
            <div class="min-w-0">
              <h2 class="truncate text-lg font-bold text-slate-900">{{ currentProject.name }}</h2>
              <p class="mt-1 text-sm text-slate-500">{{ currentProject.description || '暂无描述' }}</p>
            </div>
            <div class="flex gap-2">
              <button type="button" class="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-600" @click="createLane">新增泳道</button>
              <button type="button" class="rounded-lg bg-primary px-3 py-2 text-sm font-bold text-white" @click="openTaskDialog()">新增任务</button>
            </div>
          </div>

          <div class="min-h-0 flex-1 overflow-x-auto">
            <div class="grid min-w-[760px] grid-cols-3 gap-3">
              <section v-for="lane in boardLanes" :key="lane.laneId" class="flex min-h-[420px] flex-col rounded-lg border border-slate-200 bg-slate-50">
                <header class="flex items-center justify-between border-b border-slate-200 px-3 py-2">
                  <span class="text-sm font-bold text-slate-800">{{ lane.name }}</span>
                  <span class="rounded-full bg-white px-2 py-0.5 text-xs text-slate-500">{{ tasksByLane(lane.laneId).length }}</span>
                </header>
                <div class="flex flex-1 flex-col gap-2 overflow-y-auto p-3">
                  <button
                    v-for="task in tasksByLane(lane.laneId)"
                    :key="task.taskId"
                    type="button"
                    class="rounded-lg border border-slate-200 bg-white px-3 py-3 text-left shadow-sm transition hover:border-primary/40"
                    @click="openTaskDialog(task)"
                  >
                    <div class="flex items-start justify-between gap-2">
                      <p class="line-clamp-2 text-sm font-semibold text-slate-900">{{ task.title }}</p>
                      <span class="shrink-0 rounded-full px-2 py-0.5 text-[11px] font-medium" :class="priorityClass(task.priority)">
                        {{ priorityLabel(task.priority) }}
                      </span>
                    </div>
                    <p v-if="task.description" class="mt-2 line-clamp-2 text-xs leading-5 text-slate-500">{{ task.description }}</p>
                    <div class="mt-3 flex items-center justify-between text-xs text-slate-400">
                      <span>{{ task.ownerName || '未分配' }}</span>
                      <span>{{ formatDate(task.dueDate) }}</span>
                    </div>
                  </button>
                </div>
              </section>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="taskDialogVisible" :width="isMobile ? '100%' : '520px'" :fullscreen="isMobile" :title="editingTask ? '编辑任务' : '新增任务'" append-to-body>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="任务标题" required>
          <el-input v-model="taskForm.title" maxlength="255" placeholder="请输入任务标题" />
        </el-form-item>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <el-form-item label="泳道">
            <el-select v-model="taskForm.laneId" class="w-full">
              <el-option v-for="lane in boardLanes" :key="lane.laneId" :label="lane.name" :value="lane.laneId" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="taskForm.priority" class="w-full">
              <el-option label="低" value="LOW" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="高" value="HIGH" />
              <el-option label="紧急" value="URGENT" />
            </el-select>
          </el-form-item>
          <el-form-item label="负责人">
            <el-input v-model="taskForm.ownerName" maxlength="100" placeholder="选填" />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker v-model="taskForm.dueDate" class="w-full" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
          </el-form-item>
        </div>
        <el-form-item label="描述">
          <el-input v-model="taskForm.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-between gap-2">
          <button v-if="editingTask" type="button" class="rounded-lg border border-rose-200 px-4 py-2 text-sm font-medium text-rose-600" @click="handleDeleteTask">删除</button>
          <div class="ml-auto flex gap-2">
            <button type="button" class="rounded-lg px-4 py-2 text-sm font-medium text-slate-500 hover:bg-slate-100" @click="taskDialogVisible = false">取消</button>
            <button type="button" class="rounded-lg bg-primary px-4 py-2 text-sm font-bold text-white disabled:opacity-50" :disabled="savingTask" @click="saveTask">
              {{ savingTask ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addProject,
  addProjectLane,
  addProjectTask,
  archiveProject,
  deleteProject,
  deleteProjectTask,
  getProjectDetail,
  queryProjectPageList,
  restoreProject,
  updateProject,
  updateProjectTask
} from '@/api/project'
import { useResponsive } from '@/composables/useResponsive'
import { isRequestErrorHandled } from '@/utils/requestError'
import type { ProjectCreate, ProjectStatus, ProjectTaskSave, ProjectTaskVO, ProjectVO } from '@/types/project'

const { isMobile } = useResponsive()
const projects = ref<ProjectVO[]>([])
const loading = ref(false)
const keyword = ref('')
const status = ref<ProjectStatus | ''>('')
const page = ref(1)
const limit = ref(20)
const total = ref(0)
const projectDialogVisible = ref(false)
const savingProject = ref(false)
const editingProject = ref<ProjectVO | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const currentProject = ref<ProjectVO | null>(null)
const taskDialogVisible = ref(false)
const savingTask = ref(false)
const editingTask = ref<ProjectTaskVO | null>(null)
let searchTimer: ReturnType<typeof setTimeout> | null = null

const projectForm = reactive<ProjectCreate & { projectId?: string }>({
  name: '',
  description: '',
  customerName: '',
  startDate: '',
  dueDate: '',
  status: 'NOT_STARTED'
})

const taskForm = reactive<ProjectTaskSave>({
  title: '',
  description: '',
  laneId: '',
  ownerName: '',
  priority: 'MEDIUM',
  dueDate: ''
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / limit.value)))
const boardLanes = computed(() => currentProject.value?.lanes || [])

onMounted(() => {
  loadProjects()
})

async function loadProjects() {
  loading.value = true
  try {
    const result = await queryProjectPageList({
      keyword: keyword.value.trim() || undefined,
      status: status.value,
      page: page.value,
      limit: limit.value
    })
    projects.value = result.list || []
    total.value = result.totalRow || 0
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('加载项目列表失败')
    }
  } finally {
    loading.value = false
  }
}

function debouncedLoadProjects() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadProjects()
  }, 300)
}

function applyFilters() {
  page.value = 1
  loadProjects()
}

function changePage(nextPage: number) {
  if (nextPage < 1 || nextPage > totalPages.value || nextPage === page.value) return
  page.value = nextPage
  loadProjects()
}

function openCreateDialog() {
  editingProject.value = null
  resetProjectForm()
  projectDialogVisible.value = true
}

function openEditDialog(project: ProjectVO) {
  editingProject.value = project
  projectForm.projectId = project.projectId
  projectForm.name = project.name || ''
  projectForm.description = project.description || ''
  projectForm.customerName = project.customerName || ''
  projectForm.startDate = project.startDate || ''
  projectForm.dueDate = project.dueDate || ''
  projectForm.status = project.status || 'NOT_STARTED'
  projectDialogVisible.value = true
}

function resetProjectForm() {
  projectForm.projectId = undefined
  projectForm.name = ''
  projectForm.description = ''
  projectForm.customerName = ''
  projectForm.startDate = ''
  projectForm.dueDate = ''
  projectForm.status = 'NOT_STARTED'
}

async function saveProject() {
  if (!projectForm.name.trim()) {
    ElMessage.warning('请输入项目名称')
    return
  }
  savingProject.value = true
  const payload: ProjectCreate = {
    name: projectForm.name.trim(),
    description: projectForm.description?.trim() || undefined,
    customerName: projectForm.customerName?.trim() || undefined,
    startDate: projectForm.startDate || undefined,
    dueDate: projectForm.dueDate || undefined,
    status: projectForm.status
  }
  try {
    if (editingProject.value && projectForm.projectId) {
      await updateProject({ ...payload, projectId: projectForm.projectId })
      ElMessage.success('项目已更新')
    } else {
      await addProject(payload)
      ElMessage.success('项目已创建')
    }
    projectDialogVisible.value = false
    await loadProjects()
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('保存项目失败')
    }
  } finally {
    savingProject.value = false
  }
}

async function openDetail(project: ProjectVO) {
  detailVisible.value = true
  detailLoading.value = true
  currentProject.value = project
  try {
    currentProject.value = await getProjectDetail(project.projectId)
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('加载项目详情失败')
    }
  } finally {
    detailLoading.value = false
  }
}

async function refreshDetail() {
  if (!currentProject.value) return
  currentProject.value = await getProjectDetail(currentProject.value.projectId)
}

async function toggleArchive(project: ProjectVO) {
  try {
    if (project.status === 'ARCHIVED') {
      await restoreProject(project.projectId)
      ElMessage.success('项目已恢复')
    } else {
      await archiveProject(project.projectId)
      ElMessage.success('项目已归档')
    }
    await loadProjects()
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('更新项目状态失败')
    }
  }
}

async function handleDeleteProject(project: ProjectVO) {
  try {
    await ElMessageBox.confirm(`确定删除“${project.name}”？`, '删除项目', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteProject(project.projectId)
    ElMessage.success('项目已删除')
    await loadProjects()
  } catch (error) {
    if (!isRequestErrorHandled(error) && error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除项目失败')
    }
  }
}

async function createLane() {
  if (!currentProject.value) return
  try {
    const result = await ElMessageBox.prompt('请输入泳道名称', '新增泳道', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputValidator: value => !!value?.trim() || '泳道名称不能为空'
    })
    currentProject.value = await addProjectLane(currentProject.value.projectId, { name: result.value.trim() })
    ElMessage.success('泳道已创建')
  } catch (error) {
    if (!isRequestErrorHandled(error) && error !== 'cancel' && error !== 'close') {
      ElMessage.error('创建泳道失败')
    }
  }
}

function openTaskDialog(task?: ProjectTaskVO) {
  editingTask.value = task || null
  taskForm.taskId = task?.taskId
  taskForm.title = task?.title || ''
  taskForm.description = task?.description || ''
  taskForm.laneId = task?.laneId || boardLanes.value[0]?.laneId || ''
  taskForm.ownerName = task?.ownerName || ''
  taskForm.priority = task?.priority || 'MEDIUM'
  taskForm.dueDate = task?.dueDate || ''
  taskDialogVisible.value = true
}

async function saveTask() {
  if (!currentProject.value) return
  if (!taskForm.title.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  savingTask.value = true
  const payload: ProjectTaskSave = {
    taskId: taskForm.taskId,
    title: taskForm.title.trim(),
    description: taskForm.description?.trim() || undefined,
    laneId: taskForm.laneId || undefined,
    ownerName: taskForm.ownerName?.trim() || undefined,
    priority: taskForm.priority || 'MEDIUM',
    dueDate: taskForm.dueDate || undefined
  }
  try {
    currentProject.value = editingTask.value
      ? await updateProjectTask(currentProject.value.projectId, payload)
      : await addProjectTask(currentProject.value.projectId, payload)
    taskDialogVisible.value = false
    ElMessage.success('任务已保存')
    await loadProjects()
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('保存任务失败')
    }
  } finally {
    savingTask.value = false
  }
}

async function handleDeleteTask() {
  if (!currentProject.value || !editingTask.value) return
  try {
    await deleteProjectTask(currentProject.value.projectId, editingTask.value.taskId)
    taskDialogVisible.value = false
    ElMessage.success('任务已删除')
    await refreshDetail()
    await loadProjects()
  } catch (error) {
    if (!isRequestErrorHandled(error)) {
      ElMessage.error('删除任务失败')
    }
  }
}

function tasksByLane(laneId: string) {
  return (currentProject.value?.tasks || []).filter(task => String(task.laneId || '') === String(laneId))
}

function statusLabel(value?: string) {
  if (value === 'IN_PROGRESS') return '进行中'
  if (value === 'COMPLETED') return '已完成'
  if (value === 'PAUSED') return '已暂停'
  if (value === 'ARCHIVED') return '已归档'
  return '未开始'
}

function statusClass(value?: string) {
  if (value === 'IN_PROGRESS') return 'bg-blue-50 text-blue-600'
  if (value === 'COMPLETED') return 'bg-emerald-50 text-emerald-600'
  if (value === 'PAUSED') return 'bg-amber-50 text-amber-600'
  if (value === 'ARCHIVED') return 'bg-slate-100 text-slate-500'
  return 'bg-slate-50 text-slate-600'
}

function priorityLabel(value?: string) {
  if (value === 'LOW') return '低'
  if (value === 'HIGH') return '高'
  if (value === 'URGENT') return '紧急'
  return '中'
}

function priorityClass(value?: string) {
  if (value === 'LOW') return 'bg-slate-100 text-slate-500'
  if (value === 'HIGH') return 'bg-orange-50 text-orange-600'
  if (value === 'URGENT') return 'bg-rose-50 text-rose-600'
  return 'bg-blue-50 text-blue-600'
}

function formatDate(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
</script>

<style scoped>
.project-icon-button {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  color: rgb(100 116 139);
  transition: background-color 0.15s ease, color 0.15s ease;
}

.project-icon-button:hover {
  background: rgb(241 245 249);
  color: rgb(15 23 42);
}
</style>
