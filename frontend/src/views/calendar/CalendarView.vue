<template>
  <div class="flex h-full bg-white">
    <!-- Calendar Main -->
    <div class="flex-1 p-8 overflow-y-auto" :class="{ 'border-r border-slate-100': selectedEvent }">
      <div class="max-w-5xl mx-auto space-y-10">
        <!-- Header -->
        <div class="flex items-center justify-between flex-wrap gap-4">
          <div>
            <h2 class="text-2xl font-bold text-slate-900">智能日程安排</h2>
            <p class="text-sm text-slate-500 mt-1">{{ currentDateStr }} • 今天有 {{ todayEventCount }} 场关键会议</p>
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
            class="bg-white p-4 h-48"
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
            <div v-if="day.isToday" class="space-y-2">
              <div
                v-for="event in events"
                :key="event.id"
                @click="selectedEvent = event"
                class="p-2 bg-white border border-primary/20 rounded-lg shadow-sm cursor-pointer hover:scale-105 transition-transform"
              >
                <p class="text-[10px] font-bold text-primary truncate">{{ event.title }}</p>
                <p class="text-[8px] text-slate-400">{{ event.time }} • {{ event.customer }}</p>
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
            <div v-if="cell.isToday" class="space-y-1">
              <div
                v-for="event in events"
                :key="event.id"
                @click="selectedEvent = event"
                class="px-1.5 py-0.5 bg-primary/10 border-l-2 border-primary rounded text-[8px] font-bold text-primary truncate cursor-pointer hover:bg-primary/20"
              >{{ event.time }} {{ event.title }}</div>
            </div>
          </div>
        </div>

        <!-- List View -->
        <div v-else class="space-y-4">
          <div
            v-for="event in events"
            :key="event.id"
            @click="selectedEvent = event"
            class="p-6 bg-white border border-slate-200 rounded-2xl hover:border-primary transition-all cursor-pointer group"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-6">
                <div class="text-center shrink-0">
                  <p class="text-lg font-black text-slate-900">{{ event.time }}</p>
                  <p class="text-[10px] font-bold text-slate-400 uppercase">{{ event.duration }}</p>
                </div>
                <div class="h-10 w-px bg-slate-100"></div>
                <div>
                  <h4 class="font-bold text-slate-900 group-hover:text-primary transition-colors">{{ event.title }}</h4>
                  <div class="flex items-center gap-2 mt-1">
                    <span class="material-symbols-outlined text-xs text-slate-400">person</span>
                    <span class="text-xs text-slate-500 font-medium">{{ event.customer }}</span>
                  </div>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <div class="flex -space-x-2">
                  <div
                    v-for="(p, idx) in event.participants.slice(0, 3)"
                    :key="idx"
                    class="size-8 rounded-full bg-slate-100 border-2 border-white flex items-center justify-center text-[10px] font-bold text-slate-500"
                  >{{ p.charAt(0) }}</div>
                </div>
                <span class="material-symbols-outlined text-slate-300">chevron_right</span>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Suggested Follow-ups -->
        <section>
          <h3 class="text-sm font-bold text-slate-900 mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary">auto_awesome</span>
            AI 建议的跟进时段
          </h3>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div
              v-for="(item, i) in aiSuggestions"
              :key="i"
              class="p-5 bg-slate-50 rounded-2xl border border-slate-100 group hover:border-primary/30 transition-all"
            >
              <p class="text-[10px] font-bold text-primary uppercase mb-2">建议时段：{{ item.timeSlot }}</p>
              <h4 class="font-bold text-slate-900 mb-1">{{ item.customer }}</h4>
              <p class="text-xs text-slate-500 mb-4">{{ item.reason }}</p>
              <button class="text-xs font-bold text-primary flex items-center gap-1 group-hover:gap-2 transition-all">
                {{ item.action }}
                <span class="material-symbols-outlined text-xs">arrow_forward</span>
              </button>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- Event Detail Panel -->
    <Transition name="slide-right">
      <aside v-if="selectedEvent" class="w-[420px] bg-slate-50 flex flex-col shrink-0">
        <div class="p-8 flex-1 overflow-y-auto">
          <div class="flex items-center justify-between mb-8">
            <div class="flex items-center gap-2">
              <span class="size-2 rounded-full bg-primary animate-pulse"></span>
              <span class="text-[10px] font-bold text-primary uppercase tracking-widest">会议简报 (AI Briefing)</span>
            </div>
            <button @click="selectedEvent = null" class="size-8 flex items-center justify-center rounded-full hover:bg-white text-slate-400 transition-colors">
              <span class="material-symbols-outlined">close</span>
            </button>
          </div>

          <div class="bg-white rounded-[2rem] p-8 shadow-xl shadow-slate-200/40 border border-slate-100 mb-8">
            <h2 class="text-xl font-bold text-slate-900 mb-2">{{ selectedEvent.title }}</h2>
            <div class="flex items-center gap-4 text-sm text-slate-500 mb-6">
              <div class="flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">schedule</span>
                {{ selectedEvent.time }} ({{ selectedEvent.duration }})
              </div>
              <div class="flex items-center gap-1">
                <span class="material-symbols-outlined text-sm">location_on</span>
                腾讯会议
              </div>
            </div>

            <div class="space-y-6">
              <div>
                <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest mb-3">参会人员</p>
                <div class="flex flex-wrap gap-2">
                  <span
                    v-for="p in selectedEvent.participants"
                    :key="p"
                    class="px-3 py-1 bg-slate-50 text-slate-600 text-xs font-medium rounded-full border border-slate-100"
                  >{{ p }}</span>
                </div>
              </div>

              <div class="p-5 bg-primary/5 rounded-2xl border border-primary/10">
                <div class="flex items-center gap-2 mb-3">
                  <span class="material-symbols-outlined text-primary text-sm">auto_awesome</span>
                  <p class="text-xs font-bold text-primary">AI 核心提醒</p>
                </div>
                <p class="text-sm text-slate-700 leading-relaxed font-medium">{{ selectedEvent.aiBrief }}</p>
              </div>
            </div>
          </div>

          <!-- Historical Insights -->
          <div class="space-y-6">
            <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest px-2">历史关联洞察</h3>
            <div class="space-y-4">
              <div class="p-4 bg-white rounded-2xl border border-slate-100 flex gap-4">
                <div class="size-10 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
                  <span class="material-symbols-outlined">warning</span>
                </div>
                <div>
                  <h4 class="text-xs font-bold text-slate-900 mb-1">账单异常争议</h4>
                  <p class="text-[10px] text-slate-500 leading-relaxed">客户在 3 天前的邮件中提到 1 月份账单金额与实际用量不符，需在会议开始前确认核销进度。</p>
                </div>
              </div>
              <div class="p-4 bg-white rounded-2xl border border-slate-100 flex gap-4">
                <div class="size-10 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                  <span class="material-symbols-outlined">trending_up</span>
                </div>
                <div>
                  <h4 class="text-xs font-bold text-slate-900 mb-1">扩容紧迫性</h4>
                  <p class="text-[10px] text-slate-500 leading-relaxed">AI 监测到客户业务流量在过去 48 小时内激增 30%，目前已接近预警线，这是促成签约的绝佳时机。</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 bg-white border-t border-slate-100 flex gap-3">
          <button class="flex-1 py-3 bg-primary text-white rounded-xl text-sm font-bold hover:bg-primary/90 transition-all shadow-lg shadow-primary/20">
            进入会议
          </button>
          <button class="flex-1 py-3 border border-slate-200 rounded-xl text-slate-600 text-sm font-bold hover:bg-slate-50 transition-all">
            修改时间
          </button>
        </div>
      </aside>
    </Transition>

    <!-- Add Schedule Dialog -->
    <el-dialog
      v-model="showAddDialog"
      width="640px"
      :show-close="false"
      destroy-on-close
      align-center
      class="!rounded-2xl !p-0 overflow-hidden"
    >
      <template #header>
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-xl font-bold text-slate-900">新增日程</h2>
            <p class="text-sm text-slate-500 mt-1">手动填写或使用 AI 智能解析</p>
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
        <!-- AI Natural Language Input -->
        <div class="space-y-3">
          <label class="text-sm font-bold text-slate-700 flex items-center gap-2">
            <span class="material-symbols-outlined text-primary text-lg">auto_awesome</span>
            AI 智能解析 (可选)
          </label>
          <div class="relative">
            <textarea
              v-model="aiTextInput"
              placeholder="例如：下周二下午三点和科技创新有限公司的张总开会讨论 Q4 扩容方案..."
              class="w-full h-28 p-4 text-sm text-slate-700 bg-white border border-slate-200 focus:border-primary focus:ring-2 focus:ring-primary/20 rounded-xl outline-none transition-all resize-none shadow-sm"
            />
            <button
              @click="ElMessage.info('AI 智能解析功能开发中')"
              :disabled="!aiTextInput.trim()"
              class="absolute bottom-3 right-3 px-4 py-1.5 bg-primary text-white text-xs font-bold rounded-lg hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors shadow-sm flex items-center gap-1.5"
            >
              <span class="material-symbols-outlined text-[14px]">auto_awesome</span>
              一键解析
            </button>
          </div>
        </div>

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
                <option value="task">任务</option>
              </select>
            </div>
            <div>
              <label class="text-xs font-bold text-slate-500 mb-1.5 block">关联客户/公司</label>
              <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2 transition-all">
                <span class="material-symbols-outlined text-slate-400 text-sm">domain</span>
                <input
                  v-model="scheduleForm.company"
                  type="text"
                  placeholder="请输入公司名称"
                  class="w-full text-sm text-slate-900 bg-transparent outline-none"
                />
              </div>
            </div>
          </div>

          <div>
            <label class="text-xs font-bold text-slate-500 mb-1.5 block">参与人 (逗号分隔)</label>
            <div class="flex items-center gap-2 bg-slate-50 border border-slate-200 focus-within:border-primary focus-within:bg-white rounded-lg px-3 py-2 transition-all">
              <span class="material-symbols-outlined text-slate-400 text-sm">group</span>
              <input
                v-model="scheduleForm.participants"
                type="text"
                placeholder="例如: 张三, 李四"
                class="w-full text-sm text-slate-900 bg-transparent outline-none"
              />
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
            :disabled="!scheduleForm.title || !scheduleForm.startDate || !scheduleForm.startTime"
            class="flex-1 py-2.5 text-sm font-bold text-white bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-colors shadow-sm"
          >
            确认保存
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'

interface CalendarEvent {
  id: string
  title: string
  customer: string
  time: string
  duration: string
  type: 'meeting' | 'call' | 'followup'
  aiBrief: string
  participants: string[]
  status: 'upcoming' | 'ongoing' | 'completed'
}

const viewMode = ref<'grid' | 'month' | 'list'>('grid')
const selectedEvent = ref<CalendarEvent | null>(null)

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

const events = ref<CalendarEvent[]>([
  {
    id: '1',
    title: 'Q4 扩容方案技术评审',
    customer: '科技创新有限公司',
    time: '14:00',
    duration: '60 min',
    type: 'meeting',
    aiBrief: '客户 CTO 将出席。重点关注：数据迁移风险、Q3 账单异常解释。建议准备近 3 个月的使用量趋势报告。',
    participants: ['张经理 (采购)', '刘工 (CTO)', '陈经理 (我)'],
    status: 'upcoming'
  },
  {
    id: '2',
    title: 'TechVentures 意向确认电话',
    customer: 'Sarah Smith',
    time: '16:30',
    duration: '15 min',
    type: 'call',
    aiBrief: 'Sarah 刚结束一场行业峰会。建议以此作为切入点，询问其对"智能 CRM"的新看法。CEO 级别对话，是进入决策层的关键时点。',
    participants: ['Sarah Smith', '陈经理 (我)'],
    status: 'upcoming'
  }
])

// Add Schedule Dialog
const showAddDialog = ref(false)
const aiTextInput = ref('')

const scheduleForm = reactive({
  title: '',
  startDate: '',
  startTime: '',
  endDate: '',
  endTime: '',
  type: 'meeting' as 'meeting' | 'call' | 'task',
  company: '',
  participants: '',
  description: ''
})

const typeMap: Record<string, CalendarEvent['type']> = {
  meeting: 'meeting',
  call: 'call',
  task: 'followup'
}

function handleSaveSchedule() {
  if (!scheduleForm.title || !scheduleForm.startDate || !scheduleForm.startTime) return

  const participantList = scheduleForm.participants
    ? scheduleForm.participants.split(',').map(s => s.trim()).filter(Boolean)
    : []

  const newEvent: CalendarEvent = {
    id: String(Date.now()),
    title: scheduleForm.title,
    customer: scheduleForm.company || '',
    time: scheduleForm.startTime,
    duration: scheduleForm.endTime
      ? `${diffMinutes(scheduleForm.startTime, scheduleForm.endTime)} min`
      : '60 min',
    type: typeMap[scheduleForm.type] || 'meeting',
    aiBrief: scheduleForm.description || '',
    participants: participantList,
    status: 'upcoming'
  }

  events.value.push(newEvent)
  showAddDialog.value = false
  resetScheduleForm()
  ElMessage.success('日程创建成功')
}

function diffMinutes(start: string, end: string): number {
  const [sh, sm] = start.split(':').map(Number)
  const [eh, em] = end.split(':').map(Number)
  return (eh * 60 + em) - (sh * 60 + sm)
}

function resetScheduleForm() {
  scheduleForm.title = ''
  scheduleForm.startDate = ''
  scheduleForm.startTime = ''
  scheduleForm.endDate = ''
  scheduleForm.endTime = ''
  scheduleForm.type = 'meeting'
  scheduleForm.company = ''
  scheduleForm.participants = ''
  scheduleForm.description = ''
  aiTextInput.value = ''
}

const todayEventCount = computed(() => events.value.length)

// Week days for grid view
const weekDays = computed(() => {
  const today = now.getDay() // 0=Sun, 1=Mon...
  const mondayOffset = today === 0 ? -6 : 1 - today
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(now)
    d.setDate(now.getDate() + mondayOffset + i)
    return {
      label: ['周一','周二','周三','周四','周五','周六','周日'][i],
      date: d.getDate(),
      isToday: d.toDateString() === now.toDateString()
    }
  })
})

// Month cells
const monthCells = computed(() => {
  const year = now.getFullYear()
  const month = now.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDow = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1 // Mon=0
  const totalDays = lastDay.getDate()
  const cells = []
  for (let i = 0; i < 35; i++) {
    const date = i - startDow + 1
    const isCurrentMonth = date > 0 && date <= totalDays
    cells.push({
      date: isCurrentMonth ? date : 0,
      isCurrentMonth,
      isToday: isCurrentMonth && date === now.getDate()
    })
  }
  return cells
})

const aiSuggestions = [
  { customer: '未来教育机构', reason: '客户通常在周四下午 15:00 活跃', action: '发送回访邮件', timeSlot: '15:30 - 16:00' },
  { customer: '全球贸易集团', reason: '合同审批周期已过半，建议探测进度', action: '电话沟通', timeSlot: '15:30 - 16:00' },
  { customer: '智慧医疗中心', reason: '行业新政策发布，适合作为激活话题', action: '发送行业动态', timeSlot: '15:30 - 16:00' },
]
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
