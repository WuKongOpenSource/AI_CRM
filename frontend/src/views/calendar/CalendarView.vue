<template>
  <div class="flex h-full bg-background-light">
    <!-- Calendar Main -->
    <div class="flex-1 p-4 md:p-8 overflow-y-auto" :class="{ 'border-r border-slate-100': selectedEvent || selectedTask }">
      <div class="w-full space-y-6">
        <!-- Header -->
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 class="text-2xl font-bold text-slate-900">智能日程安排</h2>
            <p class="text-sm text-slate-500 mt-1">{{ currentDateStr }} • 今天有 {{ todayScheduleCount }} 场会议和 {{ todayTaskCount }} 个待办任务</p>
          </div>
          <div class="flex items-center gap-4">
            <div class="flex items-center bg-slate-50 p-1 rounded-lg border border-slate-200">
              <button
                v-for="mode in viewModes"
                :key="mode.value"
                @click="viewMode = mode.value"
                class="px-5 py-1.5 text-sm font-medium rounded-md transition-colors"
                :class="viewMode === mode.value
                  ? 'bg-white text-primary shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'"
              >{{ mode.label }}</button>
            </div>
            <button
              @click="showAddDialog = true"
              class="px-6 py-2.5 bg-primary text-white text-sm font-bold rounded-xl hover:bg-primary/90 transition-colors shadow-sm flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-[18px]">add</span>
              新增日程
            </button>
          </div>
        </div>

        <!-- Calendar Views -->
        <div class="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm relative">
          <!-- Week View -->
          <div v-if="viewMode === 'grid'" class="grid grid-cols-7 divide-x divide-slate-200 min-h-[400px]">
            <div
              v-for="day in weekDays"
              :key="day.label"
              class="flex flex-col bg-white"
            >
              <div class="p-5 flex items-center justify-between border-b border-slate-100">
                <div class="flex flex-col">
                  <span class="text-sm font-medium" :class="day.isToday ? 'text-primary' : 'text-slate-400'">
                    {{ day.label }}
                  </span>
                  <span class="text-[10px] text-slate-400">
                    {{ getLunarText(day.fullDate) }}
                  </span>
                </div>
                <span
                  class="size-7 flex items-center justify-center rounded-full text-sm font-bold"
                  :class="day.isToday ? 'bg-primary text-white' : 'text-slate-900'"
                >{{ day.date }}</span>
              </div>
              <div class="flex-1 p-3 space-y-3">
                <div
                  v-for="event in getEventsForDate(day.fullDate)"
                  :key="event.scheduleId"
                  @click="selectedEvent = event; selectedTask = null"
                  class="p-3 rounded-xl border border-primary/20 bg-primary/5 shadow-sm hover:shadow-md hover:bg-primary/10 transition-all cursor-pointer"
                >
                  <p class="text-xs font-bold text-primary mb-1 truncate">{{ event.title }}</p>
                  <p class="text-[10px] text-slate-500 truncate">{{ formatTime(event.startTime) }} • {{ event.customerName || '' }}</p>
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
                    <p class="text-[10px] text-slate-400 truncate">{{ task.customerName || '' }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Month View -->
          <div v-else-if="viewMode === 'month'" class="min-h-[600px] flex flex-col">
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
                class="min-h-[120px] p-2"
                :class="{ 'bg-slate-50/50': !cell.isCurrentMonth }"
              >
                <div class="flex justify-between items-start mb-1">
                  <div class="flex flex-col gap-0.5">
                    <span
                      class="size-6 flex items-center justify-center rounded-full text-xs font-medium"
                      :class="cell.isToday
                        ? 'bg-primary text-white font-bold'
                        : !cell.isCurrentMonth ? 'text-slate-300' : 'text-slate-700'"
                    >
                      {{ cell.isCurrentMonth ? cell.date : '' }}
                    </span>
                    <span v-if="cell.isCurrentMonth && cell.fullDate" class="text-[10px] text-slate-400 leading-none">
                      {{ getLunarText(cell.fullDate) }}
                    </span>
                  </div>
                </div>
                <div v-if="cell.fullDate" class="space-y-1">
                  <div
                    v-for="event in getEventsForDate(cell.fullDate)"
                    :key="event.scheduleId"
                    @click="selectedEvent = event; selectedTask = null"
                    class="px-2 py-1 text-[10px] font-medium bg-primary/10 text-primary rounded truncate cursor-pointer hover:bg-primary/20"
                  >
                    {{ formatTime(event.startTime) }} {{ event.title }}
                  </div>
                  <div
                    v-for="task in getTasksForDate(cell.fullDate)"
                    :key="task.taskId"
                    class="px-2 py-1 text-[10px] font-medium rounded truncate cursor-pointer flex items-center gap-1 bg-slate-50 text-slate-700 hover:bg-slate-100"
                  >
                    <div
                      class="shrink-0 size-3 rounded-sm border flex items-center justify-center transition-colors"
                      :class="task.status === 'COMPLETED'
                        ? 'bg-emerald-500 border-emerald-500 text-white'
                        : 'border-slate-300 text-transparent'"
                      @click.stop="handleToggleTask(task)"
                    >
                      <span class="material-symbols-outlined text-[8px] font-bold">check</span>
                    </div>
                    <span
                      @click="selectTask(task)"
                      class="truncate"
                      :class="task.status === 'COMPLETED' ? 'line-through text-slate-500' : ''"
                    >{{ task.title }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- List View -->
          <div v-else class="p-6">
            <div class="space-y-6">
              <!-- Schedules Section -->
              <div class="space-y-4">
                <div v-if="schedules.length === 0 && tasks.length === 0" class="text-center py-20 text-slate-400">
                  <span class="material-symbols-outlined text-4xl mb-2">calendar_today</span>
                  <p class="text-sm">暂无日程安排和待办任务</p>
                </div>
                <div
                  v-for="event in schedules"
                  :key="event.scheduleId"
                  @click="selectedEvent = event; selectedTask = null"
                  class="p-6 bg-white border border-slate-200 rounded-2xl hover:shadow-md transition-all cursor-pointer group"
                >
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-6">
                      <div class="text-center shrink-0">
                        <p class="text-lg font-black text-slate-900">{{ formatTime(event.startTime) }}</p>
                        <p class="text-[10px] font-bold text-slate-400 uppercase">{{ formatDate(event.startTime) }}</p>
                      </div>
                      <div class="h-10 w-px bg-slate-100"></div>
                      <div class="min-w-0">
                        <h4 class="font-bold text-slate-900 group-hover:text-primary transition-colors truncate">{{ event.title }}</h4>
                        <div class="flex items-center gap-2 mt-1 flex-wrap">
                          <span v-if="event.customerName" class="text-xs text-slate-500 font-medium">{{ event.customerName }}</span>
                          <span v-if="event.typeName" class="text-[10px] px-2 py-0.5 bg-primary/10 text-primary rounded-full font-bold">{{ event.typeName }}</span>
                        </div>
                      </div>
                    </div>
                    <span class="material-symbols-outlined text-slate-300">chevron_right</span>
                  </div>
                </div>
              </div>

              <!-- Tasks Section -->
              <div v-if="tasks.length > 0" class="space-y-4">
                <div class="flex items-center gap-2">
                  <span class="material-symbols-outlined text-sm text-slate-400">task_alt</span>
                  <span class="text-xs font-bold text-slate-400 uppercase tracking-widest">待办任务</span>
                  <span class="text-[10px] px-2 py-0.5 bg-slate-100 text-slate-500 rounded-full font-bold">{{ tasks.filter(t => t.status !== 'COMPLETED').length }}</span>
                </div>
                <div
                  v-for="task in tasks"
                  :key="task.taskId"
                  class="p-6 bg-white border border-slate-200 rounded-2xl hover:shadow-md transition-all cursor-pointer group"
                >
                  <div class="flex items-center gap-4">
                    <button
                      @click.stop="handleToggleTask(task)"
                      class="size-6 rounded-full border-2 flex items-center justify-center shrink-0 transition-colors"
                      :class="task.status === 'COMPLETED' ? 'bg-emerald-500 border-emerald-500' : 'border-slate-300 hover:border-slate-400'"
                    >
                      <span v-if="task.status === 'COMPLETED'" class="material-symbols-outlined text-white text-sm">check</span>
                    </button>
                    <div class="flex-1 min-w-0" @click="selectTask(task)">
                      <h4
                        class="font-bold transition-colors truncate"
                        :class="task.status === 'COMPLETED' ? 'line-through text-slate-400' : 'text-slate-900 group-hover:text-slate-700'"
                      >{{ task.title }}</h4>
                      <div class="flex items-center gap-2 mt-1 flex-wrap">
                        <span v-if="task.customerName" class="text-xs text-slate-500 font-medium">{{ task.customerName }}</span>
                        <span v-if="task.dueDate" class="text-[10px] px-2 py-0.5 bg-slate-100 text-slate-500 rounded-full font-bold">
                          截止 {{ formatDueDate(task.dueDate) }}
                        </span>
                        <span
                          v-if="task.priority"
                          class="text-[10px] px-2 py-0.5 rounded-full font-bold"
                          :class="{
                            'bg-red-50 text-red-500': task.priority === 'HIGH',
                            'bg-amber-50 text-amber-500': task.priority === 'MEDIUM',
                            'bg-slate-100 text-slate-500': task.priority === 'LOW',
                          }"
                        >{{ task.priority === 'HIGH' ? '高' : task.priority === 'MEDIUM' ? '中' : '低' }}</span>
                      </div>
                    </div>
                    <span class="material-symbols-outlined text-slate-300" @click="selectTask(task)">chevron_right</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Event Detail Drawer (Desktop) -->
    <el-drawer
      v-if="!isMobile"
      v-model="showScheduleDetailDrawer"
      direction="rtl"
      :size="'400px'"
      :with-header="false"
      :modal="false"
      :lock-scroll="false"
      modal-penetrable
      class="schedule-detail-drawer"
    >
      <div v-if="selectedEvent" class="h-full flex flex-col bg-white shadow-2xl">
        <!-- Header -->
        <div class="flex items-center justify-between p-6 border-b border-slate-100">
          <span class="px-3 py-1 bg-primary/10 text-primary text-[10px] font-bold rounded-full uppercase tracking-widest">
            日程详情
          </span>
          <div class="flex items-center gap-2">
            <button
              @click="handleDeleteSchedule"
              class="size-9 flex items-center justify-center rounded-full hover:bg-red-50 text-slate-400 hover:text-red-500 transition-colors"
              type="button"
              aria-label="删除日程"
              title="删除日程"
            >
              <span class="material-symbols-outlined text-[18px] leading-none">delete</span>
            </button>
            <button
              @click="selectedEvent = null"
              class="size-9 flex items-center justify-center rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 transition-colors"
              type="button"
              aria-label="关闭日程详情"
              title="关闭"
            >
              <span class="material-symbols-outlined text-xl leading-none">close</span>
            </button>
          </div>
        </div>

        <!-- Content -->
        <div class="flex-1 min-h-0 overflow-y-auto p-8">
          <h2 class="text-2xl font-bold text-slate-900 leading-tight mb-2">{{ selectedEvent.title }}</h2>

          <div class="flex items-center gap-4 text-sm text-slate-500 mb-8 flex-wrap">
            <div class="flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">schedule</span>
              {{ formatDateTime(selectedEvent.startTime) }}
              <template v-if="selectedEvent.endTime"> ~ {{ formatTime(selectedEvent.endTime) }}</template>
            </div>
            <div v-if="selectedEvent.location" class="flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">location_on</span>
              <span class="break-words">{{ selectedEvent.location }}</span>
            </div>
            <div v-if="selectedEvent.typeName" class="flex items-center gap-1">
              <span class="material-symbols-outlined text-sm">label</span>
              {{ selectedEvent.typeName }}
            </div>
          </div>

          <div class="space-y-8">
            <section v-if="selectedEvent.customerName">
              <div class="flex items-center gap-2 mb-4">
                <span class="material-symbols-outlined text-[18px] text-slate-400">corporate_fare</span>
                <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest">关联客户</h3>
              </div>
              <div class="p-4 bg-white border border-slate-200 rounded-2xl flex items-center gap-3 hover:bg-slate-50 transition-colors">
                <div class="size-10 rounded-xl bg-primary/10 text-primary flex items-center justify-center font-bold">
                  {{ selectedEvent.customerName.charAt(0) }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-bold text-slate-900 truncate">{{ selectedEvent.customerName }}</p>
                  <p v-if="selectedEvent.contactName" class="text-[10px] text-slate-400 truncate">{{ selectedEvent.contactName }}</p>
                </div>
                <span class="material-symbols-outlined ml-auto text-slate-300">chevron_right</span>
              </div>
            </section>

            <section v-if="selectedEvent.description">
              <div class="flex items-center gap-2 mb-4">
                <span class="material-symbols-outlined text-[18px] text-slate-400">notes</span>
                <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest">备注说明</h3>
              </div>
              <div class="p-5 bg-slate-50 rounded-2xl border border-slate-100">
                <p class="text-sm text-slate-600 whitespace-pre-wrap leading-relaxed">{{ selectedEvent.description }}</p>
              </div>
            </section>
          </div>
        </div>

        <!-- Footer -->
        <div class="p-6 border-t border-slate-100 bg-white">
          <button
            @click="handleDeleteSchedule"
            class="w-full py-3 bg-red-50 text-red-600 rounded-xl text-sm font-bold hover:bg-red-100 transition-colors"
          >
            删除日程
          </button>
        </div>
      </div>
    </el-drawer>

    <TaskDetailDrawer
      v-model="showCalendarTaskDetail"
      :task="selectedTask"
      :is-mobile="isMobile"
      :can-edit="false"
      :can-delete="false"
      :can-toggle-complete="true"
      @toggle-complete="handleToggleTask"
    />

    <!-- Add Schedule Dialog -->
    <el-dialog
      v-model="showAddDialog"
      width="680px"
      :show-close="false"
      destroy-on-close
      top="6vh"
      class="!rounded-2xl !p-0 overflow-hidden schedule-dialog"
    >
      <template #header>
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-xl font-bold text-slate-900">新增日程</h2>
            <p class="text-sm text-slate-500 mt-1">填写日程信息</p>
          </div>
          <button
            @click="showAddDialog = false"
            class="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
          >
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>
      </template>

      <div class="space-y-6 bg-slate-50/50 p-6">
        <!-- Form Fields -->
        <div class="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">日程标题 <span class="text-red-500">*</span></label>
            <input
              v-model="scheduleForm.title"
              type="text"
              placeholder="请输入日程标题"
              class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2 outline-none transition-all"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">开始日期 <span class="text-red-500">*</span></label>
              <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2 transition-all">
                <span class="material-symbols-outlined text-slate-400 text-sm">calendar_today</span>
                <input
                  v-model="scheduleForm.startDate"
                  type="date"
                  class="w-full text-sm text-slate-900 bg-transparent outline-none"
                />
              </div>
            </div>
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">开始时间 <span class="text-red-500">*</span></label>
              <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2 transition-all">
                <span class="material-symbols-outlined text-slate-400 text-sm">schedule</span>
                <input
                  v-model="scheduleForm.startTime"
                  type="time"
                  class="w-full text-sm text-slate-900 bg-transparent outline-none"
                />
              </div>
            </div>
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">结束日期</label>
              <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2 transition-all">
                <span class="material-symbols-outlined text-slate-400 text-sm">event</span>
                <input
                  v-model="scheduleForm.endDate"
                  type="date"
                  class="w-full text-sm text-slate-900 bg-transparent outline-none"
                />
              </div>
            </div>
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">结束时间</label>
              <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2 transition-all">
                <span class="material-symbols-outlined text-slate-400 text-sm">update</span>
                <input
                  v-model="scheduleForm.endTime"
                  type="time"
                  class="w-full text-sm text-slate-900 bg-transparent outline-none"
                />
              </div>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">类型</label>
              <select
                v-model="scheduleForm.type"
                class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2 outline-none transition-all"
              >
                <option value="meeting">会议</option>
                <option value="call">电话</option>
                <option value="visit">拜访</option>
              </select>
            </div>
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">地点</label>
              <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2 transition-all">
                <span class="material-symbols-outlined text-slate-400 text-sm">location_on</span>
                <input
                  v-model="scheduleForm.location"
                  type="text"
                  placeholder="请输入地点"
                  class="w-full text-sm text-slate-900 bg-transparent outline-none"
                />
              </div>
            </div>
          </div>

          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">描述备注</label>
            <textarea
              v-model="scheduleForm.description"
              placeholder="请输入日程备注信息..."
              class="w-full text-sm text-slate-900 bg-slate-50 border border-slate-200 focus:border-primary focus:bg-white rounded-lg px-3 py-2 outline-none transition-all resize-none h-20"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3">
          <button
            @click="showAddDialog = false"
            class="flex-1 py-2.5 text-sm font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          >
            取消
          </button>
          <button
            @click="handleSaveSchedule"
            :disabled="!scheduleForm.title || !scheduleForm.startDate || !scheduleForm.startTime || saving"
            class="flex-1 py-2.5 text-sm font-bold text-white bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-colors shadow-sm"
          >
            {{ saving ? '保存中...' : '确认保存' }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { getMySchedules, addSchedule, deleteSchedule } from '@/api/schedule'
import { getMyTasks, updateTaskStatus } from '@/api/task'
import type { ScheduleVO, ScheduleAddBO } from '@/api/schedule'
import type { Task } from '@/types/common'
import TaskDetailDrawer from '@/views/task/components/TaskDetailDrawer.vue'

const { isMobile } = useResponsive()

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

const showScheduleDetailDrawer = computed({
  get: () => !!selectedEvent.value && !isMobile.value,
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
const selectedTask = ref<Task | null>(null)
const schedules = ref<ScheduleVO[]>([])
const tasks = ref<Task[]>([])
const loading = ref(false)
const saving = ref(false)

const viewModes = [
  { value: 'grid' as const, label: '周' },
  { value: 'month' as const, label: '月' },
  { value: 'list' as const, label: '列表' },
]

const now = new Date()
const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const currentDateStr = computed(() => {
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  const d = now.getDate()
  const dayName = dayNames[now.getDay()]
  return `${y}年${m}月${d}日，${dayName}`
})

const todayScheduleCount = computed(() => {
  const todayStr = toDateStr(now)
  return schedules.value.filter(e => toDateStr(new Date(e.startTime)) === todayStr).length
})

const todayTaskCount = computed(() => {
  const todayStr = toDateStr(now)
  return tasks.value.filter(t => t.status !== 'COMPLETED' && normalizeDueDate(t.dueDate ?? '') === todayStr).length
})

// --- Data Loading ---

async function loadSchedules() {
  try {
    schedules.value = await getMySchedules('all')
  } catch (e) {
    console.error('加载日程失败', e)
  }
}

async function loadTasks() {
  try {
    tasks.value = await getMyTasks('all')
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

onMounted(async () => {
  loading.value = true
  await Promise.all([loadSchedules(), loadTasks()])
  loading.value = false
})

// --- Helpers ---

function toDateStr(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
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
    task.status = newStatus
  } catch (e: any) {
    ElMessage.error('更新任务状态失败')
  }
}

function selectTask(task: Task) {
  selectedEvent.value = null
  selectedTask.value = task
}

function formatDueDate(dueDate: string): string {
  if (!dueDate) return ''
  const d = new Date(dueDate)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

// --- Add Schedule ---

const showAddDialog = ref(false)

const scheduleForm = reactive({
  title: '',
  startDate: '',
  startTime: '',
  endDate: '',
  endTime: '',
  type: 'meeting' as string,
  location: '',
  description: ''
})

async function handleSaveSchedule() {
  if (!scheduleForm.title || !scheduleForm.startDate || !scheduleForm.startTime) return

  saving.value = true
  try {
    const data: ScheduleAddBO = {
      title: scheduleForm.title,
      startTime: `${scheduleForm.startDate}T${scheduleForm.startTime}:00`,
      type: scheduleForm.type,
      location: scheduleForm.location || undefined,
      description: scheduleForm.description || undefined,
    }
    if (scheduleForm.endDate && scheduleForm.endTime) {
      data.endTime = `${scheduleForm.endDate}T${scheduleForm.endTime}:00`
    }

    await addSchedule(data)
    showAddDialog.value = false
    resetScheduleForm()
    ElMessage.success('日程创建成功')
    await loadSchedules()
  } catch (e: any) {
    ElMessage.error('创建日程失败: ' + (e.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

function resetScheduleForm() {
  scheduleForm.title = ''
  scheduleForm.startDate = ''
  scheduleForm.startTime = ''
  scheduleForm.endDate = ''
  scheduleForm.endTime = ''
  scheduleForm.type = 'meeting'
  scheduleForm.location = ''
  scheduleForm.description = ''
}

// --- Delete Schedule ---

async function handleDeleteSchedule() {
  if (!selectedEvent.value) return
  try {
    await ElMessageBox.confirm('确定删除该日程？', '提示', { type: 'warning' })
    await deleteSchedule(selectedEvent.value.scheduleId)
    selectedEvent.value = null
    ElMessage.success('日程已删除')
    await loadSchedules()
  } catch {
    // cancelled
  }
}

// --- Week View ---

const weekDays = computed(() => {
  const today = now.getDay()
  const mondayOffset = today === 0 ? -6 : 1 - today
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(now)
    d.setDate(now.getDate() + mondayOffset + i)
    return {
      label: ['周一','周二','周三','周四','周五','周六','周日'][i],
      date: d.getDate(),
      fullDate: toDateStr(d),
      isToday: d.toDateString() === now.toDateString()
    }
  })
})

// --- Month View ---

const monthCells = computed(() => {
  const year = now.getFullYear()
  const month = now.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDow = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1
  const totalDays = lastDay.getDate()
  const cells = []
  for (let i = 0; i < 35; i++) {
    const date = i - startDow + 1
    const isCurrentMonth = date > 0 && date <= totalDays
    const cellDate = isCurrentMonth ? new Date(year, month, date) : null
    cells.push({
      date: isCurrentMonth ? date : 0,
      isCurrentMonth,
      isToday: isCurrentMonth && date === now.getDate(),
      fullDate: cellDate ? toDateStr(cellDate) : ''
    })
  }
  return cells
})
</script>

<style scoped>
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

:deep(.schedule-detail-drawer .el-drawer__body) {
  padding: 0 !important;
}
</style>

<style>
.schedule-dialog .el-dialog__body {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0;
}
</style>
