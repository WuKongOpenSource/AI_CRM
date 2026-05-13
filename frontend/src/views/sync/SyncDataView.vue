<template>
  <div class="sync-page flex min-h-full flex-col bg-[#f8fbff] text-slate-900">
    <main class="flex flex-1 items-center justify-center px-4 py-8 md:px-8 md:py-12">
      <section
        v-if="stage === 'welcome'"
        class="w-full max-w-[560px] rounded-[20px] border border-slate-100 bg-white p-6 text-center shadow-[0_24px_80px_rgba(15,23,42,0.08)] md:p-9"
      >
        <div class="mx-auto flex size-16 items-center justify-center rounded-full bg-primary text-white shadow-xl shadow-primary/20">
          <span class="material-symbols-outlined text-2xl">check</span>
        </div>
        <h1 class="mt-7 text-2xl font-bold tracking-tight text-slate-900 md:text-3xl">欢迎来到悟空AICRM</h1>
        <p class="mx-auto mt-3 max-w-sm text-sm leading-6 text-slate-500">恭喜您，身份验证已通过，您可以开始使用 AI CRM 系统。</p>

        <div class="mt-8 rounded-2xl border border-slate-100 bg-slate-50/70 p-4 text-left">
          <div class="grid gap-3">
            <label class="block">
              <span class="mb-2 block text-xs font-bold text-slate-500">企业名称</span>
              <el-select
                v-model="form.companyId"
                filterable
                class="w-full"
                :placeholder="verifiedPhone ? '选择企业名称' : '完成手机号验证后选择企业'"
                :loading="companiesLoading"
                :disabled="!verifiedPhone"
              >
                <el-option
                  v-for="company in oldCompanies"
                  :key="company.companyId"
                  :label="company.companyName || '未命名企业'"
                  :value="company.companyId"
                />
              </el-select>
            </label>
          </div>

          <div
            v-if="verifiedPhone"
            class="mt-3 flex items-center justify-between rounded-xl bg-white px-4 py-3 text-xs"
          >
            <span class="font-bold text-slate-500">当前管理手机号：{{ verifiedPhone }}</span>
            <button
              type="button"
              class="font-bold text-primary transition-colors hover:text-primary/80"
              @click="openVerificationDialog"
            >
              更换手机号
            </button>
          </div>

          <div v-if="selectedCompanyStats" class="mt-4 grid grid-cols-4 gap-2">
            <div
              v-for="stat in selectedCompanyStatsList"
              :key="stat.label"
              class="rounded-xl bg-white px-3 py-2 text-center"
            >
              <p class="text-[11px] font-bold text-slate-400">{{ stat.label }}</p>
              <p class="mt-1 text-sm font-bold text-slate-900">{{ stat.value }}</p>
            </div>
          </div>

          <div class="mt-4 flex items-center justify-between rounded-xl bg-white px-4 py-3">
            <div>
              <p class="text-sm font-bold text-slate-900">增量同步（预留）</p>
              <p class="mt-1 text-xs text-slate-400">
                {{ capabilities?.incrementalMessage || '当前仅记录增量事件，不会应用到目标业务表。' }}
              </p>
            </div>
            <el-switch v-model="form.incrementalEnabled" :disabled="!incrementalApplicationAvailable" />
          </div>

          <div v-if="form.incrementalEnabled && incrementalApplicationAvailable" class="mt-3 grid gap-3 md:grid-cols-2">
            <el-input v-model="form.mqTopic" placeholder="wk-crm-binlog" />
            <el-input v-model="form.mqGroup" placeholder="ai-crm-sync-data" />
          </div>

          <div v-if="preflightResult" class="mt-4 rounded-xl bg-white px-4 py-3 text-left">
            <div class="flex items-center justify-between gap-3">
              <p class="text-sm font-bold text-slate-900">迁移预检</p>
              <span
                :class="[
                  'rounded-full px-2.5 py-1 text-[11px] font-bold',
                  preflightResult.ready ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'
                ]"
              >
                {{ preflightResult.ready ? '可开始' : '需处理' }}
              </span>
            </div>
            <div v-if="preflightBlockingMessages.length" class="mt-3 space-y-1">
              <p
                v-for="item in preflightBlockingMessages"
                :key="`${item.code}-${item.module || ''}`"
                class="text-xs leading-5 text-rose-600"
              >
                {{ item.message }}
              </p>
            </div>
            <div v-if="preflightWarningMessages.length" class="mt-3 space-y-1">
              <p
                v-for="item in preflightWarningMessages.slice(0, 4)"
                :key="`${item.code}-${item.module || ''}-${item.message}`"
                class="text-xs leading-5 text-amber-600"
              >
                {{ item.message }}
              </p>
            </div>
            <div v-if="unsupportedModules.length" class="mt-3 flex flex-wrap gap-2">
              <span
                v-for="module in unsupportedModules"
                :key="module.key"
                class="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-bold text-slate-500"
              >
                {{ module.label }}暂不迁移：{{ formatNumber(module.rowCount) }}
              </span>
            </div>
            <p v-if="preflightResult.rerun?.message" class="mt-3 text-xs leading-5 text-slate-500">
              {{ preflightResult.rerun.message }}
            </p>
          </div>
        </div>

        <div class="mt-7 grid gap-3 md:grid-cols-2">
          <button
            type="button"
            class="flex h-12 items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-bold text-white shadow-lg shadow-primary/25 transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="syncStarting || companiesLoading || preflightLoading || !verifiedPhone || !currentTenantId"
            @click="handleStartSync"
          >
            <span>{{ syncStarting || preflightLoading ? '正在准备同步...' : '开始试用并同步数据' }}</span>
            <span class="material-symbols-outlined text-base">arrow_forward</span>
          </button>
          <button
            type="button"
            class="flex h-12 items-center justify-center rounded-xl bg-slate-100 px-5 text-sm font-bold text-slate-600 transition-colors hover:bg-slate-200"
            @click="skipSync"
          >
            暂不同步
          </button>
        </div>

        <button
          type="button"
          class="mt-4 inline-flex items-center gap-1 text-xs font-bold text-slate-400 transition-colors hover:text-primary"
          @click="showSyncRules"
        >
          <span class="material-symbols-outlined text-sm">info</span>
          了解同步规则
        </button>

        <div class="mt-8 flex items-center gap-5 text-[10px] font-bold uppercase tracking-[0.25em] text-slate-200">
          <span class="h-px flex-1 bg-slate-100"></span>
          <span>Enterprise Security Verified</span>
          <span class="h-px flex-1 bg-slate-100"></span>
        </div>
      </section>

      <section
        v-else
        class="w-full max-w-[700px] rounded-[24px] bg-white px-5 py-7 shadow-[0_28px_100px_rgba(15,23,42,0.08)] md:px-9 md:py-10"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="flex items-start gap-3">
            <div
              :class="[
                'mt-1 flex size-7 shrink-0 items-center justify-center rounded-full',
                stage === 'complete' ? 'bg-primary text-white' : stage === 'failed' ? 'bg-rose-50 text-rose-600' : 'bg-amber-50 text-amber-600'
              ]"
            >
              <span class="material-symbols-outlined text-lg">{{ stage === 'complete' ? 'check' : stage === 'failed' ? 'error' : 'bolt' }}</span>
            </div>
            <div>
              <h2 class="text-xl font-bold text-slate-900 md:text-2xl">
                {{ stageTitle }}
              </h2>
              <p class="mt-1 text-sm text-slate-400">
                {{ stageSubtitle }}
              </p>
            </div>
          </div>
          <p class="shrink-0 text-3xl font-black text-primary md:text-4xl">{{ displayProgress }}%</p>
        </div>

        <div class="mt-7 h-4 overflow-hidden rounded-full bg-slate-200">
          <div class="h-full rounded-full bg-primary transition-all duration-500" :style="{ width: `${displayProgress}%` }"></div>
        </div>

        <div class="mt-8 space-y-4">
          <div
            v-for="module in syncModules"
            :key="module.key"
            :class="[
              'flex min-h-[68px] items-center gap-4 rounded-xl px-4 py-3 transition-colors',
              module.progress > 0 || stage === 'complete' ? 'bg-[#f0f3ff]' : 'bg-slate-50 opacity-55'
            ]"
          >
            <div class="flex size-10 shrink-0 items-center justify-center rounded-full bg-white text-primary">
              <span class="material-symbols-outlined text-lg">{{ module.icon }}</span>
            </div>
            <div class="min-w-0 flex-1">
              <p class="text-sm font-bold text-slate-900">{{ module.label }}</p>
              <p
                v-if="module.message"
                :class="['mt-1 truncate text-xs', module.fail > 0 ? 'text-rose-500' : 'text-amber-500']"
              >
                {{ module.message }}
              </p>
            </div>
            <div class="flex shrink-0 items-center gap-3">
              <span
                :class="[
                  'text-xs font-black',
                  module.progress === 100 ? 'text-primary' : module.progress > 0 ? 'text-slate-500' : 'text-slate-400'
                ]"
              >
                {{ module.progress > 0 ? `${module.progress}%` : '等待中' }}
              </span>
              <span
                :class="[
                  'material-symbols-outlined text-lg',
                  module.progress === 100 ? 'text-primary' : module.progress > 0 ? 'animate-spin text-primary' : 'text-slate-300'
                ]"
              >
                {{ module.progress === 100 ? 'check_circle' : module.progress > 0 ? 'sync' : 'hourglass_empty' }}
              </span>
            </div>
          </div>
        </div>

        <p v-if="stage === 'syncing'" class="mt-8 text-center text-xs text-slate-400">
          <span class="material-symbols-outlined align-[-3px] text-sm">schedule</span>
          后端任务运行中，可离开页面后再返回查看最新进度。
        </p>

        <div v-else class="mt-8">
          <div class="mb-5 grid grid-cols-3 gap-3">
            <div class="rounded-xl bg-slate-50 px-4 py-3 text-center">
              <p class="text-xs font-bold text-slate-400">总数</p>
              <p class="mt-1 text-lg font-black text-slate-900">{{ syncSummary.total }}</p>
            </div>
            <div class="rounded-xl bg-emerald-50 px-4 py-3 text-center">
              <p class="text-xs font-bold text-emerald-500">成功</p>
              <p class="mt-1 text-lg font-black text-emerald-700">{{ syncSummary.success }}</p>
            </div>
            <div class="rounded-xl bg-rose-50 px-4 py-3 text-center">
              <p class="text-xs font-bold text-rose-500">失败</p>
              <p class="mt-1 text-lg font-black text-rose-700">{{ syncSummary.fail }}</p>
            </div>
          </div>

          <div v-if="recentJobErrors.length" class="mb-5 rounded-xl bg-rose-50 px-4 py-3 text-left">
            <p class="text-xs font-bold text-rose-600">最近错误</p>
            <div class="mt-2 space-y-2">
              <p
                v-for="error in recentJobErrors"
                :key="String(fieldValue(error, ['id']) || `${fieldValue(error, ['module_name', 'moduleName'])}-${fieldValue(error, ['source_id', 'sourceId'])}`)"
                class="text-xs leading-5 text-rose-700"
              >
                {{ fieldValue(error, ['module_name', 'moduleName']) || 'unknown' }} /
                {{ fieldValue(error, ['source_id', 'sourceId']) || '-' }}：
                {{ fieldValue(error, ['error_message', 'errorMessage']) || '未记录错误详情' }}
              </p>
            </div>
          </div>

          <button
            v-if="stage === 'complete'"
            type="button"
            class="flex h-14 w-full items-center justify-center gap-2 rounded-xl bg-primary text-sm font-bold text-white shadow-lg shadow-primary/25 transition-colors hover:bg-primary/90"
            @click="startTrial"
          >
            开始试用
            <span class="material-symbols-outlined text-lg">arrow_forward</span>
          </button>
          <button
            type="button"
            :class="[
              'flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-slate-100 text-sm font-bold text-slate-500 transition-colors hover:bg-slate-200',
              stage === 'complete' ? 'mt-3' : ''
            ]"
            @click="stage = 'welcome'"
          >
            返回同步设置
          </button>
        </div>
      </section>
    </main>

    <Teleport to="body">
      <Transition name="verify-fade">
        <div
          v-if="verifyDialogVisible"
          class="fixed inset-0 z-[500] flex items-center justify-center bg-slate-900/20 px-4 backdrop-blur-sm"
          @click.self="closeVerificationDialog"
        >
          <div class="relative w-full max-w-[520px] rounded-[28px] bg-white px-6 py-8 shadow-[0_28px_90px_rgba(15,23,42,0.22)] md:px-14 md:py-12">
            <button
              type="button"
              class="absolute right-4 top-4 flex size-9 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-600"
              aria-label="关闭身份验证"
              @click="closeVerificationDialog"
            >
              <span class="material-symbols-outlined text-[20px] leading-none">close</span>
            </button>

            <div class="mx-auto flex size-14 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <span class="material-symbols-outlined text-2xl">verified_user</span>
            </div>
            <h2 class="mt-6 text-center text-2xl font-bold text-slate-900">完成身份验证</h2>
            <p class="mt-2 text-center text-sm text-slate-500">即刻试用悟空 AICRM 系统</p>

            <div class="mt-8 space-y-5">
              <label class="block">
                <span class="mb-2 block text-xs font-bold text-slate-500">手机号码</span>
                <div class="grid grid-cols-[86px_1fr] gap-2">
                  <el-select v-model="countryCode" class="w-full">
                    <el-option label="+86" value="+86" />
                  </el-select>
                  <el-input v-model="verificationPhone" placeholder="请输入您的手机号" />
                </div>
              </label>

              <label class="block">
                <span class="mb-2 block text-xs font-bold text-slate-500">验证码</span>
                <div class="relative">
                  <el-input v-model="verificationCode" maxlength="6" placeholder="输入6位验证码" />
                  <button
                    type="button"
                    class="absolute right-1.5 top-1/2 -translate-y-1/2 rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-bold text-slate-500 transition-colors hover:bg-slate-200"
                    @click="sendVerificationCode"
                  >
                    获取验证码
                  </button>
                </div>
              </label>

              <button
                type="button"
                class="h-12 w-full rounded-xl bg-primary text-sm font-bold text-white shadow-lg shadow-primary/25 transition-colors hover:bg-primary/90"
                @click="submitVerification"
              >
                验证并开始试用
              </button>
              <button
                type="button"
                class="h-11 w-full rounded-xl bg-slate-100 text-sm font-bold text-slate-500 transition-colors hover:bg-slate-200"
                @click="skipSync"
              >
                暂不同步，返回系统
              </button>
            </div>

            <div class="mt-9 flex items-center gap-5 text-[10px] font-bold uppercase tracking-[0.25em] text-slate-200">
              <span class="h-px flex-1 bg-slate-100"></span>
              <span>Enterprise Security Verified</span>
              <span class="h-px flex-1 bg-slate-100"></span>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  bindCompany,
  getSyncJob,
  getSyncJobErrors,
  getSyncJobModules,
  queryMigrationPreflight,
  queryCompanyBindings,
  queryOldCompanies,
  querySyncCapabilities,
  startFullSync,
  type CompanyBinding,
  type MigrationPreflightResult,
  type OldCompanyOption,
  type SyncApiError,
  type SyncCapabilities,
  type SyncJobErrorRecord,
  type SyncSafeId,
  type SyncJobModuleRecord,
  type SyncJobRecord
} from '@/api/syncData'

type SyncStage = 'welcome' | 'syncing' | 'complete' | 'failed'
type BindingForm = {
  tenantId: string
  companyId?: SyncSafeId
  incrementalEnabled: boolean
  mqTopic: string
  mqGroup: string
}

type BaseModule = {
  key: string
  label: string
  icon: string
  start: number
  end: number
  moduleNames: string[]
}

const router = useRouter()
const userStore = useUserStore()
const FIXED_VERIFICATION_CODE = '888888'
const savedVerifiedPhone = sessionStorage.getItem('aicrm-sync-phone') || ''
const stage = ref<SyncStage>('welcome')
const loading = ref(false)
const companiesLoading = ref(false)
const preflightLoading = ref(false)
const syncStarting = ref(false)
const verifyDialogVisible = ref(false)
const verified = ref(sessionStorage.getItem('aicrm-sync-verified') === 'true' && Boolean(savedVerifiedPhone))
const verifiedPhone = ref(savedVerifiedPhone)
const countryCode = ref('+86')
const verificationPhone = ref(savedVerifiedPhone)
const verificationCode = ref('')
const rawProgress = ref(0)
const activeJobId = ref<SyncSafeId | null>(null)
const bindings = ref<CompanyBinding[]>([])
const oldCompanies = ref<OldCompanyOption[]>([])
const activeBinding = ref<CompanyBinding | null>(null)
const preflightResult = ref<MigrationPreflightResult | null>(null)
const capabilities = ref<SyncCapabilities | null>(null)
const jobDetail = ref<SyncJobRecord | null>(null)
const jobModules = ref<SyncJobModuleRecord[]>([])
const jobErrors = ref<SyncJobErrorRecord[]>([])
const jobErrorCount = ref(0)
let progressTimer: number | null = null
let jobPollTimer: number | null = null
let verificationDialogTimer: number | null = null

const form = reactive<BindingForm>({
  tenantId: normalizeId(userStore.userInfo?.tenantId),
  companyId: undefined,
  incrementalEnabled: false,
  mqTopic: '',
  mqGroup: ''
})

const currentTenantId = computed(() => normalizeId(userStore.userInfo?.tenantId))

const baseModules: BaseModule[] = [
  { key: 'org', label: '组织及人员', icon: 'groups', start: 0, end: 18, moduleNames: ['tenants', 'departments', 'roles', 'users', 'user_roles', 'custom_fields'] },
  { key: 'customers', label: '客户数据', icon: 'badge', start: 18, end: 34, moduleNames: ['customers'] },
  { key: 'customerFieldValues', label: '客户字段值', icon: 'dynamic_form', start: 34, end: 46, moduleNames: ['customer_custom_values'] },
  { key: 'contacts', label: '联系人', icon: 'contacts', start: 46, end: 56, moduleNames: ['contacts'] },
  { key: 'contactFieldValues', label: '联系人字段值', icon: 'fact_check', start: 56, end: 60, moduleNames: ['contact_custom_values'] },
  { key: 'followUps', label: '跟进记录', icon: 'record_voice_over', start: 60, end: 75, moduleNames: ['follow_ups'] },
  { key: 'schedules', label: '日程安排', icon: 'calendar_month', start: 75, end: 88, moduleNames: ['schedules'] },
  { key: 'tasks', label: '任务列表', icon: 'assignment', start: 88, end: 100, moduleNames: ['project_tasks', 'work_tasks'] }
]

const selectedCompanyStats = computed(() =>
  oldCompanies.value.find(company => company.companyId === form.companyId)
)

const selectedCompanyStatsList = computed(() => {
  const company = selectedCompanyStats.value
  if (!company) return []
  return [
    { label: '客户', value: formatNumber(company.customerCount) },
    { label: '联系人', value: formatNumber(company.contactCount) },
    { label: '成员', value: formatNumber(company.userCount) },
    { label: '跟进', value: formatNumber(company.followUpCount) }
  ]
})

const displayProgress = computed(() => stage.value === 'complete' ? 100 : Math.round(rawProgress.value))

const stageTitle = computed(() => {
  if (stage.value === 'complete') return '数据同步已完成'
  if (stage.value === 'failed') return '数据同步未完成'
  return '正在进行首次数据同步'
})

const stageSubtitle = computed(() => {
  if (stage.value === 'complete') {
    return jobStatus.value === 'completed_with_errors' ? '同步完成，但存在部分失败数据' : '所有业务数据已完成同步'
  }
  if (stage.value === 'failed') return '请查看错误后重试或返回同步设置'
  return '正在读取后端任务进度...'
})

const jobStatus = computed(() => String(fieldValue(jobDetail.value, ['status']) || activeBinding.value?.fullSyncStatus || ''))

const syncModules = computed(() =>
  baseModules.map(item => {
    const progress = getBackendModuleProgress(item)
    return {
      ...item,
      progress,
      message: getBackendModuleMessage(item, progress),
      fail: getBackendModuleCount(item, ['fail_count', 'failCount']),
      success: getBackendModuleCount(item, ['success_count', 'successCount']),
      total: getBackendModuleCount(item, ['total_count', 'totalCount'])
    }
  })
)

const syncSummary = computed(() => ({
  total: formatNumber(fieldNumber(jobDetail.value, ['total_count', 'totalCount'])),
  success: formatNumber(fieldNumber(jobDetail.value, ['success_count', 'successCount'])),
  fail: formatNumber(fieldNumber(jobDetail.value, ['fail_count', 'failCount']) || jobErrorCount.value)
}))

const preflightBlockingMessages = computed(() => preflightResult.value?.errors || [])
const preflightWarningMessages = computed(() => preflightResult.value?.warnings || [])
const unsupportedModules = computed(() =>
  (preflightResult.value?.modules || []).filter(module => module.status === 'unavailable' && Number(module.rowCount || 0) > 0)
)
const recentJobErrors = computed(() => jobErrors.value.slice(0, 5))
const incrementalApplicationAvailable = computed(() =>
  Boolean(capabilities.value?.incrementalApplicationAvailable || preflightResult.value?.incremental.applicationAvailable)
)

onMounted(() => {
  window.addEventListener('keydown', handleVerificationKeydown)
  void initializePage()
  if (!verified.value) {
    verificationDialogTimer = window.setTimeout(() => {
      verifyDialogVisible.value = true
      verificationDialogTimer = null
    }, 250)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleVerificationKeydown)
  clearVerificationDialogTimer()
  stopJobPolling()
  stopProgressSimulation()
})

watch(
  () => [form.tenantId, form.companyId],
  () => {
    preflightResult.value = null
  }
)

watch(
  currentTenantId,
  tenantId => {
    if (tenantId) {
      if (activeBinding.value && normalizeId(activeBinding.value.tenantId) !== tenantId) {
        activeBinding.value = null
      }
      applyCurrentTenantToForm()
      hydrateFromLatestBinding()
    }
  }
)

async function initializePage() {
  await initializeUserContext()
  await Promise.all([loadCapabilities(), refreshBaseData()])
}

async function initializeUserContext() {
  if ((!userStore.userInfo || !normalizeId(userStore.userInfo.tenantId)) && userStore.token) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      return
    }
  }
  applyCurrentTenantToForm()
}

function applyCurrentTenantToForm() {
  const tenantId = currentTenantId.value
  if (tenantId) {
    form.tenantId = tenantId
  }
}

async function refreshBaseData() {
  loading.value = true
  try {
    await loadBindings()
    if (verified.value && verifiedPhone.value) {
      await loadOldCompanies(verifiedPhone.value)
    }
    hydrateFromLatestBinding()
  } finally {
    loading.value = false
  }
}

async function loadCapabilities() {
  try {
    capabilities.value = await querySyncCapabilities()
  } catch {
    capabilities.value = {
      incrementalApplicationAvailable: false,
      incrementalStatus: 'reserved',
      incrementalMessage: '增量事件目前仅审计记录，尚未实现对目标业务表的增删改应用。'
    }
  }
}

async function loadOldCompanies(managerPhone?: string, showEmptyWarning = false): Promise<OldCompanyOption[]> {
  companiesLoading.value = true
  try {
    const companies = await queryOldCompanies(managerPhone)
    oldCompanies.value = companies
    const selectedStillAvailable = companies.some(company => company.companyId === form.companyId)
    if (!selectedStillAvailable) {
      form.companyId = companies[0]?.companyId
    }
    if (showEmptyWarning && companies.length === 0) {
      ElMessage.warning('未找到该手机号管理的企业')
    }
    return companies
  } catch (error) {
    showRequestError(error, '获取老库公司列表失败')
    return []
  } finally {
    companiesLoading.value = false
  }
}

async function loadBindings() {
  try {
    bindings.value = await queryCompanyBindings()
  } catch (error) {
    showRequestError(error, '获取绑定列表失败')
  }
}

function hydrateFromLatestBinding() {
  if (activeBinding.value || bindings.value.length === 0) return
  const tenantId = currentTenantId.value
  if (!tenantId) {
    return
  }
  const scopedBindings = tenantId
    ? bindings.value.filter(item => normalizeId(item.tenantId) === tenantId)
    : []
  if (scopedBindings.length === 0) {
    applyCurrentTenantToForm()
    return
  }
  const visibleCompanyIds = new Set(oldCompanies.value.map(company => company.companyId))
  const binding = verifiedPhone.value
    ? scopedBindings.find(item => visibleCompanyIds.has(item.sourceCompanyId))
    : scopedBindings[0]
  if (!binding) return
  activeBinding.value = binding
  applyCurrentTenantToForm()
  const bindingCompanyVisible = oldCompanies.value.some(company => company.companyId === binding.sourceCompanyId)
  if (!verifiedPhone.value || bindingCompanyVisible) {
    form.companyId = binding.sourceCompanyId
  }
  form.incrementalEnabled = false
  form.mqTopic = binding.mqTopic || ''
  form.mqGroup = binding.mqGroup || ''
  if (binding.fullSyncJobId) {
    activeJobId.value = binding.fullSyncJobId
    void loadJob(binding.fullSyncJobId)
  }
  if (binding.fullSyncStatus === 'running' && binding.fullSyncJobId) {
    stage.value = 'syncing'
    rawProgress.value = 6
    startJobPolling(binding.fullSyncJobId)
  } else if (binding.fullSyncStatus === 'completed' || binding.fullSyncStatus === 'completed_with_errors') {
    stage.value = 'complete'
    rawProgress.value = 100
  } else if (binding.fullSyncStatus === 'failed' || binding.fullSyncStatus === 'interrupted') {
    stage.value = 'failed'
  }
}

function handleStartSync() {
  if (!verified.value || !verifiedPhone.value) {
    verifyDialogVisible.value = true
    return
  }
  void startSyncFlow()
}

async function startSyncFlow() {
  await initializeUserContext()
  const tenantId = currentTenantId.value
  if (!tenantId) {
    ElMessage.warning('无法获取当前登录企业，请刷新页面或重新登录后再同步')
    return
  }
  form.tenantId = tenantId
  if (!form.companyId) {
    ElMessage.warning('请选择企业名称')
    return
  }

  const preflight = await runPreflight(tenantId, form.companyId)
  if (!preflight?.ready) {
    ElMessage.error('迁移预检未通过，请先处理阻塞项')
    return
  }

  syncStarting.value = true
  stage.value = 'syncing'
  rawProgress.value = 6
  jobDetail.value = null
  jobModules.value = []
  jobErrors.value = []
  jobErrorCount.value = 0
  startProgressSimulation()

  try {
    const binding = await bindCompany({
      tenantId,
      companyId: form.companyId,
      incrementalEnabled: form.incrementalEnabled,
      mqTopic: form.mqTopic.trim() || undefined,
      mqGroup: form.mqGroup.trim() || undefined,
      remark: 'AICRM 首次启用同步'
    })
    activeBinding.value = binding
    const result = await startFullSync(binding.bindingId)
    activeJobId.value = result.jobId
    await Promise.all([loadBindings(), loadJob(result.jobId)])
    startJobPolling(result.jobId)
    ElMessage.success('同步任务已启动')
  } catch (error) {
    stage.value = 'welcome'
    rawProgress.value = 0
    stopProgressSimulation()
    stopJobPolling()
    showRequestError(error, '全量同步失败')
  } finally {
    syncStarting.value = false
  }
}

async function runPreflight(tenantId: SyncSafeId, companyId: SyncSafeId): Promise<MigrationPreflightResult | null> {
  preflightLoading.value = true
  try {
    const result = await queryMigrationPreflight({
      tenantId,
      companyId,
      incrementalEnabled: form.incrementalEnabled
    })
    preflightResult.value = result
    return result
  } catch (error) {
    showRequestError(error, '迁移预检失败')
    return null
  } finally {
    preflightLoading.value = false
  }
}

async function loadJob(jobId: unknown) {
  const normalizedJobId = normalizeId(jobId)
  if (!normalizedJobId) {
    jobDetail.value = null
    jobModules.value = []
    jobErrorCount.value = 0
    return
  }
  try {
    const [job, modules, errors] = await Promise.all([
      getSyncJob(normalizedJobId),
      getSyncJobModules(normalizedJobId),
      getSyncJobErrors(normalizedJobId)
    ])
    jobDetail.value = job
    jobModules.value = modules
    jobErrors.value = errors
    jobErrorCount.value = errors.length
    if (modules.length > 0) {
      stopProgressSimulation()
    }
    rawProgress.value = calculateJobProgress(job, modules)
    const status = String(fieldValue(job, ['status']) || '')
    if (isTerminalJobStatus(status)) {
      stopProgressSimulation()
      stopJobPolling()
      if (status === 'completed' || status === 'completed_with_errors') {
        stage.value = 'complete'
        rawProgress.value = 100
      } else {
        stage.value = 'failed'
      }
      void loadBindings()
    }
  } catch (error) {
    if ((error as SyncApiError).status === 404) {
      jobDetail.value = null
      jobModules.value = []
      jobErrors.value = []
      jobErrorCount.value = 0
      return
    }
    throw error
  }
}

function startJobPolling(jobId: SyncSafeId) {
  stopJobPolling()
  jobPollTimer = window.setInterval(() => {
    void loadJob(jobId)
  }, 2500)
}

function stopJobPolling() {
  if (jobPollTimer) {
    clearInterval(jobPollTimer)
    jobPollTimer = null
  }
}

function isTerminalJobStatus(status: string): boolean {
  return ['completed', 'completed_with_errors', 'failed', 'interrupted'].includes(status)
}

function calculateJobProgress(job: SyncJobRecord | null, modules: SyncJobModuleRecord[]): number {
  const status = String(fieldValue(job, ['status']) || '')
  if (status === 'completed' || status === 'completed_with_errors') return 100
  if (status === 'failed' || status === 'interrupted') return Math.max(rawProgress.value, 8)
  if (modules.length === 0) return Math.max(rawProgress.value, 8)
  const total = modules.reduce((sum, module) => sum + fieldNumber(module, ['total_count', 'totalCount']), 0)
  const processed = modules.reduce((sum, module) =>
    sum + fieldNumber(module, ['success_count', 'successCount']) + fieldNumber(module, ['fail_count', 'failCount']), 0)
  if (total > 0) {
    return Math.max(8, Math.min(99, Math.round((processed / total) * 100)))
  }
  const finishedModules = modules.filter(module => isTerminalModuleStatus(moduleStatus(module))).length
  return Math.max(8, Math.min(99, Math.round((finishedModules / Math.max(1, baseModules.length)) * 100)))
}

function moduleStatus(module: SyncJobModuleRecord): string {
  return String(fieldValue(module, ['status']) || '')
}

function isTerminalModuleStatus(status: string): boolean {
  return ['completed', 'completed_with_errors', 'skipped', 'failed'].includes(status)
}

function moduleRecords(group: BaseModule): SyncJobModuleRecord[] {
  return jobModules.value.filter(module => group.moduleNames.includes(String(fieldValue(module, ['module_name', 'moduleName']) || '')))
}

function getBackendModuleCount(group: BaseModule, keys: string[]): number {
  return moduleRecords(group).reduce((sum, module) => sum + fieldNumber(module, keys), 0)
}

function getBackendModuleProgress(group: BaseModule): number {
  if (stage.value === 'complete') return 100
  const records = moduleRecords(group)
  if (records.length === 0) return getModuleProgress(group)
  const total = getBackendModuleCount(group, ['total_count', 'totalCount'])
  const processed = getBackendModuleCount(group, ['success_count', 'successCount'])
    + getBackendModuleCount(group, ['fail_count', 'failCount'])
  const allFinished = records.every(module => isTerminalModuleStatus(moduleStatus(module)))
  if (allFinished) return 100
  if (total > 0) return Math.max(1, Math.min(99, Math.round((processed / total) * 100)))
  return records.some(module => moduleStatus(module) === 'running') ? 10 : 0
}

function getBackendModuleMessage(group: BaseModule, progress: number): string {
  const records = moduleRecords(group)
  if (records.length === 0) {
    return progress > 0 && progress < 100 ? getModuleMessage(group, progress) : ''
  }
  const total = getBackendModuleCount(group, ['total_count', 'totalCount'])
  const success = getBackendModuleCount(group, ['success_count', 'successCount'])
  const fail = getBackendModuleCount(group, ['fail_count', 'failCount'])
  if (fail > 0) return `${success}/${total} 成功，${fail} 失败`
  if (progress === 100) return total > 0 ? `${success}/${total} 已处理` : '已跳过'
  return total > 0 ? `${success}/${total} 已处理` : '正在准备模块'
}

function startProgressSimulation() {
  stopProgressSimulation()
  progressTimer = window.setInterval(() => {
    if (rawProgress.value >= 92) return
    const next = rawProgress.value + (rawProgress.value < 55 ? 5 : 2)
    rawProgress.value = Math.min(92, next)
  }, 500)
}

function stopProgressSimulation() {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

function getModuleProgress(module: BaseModule): number {
  const progress = rawProgress.value
  if (progress >= module.end) return 100
  if (progress <= module.start) return 0
  return Math.max(1, Math.round(((progress - module.start) / (module.end - module.start)) * 100))
}

function getModuleMessage(module: BaseModule, progress: number): string {
  if (module.key === 'followUps') {
    return `正在同步 ${Math.max(1, Math.round(progress * 12))}/1200 条...`
  }
  return '正在校验数据完整性...'
}

function sendVerificationCode() {
  if (!verificationPhone.value.trim()) {
    ElMessage.warning('请输入手机号')
    return
  }
  ElMessage.success(`验证码已固定为 ${FIXED_VERIFICATION_CODE}`)
}

async function submitVerification() {
  const phone = verificationPhone.value.trim()
  if (!phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (verificationCode.value.trim() !== FIXED_VERIFICATION_CODE) {
    ElMessage.warning('验证码不正确，请输入 888888')
    return
  }
  const companies = await loadOldCompanies(phone, true)
  if (companies.length === 0) {
    return
  }
  verified.value = true
  verifiedPhone.value = phone
  verificationPhone.value = phone
  sessionStorage.setItem('aicrm-sync-verified', 'true')
  sessionStorage.setItem('aicrm-sync-phone', phone)
  verifyDialogVisible.value = false
}

function openVerificationDialog() {
  verificationPhone.value = verifiedPhone.value
  verificationCode.value = ''
  verifyDialogVisible.value = true
}

function closeVerificationDialog() {
  verifyDialogVisible.value = false
}

function handleVerificationKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && verifyDialogVisible.value) {
    closeVerificationDialog()
  }
}

function clearVerificationDialogTimer() {
  if (verificationDialogTimer) {
    clearTimeout(verificationDialogTimer)
    verificationDialogTimer = null
  }
}

function showSyncRules() {
  ElMessageBox.alert(
    '当前仅执行 WK CRM 到 AI CRM 的全量同步；数据只会同步到当前登录企业，重复执行会复用已有映射。',
    '同步规则',
    { confirmButtonText: '知道了' }
  )
}

function skipSync() {
  closeVerificationDialog()
  void router.push('/chat')
}

function startTrial() {
  void router.push('/chat')
}

function fieldValue(row: Record<string, unknown> | null | undefined, keys: string[]): unknown {
  if (!row) return undefined
  for (const key of keys) {
    if (row[key] !== undefined && row[key] !== null) {
      return row[key]
    }
  }
  return undefined
}

function normalizeId(value: unknown): SyncSafeId {
  const text = String(value ?? '').trim()
  if (!text || !/^\d+$/.test(text) || /^0+$/.test(text)) {
    return ''
  }
  return text
}

function fieldNumber(row: Record<string, unknown> | null | undefined, keys: string[]): number {
  const value = fieldValue(row, keys)
  if (typeof value === 'number') return value
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

function formatNumber(value?: number | string | null): string {
  const parsed = Number(value || 0)
  return Number.isFinite(parsed) ? parsed.toLocaleString('zh-CN') : '0'
}

function showRequestError(error: unknown, fallback: string) {
  const message = error instanceof Error && error.message ? error.message : fallback
  ElMessage.error(message)
}
</script>

<style scoped>
.sync-page {
  background:
    radial-gradient(circle at 50% 28%, rgba(25, 118, 210, 0.08), transparent 34rem),
    linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.verify-fade-enter-active,
.verify-fade-leave-active {
  transition: opacity 0.2s ease;
}

.verify-fade-enter-from,
.verify-fade-leave-to {
  opacity: 0;
}
</style>
