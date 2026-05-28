<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    :size="isMobile ? '100%' : '400px'"
    :with-header="false"
    :modal="isMobile"
    :lock-scroll="isMobile"
    body-class="schedule-detail-drawer__body"
    :modal-penetrable="!isMobile"
    class="schedule-detail-drawer"
  >
    <div v-if="schedule" class="h-full flex flex-col bg-white shadow-2xl">
      <div class="flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50/50 px-6 py-6 sm:px-8">
        <div class="flex min-w-0 items-center gap-3">
          <div class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-[20px] leading-none">event_note</span>
          </div>
          <div class="min-w-0">
            <h3 class="truncate text-sm font-bold text-slate-900">日程详情</h3>
            <p class="truncate text-[11px] font-bold uppercase tracking-widest text-slate-400">
              {{ schedule.typeName || '日程' }}
            </p>
          </div>
        </div>
        <div class="flex shrink-0 items-center gap-2">
          <button
            v-if="allowEdit"
            :disabled="deleting"
            class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200 hover:text-primary disabled:opacity-50"
            type="button"
            aria-label="编辑日程"
            title="编辑日程"
            @click="handleEditSchedule"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
          </button>
          <button
            v-if="allowDelete"
            :disabled="deleting"
            class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-red-50 hover:text-red-500 disabled:opacity-50"
            type="button"
            aria-label="删除日程"
            title="删除日程"
            @click="handleDeleteSchedule"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
          </button>
          <button
            class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200"
            type="button"
            aria-label="关闭日程详情"
            title="关闭"
            @click="visible = false"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">close</span>
          </button>
        </div>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto px-8 pb-8 pt-8">
        <h2 class="mb-8 text-2xl font-bold leading-tight text-slate-900">{{ schedule.title }}</h2>

        <div class="mb-8 grid grid-cols-2 gap-4">
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="min-w-0">
              <div class="mb-2 flex items-center gap-2 text-slate-400 transition-colors group-hover:text-primary">
                <span class="material-symbols-outlined text-[18px] leading-none">schedule</span>
                <p class="text-[11px] font-bold uppercase tracking-wider">开始时间</p>
              </div>
              <p class="text-sm text-[#0d0d0d]">{{ formatDateTime(schedule.startTime) }}</p>
            </div>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="min-w-0">
              <div class="mb-2 flex items-center gap-2 text-slate-400 transition-colors group-hover:text-primary">
                <span class="material-symbols-outlined text-[18px] leading-none">schedule</span>
                <p class="text-[11px] font-bold uppercase tracking-wider">结束时间</p>
              </div>
              <p class="text-sm text-[#0d0d0d]">
                {{ schedule.endTime ? formatEndDateTime(schedule.startTime, schedule.endTime) : '未填写' }}
              </p>
            </div>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="min-w-0">
              <div class="mb-2 flex items-center gap-2 text-slate-400 transition-colors group-hover:text-primary">
                <span class="material-symbols-outlined text-[18px] leading-none">label</span>
                <p class="text-[11px] font-bold uppercase tracking-wider">类型</p>
              </div>
              <p class="break-words text-sm text-[#0d0d0d]">{{ schedule.typeName || '未填写' }}</p>
            </div>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="min-w-0">
              <div class="mb-2 flex items-center gap-2 text-slate-400 transition-colors group-hover:text-primary">
                <span class="material-symbols-outlined text-[18px] leading-none">location_on</span>
                <p class="text-[11px] font-bold uppercase tracking-wider">地点</p>
              </div>
              <p class="break-words text-sm text-[#0d0d0d]">{{ schedule.location || '未填写' }}</p>
            </div>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="min-w-0">
              <div class="mb-2 flex items-center gap-2 text-slate-400 transition-colors group-hover:text-primary">
                <span class="material-symbols-outlined text-[18px] leading-none">person</span>
                <p class="text-[11px] font-bold uppercase tracking-wider">创建人</p>
              </div>
              <p class="break-words text-sm text-[#0d0d0d]">{{ displayCreateUserName }}</p>
            </div>
          </div>
          <div
            class="group rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors hover:border-primary/20"
          >
            <div class="min-w-0">
              <div class="mb-2 flex items-center gap-2 text-slate-400 transition-colors group-hover:text-primary">
                <span class="material-symbols-outlined text-[18px] leading-none">calendar_clock</span>
                <p class="text-[11px] font-bold uppercase tracking-wider">创建时间</p>
              </div>
              <p class="break-words text-sm text-[#0d0d0d]">{{ displayCreateTime }}</p>
            </div>
          </div>
        </div>

        <div class="space-y-8">
          <section v-if="schedule.customerName">
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px] text-slate-400">corporate_fare</span>
              <h3 class="text-[11px] font-bold uppercase tracking-wider text-slate-400">关联客户</h3>
            </div>
            <div
              class="flex items-center gap-4 rounded-2xl border border-slate-100 bg-slate-50 p-4 transition-colors"
              :class="schedule.customerId ? 'cursor-pointer hover:bg-slate-50' : ''"
              @click="handleGoToCustomerDetail"
            >
              <div class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white text-sm font-bold text-primary shadow-sm">
                {{ schedule.customerName.charAt(0) }}
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-bold text-slate-900">{{ schedule.customerName }}</p>
                <p v-if="schedule.contactName" class="truncate text-xs text-slate-400">{{ schedule.contactName }}</p>
              </div>
              <span class="material-symbols-outlined ml-auto text-slate-300">chevron_right</span>
            </div>
          </section>

          <section v-if="participantsLine" class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
            <p class="mb-0.5 text-[11px] font-bold uppercase tracking-wider text-slate-400">参与人</p>
            <p class="whitespace-pre-wrap text-sm font-medium leading-relaxed text-slate-700">
              {{ participantsLine }}
            </p>
          </section>

          <section v-if="schedule.description">
            <div class="rounded-2xl border border-slate-100 bg-slate-50 p-4">
              <p class="mb-0.5 text-[11px] font-bold uppercase tracking-wider text-slate-400">备注说明</p>
              <p class="whitespace-pre-wrap text-sm font-medium leading-relaxed text-slate-700">{{ schedule.description }}</p>
            </div>
          </section>
        </div>
      </div>

      <div v-if="allowDelete" class="border-t border-slate-100 bg-white p-6">
        <button
          :disabled="deleting"
          class="w-full rounded-xl bg-red-50 py-3 text-sm font-bold text-red-600 transition-colors hover:bg-red-100 disabled:opacity-50"
          type="button"
          @click="handleDeleteSchedule"
        >
          {{ deleting ? '删除中...' : '删除日程' }}
        </button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteSchedule, type ScheduleVO } from '@/api/schedule'

const props = withDefaults(defineProps<{
  modelValue: boolean
  schedule: ScheduleVO | null
  isMobile?: boolean
  canEdit?: boolean
  canDelete?: boolean
}>(), {
  isMobile: false,
  canEdit: true,
  canDelete: true
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'edit', schedule: ScheduleVO): void
  (e: 'deleted', scheduleId: string): void
}>()

const router = useRouter()
const deleting = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const isMobile = computed(() => !!props.isMobile)
const allowEdit = computed(() => props.canEdit)
const allowDelete = computed(() => props.canDelete)

const displayCreateTime = computed(() => {
  const t = props.schedule?.createTime
  if (!t) return '未知'
  return formatDateTime(t)
})

const displayCreateUserName = computed(() => {
  const name = props.schedule?.createUserName?.trim()
  return name || '未知'
})

/** 参与人一行展示：优先结构化列表用逗号拼接，否则用 participantNames */
const participantsLine = computed(() => {
  const s = props.schedule
  if (!s) return ''
  if (s.participantUsers?.length) {
    return s.participantUsers
      .map((u) => (u.realname || u.username || '').trim())
      .filter(Boolean)
      .join(', ')
  }
  return (s.participantNames || '').trim()
})

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function formatEndDateTime(startDateStr: string, endDateStr: string): string {
  if (!endDateStr) return ''
  if (!startDateStr) return formatDateTime(endDateStr)

  const startDate = new Date(startDateStr)
  const endDate = new Date(endDateStr)
  const isSameDay = startDate.getFullYear() === endDate.getFullYear()
    && startDate.getMonth() === endDate.getMonth()
    && startDate.getDate() === endDate.getDate()

  return isSameDay ? formatTime(endDateStr) : formatDateTime(endDateStr)
}

function handleGoToCustomerDetail() {
  if (!props.schedule?.customerId) return
  const href = router.resolve({ path: `/customer/${props.schedule.customerId}` }).href
  window.open(href, '_blank', 'noopener,noreferrer')
}

function handleEditSchedule() {
  if (!allowEdit.value || !props.schedule || deleting.value) return
  emit('edit', props.schedule)
}

async function handleDeleteSchedule() {
  if (!allowDelete.value || !props.schedule || deleting.value) return

  try {
    await ElMessageBox.confirm('确定删除该日程？', '提示', { type: 'warning' })
    deleting.value = true
    await deleteSchedule(props.schedule.scheduleId)
    ElMessage.success('日程已删除')
    emit('deleted', props.schedule.scheduleId)
    visible.value = false
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Delete schedule failed:', error)
    }
  } finally {
    deleting.value = false
  }
}
</script>

<style>
.schedule-detail-drawer__body {
  padding: 0 !important;
}
</style>
