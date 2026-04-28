<template>
  <div class="flex h-full bg-background-light">
    <!-- Calendar Main -->
    <div class="flex-1 p-4 md:p-8 overflow-y-auto" :class="{ 'border-r border-slate-100': selectedEvent || selectedTask }">
      <div class="w-full space-y-6">
        <!-- Header -->
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:gap-6 min-w-0">
            <div class="min-w-0">
              <h2 class="text-2xl font-bold text-slate-900">智能日程安排</h2>
              <p class="text-sm text-slate-500 mt-1">{{ currentDateStr }} • 今天有 {{ todayScheduleCount }} 场会议和 {{ todayTaskCount }} 个待办任务</p>
            </div>
            <div class="flex items-center gap-2 bg-white border border-slate-200 rounded-xl p-1 shrink-0">
              <button
                type="button"
                class="size-8 flex items-center justify-center rounded-lg hover:bg-slate-50 text-slate-600 transition-colors"
                aria-label="上一段"
                @click="shiftCalendarAnchor(-1)"
              >
                <span class="material-symbols-outlined text-[20px]">chevron_left</span>
              </button>
              <button
                type="button"
                class="px-3 py-1 text-xs font-bold text-slate-600 hover:text-primary transition-colors"
                @click="goCalendarToday"
              >
                今天
              </button>
              <button
                type="button"
                class="size-8 flex items-center justify-center rounded-lg hover:bg-slate-50 text-slate-600 transition-colors"
                aria-label="下一段"
                @click="shiftCalendarAnchor(1)"
              >
                <span class="material-symbols-outlined text-[20px]">chevron_right</span>
              </button>
            </div>
          </div>
          <div class="flex items-center gap-4 flex-wrap">
            <div class="flex items-center bg-slate-50 p-1 rounded-lg border border-slate-200">
              <button
                v-for="mode in viewModes"
                :key="mode.value"
                type="button"
                @click="viewMode = mode.value"
                class="px-5 py-1.5 text-sm font-medium rounded-md transition-colors"
                :class="viewMode === mode.value
                  ? 'bg-white text-primary'
                  : 'text-slate-600 hover:text-slate-900'"
              >{{ mode.label }}</button>
            </div>
            <button
              type="button"
              @click="openCreateScheduleDialog"
              class="px-6 py-2.5 bg-primary text-white text-sm font-bold rounded-xl hover:bg-primary/90 transition-colors shadow-sm flex items-center gap-2"
            >
              <span class="material-symbols-outlined wk-plus-button-icon">add</span>
              新增日程
            </button>
          </div>
        </div>

        <!-- Calendar Views -->
        <div class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm relative">
          <Transition name="wk-cal-view" mode="out-in">
          <!-- Week View -->
          <div v-if="viewMode === 'grid'" key="grid" class="grid grid-cols-7 divide-x divide-slate-200 min-h-[180px]">
            <div
              v-for="day in weekDays"
              :key="day.label"
              class="flex flex-col bg-white"
            >
              <div class="p-3 sm:p-5 flex items-center justify-between border-b border-slate-100">
                <div class=" sm:flex sm:flex-col">
                  <span class="text-sm font-medium" :class="day.isToday ? 'text-primary' : 'text-slate-400'">
                    {{ isMobile ? day.label.replace('周', '') : day.label }}
                  </span>
                  <span class="text-xs text-slate-400">
                    {{ isMobile ? getLunarDayText(day.fullDate) : getLunarText(day.fullDate) }}
                  </span>
                </div>
                <span
                  class="size-7 shrink-0 flex items-center justify-center rounded-full text-sm font-bold"
                  :class="day.isToday ? 'bg-primary text-white' : 'text-slate-900'"
                >{{ day.date }}</span>
              </div>
              <div
                class="flex-1 p-3 space-y-3"
                :class="isMobile && (getEventsForDate(day.fullDate).length || getTasksForDate(day.fullDate).length) ? 'cursor-pointer' : ''"
                @click="openMobileEventsDialog(day.fullDate)"
              >
                <div v-if="isMobile" class="flex h-full items-center justify-center">
                  <span
                    v-if="getEventsForDate(day.fullDate).length || getTasksForDate(day.fullDate).length"
                    class="size-2 rounded-full bg-primary/70"
                    aria-hidden="true"
                  />
                </div>
                <template v-else>
                  <div
                    v-for="event in getEventsForDate(day.fullDate)"
                    :key="event.scheduleId"
                    @click="selectedEvent = event; selectedTask = null"
                    class="p-[3px] sm:p-3 rounded-xl border border-primary/20 bg-primary/5 shadow-sm hover:shadow-md hover:bg-primary/10 transition-all cursor-pointer"
                  >
                    <p class="text-xs font-bold text-primary mb-1 truncate">{{ event.title }}</p>
                    <p class="text-xs text-slate-500 truncate">{{ formatTime(event.startTime) }} • {{ event.customerName || event.participantNames || '' }}</p>
                  </div>
                  <!-- Tasks -->
                  <div
                    v-for="task in getTasksForDate(day.fullDate)"
                    :key="task.taskId"
                    class="p-3 rounded-xl border border-slate-200 bg-white shadow-sm transition-all flex items-start gap-2"
                  >
                    <button
                      @click.stop="handleToggleTask(task)"
                      class="mt-0.5 shrink-0 size-4 rounded-sm border flex items-center justify-center transition-colors"
                      :class="task.status === 'COMPLETED'
                        ? 'bg-emerald-500 border-emerald-500 text-white'
                        : 'border-slate-300 hover:border-primary text-transparent hover:text-primary/20'"
                    >
                      <span class="material-symbols-outlined text-[12px] font-bold">check</span>
                    </button>
                    <div class="min-w-0 flex-1" @click="selectTask(task)">
                      <p class="text-xs font-bold mb-1 truncate" :class="task.status === 'COMPLETED' ? 'text-slate-500 line-through' : 'text-slate-700'">
                        {{ task.title }}
                      </p>
                      <p class="text-xs text-slate-400 truncate">{{ task.customerName || '' }}</p>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <!-- Month View -->
          <div v-else-if="viewMode === 'month'" key="month" class="min-h-[400px] flex flex-col">
            <div class="grid grid-cols-7 border-b border-slate-200 bg-slate-50">
              <div
                v-for="dayLabel in ['周一','周二','周三','周四','周五','周六','周日']"
                :key="dayLabel"
                class="py-3 text-center text-xs font-bold text-slate-500 uppercase tracking-wider"
              >
                {{ dayLabel }}
              </div>
            </div>
            <div class="flex-1 grid grid-cols-7 grid-rows-5 divide-x divide-y divide-slate-100">
              <div
                v-for="(cell, i) in monthCells"
                :key="i"
                class="min-h-[30px] p-2"
                :class="{ 'bg-slate-50/50': !cell.isCurrentMonth }"
              >
                <div class="flex justify-between items-start mb-1">
                  <div class="flex flex-col gap-0.5 item-center w-full text-center">
                    <span
                      class="size-6 flex items-center justify-center rounded-full w-full text-xs font-medium"
                      :class="cell.isToday
                        ? 'bg-primary text-white font-bold'
                        : !cell.isCurrentMonth ? 'text-slate-300' : 'text-slate-700'"
                    >
                      {{ cell.isCurrentMonth ? cell.date : '' }}
                    </span>
                    <span v-if="cell.isCurrentMonth && cell.fullDate && !isMobile" class="text-xs text-slate-400 leading-none">
                      {{ getLunarText(cell.fullDate) }}
                    </span>
                  </div>
                </div>
                <div
                  v-if="cell.fullDate"
                  class="space-y-1"
                  :class="isMobile && (getEventsForDate(cell.fullDate).length || getTasksForDate(cell.fullDate).length) ? 'cursor-pointer' : ''"
                  @click="handleMobileMonthCellClick(cell.fullDate)"
                >
                  <div
                    v-if="isMobile"
                    class="flex items-center justify-center"
                  >
                    <span
                      v-if="getEventsForDate(cell.fullDate).length || getTasksForDate(cell.fullDate).length"
                      class="size-1.5 rounded-full bg-primary/70"
                      aria-hidden="true"
                    />
                  </div>
                  <template v-else>
                    <div
                      v-for="event in getEventsForDate(cell.fullDate)"
                      :key="event.scheduleId"
                      @click="selectedEvent = event; selectedTask = null"
                      class="px-2 py-1 text-xs font-medium bg-primary/10 text-primary rounded truncate cursor-pointer hover:bg-primary/20"
                    >
                      {{ formatTime(event.startTime) }} {{ event.title }}
                    </div>
                    <div
                      v-for="task in getTasksForDate(cell.fullDate)"
                      :key="task.taskId"
                      class="px-2 py-1 text-xs font-medium rounded truncate cursor-pointer flex items-center gap-1 bg-slate-50 text-slate-700 hover:bg-slate-100"
                    >
                      <div
                        class="shrink-0 size-3 rounded-sm border flex items-center justify-center transition-colors"
                        :class="task.status === 'COMPLETED'
                          ? 'bg-emerald-500 border-emerald-500 text-white'
                          : 'border-slate-300 text-transparent'"
                        @click.stop="handleToggleTask(task)"
                      >
                        <span class="material-symbols-outlined text-xs font-bold">check</span>
                      </div>
                      <span
                        @click="selectTask(task)"
                        class="truncate"
                        :class="task.status === 'COMPLETED' ? 'line-through text-slate-500' : ''"
                      >{{ task.title }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>

          <!-- List View -->
          <div v-else key="list" class="p-6">
            <div class="max-w-3xl mx-auto space-y-8">
              <div v-if="listDayGroups.length === 0" class="text-center py-20 text-slate-400">
                <span class="material-symbols-outlined text-4xl mb-2">calendar_today</span>
                <p class="text-sm">{{ schedules.length === 0 && tasks.length === 0 ? '暂无日程安排和待办任务' : '当日暂无日程安排和待办任务' }}</p>
              </div>

              <div v-for="group in listDayGroups" :key="group.dateStr" class="space-y-4">
                <h3 class="text-sm font-bold text-slate-900 flex items-center gap-2">
                  <span class="size-2 rounded-full bg-primary"></span>
                  {{ group.header }}
                  <span class="text-xs font-normal text-slate-400 ml-2">农历 {{ group.lunar }}</span>
                </h3>

                <div class="space-y-4 ml-4 border-l-2 border-slate-100 pl-6">
                  <div
                    v-for="item in group.items"
                    :key="item.key"
                    class="relative bg-white border border-slate-200 rounded-xl p-4 hover:shadow-md transition-all group"
                    :class="item.kind === 'schedule' ? 'cursor-pointer' : ''"
                    @click="item.kind === 'schedule' ? (selectedEvent = item.payload, selectedTask = null) : selectTask(item.payload)"
                  >
                    <div
                      class="absolute -left-[32px] top-5 size-3 bg-white border-2 rounded-full"
                      :class="item.kind === 'schedule' ? 'border-primary' : 'border-slate-300'"
                    ></div>

                    <div v-if="item.kind === 'schedule'" class="flex items-start justify-between gap-4">
                      <div class="min-w-0">
                        <h4 class="text-sm font-bold text-slate-900 mb-1 group-hover:text-primary transition-colors truncate">
                          {{ item.payload.title }}
                        </h4>
                        <div class="flex items-center gap-2 flex-wrap">
                          <span v-if="item.payload.customerName" class="text-xs text-slate-500 font-medium">{{ item.payload.customerName }}</span>
                          <span v-if="item.payload.typeName" class="text-xs px-2 py-0.5 bg-primary/10 text-primary rounded-full font-bold">{{ item.payload.typeName }}</span>
                          <span v-if="item.payload.location" class="text-xs px-2 py-0.5 bg-slate-50 text-slate-600 rounded-full font-bold">{{ item.payload.location }}</span>
                        </div>
                      </div>
                      <div class="text-right shrink-0">
                        <span class="inline-block px-2 py-1 bg-slate-50 text-slate-600 text-xs font-bold rounded-lg">
                          {{ item.timeLabel }}
                        </span>
                      </div>
                    </div>

                    <div v-else class="flex items-start gap-3">
                      <button
                        @click.stop="handleToggleTask(item.payload)"
                        class="mt-0.5 shrink-0 size-5 rounded border flex items-center justify-center transition-colors"
                        :class="item.payload.status === 'COMPLETED'
                          ? 'bg-emerald-500 border-emerald-500 text-white'
                          : 'border-slate-300 hover:border-primary text-transparent hover:text-primary/20'"
                        aria-label="切换任务完成状态"
                        :title="item.payload.status === 'COMPLETED' ? '标记为未完成' : '标记为已完成'"
                      >
                        <span class="material-symbols-outlined text-[14px] font-bold">check</span>
                      </button>

                      <div class="min-w-0 flex-1" @click="selectTask(item.payload)">
                        <h4
                          class="text-sm font-bold mb-1 truncate"
                          :class="item.payload.status === 'COMPLETED' ? 'text-slate-400 line-through' : 'text-slate-900'"
                        >
                          {{ item.payload.title }}
                        </h4>
                        <div class="flex items-center gap-2 flex-wrap">
                          <span v-if="item.payload.customerName" class="text-xs text-slate-500">{{ item.payload.customerName }}</span>
                          <span v-if="item.payload.dueDate" class="text-xs px-2 py-0.5 bg-slate-50 text-slate-600 rounded-full font-bold">
                            截止 {{ formatDueDate(item.payload.dueDate) }}
                          </span>
                          <span
                            v-if="item.payload.priority"
                            class="text-xs px-2 py-0.5 rounded-full font-bold"
                            :class="{
                              'bg-red-50 text-red-500': normalizeTaskPriority(item.payload.priority) === 'HIGH',
                              'bg-amber-50 text-amber-500': normalizeTaskPriority(item.payload.priority) === 'MEDIUM',
                              'bg-slate-100 text-slate-500': normalizeTaskPriority(item.payload.priority) === 'LOW',
                            }"
                          >{{ getTaskPriorityLabel(item.payload.priority) }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          </Transition>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showMobileMonthEventsDialog"
      :width="isMobile ? '92%' : '520px'"
      top="12vh"
      :close-on-click-modal="true"
      :append-to-body="true"
      class="wk-dialog--flush"
    >
      <template #header>
        <div class="flex items-center justify-between gap-3 pr-2">
          <div class="min-w-0">
            <p class="text-base font-bold text-slate-900 truncate">
              {{ mobileMonthEventsDialogDate ? `${mobileMonthEventsDialogDate} 日程` : '日程' }}
            </p>
            <p v-if="mobileMonthEventsDialogDate" class="text-xs text-slate-400 mt-0.5">
              农历 {{ getLunarText(mobileMonthEventsDialogDate) }}
            </p>
          </div>
        </div>
      </template>

      <div class="max-h-[60vh] overflow-auto pb-[env(safe-area-inset-bottom)]">
        <div
          v-if="mobileMonthEvents.length === 0 && mobileMonthTasks.length === 0"
          class="py-10 text-center text-sm text-slate-400"
        >
          当天暂无日程与任务
        </div>
        <div v-else class="space-y-5">
          <section v-if="mobileMonthEvents.length" class="space-y-2">
            <div class="flex items-center justify-between px-1">
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">日程</p>
              <p class="text-xs font-bold text-slate-300">{{ mobileMonthEvents.length }}</p>
            </div>
            <button
              v-for="event in mobileMonthEvents"
              :key="event.scheduleId"
              type="button"
              class="w-full text-left rounded-xl border border-slate-200 bg-white px-3 py-2.5 hover:bg-slate-50 transition-colors"
              @click="handleSelectEventFromMobileDialog(event)"
            >
              <p class="text-sm font-bold text-slate-900 truncate">{{ event.title }}</p>
              <p class="mt-0.5 text-xs text-slate-500 truncate">
                {{ formatTime(event.startTime) }}
                <template v-if="event.customerName || event.participantNames">
                  • {{ event.customerName || event.participantNames }}
                </template>
              </p>
            </button>
          </section>

          <section v-if="mobileMonthTasks.length" class="space-y-2">
            <div class="flex items-center justify-between px-1">
              <p class="text-xs font-bold text-slate-400 uppercase tracking-wider">任务</p>
              <p class="text-xs font-bold text-slate-300">{{ mobileMonthTasks.length }}</p>
            </div>
            <button
              v-for="task in mobileMonthTasks"
              :key="task.taskId"
              type="button"
              class="w-full text-left rounded-xl border border-slate-200 bg-white px-3 py-2.5 hover:bg-slate-50 transition-colors"
              @click="handleSelectTaskFromMobileDialog(task)"
            >
              <p class="text-sm font-bold text-slate-900 truncate">{{ task.title }}</p>
              <p class="mt-0.5 text-xs text-slate-500 truncate">
                <template v-if="task.customerName">{{ task.customerName }}</template>
                <template v-else>—</template>
              </p>
            </button>
          </section>
        </div>
      </div>
    </el-dialog>

    <ScheduleDetailDrawer
      v-model="showScheduleDetailDrawer"
      :schedule="selectedEvent"
      :is-mobile="isMobile"
      @edit="handleEditScheduleFromDetail"
      @deleted="handleScheduleDeleted"
    />

    <TaskDetailDrawer
      v-model="showCalendarTaskDetail"
      :task="selectedTask"
      :is-mobile="isMobile"
      @edit="handleEditFromDetail"
      @mutated="handleCalendarTaskDetailMutated"
    />

    <TaskEditDialog
      v-model="showTaskEditDialog"
      :editing-task="editingTask"
      @saved="handleTaskSaved"
    />

    <ScheduleFormDialog
      v-model="showAddDialog"
      :editing-schedule="editingSchedule"
      @created="handleScheduleCreated"
      @updated="handleScheduleUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useResponsive } from '@/composables/useResponsive'
import { getMySchedules, queryScheduleList } from '@/api/schedule'
import { getMyTasks, updateTaskStatus } from '@/api/task'
import type { ScheduleVO } from '@/api/schedule'
import type { Task } from '@/types/common'
import { normalizeTaskPriority } from '@/utils/taskPriority'
import TaskDetailDrawer from '@/views/task/components/TaskDetailDrawer.vue'
import TaskEditDialog from '@/views/task/components/TaskEditDialog.vue'
import ScheduleDetailDrawer from './components/ScheduleDetailDrawer.vue'
import ScheduleFormDialog from './components/ScheduleFormDialog.vue'

const { isMobile } = useResponsive()
const route = useRoute()
const router = useRouter()

function toDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const lunarFormatter = new Intl.DateTimeFormat('zh-Hans-u-ca-chinese', {
  month: 'short',
  day: 'numeric'
})

function getLunarText(dateStr: string): string {
  try {
    if (!dateStr) return ''
    const d = new Date(dateStr)
    if (Number.isNaN(d.getTime())) return ''
    // Example output (depends on runtime): "二月18" / "闰二月18"
    return lunarFormatter.format(d)
  } catch {
    return ''
  }
}

function getLunarDayText(dateStr: string): string {
  const text = getLunarText(dateStr)
  if (!text) return ''
  return text.replace(/^(闰)?[一二三四五六七八九十冬腊\d]+月/, '')
}

function openMobileEventsDialog(dateStr: string) {
  if (!isMobile.value) return
  const events = getEventsForDate(dateStr)
  const dayTasks = getTasksForDate(dateStr)
  if (!events.length && !dayTasks.length) return
  mobileMonthEventsDialogDate.value = dateStr
  showMobileMonthEventsDialog.value = true
}

function handleMobileMonthCellClick(dateStr: string) {
  openMobileEventsDialog(dateStr)
}

function handleSelectEventFromMobileDialog(event: ScheduleVO) {
  selectedEvent.value = event
  selectedTask.value = null
}

function handleSelectTaskFromMobileDialog(task: Task) {
  selectedTask.value = task
  selectedEvent.value = null
}

const showScheduleDetailDrawer = computed({
  get: () => !!selectedEvent.value,
  set: (val: boolean) => {
    if (!val) selectedEvent.value = null
  }
})

const showCalendarTaskDetail = computed({
  get: () => !!selectedTask.value,
  set: (val: boolean) => {
    if (!val) selectedTask.value = null
  }
})

const viewMode = ref<'grid' | 'month' | 'list'>('grid')
const selectedEvent = ref<ScheduleVO | null>(null)
const editingSchedule = ref<ScheduleVO | null>(null)
const selectedTask = ref<Task | null>(null)
const schedules = ref<ScheduleVO[]>([])
const tasks = ref<Task[]>([])
const showTaskEditDialog = ref(false)
const editingTask = ref<Task | null>(null)
const showMobileMonthEventsDialog = ref(false)
const mobileMonthEventsDialogDate = ref<string | null>(null)

const viewModes = [
  { value: 'grid' as const, label: '周' },
  { value: 'month' as const, label: '月' },
  { value: 'list' as const, label: '列表' },
]

const mobileMonthEvents = computed(() => {
  if (!mobileMonthEventsDialogDate.value) return []
  return getEventsForDate(mobileMonthEventsDialogDate.value)
})

const mobileMonthTasks = computed(() => {
  if (!mobileMonthEventsDialogDate.value) return []
  return getTasksForDate(mobileMonthEventsDialogDate.value)
})

const calendarAnchorDate = ref<Date>(new Date())
const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function shiftCalendarAnchor(direction: number) {
  const d = new Date(calendarAnchorDate.value)
  if (viewMode.value === 'grid') {
    d.setDate(d.getDate() + 7 * direction)
  } else if (viewMode.value === 'month') {
    d.setMonth(d.getMonth() + direction)
  } else {
    d.setDate(d.getDate() + direction)
  }
  calendarAnchorDate.value = d
}

function goCalendarToday() {
  calendarAnchorDate.value = new Date()
}

const weekDays = computed(() => {
  const anchor = calendarAnchorDate.value
  const dow = anchor.getDay()
  const mondayOffset = dow === 0 ? -6 : 1 - dow
  const todayStr = toDateStr(new Date())
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(anchor)
    d.setDate(anchor.getDate() + mondayOffset + i)
    return {
      label: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][i],
      date: d.getDate(),
      fullDate: toDateStr(d),
      isToday: toDateStr(d) === todayStr
    }
  })
})

const monthCells = computed(() => {
  const anchor = calendarAnchorDate.value
  const year = anchor.getFullYear()
  const month = anchor.getMonth()
  const todayStr = toDateStr(new Date())
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDow = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1
  const totalDays = lastDay.getDate()
  const cells: {
    date: number
    isCurrentMonth: boolean
    isToday: boolean
    fullDate: string
  }[] = []
  for (let i = 0; i < 35; i++) {
    const date = i - startDow + 1
    const isCurrentMonth = date > 0 && date <= totalDays
    const cellDate = isCurrentMonth ? new Date(year, month, date) : null
    cells.push({
      date: isCurrentMonth ? date : 0,
      isCurrentMonth,
      isToday: !!(cellDate && toDateStr(cellDate) === todayStr),
      fullDate: cellDate ? toDateStr(cellDate) : ''
    })
  }
  return cells
})

const currentDateStr = computed(() => {
  const d = calendarAnchorDate.value
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  const day = d.getDate()
  const dayName = dayNames[d.getDay()] ?? ''
  if (viewMode.value === 'month') {
    return `${y}年${m}月`
  }
  if (viewMode.value === 'list') {
    return `${y}年${m}月${day}日，${dayName}`
  }
  const days = weekDays.value
  if (days.length >= 7) {
    const s = new Date(days[0].fullDate + 'T12:00:00')
    const e = new Date(days[6].fullDate + 'T12:00:00')
    const sm = s.getMonth() + 1
    const sd = s.getDate()
    const em = e.getMonth() + 1
    const ed = e.getDate()
    return `${s.getFullYear()}年${sm}月${sd}日 — ${e.getFullYear()}年${em}月${ed}日，本周`
  }
  return `${y}年${m}月${day}日，${dayName}`
})

const todayScheduleCount = computed(() => {
  const todayStr = toDateStr(new Date())
  return schedules.value.filter(e => toDateStr(new Date(e.startTime)) === todayStr).length
})

const todayTaskCount = computed(() => {
  const todayStr = toDateStr(new Date())
  return tasks.value.filter(t => t.status !== 'COMPLETED' && normalizeDueDate(t.dueDate ?? '') === todayStr).length
})

// --- Data Loading ---

async function loadSchedules() {
  try {
    schedules.value = await getMySchedules('all')
    syncSelectedSchedule()
  } catch (e) {
    console.error('加载日程失败', e)
  }
}

async function loadTasks() {
  try {
    tasks.value = await getMyTasks('all')
    syncSelectedTask()
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

onMounted(async () => {
  await Promise.all([loadSchedules(), loadTasks()])

  if (typeof route.query.openScheduleId === 'string') {
    await openScheduleFromRouteQuery(route.query.openScheduleId)
  }
})

watch(
  () => route.query.openScheduleId,
  (scheduleId) => {
    if (typeof scheduleId === 'string') {
      void openScheduleFromRouteQuery(scheduleId)
    }
  }
)

async function openScheduleFromRouteQuery(scheduleId: string) {
  try {
    const currentSchedule = schedules.value.find(item => item.scheduleId === scheduleId)
    if (currentSchedule) {
      selectedTask.value = null
      selectedEvent.value = currentSchedule
      return
    }

    const result = await queryScheduleList({ scheduleId, page: 1, limit: 1 })
    const schedule = result.list?.[0]
    if (schedule) {
      selectedTask.value = null
      selectedEvent.value = schedule
    }
  } catch (error) {
    console.error('Load schedule from route failed:', error)
  } finally {
    const nextQuery = { ...route.query }
    delete nextQuery.openScheduleId
    await router.replace({ path: route.path, query: nextQuery })
  }
}

// --- Helpers ---

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function getEventsForDate(dateStr: string): ScheduleVO[] {
  return schedules.value.filter(e => toDateStr(new Date(e.startTime)) === dateStr)
}

function normalizeDueDate(dueDate: string): string {
  if (!dueDate) return ''
  // dueDate could be "yyyy-MM-dd" or ISO datetime string
  if (dueDate.length === 10) return dueDate
  return toDateStr(new Date(dueDate))
}

function getTasksForDate(dateStr: string): Task[] {
  return tasks.value.filter(t => normalizeDueDate(t.dueDate ?? '') === dateStr)
}

async function handleToggleTask(task: Task) {
  const newStatus = task.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED'
  try {
    await updateTaskStatus(task.taskId, newStatus.toLowerCase())
    await loadTasks()
  } catch (error) {
    console.error('Update task status failed:', error)
  }
}

function selectTask(task: Task) {
  selectedEvent.value = null
  selectedTask.value = task
}

function syncSelectedTask() {
  if (!selectedTask.value) return
  selectedTask.value = tasks.value.find(task => task.taskId === selectedTask.value?.taskId) || null
}

function syncSelectedSchedule() {
  if (!selectedEvent.value) return
  selectedEvent.value = schedules.value.find(schedule => schedule.scheduleId === selectedEvent.value?.scheduleId) || null
}

function formatDueDate(dueDate: string): string {
  if (!dueDate) return ''
  const d = new Date(dueDate)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const showAddDialog = ref(false)

watch(showAddDialog, value => {
  if (!value) {
    editingSchedule.value = null
  }
})

watch(showTaskEditDialog, value => {
  if (!value) {
    editingTask.value = null
  }
})

function openCreateScheduleDialog() {
  editingSchedule.value = null
  showAddDialog.value = true
}

function handleEditScheduleFromDetail(schedule: ScheduleVO) {
  editingSchedule.value = schedule
  showAddDialog.value = true
}

async function handleScheduleCreated() {
  await loadSchedules()
}

async function handleScheduleUpdated(scheduleId: string) {
  await loadSchedules()
  selectedEvent.value = schedules.value.find(schedule => schedule.scheduleId === scheduleId) || selectedEvent.value
}

async function handleScheduleDeleted() {
  await loadSchedules()
  selectedEvent.value = null
}

function handleEdit(task: Task) {
  editingTask.value = task
  showTaskEditDialog.value = true
}

function handleEditFromDetail(task: Task) {
  handleEdit(task)
  if (isMobile.value) selectedTask.value = null
}

async function handleCalendarTaskDetailMutated() {
  await loadTasks()
  syncSelectedTask()
}

async function handleTaskSaved() {
  editingTask.value = null
  await loadTasks()
  syncSelectedTask()
}

type ListItem =
  | { key: string; kind: 'schedule'; time: number; timeLabel: string; payload: ScheduleVO }
  | { key: string; kind: 'task'; time: number; timeLabel: string; payload: Task }

function getDayHeader(dateStr: string): string {
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = d.getMonth() + 1
  const day = d.getDate()
  const dayName = dayNames[d.getDay()] ?? ''
  const isToday = toDateStr(d) === toDateStr(new Date())
  return `${y}年${m}月${day}日，${dayName}${isToday ? ' (今天)' : ''}`
}

function getTaskPriorityLabel(priority: Task['priority']): string {
  const normalized = normalizeTaskPriority(priority)
  return normalized === 'HIGH' ? '高' : normalized === 'MEDIUM' ? '中' : '低'
}

const listGroups = computed(() => {
  const groups = new Map<string, ListItem[]>()

  for (const e of schedules.value) {
    const dateStr = toDateStr(new Date(e.startTime))
    const items = groups.get(dateStr) ?? []
    items.push({
      key: `schedule-${e.scheduleId}`,
      kind: 'schedule',
      time: new Date(e.startTime).getTime(),
      timeLabel: formatTime(e.startTime),
      payload: e
    })
    groups.set(dateStr, items)
  }

  for (const t of tasks.value) {
    const dateStr = normalizeDueDate(t.dueDate ?? '')
    if (!dateStr) continue
    const items = groups.get(dateStr) ?? []
    const dueTs = t.dueDate ? new Date(t.dueDate).getTime() : new Date(`${dateStr}T23:59:59`).getTime()
    items.push({
      key: `task-${t.taskId}`,
      kind: 'task',
      time: Number.isFinite(dueTs) ? dueTs : new Date(`${dateStr}T23:59:59`).getTime(),
      timeLabel: '待办',
      payload: t
    })
    groups.set(dateStr, items)
  }

  return Array.from(groups.entries())
    .map(([dateStr, items]) => ({
      dateStr,
      header: getDayHeader(dateStr),
      lunar: getLunarText(dateStr),
      items: items.slice().sort((a, b) => a.time - b.time)
    }))
    .sort((a, b) => new Date(a.dateStr).getTime() - new Date(b.dateStr).getTime())
})

const listDayGroups = computed(() => {
  const anchorStr = toDateStr(calendarAnchorDate.value)
  return listGroups.value.filter(g => g.dateStr === anchorStr)
})
</script>

<style scoped>
.wk-cal-view-enter-active,
.wk-cal-view-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.wk-cal-view-enter-from,
.wk-cal-view-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
