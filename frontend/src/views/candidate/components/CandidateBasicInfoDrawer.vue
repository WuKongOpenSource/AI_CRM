<template>
  <el-drawer
    :model-value="modelValue"
    direction="rtl"
    :size="isMobile ? '100%' : '420px'"
    :z-index="isMobile ? 3500 : undefined"
    :with-header="false"
    destroy-on-close
    class="candidate-basic-info-drawer"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-if="candidate" class="candidate-basic-info-drawer__shell flex h-full flex-col bg-white shadow-2xl">
      <div class="candidate-basic-info-drawer__header flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50/60 px-6 py-5">
        <div class="flex min-w-0 items-center gap-3">
          <div class="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-base leading-none">description</span>
          </div>
          <div class="min-w-0">
            <h3 class="truncate text-base font-bold text-slate-900">基本信息</h3>
            <p class="truncate text-xs text-slate-400">查看并维护候选人详细资料</p>
          </div>
        </div>
        <div class="flex shrink-0 items-center gap-1">
          <button
            v-if="canEditCandidate"
            type="button"
            class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200 hover:text-slate-700"
            title="编辑候选人"
            aria-label="编辑候选人"
            @click="$emit('edit')"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">edit</span>
          </button>
          <button
            type="button"
            class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200 hover:text-slate-600"
            aria-label="关闭"
            @click="$emit('update:modelValue', false)"
          >
            <span class="material-symbols-outlined text-[18px] leading-none">close</span>
          </button>
        </div>
      </div>

      <div class="candidate-basic-info-drawer__body min-h-0 flex-1 overflow-y-auto px-6 py-6">
        <div class="space-y-5 text-left">
          <div>
            <p class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-400">候选人头像</p>
            <CustomerLogoUploader
              :logo-url="candidate.avatarUrl"
              :alt="candidate.name || '候选人头像'"
              label="头像"
              :disabled="!canEditCandidate || avatarSaving"
              :size="64"
              @uploaded="handleAvatarUploaded"
              @removed="handleAvatarRemoved"
            />
          </div>

          <section
            v-if="aiSummaryVisible"
            class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4"
          >
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-base text-primary">auto_awesome</span>
              <p class="text-xs font-bold uppercase tracking-wider text-slate-500">AI 分析摘要</p>
            </div>
            <div v-if="aiResult?.score" class="mt-3">
              <span class="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-bold text-primary shadow-sm">
                <span class="mr-1.5 size-1.5 rounded-full bg-primary"></span>
                匹配度 {{ aiResult.score }}
              </span>
            </div>
            <p class="mt-3 whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">
              {{ aiResult?.summary || candidate.aiEvaluation || candidate.resumeSummary || '暂无 AI 分析摘要' }}
            </p>
          </section>

          <div
            v-for="field in drawerFields"
            :key="field.fieldName"
          >
            <p class="mb-1 text-xs font-bold uppercase tracking-wider text-slate-400">{{ field.fieldLabel }}</p>
            <InlineEditableField
              :model-value="field.value"
              :field="field"
              :display-value="field.displayValue"
              :editable="canEditCandidate && field.editable !== false"
              reveal-edit-on-click
              :save-handler="(value) => handleInlineFieldSave(field.fieldName, value)"
            >
              <p
                class="text-sm font-medium text-slate-900"
                :class="field.fieldType === 'textarea' ? 'whitespace-pre-wrap break-words leading-relaxed text-slate-600' : 'break-words'"
              >
                {{ field.displayValue || '-' }}
              </p>
            </InlineEditableField>
          </div>

          <div>
            <p class="mb-1 text-xs font-bold uppercase tracking-wider text-slate-400">创建时间</p>
            <p class="text-sm font-medium text-slate-900">{{ formatDateTime(candidate.createTime) }}</p>
          </div>
          <div>
            <p class="mb-1 text-xs font-bold uppercase tracking-wider text-slate-400">更新时间</p>
            <p class="text-sm font-medium text-slate-900">{{ formatDateTime(candidate.updateTime) }}</p>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { updateCandidateField } from '@/api/candidate'
import type { CustomerAiParseVO } from '@/api/customer'
import InlineEditableField from '@/components/common/InlineEditableField.vue'
import { useResponsive } from '@/composables/useResponsive'
import { useEnumStore } from '@/stores/enums'
import { useUserStore } from '@/stores/user'
import type { CandidateDetailVO } from '@/types/candidate'
import { CANDIDATE_STAGE_OPTIONS } from '@/types/candidate'
import type { CustomField, FieldOption, FieldType } from '@/types/customField'
import CustomerLogoUploader from '@/views/customer/components/CustomerLogoUploader.vue'

type DrawerField = Partial<CustomField> & {
  fieldName: string
  fieldLabel: string
  fieldType: FieldType
  value: unknown
  displayValue: string
  editable?: boolean
}

const props = defineProps<{
  modelValue: boolean
  candidate: CandidateDetailVO | null
  aiResult?: CustomerAiParseVO | null
}>()

const emit = defineEmits<{
  'update:modelValue': [open: boolean]
  updated: [detail: CandidateDetailVO]
  edit: []
}>()

const { isMobile } = useResponsive()
const enumStore = useEnumStore()
const userStore = useUserStore()
const avatarSaving = ref(false)

const canEditCandidate = computed(() => userStore.hasPermission('candidate:edit'))
const aiSummaryVisible = computed(() =>
  !!props.aiResult?.summary || !!props.aiResult?.score || !!props.candidate?.aiEvaluation || !!props.candidate?.resumeSummary
)

const stageOptions = computed<FieldOption[]>(() =>
  enumStore.candidateStage.length
    ? enumStore.candidateStage.map(item => ({ value: item.value, label: item.label }))
    : CANDIDATE_STAGE_OPTIONS.map(item => ({ value: item.value, label: item.label }))
)

const drawerFields = computed<DrawerField[]>(() => {
  const item = props.candidate
  if (!item) return []
  return [
    createField('name', '姓名', 'text', item.name, { required: true }),
    createField('stage', '候选人阶段', 'select', item.stage, { options: stageOptions.value, displayValue: stageLabel(item.stage, item.stageName) }),
    createField('phone', '手机号', 'text', item.phone),
    createField('email', '邮箱', 'text', item.email),
    createField('wechat', '微信', 'text', item.wechat),
    createField('recruitmentJobName', '招聘岗位', 'text', item.recruitmentJobName || item.appliedPosition, { editable: false }),
    createField('currentCompany', '当前公司', 'text', item.currentCompany),
    createField('currentPosition', '当前职位', 'text', item.currentPosition),
    createField('education', '学历', 'text', item.education),
    createField('school', '学校', 'text', item.school),
    createField('major', '专业', 'text', item.major),
    createField('workYears', '工作年限', 'number', item.workYears, {
      displayValue: item.workYears === 0 || item.workYears ? `${item.workYears} 年` : '-'
    }),
    createField('expectedCity', '期望城市', 'text', item.expectedCity),
    createField('expectedSalary', '期望薪资', 'text', item.expectedSalary),
    createField('source', '来源', 'text', item.source),
    createField('ownerName', '负责人', 'text', item.ownerName, { editable: false }),
    createField('nextStepTime', '下一步时间', 'datetime', item.nextStepTime, { displayValue: formatDateTime(item.nextStepTime) }),
    createField('skillTags', '技能标签', 'textarea', item.skillTags),
    createField('jobRequirements', '岗位要求', 'textarea', item.jobRequirements),
    createField('remark', '备注', 'textarea', item.remark),
    createField('aiEvaluation', 'AI评估', 'textarea', item.aiEvaluation)
  ]
})

function createField(
  fieldName: string,
  fieldLabel: string,
  fieldType: FieldType,
  value: unknown,
  options: {
    displayValue?: string
    options?: FieldOption[]
    editable?: boolean
    required?: boolean
  } = {}
): DrawerField {
  return {
    fieldName,
    fieldLabel,
    fieldType,
    value,
    displayValue: options.displayValue ?? formatDisplayValue(value),
    options: options.options,
    editable: options.editable,
    isRequired: options.required
  }
}

async function handleInlineFieldSave(fieldName: string, value: unknown) {
  if (!props.candidate || fieldName === 'recruitmentJobName' || fieldName === 'ownerName') return
  const detail = await updateCandidateField({
    candidateId: String(props.candidate.candidateId),
    fieldName,
    value
  })
  emit('updated', detail)
}

async function handleAvatarUploaded(payload: { logo: string; logoUrl: string }) {
  if (!props.candidate) return
  avatarSaving.value = true
  try {
    const detail = await updateCandidateField({
      candidateId: String(props.candidate.candidateId),
      fieldName: 'avatar',
      value: payload.logo
    })
    detail.avatarUrl = payload.logoUrl || detail.avatarUrl
    emit('updated', detail)
  } finally {
    avatarSaving.value = false
  }
}

async function handleAvatarRemoved() {
  if (!props.candidate) return
  avatarSaving.value = true
  try {
    const detail = await updateCandidateField({
      candidateId: String(props.candidate.candidateId),
      fieldName: 'avatar',
      value: null
    })
    detail.avatarUrl = ''
    emit('updated', detail)
  } finally {
    avatarSaving.value = false
  }
}

function stageLabel(value?: string, fallback?: string) {
  return fallback
    || enumStore.candidateStageLabel(value)
    || CANDIDATE_STAGE_OPTIONS.find(option => option.value === value)?.label
    || value
    || '-'
}

function formatDisplayValue(value: unknown) {
  if (value === null || value === undefined || value === '') return '-'
  return String(value)
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN')
}
</script>
