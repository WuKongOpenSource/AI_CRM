<template>
  <div class="h-full flex flex-col bg-background-light">
    <div v-if="loading" class="flex flex-1 items-center justify-center">
      <span class="material-symbols-outlined animate-spin text-4xl text-slate-300">progress_activity</span>
    </div>

    <template v-else-if="candidate">
      <div class="min-h-0 flex-1 overflow-auto">
        <div class="static z-20 shrink-0 border-b border-slate-200/50 bg-background-light/90 px-4 pb-4 pt-4 backdrop-blur-md md:sticky md:top-0 md:px-8">
          <div class="mb-3 flex items-center gap-2 text-sm text-slate-500">
            <button type="button" class="flex items-center gap-1 transition-colors hover:text-primary" @click="goBack">
              <span class="material-symbols-outlined text-[16px]">badge</span>
              人力
            </button>
            <span class="material-symbols-outlined text-xs">chevron_right</span>
            <span class="font-medium text-slate-900">候选人详情</span>
          </div>

          <div class="min-w-0 overflow-hidden rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div class="flex flex-col justify-between gap-3 md:flex-row md:gap-0">
              <div class="flex min-w-0 gap-4">
                <div class="flex size-14 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-100">
                  <img
                    v-if="candidate.avatarUrl"
                    :src="candidate.avatarUrl"
                    :alt="candidate.name || '候选人头像'"
                    class="size-full object-cover"
                  />
                  <span v-else class="text-2xl font-bold text-slate-400">{{ candidateInitial }}</span>
                </div>
                <div class="min-w-0 space-y-2">
                  <div class="flex flex-col items-start gap-1 md:flex-row md:flex-wrap md:items-center md:gap-3">
                    <h2 class="min-w-0 w-full truncate text-lg font-bold text-slate-900 md:w-auto md:text-xl">
                      {{ candidate.name }}
                    </h2>
                    <span class="rounded px-2 py-0.5 text-xs font-bold" :class="stageBadgeClass(candidate.stage)">
                      {{ candidateStageLabel(candidate.stage, candidate.stageName) }}
                    </span>
                  </div>
                  <div class="flex w-full min-w-0 flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                    <div class="flex shrink-0 items-center gap-1">
                      <span class="text-slate-400">岗位:</span>
                      <span class="font-medium text-slate-600">{{ activeJobName || '-' }}</span>
                    </div>
                    <div class="flex shrink-0 items-center gap-1">
                      <span class="text-slate-400">手机:</span>
                      <span class="font-medium text-slate-600">{{ candidate.phone || '-' }}</span>
                    </div>
                    <div class="flex shrink-0 items-center gap-1">
                      <span class="text-slate-400">状态:</span>
                      <span class="font-bold text-primary">{{ candidateStageLabel(candidate.stage, candidate.stageName) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="flex w-full shrink-0 flex-nowrap justify-start gap-2 md:w-auto md:overflow-visible">
                <button v-if="canEditCandidate" type="button" class="candidate-action-btn" @click="openEditDialog">
                  <span class="material-symbols-outlined text-[17px]">edit</span>
                  编辑
                </button>
                <button type="button" class="candidate-action-btn" @click="openChat">
                  <span class="material-symbols-outlined text-[17px]">forum</span>
                  对话
                </button>
                <button type="button" class="candidate-primary-btn" @click="basicInfoDrawerVisible = true">
                  <span class="material-symbols-outlined text-[17px]">description</span>
                  基本信息
                </button>
              </div>
            </div>

            <div class="mt-5 min-w-0 border-t border-slate-100 pt-4">
              <div class="wk-candidate-stage-scroll relative w-full max-w-full overflow-x-auto overflow-y-hidden pb-2">
                <div class="relative flex min-w-max flex-nowrap items-stretch gap-x-0">
                  <div
                    v-for="(stage, index) in stageOptions"
                    :key="stage.value"
                    class="group relative h-8 w-[180px] flex-none"
                    :class="canChangeStage ? 'cursor-pointer' : 'cursor-default'"
                    :title="stage.label"
                    :style="{ zIndex: getStepperZIndex(stage.value, index) }"
                    @click="handleStageChange(stage.value)"
                  >
                    <div
                      class="absolute inset-0 transition-all duration-300"
                      :class="getStepperSegmentBgClass(stage.value, index)"
                      :style="{ clipPath: getStepperClipPath(index) }"
                    ></div>
                    <div
                      class="absolute inset-0 opacity-0 transition-opacity duration-200 group-hover:opacity-100"
                      :class="getStepperHoverOverlayClass(stage.value, index)"
                      :style="{ clipPath: getStepperClipPath(index) }"
                    ></div>
                    <div class="relative z-10 flex h-full items-center justify-center overflow-hidden px-4 transition-transform duration-200 group-hover:scale-[1.02]">
                      <div class="flex min-w-0 max-w-full items-center justify-center gap-2">
                        <span class="material-symbols-outlined shrink-0 text-[14px] font-bold leading-none transition-colors" :class="getStepperLabelClass(stage.value, index)">
                          {{ getStepperStageIcon(stage.value) }}
                        </span>
                        <span class="block min-w-0 truncate text-[14px] font-bold tracking-wider transition-colors" :class="getStepperLabelClass(stage.value, index)">
                          {{ stage.label }}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="px-4 pb-8 pt-3 md:px-8">
          <div class="mb-4 lg:hidden">
            <div class="flex items-center gap-2 rounded-xl bg-slate-100 p-1">
              <button
                type="button"
                class="h-9 flex-1 rounded-lg px-3 text-sm font-bold transition-colors"
                :class="detailTab === 'ai' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
                @click="detailTab = 'ai'"
              >
                AI分析
              </button>
              <button
                type="button"
                class="h-9 flex-1 rounded-lg px-3 text-sm font-bold transition-colors"
                :class="detailTab === 'work' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
                @click="detailTab = 'work'"
              >
                招聘面试
              </button>
              <button
                type="button"
                class="h-9 flex-1 rounded-lg px-3 text-sm font-bold transition-colors"
                :class="detailTab === 'related' ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
                @click="detailTab = 'related'"
              >
                关联模块
              </button>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 lg:grid-cols-12">
            <div :class="[detailTab === 'ai' ? 'block' : 'hidden', 'lg:block lg:col-span-3 space-y-4']">
              <section class="rounded-xl border border-slate-200 bg-white px-4 py-4 shadow-sm">
                <div class="mb-4 flex items-center justify-between gap-2">
                  <div class="flex min-w-0 flex-1 items-center gap-2">
                    <span :class="sectionIconBoxClass" :style="getSectionIconStyle('ai')">
                      <span class="material-symbols-outlined text-[14px] leading-none">auto_awesome</span>
                    </span>
                    <h3 class="min-w-0 break-words text-sm font-bold leading-snug text-slate-900">AI分析</h3>
                  </div>
                  <p v-if="candidate.updateTime" class="shrink-0 text-xs text-slate-400">{{ formatDateTime(candidate.updateTime) }}更新</p>
                </div>
                <AiParseInsightSidebar
                  :result="candidateAiResult"
                  :show-tip="false"
                  compact-score
                  unified
                  empty-title="暂无 AI 分析"
                  empty-description="上传或解析简历后，系统会在这里展示候选人匹配度、风险点和面试建议。"
                  score-caption="基于简历完整度、岗位匹配度和候选人背景估算"
                />
              </section>
            </div>

            <div :class="[detailTab === 'work' ? 'block' : 'hidden', 'lg:block lg:col-span-6 space-y-4']">
              <section class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
                <div class="mb-4 flex items-center justify-between gap-3">
                  <div class="flex min-w-0 items-center gap-2">
                    <span :class="sectionIconBoxClass" :style="getSectionIconStyle('recruitment')">
                      <span class="material-symbols-outlined text-[14px] leading-none">assignment_ind</span>
                    </span>
                    <h3 class="min-w-0 text-sm font-bold text-slate-900">招聘岗位</h3>
                  </div>
                  <button type="button" class="candidate-module-action-btn" @click="openEditDialog">
                    <span class="material-symbols-outlined text-[16px] leading-none">sync_alt</span>
                    变更
                  </button>
                </div>
                <div v-if="activeRecruitmentJob" class="space-y-3">
                  <div class="rounded-xl border border-slate-200 bg-white p-3">
                    <div class="flex items-start justify-between gap-3">
                      <div class="min-w-0">
                        <p class="truncate text-sm font-bold text-slate-900">{{ activeRecruitmentJob.jobName }}</p>
                        <p class="mt-1 truncate text-xs text-slate-400">
                          {{ [activeRecruitmentJob.department, activeRecruitmentJob.city, activeRecruitmentJob.salaryRange].filter(Boolean).join(' · ') || '招聘岗位' }}
                        </p>
                      </div>
                      <span class="shrink-0 rounded-md bg-slate-100 px-2 py-1 text-[11px] font-bold text-slate-500">
                        {{ getRecruitmentJobStatusLabel(activeRecruitmentJob.status) }}
                      </span>
                    </div>
                    <div v-if="recruitmentJobInfoItems.length" class="mt-3 grid grid-cols-2 gap-2">
                      <div v-for="item in recruitmentJobInfoItems" :key="item.label" class="rounded-lg bg-slate-50 px-3 py-2">
                        <p class="text-[11px] font-bold text-slate-400">{{ item.label }}</p>
                        <p class="mt-1 break-words text-xs font-semibold leading-5 text-slate-700">{{ item.value || '-' }}</p>
                      </div>
                    </div>
                    <div v-if="activeRecruitmentJob.responsibilities || activeRecruitmentJob.requirements" class="mt-3 space-y-3 border-t border-slate-100 pt-3">
                      <div v-if="activeRecruitmentJob.responsibilities">
                        <p class="mb-1 text-xs font-bold text-slate-400">岗位职责</p>
                        <p class="whitespace-pre-wrap break-words text-xs leading-6 text-slate-600">{{ activeRecruitmentJob.responsibilities }}</p>
                      </div>
                      <div v-if="activeRecruitmentJob.requirements">
                        <p class="mb-1 text-xs font-bold text-slate-400">任职要求</p>
                        <p class="whitespace-pre-wrap break-words text-xs leading-6 text-slate-600">{{ activeRecruitmentJob.requirements }}</p>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="rounded-xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-8 text-center">
                  <span class="material-symbols-outlined text-3xl text-slate-300">assignment_ind</span>
                  <p class="mt-2 text-sm font-medium text-slate-400">暂未关联招聘岗位</p>
                </div>
              </section>

              <section class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
                <div class="mb-4 flex items-center justify-between gap-3">
                  <div class="flex min-w-0 items-center gap-2">
                    <span :class="sectionIconBoxClass" :style="getSectionIconStyle('schedule')">
                      <span class="material-symbols-outlined text-[14px] leading-none">event_available</span>
                    </span>
                    <h3 class="min-w-0 text-sm font-bold text-slate-900">安排面试</h3>
                  </div>
                  <button type="button" class="candidate-module-action-btn" @click="openCreateInterview">
                    <span class="material-symbols-outlined text-[16px] leading-none">add</span>
                    创建面试
                  </button>
                </div>
                <div v-if="interviewSchedules.length === 0" class="rounded-xl border-2 border-dashed border-slate-200 bg-slate-50/70 py-8 text-center">
                  <span class="material-symbols-outlined text-3xl text-slate-300">event_busy</span>
                  <p class="mt-2 text-sm font-medium text-slate-400">暂无面试安排</p>
                </div>
                <div v-else class="space-y-3">
                  <button
                    v-for="schedule in interviewSchedules"
                    :key="schedule.scheduleId"
                    type="button"
                    class="w-full rounded-xl border border-slate-200 bg-white p-3 text-left transition-all hover:shadow-md"
                    @click="editSchedule(schedule)"
                  >
                    <p class="truncate text-sm font-bold text-slate-900">{{ schedule.title || '候选人面试' }}</p>
                    <p class="mt-1 truncate text-xs leading-5 text-slate-500">
                      {{ [formatDateTime(schedule.startTime), schedule.location].filter(Boolean).join(' · ') || '-' }}
                    </p>
                  </button>
                </div>
              </section>
            </div>

            <div :class="[detailTab === 'related' ? 'block' : 'hidden', 'lg:block lg:col-span-3 space-y-4']">
              <section class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
                <div class="mb-4 flex items-center gap-2">
                  <span :class="sectionIconBoxClass" :style="getSectionIconStyle('related')">
                    <span class="material-symbols-outlined text-[14px] leading-none">widgets</span>
                  </span>
                  <h3 class="min-w-0 text-sm font-bold text-slate-900">关联业务模块</h3>
                </div>
                <div class="grid grid-cols-3 gap-2 text-center">
                  <div class="rounded-xl border border-slate-200 bg-slate-50/70 px-2 py-2">
                    <p class="text-[11px] font-medium text-slate-400">简历</p>
                    <p class="mt-0.5 text-sm font-bold text-slate-800">{{ candidate.resumeCount || candidate.resumes?.length || 0 }}</p>
                  </div>
                  <div class="rounded-xl border border-slate-200 bg-slate-50/70 px-2 py-2">
                    <p class="text-[11px] font-medium text-slate-400">面试</p>
                    <p class="mt-0.5 text-sm font-bold text-slate-800">{{ candidate.interviewCount || interviewSchedules.length || 0 }}</p>
                  </div>
                  <div class="rounded-xl border border-slate-200 bg-slate-50/70 px-2 py-2">
                    <p class="text-[11px] font-medium text-slate-400">任务</p>
                    <p class="mt-0.5 text-sm font-bold text-slate-800">{{ candidate.tasks?.length || 0 }}</p>
                  </div>
                </div>
              </section>

              <RelatedTasksModule
                :tasks="candidate.tasks || []"
                :embedded-layout="true"
                :can-create="true"
                :clickable="false"
                @add="openCreateTask"
              />

              <RelatedDocumentsModule
                :documents="candidate.resumes || []"
                :embedded-layout="true"
                :can-upload="false"
                :clickable="false"
                empty-text="暂无简历文档"
              />
            </div>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="flex flex-1 items-center justify-center text-sm text-slate-400">候选人不存在</div>

    <CandidateUpsertDialog
      v-model="editDialogVisible"
      :editing-candidate="candidate"
      @saved="loadCandidate"
    />
    <CandidateBasicInfoDrawer
      v-model="basicInfoDrawerVisible"
      :candidate="candidate"
      :ai-result="candidateAiResult"
      @updated="handleCandidateUpdated"
      @edit="handleBasicInfoEdit"
    />
    <ScheduleFormDialog
      v-model="scheduleDialogVisible"
      :editing-schedule="editingSchedule"
      :default-candidate="defaultCandidate"
      @created="handleRelatedSaved"
      @updated="handleRelatedSaved"
    />
    <TaskEditDialog
      v-model="taskDialogVisible"
      :default-candidate="defaultCandidate"
      @saved="handleRelatedSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCandidateDetail, updateCandidateStage } from '@/api/candidate'
import type { CustomerAiParseVO } from '@/api/customer'
import type { ScheduleVO } from '@/api/schedule'
import AiParseInsightSidebar from '@/components/crm/AiParseInsightSidebar.vue'
import RelatedDocumentsModule from '@/components/customer/related/RelatedDocumentsModule.vue'
import RelatedTasksModule from '@/components/customer/related/RelatedTasksModule.vue'
import { useChatStore } from '@/stores/chat'
import { useEnumStore } from '@/stores/enums'
import { useUserStore } from '@/stores/user'
import type { CandidateDetailVO, CandidateStage } from '@/types/candidate'
import { CANDIDATE_STAGE_OPTIONS } from '@/types/candidate'
import { RECRUITMENT_JOB_STATUS_OPTIONS } from '@/types/recruitmentJob'
import ScheduleFormDialog from '@/views/calendar/components/ScheduleFormDialog.vue'
import TaskEditDialog from '@/views/task/components/TaskEditDialog.vue'
import CandidateUpsertDialog from './CandidateUpsertDialog.vue'
import CandidateBasicInfoDrawer from './components/CandidateBasicInfoDrawer.vue'

type DetailTab = 'ai' | 'work' | 'related'
type SectionIconKey = 'ai' | 'recruitment' | 'schedule' | 'related'

const route = useRoute()
const router = useRouter()
const chatStore = useChatStore()
const enumStore = useEnumStore()
const userStore = useUserStore()

const candidate = ref<CandidateDetailVO | null>(null)
const loading = ref(false)
const changingStage = ref(false)
const editDialogVisible = ref(false)
const basicInfoDrawerVisible = ref(false)
const scheduleDialogVisible = ref(false)
const taskDialogVisible = ref(false)
const editingSchedule = ref<ScheduleVO | null>(null)
const detailTab = ref<DetailTab>('ai')

const sectionIconBoxClass = 'inline-flex size-6 shrink-0 items-center justify-center rounded-md text-white shadow-[0_6px_14px_rgba(15,23,42,0.08)]'
const sectionIconBgColors: Record<SectionIconKey, string> = {
  ai: '#8d4f34',
  recruitment: '#1f1e1c',
  schedule: '#8d4f34',
  related: '#1f1e1c'
}

const candidateId = computed(() => String(route.params.id || ''))
const stageOptions = computed(() => enumStore.candidateStage.length ? enumStore.candidateStage : CANDIDATE_STAGE_OPTIONS)
const stageFlow = computed(() => stageOptions.value.map(option => String(option.value)))
const canEditCandidate = computed(() => userStore.hasPermission('candidate:edit'))
const canChangeStage = computed(() => userStore.hasPermission('candidate:change_stage'))
const defaultCandidate = computed(() => candidate.value
  ? { candidateId: candidate.value.candidateId, candidateName: candidate.value.name }
  : null)
const candidateInitial = computed(() => candidate.value?.name?.trim().charAt(0) || '?')

const activeRecruitmentJob = computed(() => candidate.value?.recruitmentJob || null)
const activeJobName = computed(() =>
  activeRecruitmentJob.value?.jobName || candidate.value?.recruitmentJobName || candidate.value?.appliedPosition || ''
)

const interviewSchedules = computed(() =>
  (candidate.value?.schedules || []).filter(schedule =>
    String(schedule.type || '').toLowerCase() === 'interview' || String(schedule.typeName || '').includes('面试')
  )
)

const skillTags = computed(() =>
  String(candidate.value?.skillTags || activeRecruitmentJob.value?.skillTags || '')
    .split(/[,，;；\n]/)
    .map(tag => tag.trim())
    .filter(Boolean)
    .slice(0, 12)
)

const recruitmentJobInfoItems = computed(() => {
  const job = activeRecruitmentJob.value
  if (!job) return []
  return [
    { label: '部门', value: job.department },
    { label: '招聘人数', value: job.headcount === 0 || job.headcount ? `${job.headcount} 人` : '' },
    { label: '工作年限', value: job.workYears === 0 || job.workYears ? `${job.workYears} 年` : '' },
    { label: '学历要求', value: job.education },
    { label: '工作城市', value: job.city },
    { label: '薪资范围', value: job.salaryRange },
    { label: '技能标签', value: job.skillTags }
  ].filter(item => item.value)
})

const candidateAiResult = computed<CustomerAiParseVO | null>(() => {
  const summary = buildAiSummary()
  const keyPoints = buildAiKeyPoints()
  const tags = skillTags.value.slice(0, 4)
  if (!summary && keyPoints.length === 0 && tags.length === 0) return null
  return {
    score: estimateAiScore(),
    tags,
    summary,
    nextStep: buildAiNextStep(),
    keyPoints
  }
})

onMounted(async () => {
  await enumStore.ensureCandidateStage()
  await loadCandidate()
})

watch(candidateId, () => {
  void loadCandidate()
})

async function loadCandidate() {
  if (!candidateId.value) return
  loading.value = true
  try {
    candidate.value = await getCandidateDetail(candidateId.value)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/candidate')
}

function openEditDialog() {
  if (!canEditCandidate.value) return
  editDialogVisible.value = true
}

function handleBasicInfoEdit() {
  basicInfoDrawerVisible.value = false
  openEditDialog()
}

function handleCandidateUpdated(detail: CandidateDetailVO) {
  candidate.value = detail
}

async function openChat() {
  if (!candidate.value) return
  const sessionId = await chatStore.openCandidateChat(candidate.value)
  await router.push({ path: '/chat', query: { sessionId, candidateId: String(candidate.value.candidateId) } })
}

function openCreateInterview() {
  editingSchedule.value = null
  scheduleDialogVisible.value = true
}

function editSchedule(schedule: ScheduleVO) {
  editingSchedule.value = schedule
  scheduleDialogVisible.value = true
}

function openCreateTask() {
  taskDialogVisible.value = true
}

async function handleRelatedSaved() {
  await loadCandidate()
}

async function handleStageChange(stage: string) {
  if (!candidate.value || !canChangeStage.value || changingStage.value || candidate.value.stage === stage) return
  changingStage.value = true
  try {
    await updateCandidateStage(String(candidate.value.candidateId), stage)
    candidate.value.stage = stage as CandidateStage
    candidate.value.stageName = candidateStageLabel(stage)
    ElMessage.success('候选人状态已更新')
    await loadCandidate()
  } finally {
    changingStage.value = false
  }
}

function candidateStageLabel(value?: string, fallback?: string) {
  return fallback || enumStore.candidateStageLabel(value) || CANDIDATE_STAGE_OPTIONS.find(option => option.value === value)?.label || value || '新候选人'
}

function stageBadgeClass(stage?: string) {
  const map: Record<string, string> = {
    new: 'bg-slate-100 text-slate-600',
    screening: 'bg-blue-50 text-blue-600',
    interview_scheduled: 'bg-amber-50 text-amber-600',
    interviewing: 'bg-amber-50 text-amber-600',
    interview_passed: 'bg-emerald-50 text-emerald-600',
    offer: 'bg-violet-50 text-violet-600',
    hired: 'bg-green-50 text-green-700',
    rejected: 'bg-rose-50 text-rose-600'
  }
  return map[String(stage || '')] || 'bg-slate-100 text-slate-600'
}

const STEPPER_SEGMENT_WIDTH = 180
const STEPPER_SEGMENT_HEIGHT = 32
const STEPPER_CHEVRON_SIZE = 12
const STEPPER_END_RADIUS = STEPPER_SEGMENT_HEIGHT / 2

function getStageIndex(stage?: string): number {
  if (!stage) return -1
  return stageFlow.value.findIndex(value => value === stage)
}

function getStepperClipPath(index: number): string {
  const isFirstNode = index === 0
  const isLastNode = index === stageFlow.value.length - 1
  if (isFirstNode) {
    return `path('M ${STEPPER_END_RADIUS} 0 H ${STEPPER_SEGMENT_WIDTH - STEPPER_CHEVRON_SIZE} L ${STEPPER_SEGMENT_WIDTH} ${STEPPER_SEGMENT_HEIGHT / 2} L ${STEPPER_SEGMENT_WIDTH - STEPPER_CHEVRON_SIZE} ${STEPPER_SEGMENT_HEIGHT} H ${STEPPER_END_RADIUS} Q 0 ${STEPPER_SEGMENT_HEIGHT} 0 ${STEPPER_SEGMENT_HEIGHT / 2} Q 0 0 ${STEPPER_END_RADIUS} 0 Z')`
  }
  if (isLastNode) {
    return `path('M 0 0 H ${STEPPER_SEGMENT_WIDTH - STEPPER_END_RADIUS} Q ${STEPPER_SEGMENT_WIDTH} 0 ${STEPPER_SEGMENT_WIDTH} ${STEPPER_SEGMENT_HEIGHT / 2} Q ${STEPPER_SEGMENT_WIDTH} ${STEPPER_SEGMENT_HEIGHT} ${STEPPER_SEGMENT_WIDTH - STEPPER_END_RADIUS} ${STEPPER_SEGMENT_HEIGHT} H 0 L ${STEPPER_CHEVRON_SIZE} ${STEPPER_SEGMENT_HEIGHT / 2} Z')`
  }
  return 'polygon(0% 0%, calc(100% - 12px) 0%, 100% 50%, calc(100% - 12px) 100%, 0% 100%, 12px 50%)'
}

function getStepperSegmentBgClass(stage: string, index: number): string {
  const state = getStepperVisualState(stage, index)
  const classes: Record<string, string> = {
    completed: 'bg-gradient-to-r from-[#22c55e] to-[#16c458] shadow-[inset_0_1px_0_rgba(255,255,255,0.18)]',
    current: 'bg-gradient-to-r from-[#113f98] to-[#194fa8] shadow-[0_8px_20px_rgba(17,63,152,0.18)]',
    pending: 'bg-[#d9d9d9]'
  }
  return classes[state]
}

function getStepperStageIcon(stage: string) {
  return stageIcon(stage)
}

function getStepperZIndex(stage: string, index: number): number {
  if (candidate.value?.stage === stage) return 10
  return stageFlow.value.length - index
}

function getStepperLabelClass(stage: string, index: number): string {
  return getStepperVisualState(stage, index) === 'pending' ? 'text-slate-600' : 'text-white'
}

function getStepperHoverOverlayClass(stage: string, index: number): string {
  return getStepperVisualState(stage, index) === 'pending' ? 'bg-white/35' : 'bg-white/10'
}

function getStepperVisualState(stage: string, index: number): 'completed' | 'current' | 'pending' {
  const currentStage = candidate.value?.stage
  if (!currentStage) return 'pending'
  if (currentStage === stage) return 'current'
  const currentIndex = getStageIndex(currentStage)
  return currentIndex > index ? 'completed' : 'pending'
}

function stageIcon(stage: string) {
  const map: Record<string, string> = {
    new: 'fiber_new',
    screening: 'filter_alt',
    interview_scheduled: 'event_available',
    interviewing: 'record_voice_over',
    interview_passed: 'check_circle',
    offer: 'workspace_premium',
    hired: 'how_to_reg',
    rejected: 'block'
  }
  return map[stage] || 'radio_button_checked'
}

function getSectionIconStyle(key: SectionIconKey): { backgroundColor: string } {
  return { backgroundColor: sectionIconBgColors[key] }
}

function buildAiSummary() {
  const evaluation = String(candidate.value?.aiEvaluation || '').trim()
  if (evaluation) return evaluation
  const summary = String(candidate.value?.resumeSummary || '').trim()
  if (summary) {
    return `已根据简历信息形成初步判断：${summary}\n建议结合招聘岗位要求继续核验候选人的项目深度、沟通表达和到岗意愿。`
  }
  if (activeJobName.value || skillTags.value.length) {
    return `候选人已关联${activeJobName.value || '目标岗位'}，可继续补充简历或岗位要求以生成更完整的匹配分析。`
  }
  return ''
}

function buildAiNextStep() {
  const stage = candidate.value?.stage
  if (stage === 'interview_passed') return '推进 Offer 沟通，确认薪资预期、到岗时间与背调安排。'
  if (stage === 'interview_scheduled' || stage === 'interviewing') return '围绕岗位要求准备面试问题，并记录面试反馈。'
  if (stage === 'screening') return '补充岗位要求并完成初筛，确认是否进入面试安排。'
  return '补充简历信息、招聘岗位和候选人期望，完成下一步筛选。'
}

function buildAiKeyPoints() {
  const item = candidate.value
  const job = activeRecruitmentJob.value
  if (!item) return []
  return [
    activeJobName.value ? `招聘岗位：${activeJobName.value}` : '',
    item.workYears === 0 || item.workYears ? `工作年限：${item.workYears} 年` : '',
    job?.workYears === 0 || job?.workYears ? `岗位年限：${job.workYears} 年` : '',
    job?.city ? `工作城市：${job.city}` : '',
    job?.salaryRange ? `薪资范围：${job.salaryRange}` : '',
    skillTags.value.length ? `技能标签：${skillTags.value.join('、')}` : '',
    job?.requirements ? `岗位要求：${job.requirements}` : item.jobRequirements ? `岗位要求：${item.jobRequirements}` : ''
  ].filter(Boolean)
}

function estimateAiScore() {
  const item = candidate.value
  if (!item) return 0
  let score = 48
  if (activeJobName.value) score += 10
  if (item.resumeSummary) score += 12
  if (item.aiEvaluation) score += 8
  if (skillTags.value.length) score += Math.min(12, skillTags.value.length * 3)
  if (item.workYears === 0 || item.workYears) score += 6
  if (activeRecruitmentJob.value?.requirements || item.jobRequirements) score += 4
  return Math.max(50, Math.min(92, score))
}

function getRecruitmentJobStatusLabel(status?: string) {
  return RECRUITMENT_JOB_STATUS_OPTIONS.find(item => item.value === status)?.label || status || '招聘中'
}

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

<style scoped>
.candidate-action-btn,
.candidate-primary-btn,
.candidate-module-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.candidate-action-btn,
.candidate-primary-btn {
  height: 34px;
  padding: 0 12px;
}

.candidate-module-action-btn {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #64748b;
}

.candidate-action-btn {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
}

.candidate-action-btn:hover,
.candidate-module-action-btn:hover {
  border-color: #bae6fd;
  background: #f8fafc;
  color: var(--el-color-primary);
}

.candidate-primary-btn {
  border: 1px solid var(--el-color-primary);
  background: var(--el-color-primary);
  color: #fff;
}

.candidate-primary-btn:hover {
  background: var(--el-color-primary-light-3);
}

.wk-candidate-stage-scroll {
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.wk-candidate-stage-scroll::-webkit-scrollbar {
  display: none;
  width: 0;
  height: 0;
}

@media (min-width: 769px) {
  :global(html.wk-native-mobile) .wk-candidate-stage-scroll {
    width: 100%;
    max-width: 100%;
    overflow-x: auto !important;
    overflow-y: hidden !important;
    padding-bottom: 8px;
    scrollbar-width: thin;
    scrollbar-color: var(--wk-scrollbar-thumb) transparent;
  }

  :global(html.wk-native-mobile) .wk-candidate-stage-scroll::-webkit-scrollbar {
    display: block;
    width: var(--wk-scrollbar-size);
    height: var(--wk-scrollbar-size);
  }

  :global(html.wk-native-mobile) .wk-candidate-stage-scroll::-webkit-scrollbar-track {
    background: transparent;
    border-radius: var(--wk-scrollbar-radius);
  }

  :global(html.wk-native-mobile) .wk-candidate-stage-scroll::-webkit-scrollbar-thumb {
    background: var(--wk-scrollbar-thumb);
    border-radius: var(--wk-scrollbar-radius);
  }
}
</style>
