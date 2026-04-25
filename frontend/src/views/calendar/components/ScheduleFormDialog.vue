<template>
  <el-dialog
    v-model="visible"
    :width="isMobile ? '96%' : '720px'"
    :show-close="false"
    destroy-on-close
    top="6vh"
    class="schedule-dialog !rounded-2xl !p-0 overflow-hidden wk-crm-el-field-scope"
  >
    <template #header>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="size-11 rounded-2xl bg-primary/10 flex items-center justify-center shadow-sm">
            <span class="material-symbols-outlined text-[22px] text-primary">calendar_month</span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-900">{{ isEdit ? '编辑日程' : '新增日程' }}</h2>
            <p class="text-xs text-slate-500 mt-0.5">{{ isEdit ? '调整日程信息，保存后同步日程安排' : '手动填写更高效，也支持 AI 智能解析' }}</p>
          </div>
        </div>
        <button
          class="inline-flex size-9 shrink-0 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
          type="button"
          aria-label="关闭"
          @click="visible = false"
        >
          <span class="material-symbols-outlined text-[22px] leading-none">close</span>
        </button>
      </div>
    </template>

    <div class="space-y-6 bg-white px-5 pb-6 pt-5 md:px-6 md:pb-7 md:pt-6">
      <section
        v-if="!isEdit"
        class="rounded-2xl border border-primary/15 bg-gradient-to-br from-primary/5 via-white to-slate-50 p-4 shadow-sm"
      >
        <div class="flex items-center gap-2 mb-3">
          <WkIcon name="ai" class="text-primary text-sm" />
          <span class="text-xs font-bold text-primary uppercase tracking-wide">智能解析</span>
          <span class="text-xs text-slate-400">一句话补齐时间、客户和系统员工参与人</span>
        </div>
        <div class="relative">
          <el-input
            v-model="aiParseInput"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="例如：下周二下午三点和科技创新有限公司的张总开会讨论 Q4 扩容方案，地点在总部 8 楼会议室，参与人有李四、王敏。"
            class="wk-crm-el-field-input wk-crm-el-field-ai w-full"
          />
          <button
            :disabled="!aiParseInput.trim() || aiParsing"
            class="absolute bottom-3 right-3 flex items-center gap-1.5 rounded-lg bg-slate-800 px-3 py-1.5 text-xs font-bold text-white transition-colors hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-50"
            type="button"
            @click="handleAiParse"
          >
            <span v-if="aiParsing" class="material-symbols-outlined text-sm animate-spin">progress_activity</span>
            <WkIcon v-else name="ai" class="text-sm" />
            {{ aiParsing ? '解析中...' : '一键解析' }}
          </button>
        </div>
      </section>

      <div class="space-y-5">
        <div>
          <label class="mb-1.5 block text-xs font-bold text-slate-500">
            日程标题
            <span class="text-red-500">*</span>
          </label>
          <el-input
            v-model="scheduleForm.title"
            placeholder="请输入日程标题"
            size="large"
            class="w-full wk-crm-el-field-input"
          />
        </div>

        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label class="mb-1.5 block text-xs font-bold text-slate-500">
              开始时间
              <span class="text-red-500">*</span>
            </label>
            <el-date-picker
              v-model="scheduleForm.startTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择开始时间"
              size="large"
              class="w-full wk-crm-el-field-date"
            />
          </div>
          <div>
            <label class="mb-1.5 block text-xs font-bold text-slate-500">结束时间</label>
            <el-date-picker
              v-model="scheduleForm.endTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              format="YYYY-MM-DD HH:mm"
              placeholder="选择结束时间"
              clearable
              size="large"
              class="w-full wk-crm-el-field-date"
            />
          </div>
        </div>

        <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label class="mb-1.5 block text-xs font-bold text-slate-500">类型</label>
            <el-select
              v-model="scheduleForm.type"
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option label="会议" value="meeting" />
              <el-option label="电话" value="call" />
              <el-option label="拜访" value="visit" />
              <el-option label="其他" value="other" />
            </el-select>
          </div>
          <div>
            <label class="mb-1.5 block text-xs font-bold text-slate-500">关联客户/公司</label>
            <el-select
              v-model="scheduleForm.customerId"
              filterable
              remote
              reserve-keyword
              clearable
              default-first-option
              placeholder="输入公司名称"
              :remote-method="searchCustomers"
              :loading="customerSearchLoading"
              class="w-full wk-crm-el-field-select"
              size="large"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </div>

        <div>
          <label class="mb-1.5 block text-xs font-bold text-slate-500">参与人</label>
          <el-select
            v-model="selectedParticipantUserIds"
            multiple
            filterable
            remote
            reserve-keyword
            clearable
            default-first-option
            placeholder="搜索并选择系统员工"
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            class="w-full wk-crm-el-field-select"
            size="large"
          >
            <el-option
              v-for="item in userOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <p v-if="!isEdit && participantAiWarning" class="mt-1 text-xs font-medium text-amber-600">
            AI 识别到但未匹配到系统员工：{{ participantAiWarning }}
          </p>
        </div>

        <div>
          <label class="mb-1.5 block text-xs font-bold text-slate-500">地点</label>
          <el-input
            v-model="scheduleForm.location"
            placeholder="请输入地点"
            size="large"
            class="w-full wk-crm-el-field-input"
          />
        </div>

        <div>
          <label class="mb-1.5 block text-xs font-bold text-slate-500">描述备注</label>
          <el-input
            v-model="scheduleForm.description"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="请输入日程备注信息..."
            class="w-full wk-crm-el-field-input"
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-3">
        <button
          class="flex-1 rounded-xl bg-slate-100 py-2.5 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-200"
          type="button"
          @click="visible = false"
        >
          取消
        </button>
        <button
          :disabled="!canSave"
          class="flex-1 rounded-xl bg-primary py-2.5 text-sm font-bold text-white shadow-sm transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
          type="button"
          @click="handleSaveSchedule"
        >
          {{ saving ? '保存中...' : (isEdit ? '确认更新' : '确认创建') }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { queryUserList } from '@/api/auth'
import { queryCustomerList } from '@/api/customer'
import {
  addSchedule,
  aiParseSchedule,
  updateSchedule,
  type ScheduleAddBO,
  type ScheduleAiParseVO,
  type ScheduleParticipantUser,
  type ScheduleUpdateBO,
  type ScheduleVO
} from '@/api/schedule'
import WkIcon from '@/components/common/WkIcon.vue'
import { useResponsive } from '@/composables/useResponsive'

type Option = {
  value: string
  label: string
}

type ScheduleFormState = {
  title: string
  startTime: string
  endTime: string
  type: string
  customerId: string
  contactId: string
  location: string
  description: string
}

const props = defineProps<{
  modelValue: boolean
  editingSchedule?: ScheduleVO | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'created'): void
  (e: 'updated', scheduleId: string): void
}>()

const { isMobile } = useResponsive()

function createDefaultFormState(): ScheduleFormState {
  return {
    title: '',
    startTime: '',
    endTime: '',
    type: 'meeting',
    customerId: '',
    contactId: '',
    location: '',
    description: ''
  }
}

const saving = ref(false)
const aiParsing = ref(false)
const aiParseInput = ref('')
const participantAiWarning = ref('')
const scheduleForm = reactive<ScheduleFormState>(createDefaultFormState())
const selectedParticipantUserIds = ref<string[]>([])
const customerOptions = ref<Option[]>([])
const customerSearchLoading = ref(false)
const userOptions = ref<Option[]>([])
const userSearchLoading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const isEdit = computed(() => !!props.editingSchedule?.scheduleId)

const canSave = computed(() =>
  !!scheduleForm.title.trim() && !!scheduleForm.startTime && !saving.value
)

watch(
  () => props.modelValue,
  value => {
    if (value) {
      initScheduleForm()
    } else {
      resetScheduleForm()
    }
  }
)

watch(
  () => props.editingSchedule?.scheduleId,
  () => {
    if (visible.value) {
      initScheduleForm()
    }
  }
)

function resetScheduleForm() {
  Object.assign(scheduleForm, createDefaultFormState())
  aiParseInput.value = ''
  participantAiWarning.value = ''
  selectedParticipantUserIds.value = []
  customerOptions.value = []
  userOptions.value = []
}

function initScheduleForm() {
  resetScheduleForm()
  const schedule = props.editingSchedule
  if (!schedule) return

  Object.assign(scheduleForm, {
    title: schedule.title || '',
    startTime: parseAiDateTime(schedule.startTime) || schedule.startTime || '',
    endTime: schedule.endTime ? (parseAiDateTime(schedule.endTime) || schedule.endTime) : '',
    type: normalizeScheduleType(schedule.type),
    customerId: schedule.customerId ? String(schedule.customerId) : '',
    contactId: schedule.contactId ? String(schedule.contactId) : '',
    location: schedule.location || '',
    description: schedule.description || ''
  })

  if (schedule.customerId) {
    customerOptions.value = [{
      value: String(schedule.customerId),
      label: schedule.customerName || String(schedule.customerId)
    }]
  }

  if (schedule.participantUsers?.length) {
    const options = schedule.participantUsers.map(user => buildUserOption(user))
    mergeUserOptions(options)
    selectedParticipantUserIds.value = schedule.participantUsers.map(user => String(user.userId))
  } else if (schedule.participantUserIds?.length) {
    selectedParticipantUserIds.value = schedule.participantUserIds.map(userId => String(userId))
  }
}

function normalizeScheduleType(type?: string): string {
  const raw = (type || '').trim().toLowerCase()
  if (['meeting', 'call', 'visit', 'other'].includes(raw)) {
    return raw
  }
  switch (type?.trim()) {
    case '会议':
      return 'meeting'
    case '电话':
      return 'call'
    case '拜访':
      return 'visit'
    case '其他':
      return 'other'
    default:
      return 'meeting'
  }
}

function formatDatePart(value: number): string {
  return value.toString().padStart(2, '0')
}

function formatDateTimeValue(date: Date): string {
  return `${date.getFullYear()}-${formatDatePart(date.getMonth() + 1)}-${formatDatePart(date.getDate())} ${formatDatePart(date.getHours())}:${formatDatePart(date.getMinutes())}:${formatDatePart(date.getSeconds())}`
}

function parseAiDateTime(value?: string) {
  if (!value) return null

  const directMatch = value.trim().match(/^(\d{4})-(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{2})(?::(\d{2}))?/)
  if (directMatch) {
    const [, year, month, day, hour, minute, second] = directMatch
    return `${year}-${formatDatePart(Number(month))}-${formatDatePart(Number(day))} ${formatDatePart(Number(hour))}:${minute}:${second || '00'}`
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return null
  }

  return formatDateTimeValue(parsed)
}

function buildUserOption(user: ScheduleParticipantUser | Record<string, any>): Option {
  const userId = String(user.userId ?? '')
  const realname = String(user.realname || '').trim()
  const username = String(user.username || '').trim()
  const displayName = realname || username || userId
  return {
    value: userId,
    label: username && username !== displayName ? `${displayName} (${username})` : displayName
  }
}

function mergeUserOptions(options: Option[]) {
  const optionMap = new Map(userOptions.value.map(item => [item.value, item]))
  options.forEach(item => {
    optionMap.set(item.value, item)
  })
  userOptions.value = Array.from(optionMap.values())
}

function normalizeCompanyName(value?: string): string {
  return String(value || '')
    .trim()
    .replace(/[\s()（）]/g, '')
    .replace(/(?:有限责任公司|股份有限公司|集团有限公司|有限公司|集团公司|公司|集团)$/u, '')
}

function findExactCustomerMatch(customers: Array<{ customerId: string | number, companyName: string }>, customerName: string) {
  const rawName = customerName.trim()
  const normalizedName = normalizeCompanyName(rawName)
  const matchedCustomers = customers.filter(customer =>
    customer.companyName === rawName
    || normalizeCompanyName(customer.companyName) === normalizedName
  )

  return matchedCustomers.length === 1 ? matchedCustomers[0] : null
}

async function searchCustomers(query: string) {
  if (!query) {
    return
  }
  customerSearchLoading.value = true
  try {
    const res = await queryCustomerList({ keyword: query, page: 1, limit: 20 })
    customerOptions.value = (res.list || []).map((customer: any) => ({
      value: String(customer.customerId),
      label: customer.companyName
    }))
  } catch (error) {
    console.warn('客户搜索失败:', error)
    customerOptions.value = []
  } finally {
    customerSearchLoading.value = false
  }
}

async function searchUsers(query: string) {
  if (!query) {
    return
  }
  userSearchLoading.value = true
  try {
    const res = await queryUserList({ search: query })
    const activeUsers = (res.list || []).filter((user: any) => user.status === 1 || user.status === undefined)
    mergeUserOptions(activeUsers.map((user: any) => buildUserOption(user)))
  } catch (error) {
    console.warn('用户搜索失败:', error)
  } finally {
    userSearchLoading.value = false
  }
}

function hasMeaningfulAiParseResult(result: ScheduleAiParseVO): boolean {
  return Boolean(
    result.title?.trim()
    || result.startTime?.trim()
    || result.endTime?.trim()
    || result.customerName?.trim()
    || result.location?.trim()
    || result.description?.trim()
    || result.participantUsers?.length
    || result.unmatchedParticipantNames?.trim()
  )
}

async function handleAiParse() {
  if (!aiParseInput.value.trim()) return

  aiParsing.value = true
  try {
    const result = await aiParseSchedule(aiParseInput.value.trim())
    if (!hasMeaningfulAiParseResult(result)) {
      participantAiWarning.value = ''
      ElMessage.warning('本次智能解析没有提取到可填充的信息，请调整描述后重试')
      return
    }

    if (result.title) scheduleForm.title = result.title
    if (result.type) scheduleForm.type = normalizeScheduleType(result.type)
    if (result.location) scheduleForm.location = result.location
    if (result.description) scheduleForm.description = result.description

    const parsedStart = parseAiDateTime(result.startTime)
    if (parsedStart) {
      scheduleForm.startTime = parsedStart
    }

    const parsedEnd = parseAiDateTime(result.endTime)
    if (parsedEnd) {
      scheduleForm.endTime = parsedEnd
    }

    if (result.customerId && result.customerName) {
      customerOptions.value = [{
        value: String(result.customerId),
        label: result.customerName
      }]
      scheduleForm.customerId = String(result.customerId)
    } else if (result.customerName) {
      const res = await queryCustomerList({ keyword: result.customerName, page: 1, limit: 5 })
      const customers = res.list || []
      customerOptions.value = customers.map((customer: any) => ({
        value: String(customer.customerId),
        label: customer.companyName
      }))

      const matchedCustomer = findExactCustomerMatch(customers, result.customerName)
      scheduleForm.customerId = matchedCustomer ? String(matchedCustomer.customerId) : ''
    } else {
      scheduleForm.customerId = ''
      customerOptions.value = []
    }

    if (result.participantUsers?.length) {
      const options = result.participantUsers.map(user => buildUserOption(user))
      mergeUserOptions(options)
      selectedParticipantUserIds.value = result.participantUsers.map(user => String(user.userId))
    } else {
      selectedParticipantUserIds.value = []
    }

    participantAiWarning.value = result.unmatchedParticipantNames || ''

    if (participantAiWarning.value) {
      ElMessage.warning('部分参与人未匹配到系统员工，请手动确认')
    } else {
      ElMessage.success(`智能解析完成，请确认后再${isEdit.value ? '保存' : '创建'}`)
    }
  } catch (error) {
    console.error('AI 解析日程失败', error)
  } finally {
    aiParsing.value = false
  }
}

async function handleSaveSchedule() {
  if (!canSave.value) return

  const startTime = scheduleForm.startTime
  const endTime = scheduleForm.endTime || undefined

  if (endTime && endTime < startTime) {
    ElMessage.warning('结束时间不能早于开始时间')
    return
  }

  saving.value = true
  try {
    const data: ScheduleAddBO = {
      title: scheduleForm.title.trim(),
      startTime,
      endTime,
      type: scheduleForm.type,
      customerId: scheduleForm.customerId || undefined,
      contactId: scheduleForm.contactId || undefined,
      location: scheduleForm.location || undefined,
      description: scheduleForm.description || undefined,
      participantUserIds: selectedParticipantUserIds.value.length ? selectedParticipantUserIds.value : undefined
    }

    if (isEdit.value && props.editingSchedule) {
      const updateData: ScheduleUpdateBO = {
        scheduleId: props.editingSchedule.scheduleId,
        ...data
      }
      await updateSchedule(updateData)
      ElMessage.success('日程更新成功')
      emit('updated', props.editingSchedule.scheduleId)
    } else {
      await addSchedule(data)
      ElMessage.success('日程创建成功')
      emit('created')
    }
    visible.value = false
  } catch (error) {
    console.error(`${isEdit.value ? 'Update' : 'Create'} schedule failed:`, error)
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
  max-height: 72vh;
  overflow-y: auto;
  padding: 0 !important;
}

.schedule-dialog .el-dialog__footer {
  padding: 14px 24px 22px !important;
}

.el-overlay:has(.schedule-dialog) {
  overflow: hidden;
}
</style>
