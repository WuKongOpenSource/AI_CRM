<template>
  <el-drawer
    v-model="open"
    direction="rtl"
    :size="isMobile ? '100%' : '460px'"
    :with-header="false"
    class="wk-project-task-drawer"
  >
    <div v-if="task" class="flex h-full flex-col bg-white">
      <div class="flex items-start justify-between border-b border-slate-100 bg-slate-50 px-6 py-5">
        <div class="min-w-0">
          <div class="mb-3 flex items-center gap-2">
            <span class="inline-flex size-10 items-center justify-center rounded-2xl bg-primary/10 text-primary">
              <span class="material-symbols-outlined text-[20px]">task_alt</span>
            </span>
            <div>
              <p class="text-sm font-bold text-slate-900">项目任务详情</p>
              <p class="text-xs text-slate-500">{{ task.status }}</p>
            </div>
          </div>
          <h2 class="line-clamp-2 text-xl font-bold leading-8 text-slate-900">{{ task.title }}</h2>
        </div>
        <button
          type="button"
          class="flex size-8 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
          @click="open = false"
        >
          <span class="material-symbols-outlined text-[18px]">close</span>
        </button>
      </div>

      <div class="flex-1 space-y-6 overflow-y-auto px-6 py-6">
        <div class="grid grid-cols-2 gap-3">
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">所属项目</p>
            <p class="mt-2 text-sm font-semibold text-slate-900">{{ projectName || '-' }}</p>
          </article>
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">所属泳道</p>
            <p class="mt-2 text-sm font-semibold text-slate-900">{{ task.status }}</p>
          </article>
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">优先级</p>
            <span class="mt-2 inline-flex rounded-full px-2.5 py-1 text-sm font-semibold" :class="projectTaskPriorityClass(task.priority)">
              {{ projectTaskPriorityLabel(task.priority) }}
            </span>
          </article>
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">负责人</p>
            <p class="mt-2 text-sm text-slate-900">{{ task.ownerName || '未分配' }}</p>
          </article>
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">截止时间</p>
            <p class="mt-2 text-sm text-slate-900">{{ formatDateTime(task.dueDate) }}</p>
          </article>
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">关联客户</p>
            <p class="mt-2 text-sm text-slate-900">{{ task.customerName || '未关联' }}</p>
          </article>
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">创建时间</p>
            <p class="mt-2 text-sm text-slate-900">{{ formatDateTime(task.createTime) }}</p>
          </article>
          <article class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">更新时间</p>
            <p class="mt-2 text-sm text-slate-900">{{ formatDateTime(task.updateTime) }}</p>
          </article>
        </div>

        <section v-if="task.description" class="rounded-3xl border border-slate-100 bg-slate-50 p-5">
          <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">任务描述</p>
          <p class="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{{ task.description }}</p>
        </section>

        <section class="rounded-3xl border border-slate-100 bg-slate-50 p-5">
          <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">任务协作信息</p>
          <div class="mt-4 space-y-2 text-sm text-slate-700">
            <p>参与人：{{ task.participantNames?.length ? task.participantNames.join('、') : '无' }}</p>
            <p>关联附件：{{ task.attachments.length ? `${task.attachments.length} 个` : '无' }}</p>
            <p>关联日程：{{ task.schedules.length ? `${task.schedules.length} 个` : '无' }}</p>
            <p>AI 生成来源：{{ task.generatedByAi ? 'AI 对话创建' : '手动创建' }}</p>
          </div>
        </section>

        <section v-if="task.attachments.length" class="rounded-3xl border border-slate-100 bg-slate-50 p-5">
          <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">关联附件</p>
          <div class="mt-3 space-y-2 text-sm text-slate-700">
            <article v-for="attachment in task.attachments" :key="attachment.attachmentId" class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
              <p class="font-semibold text-slate-900">{{ attachment.name }}</p>
              <p class="mt-1 text-xs text-slate-400">{{ attachment.createdByName || '系统' }} · {{ formatDateTime(attachment.createTime) }}</p>
            </article>
          </div>
        </section>

        <section v-if="task.schedules.length" class="rounded-3xl border border-slate-100 bg-slate-50 p-5">
          <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">关联日程</p>
          <div class="mt-3 space-y-2 text-sm text-slate-700">
            <article v-for="schedule in task.schedules" :key="schedule.scheduleId" class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
              <p class="font-semibold text-slate-900">{{ schedule.title }}</p>
              <p class="mt-1 text-xs text-slate-400">
                {{ schedule.scheduleTime ? formatDateTime(schedule.scheduleTime) : '未设置时间' }} · {{ schedule.createdByName || '系统' }}
              </p>
            </article>
          </div>
        </section>

        <section v-if="task.notes.length" class="rounded-3xl border border-slate-100 bg-slate-50 p-5">
          <p class="text-[11px] font-bold uppercase tracking-wider text-slate-400">任务备注</p>
          <div class="mt-3 space-y-2 text-sm text-slate-700">
            <article v-for="note in task.notes" :key="note.noteId" class="rounded-2xl border border-slate-200 bg-white px-4 py-3">
              <p class="leading-6 text-slate-800">{{ note.content }}</p>
              <p class="mt-1 text-xs text-slate-400">{{ note.createdByName || '系统' }} · {{ formatDateTime(note.createTime) }}</p>
            </article>
          </div>
        </section>

        <section v-if="task.aiSourceText" class="rounded-[28px] bg-slate-900 p-5 text-white">
          <div class="mb-3 flex items-center gap-2">
            <span class="material-symbols-outlined text-emerald-400">auto_awesome</span>
            <p class="text-sm font-semibold">AI 创建来源</p>
          </div>
          <p class="text-sm leading-6 text-slate-200">{{ task.aiSourceText }}</p>
        </section>
      </div>

      <div class="border-t border-slate-100 p-5">
        <div class="flex flex-wrap gap-3">
          <button
            type="button"
            class="inline-flex flex-1 items-center justify-center gap-2 rounded-xl bg-slate-900 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-slate-700"
            @click="$emit('enter-chat', task)"
            data-ai-dialog-entry="task-drawer"
          >
            进入任务对话
          </button>
          <button
            v-if="canEdit"
            type="button"
            class="flex-1 rounded-xl border border-slate-200 py-2.5 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
            @click="$emit('edit', task)"
          >
            编辑任务
          </button>
          <button
            v-if="canDelete"
            type="button"
            class="rounded-xl bg-red-50 px-4 py-2.5 text-sm font-semibold text-red-600 transition-colors hover:bg-red-100"
            @click="$emit('delete', task)"
          >
            删除
          </button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ProjectTask } from '@/types/project'
import {
  formatDateTime,
  projectTaskPriorityClass,
  projectTaskPriorityLabel
} from '@/utils/project'

const props = withDefaults(defineProps<{
  modelValue: boolean
  task: ProjectTask | null
  projectName?: string
  isMobile?: boolean
  canEdit?: boolean
  canDelete?: boolean
}>(), {
  projectName: '',
  isMobile: false,
  canEdit: true,
  canDelete: true
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
  (event: 'enter-chat', value: ProjectTask): void
  (event: 'edit', value: ProjectTask): void
  (event: 'delete', value: ProjectTask): void
}>()

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
</script>

<style>
.wk-project-task-drawer .el-drawer__body {
  padding: 0 !important;
}

[data-ai-dialog-entry='task-drawer']::before {
  display: inline-flex;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 9999px;
  background: #202020;
  color: #fff;
  box-shadow: 0 12px 30px rgb(15 23 42 / 0.14);
  content: 'auto_awesome';
  font-family: 'Material Symbols Outlined';
  font-size: 13px;
  font-style: normal;
  font-weight: 400;
  line-height: 1;
  font-feature-settings: 'liga';
  -webkit-font-smoothing: antialiased;
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
}
</style>
