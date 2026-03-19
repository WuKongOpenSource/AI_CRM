<template>
  <el-dialog
    v-model="visible"
    width="680px"
    :show-close="false"
    destroy-on-close
    top="6vh"
    class="schedule-dialog !rounded-2xl !p-0 overflow-hidden"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-xl font-bold text-slate-900">新增日程</h2>
          <p class="text-sm text-slate-500 mt-1">填写日程信息</p>
        </div>
        <button
          class="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-full transition-colors"
          @click="visible = false"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </template>

    <div class="space-y-6 bg-white px-5 pb-6 pt-5 md:px-6 md:pb-7 md:pt-6">
      <div class="space-y-5">
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
          class="flex-1 py-2.5 text-sm font-bold text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors"
          @click="visible = false"
        >
          取消
        </button>
        <button
          :disabled="!canSave"
          class="flex-1 py-2.5 text-sm font-bold text-white bg-primary hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed rounded-xl transition-colors shadow-sm"
          @click="handleSaveSchedule"
        >
          {{ saving ? '保存中...' : '确认保存' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { addSchedule, type ScheduleAddBO } from '@/api/schedule'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'created'): void
}>()

type ScheduleFormState = {
  title: string
  startDate: string
  startTime: string
  endDate: string
  endTime: string
  type: string
  location: string
  description: string
}

function createDefaultFormState(): ScheduleFormState {
  return {
    title: '',
    startDate: '',
    startTime: '',
    endDate: '',
    endTime: '',
    type: 'meeting',
    location: '',
    description: ''
  }
}

const saving = ref(false)
const scheduleForm = reactive<ScheduleFormState>(createDefaultFormState())

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const canSave = computed(() =>
  !!scheduleForm.title && !!scheduleForm.startDate && !!scheduleForm.startTime && !saving.value
)

watch(
  () => props.modelValue,
  value => {
    if (!value) resetScheduleForm()
  }
)

function resetScheduleForm() {
  Object.assign(scheduleForm, createDefaultFormState())
}

async function handleSaveSchedule() {
  if (!canSave.value) return

  saving.value = true
  try {
    const data: ScheduleAddBO = {
      title: scheduleForm.title,
      startTime: `${scheduleForm.startDate}T${scheduleForm.startTime}:00`,
      type: scheduleForm.type,
      location: scheduleForm.location || undefined,
      description: scheduleForm.description || undefined
    }

    if (scheduleForm.endDate && scheduleForm.endTime) {
      data.endTime = `${scheduleForm.endDate}T${scheduleForm.endTime}:00`
    }

    await addSchedule(data)
    ElMessage.success('日程创建成功')
    emit('created')
    visible.value = false
  } catch (e: any) {
    ElMessage.error('创建日程失败: ' + (e?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}
</script>

<style>
.schedule-dialog .el-dialog__header {
  padding: 22px 24px 16px !important;
  margin-right: 0;
}

.schedule-dialog .el-dialog__body {
  max-height: 70vh;
  overflow-y: auto;
  padding: 0 !important;
}

.schedule-dialog .el-dialog__footer {
  padding: 14px 24px 22px !important;
}
</style>
