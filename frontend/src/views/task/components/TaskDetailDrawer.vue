<template>
  <!-- Desktop -->
  <el-drawer
    v-if="!isMobile"
    v-model="open"
    direction="rtl"
    :size="desktopWidth"
    :with-header="false"
    :modal="false"
    modal-penetrable
    class="task-detail-drawer"
  >
    <div v-if="task" class="h-full flex flex-col bg-white shadow-2xl">
      <!-- Header：与 ScheduleDetailDrawer 一致，无底部分割线 -->
      <div class="flex items-center justify-between px-8 pt-8 pb-4">
        <span
          class="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-primary"
        >
          任务详情
        </span>
        <div class="flex items-center gap-2">
          <button
            v-if="canEdit"
            class="inline-flex size-9 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-primary/10 hover:text-primary"
            type="button"
            aria-label="编辑任务"
            title="编辑任务"
            @click="$emit('edit', task)"
          >
            <span class="material-symbols-outlined flex size-[18px] items-center justify-center text-[18px] leading-none">edit</span>
          </button>
          <button
            class="inline-flex size-9 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
            type="button"
            aria-label="关闭任务详情"
            title="关闭"
            @click="open = false"
          >
            <span class="material-symbols-outlined flex size-[18px] items-center justify-center text-[18px] leading-none">close</span>
          </button>
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 min-h-0 overflow-y-auto px-8 pb-8 pt-0">
        <h2 class="mb-8 line-clamp-2 text-2xl font-bold leading-tight text-slate-900">{{ task.title }}</h2>

        <div class="mb-8 grid grid-cols-2 gap-4">
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="mb-2 flex items-center gap-2">
              <span class="material-symbols-outlined shrink-0 text-[16px] text-primary leading-none">schedule</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">截止时间</p>
            </div>
            <p :class="['text-xs font-bold', isOverdue(task) ? 'text-red-500' : 'text-slate-700']">
              {{ task.dueDate ? formatDateTime(task.dueDate) : '未设定' }}
            </p>
            <p v-if="isOverdue(task)" class="mt-1 text-xs font-bold text-red-500">(已延期)</p>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="mb-2 flex items-center gap-2">
              <span class="material-symbols-outlined shrink-0 text-[16px] text-primary leading-none">priority_high</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">优先级</p>
            </div>
            <p :class="['text-xs font-bold uppercase', getPriorityColor(task.priority)]">
              {{ getPriorityLabel(task.priority) }}
            </p>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="mb-2 flex items-center gap-2">
              <span class="material-symbols-outlined shrink-0 text-[16px] text-primary leading-none">person</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">负责人</p>
            </div>
            <p class="text-xs font-bold text-slate-700">{{ task.assignedToName || '未分配' }}</p>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="mb-2 flex items-center gap-2">
              <span class="material-symbols-outlined shrink-0 text-[16px] text-primary leading-none">task_alt</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">任务状态</p>
            </div>
            <p class="text-xs font-bold uppercase text-primary">{{ getStatusLabel(task.status) }}</p>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="mb-2 flex items-center gap-2">
              <span class="material-symbols-outlined shrink-0 text-[16px] text-primary leading-none">person</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">创建人</p>
            </div>
            <p class="text-xs font-bold text-slate-700 break-words">{{ displayCreateUserName }}</p>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="mb-2 flex items-center gap-2">
              <span class="material-symbols-outlined shrink-0 text-[16px] text-primary leading-none">calendar_clock</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">创建时间</p>
            </div>
            <p class="text-xs font-bold text-slate-700 break-words">{{ displayCreateTime }}</p>
          </div>
        </div>

        <div class="space-y-8">
          <section v-if="task.participantNames">
            <div class="mb-2 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px] text-slate-400">group</span>
              <h3 class="text-xs font-bold uppercase tracking-widest text-slate-400">参与人</h3>
            </div>
            <p class="whitespace-pre-wrap text-sm font-medium leading-relaxed text-slate-700">
              {{ task.participantNames }}
            </p>
          </section>

          <section v-if="task.customerName">
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px] text-slate-400">corporate_fare</span>
              <h3 class="text-xs font-bold uppercase tracking-widest text-slate-400">关联客户</h3>
            </div>
            <div
              class="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 transition-colors"
              :class="task.customerId ? 'cursor-pointer hover:bg-slate-50' : ''"
              @click="handleGoToCustomerDetail"
            >
              <div class="flex size-10 items-center justify-center rounded-xl bg-primary/10 font-bold text-primary">
                {{ task.customerName.charAt(0) }}
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-bold text-slate-900">{{ task.customerName }}</p>
              </div>
              <span v-if="task.customerId" class="material-symbols-outlined ml-auto text-slate-300">chevron_right</span>
            </div>
          </section>

          <section v-if="task.description">
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px] text-slate-400">notes</span>
              <h3 class="text-xs font-bold uppercase tracking-widest text-slate-400">任务描述</h3>
            </div>
            <div class="rounded-2xl border border-slate-100 bg-slate-50 p-5">
              <p class="whitespace-pre-wrap text-sm leading-relaxed text-slate-600">{{ task.description }}</p>
            </div>
          </section>

          <section v-if="displayAiInsight" class="p-6 bg-slate-900 rounded-[2rem] text-white">
            <div class="flex items-center gap-2 mb-4">
              <WkIcon name="ai" class="text-emerald-400" />
              <h3 class="text-sm font-bold">AI 推荐沟通话术</h3>
            </div>
            <p class="text-xs text-slate-300 leading-relaxed italic">
              "{{ displayAiInsight }}"
            </p>
          </section>
        </div>
      </div>

      <!-- Bottom Actions -->
      <div v-if="canToggleComplete || canDelete" class="p-6 border-t border-slate-100">
        <div class="flex gap-3 items-stretch">
          <button
            v-if="canToggleComplete && task.status !== 'COMPLETED'"
            @click="handleToggleComplete"
            class="flex-1 py-3 bg-emerald-500 text-white rounded-xl text-sm font-bold hover:bg-emerald-600 transition-all shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
            type="button"
          >
            <span class="material-symbols-outlined text-lg">check_circle</span>
            标记为完成
          </button>
          <button
            v-else-if="canToggleComplete"
            @click="handleToggleComplete"
            class="flex-1 py-3 bg-slate-100 text-slate-600 rounded-xl text-sm font-bold hover:bg-slate-200 transition-all flex items-center justify-center gap-2"
            type="button"
          >
            <span class="material-symbols-outlined text-lg">undo</span>
            重新打开
          </button>

          <button
            v-if="canDelete"
            @click="handleDelete"
            class="size-12 flex items-center justify-center rounded-xl text-slate-400 hover:text-red-500 hover:bg-red-50 transition-colors shrink-0"
            type="button"
            aria-label="删除任务"
            title="删除任务"
          >
            <span class="material-symbols-outlined">delete</span>
          </button>
        </div>
      </div>
    </div>
  </el-drawer>

  <!-- Mobile -->
  <el-drawer
    v-else
    v-model="open"
    direction="rtl"
    size="100%"
    :with-header="false"
    class="task-detail-drawer task-detail-drawer--mobile"
  >
    <div v-if="task" class="h-full flex flex-col bg-white">
      <!-- Header -->
      <div class="flex items-center justify-between px-5 pt-5 pb-3">
        <span class="rounded-full bg-primary/10 px-3 py-1 text-[10px] font-bold uppercase tracking-widest text-primary">
          任务详情
        </span>
        <div class="flex items-center gap-2">
          <button
            v-if="canEdit"
            class="inline-flex size-9 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-primary/10 hover:text-primary"
            type="button"
            aria-label="编辑任务"
            title="编辑任务"
            @click="$emit('edit', task)"
          >
            <span class="material-symbols-outlined flex size-[18px] items-center justify-center text-[18px] leading-none">edit</span>
          </button>
          <button
            class="inline-flex size-9 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
            type="button"
            aria-label="关闭任务详情"
            title="关闭"
            @click="open = false"
          >
            <span class="material-symbols-outlined flex size-[18px] items-center justify-center text-[18px] leading-none">close</span>
          </button>
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 min-h-0 overflow-y-auto px-5 pb-5">
        <h2 class="text-lg font-bold text-slate-900 mb-4">{{ task.title }}</h2>

        <div class="mb-6 grid grid-cols-2 gap-3">
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-3">
            <div class="mb-1.5 flex items-center gap-1.5">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-primary leading-none">schedule</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">截止时间</p>
            </div>
            <p :class="['text-xs font-bold', isOverdue(task) ? 'text-red-500' : 'text-slate-700']">
              {{ task.dueDate ? formatDateTime(task.dueDate) : '未设定' }}
            </p>
            <p v-if="isOverdue(task)" class="mt-0.5 text-[11px] font-bold text-red-500">(已延期)</p>
          </div>
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-3">
            <div class="mb-1.5 flex items-center gap-1.5">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-primary leading-none">priority_high</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">优先级</p>
            </div>
            <p :class="['text-xs font-bold uppercase', getPriorityColor(task.priority)]">
              {{ getPriorityLabel(task.priority) }}
            </p>
          </div>
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-3">
            <div class="mb-1.5 flex items-center gap-1.5">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-primary leading-none">person</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">负责人</p>
            </div>
            <p class="text-xs font-bold text-slate-700">{{ task.assignedToName || '未分配' }}</p>
          </div>
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-3">
            <div class="mb-1.5 flex items-center gap-1.5">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-primary leading-none">task_alt</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">状态</p>
            </div>
            <p class="text-xs font-bold uppercase text-primary">{{ getStatusLabel(task.status) }}</p>
          </div>
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-3">
            <div class="mb-1.5 flex items-center gap-1.5">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-primary leading-none">person</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">创建人</p>
            </div>
            <p class="text-xs font-bold text-slate-700 break-words">{{ displayCreateUserName }}</p>
          </div>
          <div class="rounded-2xl border border-slate-100 bg-slate-50 p-3">
            <div class="mb-1.5 flex items-center gap-1.5">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-primary leading-none">calendar_clock</span>
              <p class="text-[10px] font-bold uppercase tracking-wider text-slate-400">创建时间</p>
            </div>
            <p class="text-xs font-bold text-slate-700 break-words">{{ displayCreateTime }}</p>
          </div>
        </div>

        <div v-if="task.participantNames" class="mb-6">
          <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">参与人</h3>
          <p class="text-sm text-slate-700">{{ task.participantNames }}</p>
        </div>

        <div v-if="task.customerName" class="mb-6">
          <div class="mb-2 flex items-center gap-2">
            <span class="material-symbols-outlined text-base text-slate-400">corporate_fare</span>
            <h3 class="text-xs font-bold uppercase text-slate-400">关联客户</h3>
          </div>
          <div
            class="flex items-center gap-3 rounded-xl border border-slate-200 bg-white p-3 transition-colors"
            :class="task.customerId ? 'cursor-pointer active:bg-slate-50' : ''"
            role="button"
            :tabindex="task.customerId ? 0 : -1"
            @click="handleGoToCustomerDetail"
          >
            <div class="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-sm font-bold text-primary">
              {{ task.customerName.charAt(0) }}
            </div>
            <p class="min-w-0 flex-1 text-sm font-bold text-slate-900">{{ task.customerName }}</p>
            <span v-if="task.customerId" class="material-symbols-outlined shrink-0 text-slate-300">chevron_right</span>
          </div>
        </div>

        <div v-if="task.description" class="mb-6">
          <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">描述</h3>
          <p class="text-sm text-slate-600 whitespace-pre-wrap">{{ task.description }}</p>
        </div>

        <div v-if="displayAiInsight" class="p-4 bg-slate-900 rounded-2xl text-white">
          <div class="flex items-center gap-2 mb-3">
            <WkIcon name="ai" class="text-emerald-400 text-sm" />
            <h3 class="text-sm font-bold">AI 推荐沟通话术</h3>
          </div>
          <p class="text-xs text-slate-300 leading-relaxed italic">"{{ displayAiInsight }}"</p>
        </div>
      </div>

      <!-- Bottom Actions -->
      <div v-if="task" class="p-4 border-t border-slate-100">
        <div class="space-y-3">
          <div class="flex gap-3">
            <button
              v-if="canEdit"
              @click="$emit('edit', task)"
              class="flex-1 py-2.5 border border-slate-200 rounded-xl text-sm font-bold text-slate-600 flex items-center justify-center gap-2"
              type="button"
            >
              <span class="material-symbols-outlined text-base">edit</span>
              编辑任务
            </button>
            <button
              v-if="canToggleComplete && task.status !== 'COMPLETED'"
              @click="handleToggleComplete"
              class="flex-1 py-2.5 bg-emerald-500 text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2"
              type="button"
            >
              <span class="material-symbols-outlined text-base">check_circle</span>
              标记完成
            </button>
          </div>
          <button
            v-if="canDelete"
            @click="handleDelete"
            class="w-full flex items-center justify-center gap-1.5 py-2 text-xs text-slate-400 hover:text-red-500 transition-colors"
            type="button"
          >
            <span class="material-symbols-outlined text-sm">delete</span>
            删除此任务
          </button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTaskStore } from '@/stores/task'
import type { Task, TaskPriority, TaskStatus } from '@/types/common'
import { getTaskAiInsightText } from '@/utils/taskAiInsight'
import { normalizeTaskPriority } from '@/utils/taskPriority'

const router = useRouter()
const taskStore = useTaskStore()

const props = withDefaults(defineProps<{
  modelValue: boolean
  task: Task | null
  isMobile: boolean
  desktopWidth?: string
  /** 非空时覆盖根据任务推导的默认 AI 话术 */
  aiInsight?: string
  canEdit?: boolean
  canDelete?: boolean
  canToggleComplete?: boolean
}>(), {
  desktopWidth: '400px',
  aiInsight: '',
  canEdit: true,
  canDelete: true,
  canToggleComplete: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'edit', task: Task): void
  /** 完成状态切换或删除成功后触发，由父级刷新列表/客户数据 */
  (e: 'mutated'): void
}>()

const open = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const displayAiInsight = computed(() => {
  const override = props.aiInsight?.trim()
  if (override) return props.aiInsight!.trim()
  if (!props.task) return ''
  return getTaskAiInsightText(props.task)
})

const displayCreateTime = computed(() => {
  const t = props.task?.createTime
  if (!t) return '未知'
  return formatDateTime(t)
})

const displayCreateUserName = computed(() => {
  const name = props.task?.createUserName?.trim()
  return name || '未知'
})

async function handleToggleComplete() {
  const task = props.task
  if (!task) return
  const newStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  await taskStore.changeTaskStatus(task.taskId, newStatus)
  emit('mutated')
  if (props.isMobile) {
    open.value = false
  }
}

async function handleDelete() {
  const task = props.task
  if (!task) return
  try {
    await ElMessageBox.confirm(`确定要删除任务「${task.title}」吗？`, '提示', { type: 'warning' })
    await taskStore.removeTask(task.taskId)
    ElMessage.success('删除成功')
    open.value = false
    emit('mutated')
  } catch {
    /* 取消 */
  }
}

function handleGoToCustomerDetail() {
  if (!props.task?.customerId) return
  const href = router.resolve({ path: `/customer/${props.task.customerId}` }).href
  window.open(href, '_blank', 'noopener,noreferrer')
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function isOverdue(task: Task): boolean {
  if (!task?.dueDate) return false
  if (task.status === 'COMPLETED') return false
  return new Date(task.dueDate).getTime() < Date.now()
}

function getPriorityLabel(p: TaskPriority): string {
  const priority = normalizeTaskPriority(p)
  return priority === 'HIGH' ? '高' : priority === 'MEDIUM' ? '中' : '低'
}

function getPriorityColor(p: TaskPriority): string {
  const priority = normalizeTaskPriority(p)
  return priority === 'HIGH' ? 'text-red-500' : priority === 'MEDIUM' ? 'text-amber-500' : 'text-slate-500'
}

function getStatusLabel(s: TaskStatus): string {
  if (s === 'COMPLETED') return '已完成'
  if (s === 'IN_PROGRESS') return '进行中'
  if (s === 'CANCELLED') return '已取消'
  return '待处理'
}
</script>

<style>
/* el-drawer is teleported; must be global */
.task-detail-drawer .el-drawer__body {
  padding: 0 !important;
}
</style>
