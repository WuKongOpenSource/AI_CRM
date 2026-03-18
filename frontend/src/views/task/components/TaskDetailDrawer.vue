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
      <!-- Header -->
      <div class="flex items-center justify-between p-6 border-b border-slate-100">
        <span class="px-3 py-1 bg-primary/10 text-primary text-[10px] font-bold rounded-full uppercase tracking-widest">
          任务详情
        </span>
        <div class="flex items-center gap-2">
          <button
            v-if="canEdit"
            @click="$emit('edit', task)"
            class="size-9 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-primary transition-colors"
            type="button"
            aria-label="编辑任务"
            title="编辑任务"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
          </button>
          <button
            @click="open = false"
            class="size-9 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
            type="button"
            aria-label="关闭任务详情"
            title="关闭"
          >
            <span class="material-symbols-outlined text-xl leading-none">close</span>
          </button>
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 min-h-0 overflow-y-auto p-8">
        <h2 class="text-2xl font-bold text-slate-900 mb-2 line-clamp-2">{{ task.title }}</h2>

        <div class="grid grid-cols-2 gap-4 my-8">
          <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">截止时间</p>
            <p :class="['text-xs font-bold', isOverdue(task) ? 'text-red-500' : 'text-slate-700']">
              {{ task.dueDate ? formatDateTime(task.dueDate) : '未设定' }}
            </p>
            <p v-if="isOverdue(task)" class="text-[10px] text-red-500 font-bold mt-1">(已延期)</p>
          </div>
          <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">优先级</p>
            <p :class="['text-xs font-bold uppercase', getPriorityColor(task.priority)]">
              {{ getPriorityLabel(task.priority) }}
            </p>
          </div>
          <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">负责人</p>
            <p class="text-xs font-bold text-slate-700">{{ task.assignedToName || '未分配' }}</p>
          </div>
          <div class="p-3 bg-slate-50 rounded-xl border border-slate-100">
            <p class="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">任务状态</p>
            <p class="text-xs font-bold text-primary uppercase">{{ getStatusLabel(task.status) }}</p>
          </div>
        </div>

        <div class="space-y-8">
          <section v-if="task.participantNames">
            <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">参与人</h3>
            <p class="text-sm text-slate-700">{{ task.participantNames }}</p>
          </section>

          <section v-if="task.customerName">
            <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">关联客户</h3>
            <div class="p-4 bg-white border border-slate-200 rounded-2xl flex items-center gap-3 hover:bg-slate-50 transition-colors">
              <div class="size-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold">
                {{ task.customerName.charAt(0) }}
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-bold text-slate-900 truncate">{{ task.customerName }}</p>
                <p class="text-[10px] text-slate-400">点击查看客户详情</p>
              </div>
              <span class="material-symbols-outlined ml-auto text-slate-300">chevron_right</span>
            </div>
          </section>

          <section v-if="task.description">
            <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-4">任务描述</h3>
            <div class="p-4 bg-slate-50 border border-slate-100 rounded-2xl">
              <p class="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">{{ task.description }}</p>
            </div>
          </section>

          <section v-if="aiInsight" class="p-6 bg-slate-900 rounded-[2rem] text-white">
            <div class="flex items-center gap-2 mb-4">
              <span class="material-symbols-outlined text-emerald-400">auto_awesome</span>
              <h3 class="text-sm font-bold">AI 推荐沟通话术</h3>
            </div>
            <p class="text-xs text-slate-300 leading-relaxed italic">
              "{{ aiInsight }}"
            </p>
          </section>
        </div>
      </div>

      <!-- Bottom Actions -->
      <div v-if="canToggleComplete || canDelete" class="p-6 border-t border-slate-100">
        <div class="flex gap-3 items-stretch">
          <button
            v-if="canToggleComplete && task.status !== 'COMPLETED'"
            @click="$emit('toggle-complete', task)"
            class="flex-1 py-3 bg-emerald-500 text-white rounded-xl text-sm font-bold hover:bg-emerald-600 transition-all shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
            type="button"
          >
            <span class="material-symbols-outlined text-lg">check_circle</span>
            标记为完成
          </button>
          <button
            v-else-if="canToggleComplete"
            @click="$emit('toggle-complete', task)"
            class="flex-1 py-3 bg-slate-100 text-slate-600 rounded-xl text-sm font-bold hover:bg-slate-200 transition-all flex items-center justify-center gap-2"
            type="button"
          >
            <span class="material-symbols-outlined text-lg">undo</span>
            重新打开
          </button>

          <button
            v-if="canDelete"
            @click="$emit('delete', task)"
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
  <el-dialog
    v-else
    v-model="open"
    title="任务详情"
    width="95%"
    fullscreen
    class="task-detail-dialog"
  >
    <template v-if="task">
      <h2 class="text-lg font-bold text-slate-900 mb-4">{{ task.title }}</h2>

      <div class="grid grid-cols-2 gap-3 mb-6">
        <div class="p-3 bg-slate-50 rounded-xl">
          <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">截止时间</p>
          <p :class="['text-xs font-bold', isOverdue(task) ? 'text-red-500' : 'text-slate-700']">
            {{ task.dueDate ? formatDateTime(task.dueDate) : '未设定' }}
          </p>
        </div>
        <div class="p-3 bg-slate-50 rounded-xl">
          <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">优先级</p>
          <p :class="['text-xs font-bold', getPriorityColor(task.priority)]">
            {{ getPriorityLabel(task.priority) }}
          </p>
        </div>
        <div class="p-3 bg-slate-50 rounded-xl">
          <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">负责人</p>
          <p class="text-xs font-bold text-slate-700">{{ task.assignedToName || '未分配' }}</p>
        </div>
        <div class="p-3 bg-slate-50 rounded-xl">
          <p class="text-[10px] font-bold text-slate-400 uppercase mb-1">状态</p>
          <p class="text-xs font-bold text-primary">{{ getStatusLabel(task.status) }}</p>
        </div>
      </div>

      <div v-if="task.participantNames" class="mb-6">
        <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">参与人</h3>
        <p class="text-sm text-slate-700">{{ task.participantNames }}</p>
      </div>

      <div v-if="task.customerName" class="mb-6">
        <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">关联客户</h3>
        <div class="p-3 bg-white border border-slate-200 rounded-xl flex items-center gap-3">
          <div class="size-8 rounded-lg bg-primary/10 text-primary flex items-center justify-center font-bold text-sm">
            {{ task.customerName.charAt(0) }}
          </div>
          <p class="text-sm font-bold text-slate-900">{{ task.customerName }}</p>
        </div>
      </div>

      <div v-if="task.description" class="mb-6">
        <h3 class="text-xs font-bold text-slate-400 uppercase mb-2">描述</h3>
        <p class="text-sm text-slate-600 whitespace-pre-wrap">{{ task.description }}</p>
      </div>

      <div v-if="aiInsight" class="p-4 bg-slate-900 rounded-2xl text-white">
        <div class="flex items-center gap-2 mb-3">
          <span class="material-symbols-outlined text-emerald-400 text-sm">auto_awesome</span>
          <h3 class="text-sm font-bold">AI 推荐沟通话术</h3>
        </div>
        <p class="text-xs text-slate-300 leading-relaxed italic">"{{ aiInsight }}"</p>
      </div>
    </template>

    <template v-if="task" #footer>
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
            @click="$emit('toggle-complete', task)"
            class="flex-1 py-2.5 bg-emerald-500 text-white rounded-xl text-sm font-bold flex items-center justify-center gap-2"
            type="button"
          >
            <span class="material-symbols-outlined text-base">check_circle</span>
            标记完成
          </button>
        </div>
        <button
          v-if="canDelete"
          @click="$emit('delete', task)"
          class="w-full flex items-center justify-center gap-1.5 py-2 text-xs text-slate-400 hover:text-red-500 transition-colors"
          type="button"
        >
          <span class="material-symbols-outlined text-sm">delete</span>
          删除此任务
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Task, TaskPriority, TaskStatus } from '@/types/common'

const props = withDefaults(defineProps<{
  modelValue: boolean
  task: Task | null
  isMobile: boolean
  desktopWidth?: string
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
  (e: 'delete', task: Task): void
  (e: 'toggle-complete', task: Task): void
}>()

const open = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

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
  return p === 'HIGH' ? '高' : p === 'MEDIUM' ? '中' : '低'
}

function getPriorityColor(p: TaskPriority): string {
  return p === 'HIGH' ? 'text-red-500' : p === 'MEDIUM' ? 'text-amber-500' : 'text-slate-500'
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

