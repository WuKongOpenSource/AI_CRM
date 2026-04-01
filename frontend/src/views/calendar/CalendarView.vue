<template>
  <div class="flex h-full bg-white">
    <!-- Calendar Main -->
    <div class="flex-1 p-8 overflow-y-auto" :class="{ 'border-r border-slate-100': selectedEvent }">
      <div class="max-w-5xl mx-auto space-y-10">
        <!-- Header -->
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div>
            <h2 class="text-2xl font-bold text-slate-900">智能日程安排</h2>
            <p class="text-sm text-slate-500 mt-1">{{ currentDateStr }} • 今天有 {{ todayEventCount }} 个日程</p>
          </div>
          <div class="flex gap-3">
            <div class="flex bg-slate-100 p-1 rounded-xl border border-slate-200">
              <button
                v-for="mode in viewModes"
                :key="mode.value"
                @click="viewMode = mode.value"
                class="px-4 py-1.5 text-xs font-bold rounded-lg transition-all"
                :class="viewMode === mode.value
                  ? 'bg-white text-primary shadow-sm'
                  : 'text-slate-500 hover:text-slate-700'"
              >{{ mode.label }}</button>
            </div>
            <button
              @click="showAddDialog = true"
              class="px-4 py-2 bg-primary text-white rounded-xl text-sm font-bold hover:bg-primary/90 shadow-lg shadow-primary/20 flex items-center gap-1.5"
            >
              <span class="material-symbols-outlined text-sm">add</span>
              新增日程
            </button>
          </div>
        </div>

        <!-- Week View -->
        <div v-if="viewMode === 'grid'" class="grid grid-cols-7 gap-px bg-slate-200 border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
          <div
            v-for="day in weekDays"
            :key="day.label"
            class="bg-white p-4 min-h-48"
            :class="{ 'bg-primary/5': day.isToday }"
          >
            <div class="flex items-center justify-between mb-4">
              <span class="text-[10px] font-black uppercase tracking-widest" :class="day.isToday ? 'text-primary' : 'text-slate-400'">
                {{ day.label }}
              </span>
              <span
                class="size-6 flex items-center justify-center rounded-full text-xs font-bold"
                :class="day.isToday ? 'bg-primary text-white' : 'text-slate-900'"
              >{{ day.date }}</span>
            </div>
            <div class="space-y-2">
              <div
                v-for="event in getEventsForDate(day.fullDate)"
                :key="event.scheduleId"
                @click="selectedEvent = event"
                class="p-2 bg-white border border-primary/20 rounded-lg shadow-sm cursor-pointer hover:scale-105 transition-transform"
              >
                <p class="text-[10px] font-bold text-primary truncate">{{ event.title }}</p>
                <p class="text-[8px] text-slate-400">{{ formatTime(event.startTime) }} • {{ event.customerName || '' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Month View -->
        <div v-else-if="viewMode === 'month'" class="grid grid-cols-7 gap-px bg-slate-200 border border-slate-200 rounded-2xl overflow-hidden shadow-sm">
          <div v-for="dayLabel in ['周一','周二','周三','周四','周五','周六','周日']" :key="dayLabel" class="bg-slate-50 p-2 text-center border-b border-slate-200">
            <span class="text-[10px] font-black text-slate-400 uppercase tracking-widest">{{ dayLabel }}</span>
          </div>
          <div
            v-for="(cell, i) in monthCells"
            :key="i"
            class="bg-white p-2 h-28 border-b border-r border-slate-100"
            :class="{ 'bg-slate-50/50': !cell.isCurrentMonth }"
          >
            <div class="flex justify-between items-start mb-1">
              <span
                class="text-xs font-bold"
                :class="[
                  cell.isCurrentMonth ? 'text-slate-900' : 'text-slate-300',
                  cell.isToday ? 'size-6 flex items-center justify-center bg-primary text-white rounded-full' : ''
                ]"
              >{{ cell.isCurrentMonth ? cell.date : '' }}</span>
            </div>
            <div v-if="cell.fullDate" class="space-y-1">
              <div
                v-for="event in getEventsForDate(cell.fullDate)"
                :key="event.scheduleId"
                @click="selectedEvent = event"
                class="px-1.5 py-0.5 bg-primary/10 border-l-2 border-primary rounded text-[8px] font-bold text-primary truncate cursor-pointer hover:bg-primary/20"
              >{{ formatTime(event.startTime) }} {{ event.title }}</div>
            </div>
          </div>
        </div>

        <!-- List View -->
        <div v-else class="space-y-4">
          <div v-if="schedules.length === 0" class="text-center py-20 text-slate-400">
            <span class="material-symbols-outlined text-4xl mb-2">calendar_today</span>
            <p class="text-sm">暂无日程安排</p>
          </div>
          <div
            v-for="event in schedules"
            :key="event.scheduleId"
            @click="selectedEvent = event"
            class="p-6 bg-white border border-slate-200 rounded-2xl hover:border-primary transition-all cursor-pointer group"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-6">
                <div class="text-center shrink-0">
                  <p class="text-lg font-black text-slate-900">{{ formatTime(event.startTime) }}</p>
                  <p class="text-[10px] font-bold text-slate-400 uppercase">{{ formatDate(event.startTime) }}</p>
                </div>
                <div class="h-10 w-px bg-slate-100"></div>
                <div>
                  <h4 class="font-bold text-slate-900 group-hover:text-primary transition-colors">{{ event.title }}</h4>
                  <div class="flex items-center gap-2 mt-1">
                    <span v-if="event.customerName" class="text-xs text-slate-500 font-medium">{{ event.customerName }}</span>
                    <span v-if="event.typeName" class="text-[10px] px-2 py-0.5 bg-primary/10 text-primary rounded-full font-bold">{{ event.typeName }}</span>
                  </div>
                </div>
              </div>
              <span class="material-symbols-outlined text-slate-300">chevron_right</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Event Detail Panel -->
    <Transition name="slide-right">
      <aside v-if="selectedEvent" class="w-[420px] bg-slate-50 flex flex-col shrink-0">
        <div class="p-8 flex-1 overflow-y-auto">
          <div class="flex items-center justify-between mb-8">
            <div class="flex items-center gap-2">
              <span class="size-2 rounded-full bg-primary animate-pulse"></span>
              <span class="text-[10px] font-bold text-primary uppercase tracking-widest">日程详情</span>
            </div>
            <button @click="selectedEvent = null" class="size-8 flex items-center justify-center rounded-full hover:bg-white text-slate-400 transition-colors">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="bg-white rounded-[2rem] p-8 shadow-xl shadow-slate-200/40 border border-slate-100 mb-8">
            <h2 class="text-xl font-bold text-slate-900 mb-2">{{ selectedEvent.title }}</h2>
            <div class="flex items-center gap-4 text-sm text-slate-500 mb-6 flex-wrap">
              <div class="flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">schedule</span>
                {{ formatDateTime(selectedEvent.startTime) }}
                <template v-if="selectedEvent.endTime"> ~ {{ formatTime(selectedEvent.endTime) }}</template>
              </div>
              <div v-if="selectedEvent.location" class="flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">location_on</span>
                {{ selectedEvent.location }}
              </div>
              <div v-if="selectedEvent.typeName" class="flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">label</span>
                {{ selectedEvent.typeName }}
              </div>
            </div>

            <div class="space-y-6">
              <div v-if="selectedEvent.customerName">
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">关联客户</p>
                <span class="px-3 py-1 bg-slate-50 text-slate-600 text-xs font-medium rounded-full border border-slate-100">{{ selectedEvent.customerName }}</span>
              </div>
              <div v-if="selectedEvent.contactName">
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">联系人</p>
                <span class="px-3 py-1 bg-slate-50 text-slate-600 text-xs font-medium rounded-full border border-slate-100">{{ selectedEvent.contactName }}</span>
              </div>
              <div v-if="selectedEvent.description">
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">描述</p>
                <p class="text-sm text-slate-700 leading-relaxed">{{ selectedEvent.description }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 bg-white border-t border-slate-100 flex gap-3">
          <button
            @click="handleDeleteSchedule"
            class="flex-1 py-3 border border-red-200 text-red-500 rounded-xl text-sm font-bold hover:bg-red-50 transition-all"
          >
            删除日程
          </button>
        </div>
      </aside>
    </Transition>

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
import { getMySchedules, addSchedule, deleteSchedule } from '@/api/schedule'
import type { ScheduleVO, ScheduleAddBO } from '@/api/schedule'

const viewMode = ref<'grid' | 'month' | 'list'>('grid')
const selectedEvent = ref<ScheduleVO | null>(null)
const schedules = ref<ScheduleVO[]>([])
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

const todayEventCount = computed(() => {
  const todayStr = toDateStr(now)
  return schedules.value.filter(e => toDateStr(new Date(e.startTime)) === todayStr).length
})

// --- Data Loading ---

async function loadSchedules() {
  loading.value = true
  try {
    schedules.value = await getMySchedules('all')
  } catch (e) {
    console.error('加载日程失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSchedules()
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
</style>

<style>
.schedule-dialog .el-dialog__body {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0;
}
</style>
