<template>
  <el-dialog
    v-model="open"
    :width="isMobile ? 'calc(100% - 32px)' : '720px'"
    :show-close="false"
    destroy-on-close
    :top="isMobile ? '16px' : '10vh'"
    :class="[
      '!rounded-2xl !p-0 overflow-hidden task-dialog wk-crm-el-field-scope',
      isMobile ? 'task-dialog--mobile' : 'task-dialog--desktop'
    ]"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div :class="[
            'size-10 rounded-xl flex items-center justify-center',
            editingTask ? 'bg-blue-50' : 'bg-primary/10'
          ]">
            <span :class="[
              'material-symbols-outlined text-xl',
              editingTask ? 'text-blue-500' : 'text-primary'
            ]">
              {{ editingTask ? 'edit_note' : 'task_alt' }}
            </span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-900">{{ editingTask ? '编辑任务' : '新建任务' }}</h2>
            <p class="text-xs text-slate-500 mt-0.5">{{ editingTask ? '修改任务详细信息' : '手动填写或使用 AI 智能解析' }}</p>
          </div>
        </div>
        <button
          @click="open = false"
          class="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
          type="button"
          aria-label="关闭"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </template>

    <div class="space-y-8 bg-white px-5 pb-6 pt-5 md:px-6 md:pb-7 md:pt-6">
      <!-- AI Parse Section (Create only) -->
      <div v-if="!editingTask" class="space-y-3">
        <div class="flex items-center gap-2 mb-3">
          <WkIcon name="ai" class="text-primary text-sm" />
          <span class="text-xs font-bold text-primary">AI 智能解析 (可选)</span>
        </div>
        <div class="relative">
          <el-input
            v-model="aiParseInput"
            type="textarea"
            :rows="5"
            resize="none"
            placeholder="例如：明天下午两点前给科技创新有限公司的张总发一份 Q4 扩容方案的报价单，标记为高优先级..."
            class="wk-crm-el-field-input wk-crm-el-field-ai w-full"
          />
          <button
            @click="handleAiParse"
            :disabled="!aiParseInput.trim() || aiParsing"
            class="absolute right-3 bottom-3 flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 text-white text-xs font-bold rounded-lg hover:bg-slate-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            type="button"
          >
            <span v-if="aiParsing" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
            <WkIcon v-else name="ai" class="text-sm" />
            {{ aiParsing ? '解析中...' : '一键解析' }}
          </button>
        </div>
      </div>

      <!-- Form Fields -->
      <div class="space-y-5">
        <div>
          <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务标题 <span class="text-red-500">*</span></label>
          <el-input
            v-model="formData.title"
            placeholder="请输入任务标题"
            size="large"
            class="w-full wk-crm-el-field-input"
          />
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">截止时间 <span class="text-red-500">*</span></label>
            <el-date-picker
              v-model="formData.dueDate"
              type="datetime"
              placeholder="选择截止时间"
              value-format="YYYY-MM-DDTHH:mm"
              format="YYYY-MM-DD HH:mm"
              :editable="false"
              size="large"
              class="w-full wk-crm-el-field-date"
            />
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">优先级</label>
            <el-select
              v-model="priority"
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option label="高" value="HIGH" />
              <el-option label="中" value="MEDIUM" />
              <el-option label="低" value="LOW" />
            </el-select>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">所属项目（可选）</label>
            <el-select
              v-model="formData.projectId"
              filterable
              clearable
              placeholder="不关联项目"
              :loading="projectLoading"
              class="w-full wk-crm-el-field-select"
              size="large"
              @change="handleProjectChange"
            >
              <el-option label="不关联项目" :value="''" />
              <el-option
                v-for="project in projectOptions"
                :key="project.projectId"
                :label="project.name"
                :value="project.projectId"
              />
            </el-select>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">所属泳道</label>
            <el-select
              v-model="formData.laneId"
              filterable
              clearable
              placeholder="选择项目后可选泳道"
              :disabled="!formData.projectId"
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option
                v-for="lane in laneOptions"
                :key="lane.laneId"
                :label="lane.name"
                :value="lane.laneId"
              />
            </el-select>
          </div>
        </div>

        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务类型</label>
            <el-select
              v-model="formData.taskType"
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option label="请选择" :value="''" />
              <el-option label="跟进" value="跟进" />
              <el-option label="文档" value="文档" />
              <el-option label="会议" value="会议" />
              <el-option label="电话" value="电话" />
              <el-option label="其他" value="其他" />
            </el-select>
          </div>
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">关联客户</label>
            <el-select
              v-model="formData.customerId"
              filterable
              remote
              reserve-keyword
              clearable
              default-first-option
              placeholder="搜索客户名称"
              :remote-method="searchCustomers"
              :loading="customerSearchLoading"
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </div>

        <div :class="editingTask ? 'grid grid-cols-1 sm:grid-cols-2 gap-4' : ''">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">参与人</label>
            <el-select
              v-model="participants"
              multiple
              filterable
              remote
              reserve-keyword
              clearable
              allow-create
              default-first-option
              placeholder="搜索或输入用户名称"
              :remote-method="searchUsers"
              :loading="userSearchLoading"
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option
                v-for="item in userOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
          <div v-if="editingTask">
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">负责人</label>
            <el-input
              v-model="formData.assignedToName"
              placeholder="请输入负责人"
              size="large"
              class="w-full wk-crm-el-field-input"
            />
          </div>
        </div>

        <div>
          <label class="text-xs font-bold text-slate-500 mb-1.5 block">任务描述</label>
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="请输入详细描述..."
            class="w-full wk-crm-el-field-input"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-3">
        <button
          @click="open = false"
          class="flex-1 py-2.5 text-sm font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          type="button"
        >
          取消
        </button>
        <button
          @click="handleSubmit"
          :disabled="!formData.title.trim() || submitting"
          class="flex-1 py-2.5 text-sm font-bold text-white bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-colors shadow-sm"
          type="button"
        >
          {{ submitting ? '提交中...' : (editingTask ? '保存修改' : '确认创建') }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { queryUserList } from '@/api/auth'
import { queryCustomerList } from '@/api/customer'
import { aiParseTask } from '@/api/task'
import { useResponsive } from '@/composables/useResponsive'
import { useProjectStore } from '@/stores/project'
import { useTaskStore } from '@/stores/task'
import type { Task, TaskAddBO, TaskStatus } from '@/types/common'
import { normalizeTaskPriority } from '@/utils/taskPriority'

type Option = { value: string; label: string }
type DefaultCustomer = { customerId?: string | number; companyName?: string | null } | null
type DefaultRelation = { relationId?: string | number; name?: string | null } | null
type DefaultCandidate = { candidateId?: string | number; candidateName?: string | null } | null
type DefaultAssignee = { userId?: string | number; realname?: string | null; username?: string | null } | null
type TaskEditSavedPayload = {
  mode: 'create' | 'edit'
  taskId?: string
}

const props = withDefaults(defineProps<{
  modelValue: boolean
  editingTask?: Task | null
  defaultCustomer?: DefaultCustomer
  defaultRelation?: DefaultRelation
  defaultCandidate?: DefaultCandidate
  defaultAssignee?: DefaultAssignee
  refreshStoreAfterSave?: boolean
}>(), {
  editingTask: null,
  defaultCustomer: null,
  defaultRelation: null,
  defaultCandidate: null,
  defaultAssignee: null,
  refreshStoreAfterSave: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved', payload: TaskEditSavedPayload): void
}>()

const taskStore = useTaskStore()
const projectStore = useProjectStore()
const { isMobile } = useResponsive()

const submitting = ref(false)
const aiParsing = ref(false)
const aiParseInput = ref('')
const userOptions = ref<Option[]>([])
const userSearchLoading = ref(false)
const customerOptions = ref<Option[]>([])
const customerSearchLoading = ref(false)
const projectLoading = ref(false)
const selectedParticipants = ref<string[]>([])
const formData = reactive<TaskAddBO & { status?: TaskStatus; customerId?: string; relationId?: string; candidateId?: string; assignedToName?: string }>({
  title: '',
  description: '',
  priority: 'MEDIUM',
  dueDate: undefined,
  status: undefined,
  taskType: '',
  projectId: '',
  laneId: '',
  customerId: '',
  relationId: '',
  candidateId: '',
  assignedTo: '',
  assignedToName: ''
})

const open = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const priority = computed({
  get: () => normalizeTaskPriority(formData.priority),
  set: (v: string) => {
    formData.priority = normalizeTaskPriority(v)
  }
})

const participants = computed({
  get: () => selectedParticipants.value,
  set: (v: string[]) => {
    selectedParticipants.value = v
  }
})

const projectOptions = computed(() => projectStore.accessibleProjectSummaries)

const selectedProject = computed(() => {
  const projectId = formData.projectId
  if (!projectId) return null
  return projectStore.getProjectById(projectId)
})

const laneOptions = computed(() => selectedProject.value?.lanes || [])

watch(
  () => [
    props.modelValue,
    props.editingTask?.taskId,
    props.defaultCustomer?.customerId,
    props.defaultCustomer?.companyName,
    props.defaultRelation?.relationId,
    props.defaultRelation?.name,
    props.defaultCandidate?.candidateId,
    props.defaultCandidate?.candidateName,
    props.defaultAssignee?.userId,
    props.defaultAssignee?.realname,
    props.defaultAssignee?.username
  ] as const,
  ([visible]) => {
    if (visible) {
      void ensureProjectOptions()
      hydrateForm()
    }
  },
  { immediate: true }
)

function hydrateForm() {
  const task = props.editingTask
  aiParseInput.value = ''
  selectedParticipants.value = []
  customerOptions.value = []
  userOptions.value = []

  if (task) {
    Object.assign(formData, {
      title: task.title,
      description: task.description || '',
      priority: normalizeTaskPriority(task.priority),
      dueDate: task.dueDate ? formatDateTimeLocal(task.dueDate) : undefined,
      status: task.status,
      taskType: task.taskType || '',
      projectId: task.projectId ? String(task.projectId) : '',
      laneId: task.laneId ? String(task.laneId) : '',
      customerId: task.customerId || '',
      relationId: task.relationId || '',
      candidateId: task.candidateId || '',
      assignedTo: task.assignedTo || '',
      assignedToName: task.assignedToName || ''
    })

    if (task.customerId && task.customerName) {
      customerOptions.value = [{ value: String(task.customerId), label: task.customerName }]
    }
    if (task.projectId) {
      void ensureProjectLoaded(String(task.projectId))
    }
    selectedParticipants.value = splitParticipants(task.participantNames)
    userOptions.value = selectedParticipants.value.map(name => ({ value: name, label: name }))
    return
  }

  Object.assign(formData, {
    title: '',
    description: '',
    priority: 'MEDIUM',
    dueDate: undefined,
    status: undefined,
    taskType: '',
    projectId: '',
    laneId: '',
    customerId: '',
    relationId: '',
    candidateId: '',
    assignedTo: '',
    assignedToName: ''
  })
  applyDefaultCustomer()
  applyDefaultRelation()
  applyDefaultCandidate()
  applyDefaultAssignee()
}

function applyDefaultCustomer() {
  const customer = props.defaultCustomer
  if (!customer?.customerId) return

  formData.customerId = String(customer.customerId)
  customerOptions.value = [{
    value: String(customer.customerId),
    label: customer.companyName || ''
  }]
}

function applyDefaultRelation() {
  const relation = props.defaultRelation
  if (!relation?.relationId) return
  formData.relationId = String(relation.relationId)
}

function applyDefaultCandidate() {
  const candidate = props.defaultCandidate
  if (!candidate?.candidateId) return
  formData.candidateId = String(candidate.candidateId)
  if (!formData.taskType) {
    formData.taskType = '面试'
  }
}

function applyDefaultAssignee() {
  const assignee = props.defaultAssignee
  if (!assignee?.userId) return
  formData.assignedTo = String(assignee.userId)
  formData.assignedToName = assignee.realname || assignee.username || ''
}

async function ensureProjectOptions() {
  if (projectLoading.value) return
  projectLoading.value = true
  try {
    await projectStore.ensureInitialized()
  } catch (error) {
    console.warn('项目列表加载失败:', error)
  } finally {
    projectLoading.value = false
  }
}

async function ensureProjectLoaded(projectId: string) {
  if (!projectId) return
  await ensureProjectOptions()
  const cached = projectStore.getProjectById(projectId)
  if (!cached || cached.lanes.length === 0) {
    try {
      await projectStore.loadProject(projectId)
    } catch (error) {
      console.warn('项目详情加载失败:', error)
      return
    }
  }
  const project = projectStore.getProjectById(projectId)
  if (project?.lanes.length && !formData.laneId) {
    formData.laneId = project.lanes[0].laneId
  }
}

function handleProjectChange(value: string | number | boolean | undefined) {
  const projectId = value ? String(value) : ''
  formData.projectId = projectId
  formData.laneId = ''
  if (projectId) {
    void ensureProjectLoaded(projectId)
  }
}

async function searchCustomers(query: string) {
  if (!query.trim()) {
    customerOptions.value = []
    return
  }

  customerSearchLoading.value = true
  try {
    const res = await queryCustomerList({ keyword: query, page: 1, limit: 20 })
    customerOptions.value = (res.list || []).map((customer: { customerId: string; companyName?: string }) => ({
      value: String(customer.customerId),
      label: customer.companyName || ''
    }))
  } catch (e) {
    console.warn('客户搜索失败:', e)
    customerOptions.value = []
  } finally {
    customerSearchLoading.value = false
  }
}

async function searchUsers(query: string) {
  if (!query.trim()) {
    userOptions.value = []
    return
  }

  userSearchLoading.value = true
  try {
    const res = await queryUserList({ search: query })
    userOptions.value = (res.list || []).map((user: { realname?: string; username?: string }) => ({
      value: user.realname || user.username || '',
      label: user.realname || user.username || ''
    })).filter((option: Option) => option.value)
  } catch (e) {
    console.warn('用户搜索失败:', e)
    userOptions.value = []
  } finally {
    userSearchLoading.value = false
  }
}

async function handleAiParse() {
  if (!aiParseInput.value.trim()) return

  aiParsing.value = true
  try {
    const result = await aiParseTask(aiParseInput.value)
    if (result.title) formData.title = result.title
    if (result.dueDate) formData.dueDate = result.dueDate
    if (result.priority) formData.priority = normalizeTaskPriority(result.priority)
    if (result.taskType) formData.taskType = result.taskType
    if (result.customerName) {
      const res = await queryCustomerList({ keyword: result.customerName, page: 1, limit: 5 })
      const list = res.list || []
      if (list.length > 0) {
        customerOptions.value = list.map((customer: { customerId: string; companyName?: string }) => ({
          value: String(customer.customerId),
          label: customer.companyName || ''
        }))
        formData.customerId = String(list[0].customerId)
      }
    }
    if (result.participantNames) {
      selectedParticipants.value = splitParticipants(result.participantNames)
      userOptions.value = selectedParticipants.value.map(name => ({ value: name, label: name }))
    }
    if (result.description) formData.description = result.description
    if (result.assignedToName) formData.assignedToName = result.assignedToName
    ElMessage.success('AI 解析完成，请确认并补充信息')
  } catch (error) {
    console.error('AI parse task failed:', error)
  } finally {
    aiParsing.value = false
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
    const submitData = {
      title: formData.title,
      description: formData.description,
      priority: normalizeTaskPriority(formData.priority),
      dueDate: formData.dueDate,
      taskType: formData.taskType,
      participantNames: selectedParticipants.value.join(', '),
      projectId: formData.projectId || undefined,
      laneId: formData.projectId ? (formData.laneId || undefined) : undefined,
      customerId: formData.customerId || undefined,
      relationId: formData.relationId || undefined,
      candidateId: formData.candidateId || undefined,
      assignedTo: formData.assignedTo || undefined
    }

    if (props.editingTask) {
      await taskStore.editTask({
        ...submitData,
        taskId: props.editingTask.taskId,
        status: formData.status
      }, {
        refreshList: props.refreshStoreAfterSave,
        refreshMyTasks: props.refreshStoreAfterSave
      })
      ElMessage.success('更新成功')
      refreshAffectedProjects(props.editingTask.projectId, submitData.projectId)
      open.value = false
      emit('saved', { mode: 'edit', taskId: props.editingTask.taskId })
    } else {
      const taskId = await taskStore.createTask(submitData, {
        refreshList: props.refreshStoreAfterSave,
        refreshMyTasks: props.refreshStoreAfterSave
      })
      ElMessage.success('创建成功')
      refreshAffectedProjects(submitData.projectId)
      open.value = false
      emit('saved', { mode: 'create', taskId })
    }

    hydrateForm()
  } finally {
    submitting.value = false
  }
}

function refreshAffectedProjects(...projectIds: Array<string | undefined>) {
  const ids = Array.from(new Set(projectIds.filter((id): id is string => Boolean(id))))
  if (ids.length === 0) return
  void projectStore.ensureInitialized(true)
}

function splitParticipants(value?: string) {
  return value ? value.split(/[,，]\s*/).filter(Boolean) : []
}

function formatDateTimeLocal(dateStr: string): string {
  const d = new Date(dateStr)
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<style>
.task-dialog .el-dialog__header {
  padding: 22px 24px 16px !important;
  margin-right: 0;
  flex-shrink: 0;
}
.task-dialog .el-dialog__body {
  padding: 0 !important;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}
.task-dialog .el-dialog__footer {
  padding: 14px 24px 22px !important;
  flex-shrink: 0;
}
.task-dialog.el-dialog {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.task-dialog--desktop.el-dialog {
  max-height: calc(100vh - 20vh);
  margin-bottom: 10vh;
}
.task-dialog--mobile.el-dialog {
  height: calc(100vh - 32px);
  max-height: calc(100vh - 32px);
  height: calc(100dvh - 32px);
  max-height: calc(100dvh - 32px);
  margin: 16px auto !important;
  border-radius: 1rem !important;
}

html.wk-native-mobile .task-dialog--mobile.el-dialog {
  height: calc(100dvh - 32px - var(--wk-safe-top) - var(--wk-safe-bottom));
  max-height: calc(100dvh - 32px - var(--wk-safe-top) - var(--wk-safe-bottom));
  margin: calc(16px + var(--wk-safe-top)) auto calc(16px + var(--wk-safe-bottom)) !important;
}

/* Prevent overlay from scrolling — dialog body scrolls internally */
.el-overlay:has(.task-dialog),
.el-overlay-dialog:has(.task-dialog) {
  overflow: hidden;
}


</style>
