<template>
  <div class="wk-object-detail-embedded h-full overflow-y-auto bg-[var(--wk-bg-surface)] px-4 py-4">
    <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div class="flex items-start gap-3">
        <img v-if="avatarUrl" :src="avatarUrl" class="size-12 rounded-lg object-contain" alt="关系人头像" />
        <div v-else class="flex size-12 shrink-0 items-center justify-center rounded-lg bg-slate-900 text-base font-bold text-white">
          {{ relation.name?.charAt(0) || '?' }}
        </div>
        <div class="min-w-0 flex-1">
          <h3 class="truncate text-base font-bold text-slate-900">{{ relation.name || '未命名关系人' }}</h3>
          <p class="mt-1 text-sm text-slate-500">{{ relationTypeLabel }}</p>
        </div>
      </div>
      <div class="mt-4 space-y-2 text-sm text-slate-600">
        <p><span class="text-slate-400">手机号：</span>{{ relation.phone || '-' }}</p>
        <p><span class="text-slate-400">微信号：</span>{{ relation.wechat || '-' }}</p>
        <p><span class="text-slate-400">邮箱：</span>{{ relation.email || '-' }}</p>
        <p><span class="text-slate-400">所属公司：</span>{{ relation.customerName || '-' }}</p>
        <p><span class="text-slate-400">来源：</span>{{ relation.sourceName || sourceLabel }}</p>
      </div>
      <p class="mt-4 whitespace-pre-wrap rounded-lg bg-slate-50 p-3 text-sm leading-6 text-slate-600">
        {{ relation.remark || '暂无备注' }}
      </p>
    </section>

    <ObjectRelatedModules
      :tasks="tasks"
      :schedules="schedules"
      :attachments="attachments"
      :selected-task-id="selectedTaskId"
      :selected-schedule-id="selectedScheduleId"
      @add-task="emit('add-task')"
      @add-schedule="emit('add-schedule')"
      @add-attachment="emit('add-attachment')"
      @view-task="emit('view-task', $event)"
      @view-schedule="emit('view-schedule', $event)"
      @view-attachment="emit('view-attachment', $event)"
    />

    <section class="mt-5 border-t border-slate-100 pt-5">
      <div class="mb-4 flex items-center justify-between">
        <h4 class="flex items-center gap-2 text-sm font-bold text-slate-900">
          <span class="inline-flex size-7 shrink-0 items-center justify-center rounded-lg bg-slate-900 text-white shadow-sm">
            <span class="material-symbols-outlined text-[17px] leading-none">history</span>
          </span>
          历史记录
        </h4>
        <span class="text-xs font-medium text-slate-400">{{ histories.length }}</span>
      </div>

      <div v-if="histories.length === 0" class="rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-4 text-center">
        <span class="material-symbols-outlined mb-1 text-[22px] text-slate-300">history</span>
        <p class="text-xs font-medium text-slate-400">暂无数据</p>
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="history in histories"
          :key="history.followUpId"
          class="rounded-xl border border-slate-200 bg-white p-3"
        >
          <p class="line-clamp-2 text-sm font-semibold leading-5 text-slate-800">{{ history.content || '-' }}</p>
          <p class="mt-1 truncate text-xs leading-5 text-slate-500">
            {{ [history.typeName || history.type, history.followTime ? formatDateTime(history.followTime) : ''].filter(Boolean).join(' · ') || '-' }}
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useEnumStore } from '@/stores/enums'
import { resolveRelationTypeLabel as resolveRelationTypeDisplayLabel } from '@/views/relation/constants'
import type { ScheduleVO } from '@/api/schedule'
import type { Knowledge, Task } from '@/types/common'
import type { RelationDetailVO } from '@/types/relation'
import ObjectRelatedModules from './ObjectRelatedModules.vue'

const props = defineProps<{
  detail: RelationDetailVO
  selectedTaskId?: string | number | null
  selectedScheduleId?: string | number | null
}>()

const emit = defineEmits<{
  (e: 'add-task'): void
  (e: 'add-schedule'): void
  (e: 'add-attachment'): void
  (e: 'view-task', task: Task): void
  (e: 'view-schedule', schedule: ScheduleVO): void
  (e: 'view-attachment', attachment: Knowledge): void
}>()

const relation = computed(() => props.detail.relation)
const enumStore = useEnumStore()
enumStore.ensureRelationType()
const tasks = computed(() => props.detail.tasks || [])
const schedules = computed(() => props.detail.schedules || [])
const attachments = computed(() => props.detail.attachments || [])
const histories = computed(() => props.detail.histories || [])
const avatarUrl = computed(() => relation.value.avatarUrl || '')
const relationTypeLabel = computed(() => {
  return resolveRelationTypeDisplayLabel(relation.value.relationType, relation.value.relationTypeName, enumStore.relationType)
})
const sourceLabel = computed(() => {
  if (relation.value.source === 'customer_contact') return '客户联系人'
  if (relation.value.source === 'manual') return '手动创建'
  return relation.value.source || '-'
})

function formatDateTime(value?: string) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>
