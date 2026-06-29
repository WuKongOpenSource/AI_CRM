<template>
  <div class="wk-object-detail-embedded h-full overflow-y-auto bg-white px-4 py-4">
    <section class="pb-5">
      <div class="flex items-start gap-3">
        <div class="flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-slate-200 bg-slate-50 text-sm font-bold text-slate-500">
          <img
            v-if="detail.avatarUrl"
            :src="detail.avatarUrl"
            :alt="detail.name || '候选人头像'"
            class="size-full object-cover"
          />
          <span v-else>{{ candidateInitial }}</span>
        </div>
        <div class="min-w-0 flex-1">
          <div class="flex min-w-0 items-center gap-2">
            <h3 class="min-w-0 flex-1 truncate text-[15px] font-semibold text-[#0d0d0d]">{{ detail.name || '未命名候选人' }}</h3>
            <el-dropdown
              v-if="canChangeStage && normalizedStageOptions.length"
              trigger="click"
              @command="handleStageCommand"
            >
              <button
                type="button"
                class="inline-flex h-7 max-w-[126px] shrink-0 items-center gap-1.5 rounded-[8px] px-2.5 text-[12px] font-semibold transition-colors"
                :class="stageButtonClass"
                :disabled="stageChanging"
              >
                <span class="material-symbols-outlined text-[15px] leading-none">{{ stageIcon(detail.stage) }}</span>
                <span class="min-w-0 truncate">{{ stageLabel }}</span>
                <span class="material-symbols-outlined text-[15px] leading-none">expand_more</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="stage in normalizedStageOptions"
                    :key="stage.value"
                    :command="stage.value"
                  >
                    <span class="flex items-center gap-2">
                      <span class="material-symbols-outlined shrink-0 text-[16px] leading-none">{{ stageIcon(stage.value) }}</span>
                      {{ stage.label }}
                    </span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <span
              v-else
              class="inline-flex h-7 max-w-[126px] shrink-0 items-center gap-1.5 rounded-[8px] px-2.5 text-[12px] font-semibold"
              :class="stageButtonClass"
              :title="stageLabel"
            >
              <span class="material-symbols-outlined text-[15px] leading-none">{{ stageIcon(detail.stage) }}</span>
              <span class="min-w-0 truncate">{{ stageLabel }}</span>
            </span>
          </div>
          <p class="mt-1 truncate text-xs text-[#8f8f8f]">{{ activeJobName || detail.currentPosition || '候选人' }}</p>
        </div>
      </div>

      <div class="mt-4 grid grid-cols-3 gap-2 text-center">
        <MetricItem label="简历" :value="detail.resumeCount || resumes.length || 0" />
        <MetricItem label="面试" :value="detail.interviewCount || interviewSchedules.length || 0" />
        <MetricItem label="任务" :value="tasks.length || 0" />
      </div>
    </section>

    <section class="mt-5 border-t border-slate-100 pt-5">
      <div class="mb-4 flex items-center justify-between gap-2">
        <div class="flex min-w-0 items-center gap-2">
          <span :class="sectionIconBoxClass" :style="getSectionIconStyle('ai')">
            <WkIcon name="ai" :size="14" />
          </span>
          <h4 class="min-w-0 text-sm font-bold text-slate-900">AI分析</h4>
        </div>
        <p v-if="detail.updateTime" class="shrink-0 text-xs text-slate-400">{{ formatDateTime(detail.updateTime) }}更新</p>
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

    <section class="mt-5 border-t border-slate-100 pt-5">
      <div class="mb-4 flex items-center justify-between gap-2">
        <div class="flex min-w-0 items-center gap-2">
          <span :class="sectionIconBoxClass" :style="getSectionIconStyle('recruitment')">
            <span :class="sectionMaterialIconClass">assignment_ind</span>
          </span>
          <h4 class="min-w-0 text-sm font-bold text-slate-900">招聘岗位</h4>
        </div>
        <button
          v-if="canEditCandidate"
          type="button"
          class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
          aria-label="新增招聘岗位"
          @click="openRecruitmentJobDialog()"
        >
          <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">add</span>
          <span
            class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
            role="tooltip"
          >
            新增岗位
          </span>
        </button>
      </div>
      <div class="space-y-3">
        <el-select
          v-model="selectedRecruitmentJobId"
          class="w-full"
          filterable
          clearable
          :disabled="!canEditCandidate || recruitmentJobChanging"
          placeholder="选择关联招聘岗位"
          @change="handleRecruitmentJobChange"
        >
          <el-option
            v-for="job in recruitmentJobs"
            :key="job.recruitmentJobId"
            :label="job.jobName"
            :value="String(job.recruitmentJobId)"
          >
            <div class="flex items-center justify-between gap-3">
              <span class="truncate">{{ job.jobName }}</span>
              <span class="shrink-0 text-xs text-slate-400">{{ [job.department, getRecruitmentJobStatusLabel(job.status)].filter(Boolean).join(' · ') }}</span>
            </div>
          </el-option>
        </el-select>

        <div v-if="activeRecruitmentJob" class="rounded-xl border border-slate-200 bg-white p-3">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="truncate text-sm font-bold text-slate-900">{{ activeRecruitmentJob.jobName }}</p>
              <p class="mt-1 truncate text-xs text-slate-400">
                {{ [activeRecruitmentJob.department, activeRecruitmentJob.city, activeRecruitmentJob.salaryRange].filter(Boolean).join(' · ') || '招聘岗位' }}
              </p>
            </div>
            <div class="flex shrink-0 items-center gap-2">
              <span class="rounded-md bg-slate-100 px-2 py-1 text-[11px] font-bold text-slate-500">
                {{ getRecruitmentJobStatusLabel(activeRecruitmentJob.status) }}
              </span>
              <button
                v-if="canEditCandidate"
                type="button"
                class="flex size-7 items-center justify-center rounded-lg text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700"
                aria-label="编辑岗位"
                @click="openRecruitmentJobDialog(activeRecruitmentJob)"
              >
                <span class="material-symbols-outlined text-[16px] leading-none">edit</span>
              </button>
            </div>
          </div>

          <div class="mt-3 grid grid-cols-2 gap-2">
            <div
              v-for="item in recruitmentJobInfoItems"
              :key="item.label"
              class="rounded-lg bg-slate-50 px-3 py-2"
            >
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

        <RelatedEmptyState
          v-else
          icon="assignment_ind"
          text="暂未关联招聘岗位"
          hint="可以从已配置岗位中选择，或新增一个岗位"
        />
      </div>
    </section>

    <section class="mt-5 border-t border-slate-100 pt-5">
      <div class="mb-4 flex items-center justify-between">
        <h4 class="flex items-center gap-2 text-sm font-bold text-slate-900">
          <span :class="sectionIconBoxClass" :style="getSectionIconStyle('schedule')">
            <span :class="sectionMaterialIconClass">event_available</span>
          </span>
          安排面试
        </h4>
        <button
          type="button"
          class="group/module-action relative flex size-7 items-center justify-center rounded-lg border border-slate-200 bg-white text-slate-400 transition-all hover:border-primary/30 hover:bg-[#efefef] hover:text-primary"
          aria-label="创建面试"
          @click="emit('add-schedule')"
        >
          <span class="material-symbols-outlined wk-plus-button-icon wk-plus-button-icon--compact">add</span>
          <span
            class="pointer-events-none absolute right-full top-1/2 z-[200] mr-2 -translate-y-1/2 whitespace-nowrap rounded-lg bg-black px-3 py-1.5 text-[13px] font-medium text-white opacity-0 shadow-md transition-opacity duration-150 group-hover/module-action:opacity-100"
            role="tooltip"
          >
            创建面试
          </span>
        </button>
      </div>
      <RelatedEmptyState v-if="interviewSchedules.length === 0" icon="event_busy" text="暂无面试日程" />
      <div v-else class="space-y-3">
        <button
          v-for="schedule in interviewSchedules"
          :key="schedule.scheduleId"
          type="button"
          class="w-full rounded-xl border border-slate-200 bg-white p-3 text-left transition-all hover:shadow-md"
          :class="selectedScheduleId && String(schedule.scheduleId) === String(selectedScheduleId) ? 'border-primary ring-1 ring-primary/20' : ''"
          @click="emit('view-schedule', schedule)"
        >
          <p class="truncate text-sm font-bold text-slate-900">{{ schedule.title || '候选人面试' }}</p>
          <p class="mt-1 truncate text-xs leading-5 text-slate-500">
            {{ [formatDateTime(schedule.startTime), schedule.location].filter(Boolean).join(' · ') || '-' }}
          </p>
        </button>
      </div>
    </section>

    <RelatedTasksModule
      :tasks="tasks"
      :embedded-layout="true"
      :can-create="true"
      :clickable="true"
      :selected-task-id="selectedTaskId"
      @add="emit('add-task')"
      @view="emit('view-task', $event)"
    />

    <RelatedDocumentsModule
      :documents="resumes"
      :embedded-layout="true"
      :can-upload="true"
      :clickable="true"
      empty-text="暂无简历文档"
      @upload="emit('add-attachment')"
      @open="emit('view-attachment', $event)"
    />

    <RecruitmentJobUpsertDialog
      v-model="recruitmentJobDialogVisible"
      :editing-job="editingRecruitmentJob"
      @saved="handleRecruitmentJobSaved"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AiParseInsightSidebar from '@/components/crm/AiParseInsightSidebar.vue'
import WkIcon from '@/components/common/WkIcon.vue'
import RelatedDocumentsModule from '@/components/customer/related/RelatedDocumentsModule.vue'
import RelatedEmptyState from '@/components/customer/related/RelatedEmptyState.vue'
import RelatedTasksModule from '@/components/customer/related/RelatedTasksModule.vue'
import { updateCandidateField } from '@/api/candidate'
import { listRecruitmentJobOptions } from '@/api/recruitmentJob'
import type { CustomerAiParseVO } from '@/api/customer'
import type { ScheduleVO } from '@/api/schedule'
import { useEnumStore } from '@/stores/enums'
import { useUserStore } from '@/stores/user'
import type { Knowledge, Task } from '@/types/common'
import type { CandidateDetailVO } from '@/types/candidate'
import { CANDIDATE_STAGE_OPTIONS } from '@/types/candidate'
import type { RecruitmentJobVO } from '@/types/recruitmentJob'
import { RECRUITMENT_JOB_STATUS_OPTIONS } from '@/types/recruitmentJob'
import RecruitmentJobUpsertDialog from '@/views/candidate/RecruitmentJobUpsertDialog.vue'

type StageOption = { value: string; label: string }
type SectionIconKey = 'ai' | 'recruitment' | 'schedule'

const props = withDefaults(defineProps<{
  detail: CandidateDetailVO
  selectedTaskId?: string | number | null
  selectedScheduleId?: string | number | null
  stageOptions?: StageOption[]
  canChangeStage?: boolean
  stageChanging?: boolean
}>(), {
  selectedTaskId: null,
  selectedScheduleId: null,
  stageOptions: () => [],
  canChangeStage: false,
  stageChanging: false
})

const emit = defineEmits<{
  (e: 'add-task'): void
  (e: 'add-schedule'): void
  (e: 'add-attachment'): void
  (e: 'view-task', task: Task): void
  (e: 'view-schedule', schedule: ScheduleVO): void
  (e: 'view-attachment', attachment: Knowledge): void
  (e: 'change-stage', stage: string): void
  (e: 'updated', detail: CandidateDetailVO): void
}>()

const enumStore = useEnumStore()
const userStore = useUserStore()
const recruitmentJobs = ref<RecruitmentJobVO[]>([])
const selectedRecruitmentJobId = ref('')
const recruitmentJobChanging = ref(false)
const recruitmentJobDialogVisible = ref(false)
const editingRecruitmentJob = ref<RecruitmentJobVO | null>(null)

const sectionIconBoxClass = 'inline-flex size-6 shrink-0 items-center justify-center rounded-md text-white shadow-[0_6px_14px_rgba(15,23,42,0.08)]'
const sectionMaterialIconClass = 'material-symbols-outlined text-[14px] leading-none'
const sectionIconBgColors: Record<SectionIconKey, string> = {
  ai: '#8d4f34',
  recruitment: '#1f1e1c',
  schedule: '#8d4f34'
}

const tasks = computed(() => props.detail.tasks || [])
const schedules = computed(() => props.detail.schedules || [])
const resumes = computed(() => props.detail.resumes || [])
const candidateInitial = computed(() => props.detail.name?.trim().charAt(0) || '?')
const canEditCandidate = computed(() => userStore.hasPermission('candidate:edit'))
const normalizedStageOptions = computed(() =>
  props.stageOptions.length
    ? props.stageOptions
    : enumStore.candidateStage.length
      ? enumStore.candidateStage.map(item => ({ value: item.value, label: item.label }))
      : CANDIDATE_STAGE_OPTIONS.map(item => ({ value: item.value, label: item.label }))
)

const skillTags = computed(() =>
  String(props.detail.skillTags || activeRecruitmentJob.value?.skillTags || '')
    .split(/[,，;；\n]/)
    .map(tag => tag.trim())
    .filter(Boolean)
    .slice(0, 12)
)

const interviewSchedules = computed(() =>
  schedules.value.filter(schedule => String(schedule.type || '').toLowerCase() === 'interview' || String(schedule.typeName || '').includes('面试'))
)

const stageLabel = computed(() =>
  props.detail.stageName
    || enumStore.candidateStageLabel(props.detail.stage)
    || normalizedStageOptions.value.find(item => item.value === props.detail.stage)?.label
    || props.detail.stage
    || '新候选人'
)

const stageButtonClass = computed(() => getStageButtonClass(props.detail.stage))

const activeRecruitmentJob = computed(() => {
  const selectedId = selectedRecruitmentJobId.value || (props.detail.recruitmentJobId ? String(props.detail.recruitmentJobId) : '')
  if (selectedId) {
    const matched = recruitmentJobs.value.find(job => String(job.recruitmentJobId) === selectedId)
    if (matched) return matched
    if (props.detail.recruitmentJob && String(props.detail.recruitmentJob.recruitmentJobId) === selectedId) {
      return props.detail.recruitmentJob
    }
  }
  return props.detail.recruitmentJob || null
})

const activeJobName = computed(() =>
  activeRecruitmentJob.value?.jobName || props.detail.recruitmentJobName || props.detail.appliedPosition || ''
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

onMounted(() => {
  void enumStore.ensureCandidateStage()
  void loadRecruitmentJobs()
})

watch(
  () => props.detail.recruitmentJobId,
  value => {
    selectedRecruitmentJobId.value = value ? String(value) : ''
  },
  { immediate: true }
)

async function loadRecruitmentJobs() {
  try {
    recruitmentJobs.value = await listRecruitmentJobOptions({ limit: 100 })
  } catch (error) {
    console.error('Failed to load recruitment jobs:', error)
    recruitmentJobs.value = props.detail.recruitmentJob ? [props.detail.recruitmentJob] : []
  }
}

function handleStageCommand(value: string | number | object) {
  emit('change-stage', String(value))
}

async function handleRecruitmentJobChange(value: string | number | boolean | object) {
  recruitmentJobChanging.value = true
  try {
    const nextValue = value ? String(value) : null
    const detail = await updateCandidateField({
      candidateId: String(props.detail.candidateId),
      fieldName: 'recruitmentJobId',
      value: nextValue
    })
    emit('updated', detail)
    ElMessage.success(nextValue ? '招聘岗位已关联' : '已取消关联招聘岗位')
  } finally {
    recruitmentJobChanging.value = false
  }
}

function openRecruitmentJobDialog(job?: RecruitmentJobVO | null) {
  editingRecruitmentJob.value = job || null
  recruitmentJobDialogVisible.value = true
}

async function handleRecruitmentJobSaved(jobId?: string) {
  const wasEditingJob = !!editingRecruitmentJob.value?.recruitmentJobId
  await loadRecruitmentJobs()
  if (jobId && !wasEditingJob) {
    selectedRecruitmentJobId.value = String(jobId)
    await handleRecruitmentJobChange(String(jobId))
  } else {
    emit('updated', props.detail)
  }
}

function getSectionIconStyle(key: SectionIconKey): { backgroundColor: string } {
  return { backgroundColor: sectionIconBgColors[key] }
}

function stageIcon(stage?: string) {
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
  return map[String(stage || '')] || 'radio_button_checked'
}

function getStageButtonClass(stage?: string) {
  const map: Record<string, string> = {
    new: 'bg-slate-100 text-slate-700',
    screening: 'bg-blue-50 text-blue-700',
    interview_scheduled: 'bg-orange-50 text-orange-700',
    interviewing: 'bg-orange-50 text-orange-700',
    interview_passed: 'bg-emerald-50 text-emerald-700',
    offer: 'bg-indigo-50 text-indigo-700',
    hired: 'bg-green-50 text-green-700',
    rejected: 'bg-rose-50 text-rose-700'
  }
  return map[String(stage || '')] || 'bg-primary/10 text-primary'
}

function buildAiSummary() {
  const evaluation = String(props.detail.aiEvaluation || '').trim()
  if (evaluation) return evaluation
  const summary = String(props.detail.resumeSummary || '').trim()
  if (summary) {
    return `已根据简历信息形成初步判断：${summary}\n建议结合招聘岗位要求继续核验候选人的项目深度、沟通表达和到岗意愿。`
  }
  if (activeJobName.value || skillTags.value.length) {
    return `候选人已关联${activeJobName.value || '目标岗位'}，可继续补充简历或岗位要求以生成更完整的匹配分析。`
  }
  return ''
}

function buildAiNextStep() {
  if (props.detail.stage === 'interview_passed') return '推进 Offer 沟通，确认薪资预期、到岗时间与背调安排。'
  if (props.detail.stage === 'interview_scheduled' || props.detail.stage === 'interviewing') return '围绕岗位要求准备面试问题，并记录面试反馈。'
  if (props.detail.stage === 'screening') return '补充岗位要求并完成初筛，确认是否进入面试安排。'
  return '补充简历信息、招聘岗位和候选人期望，完成下一步筛选。'
}

function buildAiKeyPoints() {
  const job = activeRecruitmentJob.value
  return [
    activeJobName.value ? `招聘岗位：${activeJobName.value}` : '',
    props.detail.workYears === 0 || props.detail.workYears ? `工作年限：${props.detail.workYears} 年` : '',
    job?.workYears === 0 || job?.workYears ? `岗位年限：${job.workYears} 年` : '',
    job?.city ? `工作城市：${job.city}` : '',
    job?.salaryRange ? `薪资范围：${job.salaryRange}` : '',
    skillTags.value.length ? `技能标签：${skillTags.value.join('、')}` : '',
    job?.requirements ? `岗位要求：${job.requirements}` : props.detail.jobRequirements ? `岗位要求：${props.detail.jobRequirements}` : ''
  ].filter(Boolean)
}

function estimateAiScore() {
  let score = 48
  if (activeJobName.value) score += 10
  if (props.detail.resumeSummary) score += 12
  if (props.detail.aiEvaluation) score += 8
  if (skillTags.value.length) score += Math.min(12, skillTags.value.length * 3)
  if (props.detail.workYears === 0 || props.detail.workYears) score += 6
  if (activeRecruitmentJob.value?.requirements || props.detail.jobRequirements) score += 4
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

const MetricItem = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: [String, Number], default: '-' }
  },
  setup(metricProps) {
    return () => h('div', { class: 'rounded-xl border border-slate-200 bg-slate-50/70 px-2 py-2' }, [
      h('p', { class: 'text-[11px] font-medium text-slate-400' }, metricProps.label),
      h('p', { class: 'mt-0.5 text-sm font-bold text-slate-800' }, String(metricProps.value ?? '-'))
    ])
  }
})
</script>
