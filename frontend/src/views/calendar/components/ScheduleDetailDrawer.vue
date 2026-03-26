<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    :size="'400px'"
    :with-header="false"
    :modal="false"
    :lock-scroll="false"
    body-class="schedule-detail-drawer__body"
    modal-penetrable
    class="schedule-detail-drawer"
  >
    <div v-if="schedule" class="h-full flex flex-col bg-white shadow-2xl">
      <div class="flex items-center justify-between border-b border-slate-100 p-6">
        <span class="rounded-full bg-primary/10 px-3 py-1 text-xs font-bold uppercase tracking-widest text-primary">
          日程详情
        </span>
        <div class="flex items-center gap-2">
          <button
            :disabled="deleting"
            class="size-9 rounded-full text-slate-400 transition-colors hover:bg-red-50 hover:text-red-500 disabled:opacity-50"
            type="button"
            aria-label="删除日程"
            title="删除日程"
            @click="handleDeleteSchedule"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
          </button>
          <button
            class="size-9 rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
            type="button"
            aria-label="关闭日程详情"
            title="关闭"
            @click="visible = false"
          >
            <span class="material-symbols-outlined text-xl leading-none">close</span>
          </button>
        </div>
      </div>

      <div class="flex-1 min-h-0 overflow-y-auto p-8">
        <h2 class="mb-2 text-2xl font-bold leading-tight text-slate-900">{{ schedule.title }}</h2>

        <div class="mb-8 flex flex-wrap items-center gap-4 text-sm text-slate-500">
          <div class="flex items-center gap-1">
            <span class="material-symbols-outlined text-sm">schedule</span>
            {{ formatDateTime(schedule.startTime) }}
            <template v-if="schedule.endTime"> ~ {{ formatEndDateTime(schedule.startTime, schedule.endTime) }}</template>
          </div>
          <div v-if="schedule.location" class="flex items-center gap-1">
            <span class="material-symbols-outlined text-sm">location_on</span>
            <span class="break-words">{{ schedule.location }}</span>
          </div>
          <div v-if="schedule.typeName" class="flex items-center gap-1">
            <span class="material-symbols-outlined text-sm">label</span>
            {{ schedule.typeName }}
          </div>
        </div>

        <div class="space-y-8">
          <section v-if="schedule.customerName">
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px] text-slate-400">corporate_fare</span>
              <h3 class="text-xs font-bold uppercase tracking-widest text-slate-400">关联客户</h3>
            </div>
            <div
              class="flex items-center gap-3 rounded-2xl border border-slate-200 bg-white p-4 transition-colors"
              :class="schedule.customerId ? 'cursor-pointer hover:bg-slate-50' : ''"
              @click="handleGoToCustomerDetail"
            >
              <div class="flex size-10 items-center justify-center rounded-xl bg-primary/10 font-bold text-primary">
                {{ schedule.customerName.charAt(0) }}
              </div>
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-bold text-slate-900">{{ schedule.customerName }}</p>
                <p v-if="schedule.contactName" class="truncate text-xs text-slate-400">{{ schedule.contactName }}</p>
              </div>
              <span class="material-symbols-outlined ml-auto text-slate-300">chevron_right</span>
            </div>
          </section>

          <section v-if="schedule.participantUsers?.length || schedule.participantNames">
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px] text-slate-400">group</span>
              <h3 class="text-xs font-bold uppercase tracking-widest text-slate-400">参与人</h3>
            </div>
            <div class="rounded-2xl border border-slate-100 bg-slate-50 p-5">
              <div v-if="schedule.participantUsers?.length" class="flex flex-wrap gap-2">
                <div
                  v-for="user in schedule.participantUsers"
                  :key="user.userId"
                  class="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-sm text-slate-700"
                >
                  <span class="flex size-6 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                    {{ (user.realname || user.username || '?').charAt(0) }}
                  </span>
                  <span class="font-medium">{{ user.realname || user.username }}</span>
                </div>
              </div>
              <p v-else class="whitespace-pre-wrap text-sm leading-relaxed text-slate-600">{{ schedule.participantNames }}</p>
            </div>
          </section>

          <section v-if="schedule.description">
            <div class="mb-4 flex items-center gap-2">
              <span class="material-symbols-outlined text-[18px] text-slate-400">notes</span>
              <h3 class="text-xs font-bold uppercase tracking-widest text-slate-400">备注说明</h3>
            </div>
            <div class="rounded-2xl border border-slate-100 bg-slate-50 p-5">
              <p class="whitespace-pre-wrap text-sm leading-relaxed text-slate-600">{{ schedule.description }}</p>
            </div>
          </section>
        </div>
      </div>

      <div class="border-t border-slate-100 bg-white p-6">
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

const props = defineProps<{
  modelValue: boolean
  schedule: ScheduleVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'deleted', scheduleId: string): void
}>()

const router = useRouter()
const deleting = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
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

async function handleGoToCustomerDetail() {
  if (!props.schedule?.customerId) return
  visible.value = false
  await router.push(`/customer/${props.schedule.customerId}`)
}

async function handleDeleteSchedule() {
  if (!props.schedule || deleting.value) return

  try {
    await ElMessageBox.confirm('确定删除该日程？', '提示', { type: 'warning' })
    deleting.value = true
    await deleteSchedule(props.schedule.scheduleId)
    ElMessage.success('日程已删除')
    emit('deleted', props.schedule.scheduleId)
    visible.value = false
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error('删除日程失败')
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
