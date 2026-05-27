<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="modelValue" class="fixed inset-0 z-[300] flex items-center justify-center p-4 sm:p-6">
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-slate-900/60 backdrop-blur-sm" @click="handleClose" />

        <!-- Modal Container -->
        <div :class="[
          'relative w-full bg-slate-50 shadow-2xl overflow-hidden flex flex-col wk-crm-el-field-scope',
          isMobile ? 'max-w-full max-h-full inset-0 rounded-[1rem]' : 'max-w-5xl max-h-[90vh] rounded-[2.5rem]'
        ]">
          <!-- Header -->
          <div class="bg-white border-b border-slate-200 px-6 sm:px-8 py-4 sm:py-5 flex items-center justify-between shrink-0">
            <div class="flex items-center gap-3 sm:gap-4">
              <div class="size-10 sm:size-12 rounded-2xl bg-primary/10 flex items-center justify-center text-primary">
                <span class="material-symbols-outlined">{{ isEdit ? 'edit' : 'person_add' }}</span>
              </div>
              <div>
                <h2 class="text-lg sm:text-xl font-bold text-slate-900">{{ isEdit ? '编辑客户' : '新增客户' }}</h2>
                <p class="text-xs text-slate-500">{{ isEdit ? '修改客户基本信息和联系方式' : '使用 AI 智能录入或手动填写客户信息' }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2 sm:gap-3">
              <button
                type="button"
                @click="handleClose"
                class="px-4 sm:px-6 py-2 sm:py-2.5 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-100 transition-colors whitespace-nowrap"
              >
                取消
              </button>
              <button
                type="button"
                @click="handleSubmit"
                :disabled="submitting"
                class="px-5 sm:px-8 py-2 sm:py-2.5 bg-primary text-white rounded-xl text-sm font-bold shadow-lg shadow-primary/20 hover:bg-primary/90 transition-all flex items-center gap-2 disabled:opacity-50 whitespace-nowrap"
              >
                <span v-if="submitting" class="size-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                <span v-else class="material-symbols-outlined text-sm">save</span>
                {{ isEdit ? '保存' : '创建客户' }}
              </button>
            </div>
          </div>

          <!-- Content -->
          <div class="flex-1 overflow-y-auto p-4 sm:p-6">
            <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
              <!-- Left Column -->
              <div :class="[isEdit ? 'lg:col-span-12' : 'lg:col-span-7', 'space-y-5']">
                <AiSmartEntrySection
                  v-if="!isEdit"
                  v-model="aiInputText"
                  placeholder="在此粘贴客户描述、邮件内容，或直接粘贴 (Ctrl+V) 名片图片..."
                  :ai-image-preview="aiImagePreview"
                  :ai-parsing="aiParsing"
                  :can-extract="Boolean(aiInputText.trim() || aiImageFile)"
                  :show-image-hint="Boolean(aiImagePreview && !aiParsing)"
                  @paste="handleAiPaste"
                  @extract="handleAiExtract"
                  @remove-image="removeAiImage"
                >
                  <template v-if="isMobile && aiParseResult" #after-actions>
                    <div class="p-3 bg-primary/5 rounded-xl border border-primary/10 text-xs text-slate-600 space-y-1">
                      <div v-if="aiParseResult.score != null" class="font-bold text-primary">潜力评分: {{ aiParseResult.score }}/100</div>
                      <div v-if="aiParseResult.summary">{{ aiParseResult.summary }}</div>
                      <div v-if="aiParseResult.tags?.length" class="flex flex-wrap gap-1 mt-1">
                        <span v-for="tag in aiParseResult.tags" :key="tag" class="px-2 py-0.5 bg-primary/10 text-primary text-xs font-bold rounded-full">{{ tag }}</span>
                      </div>
                    </div>
                  </template>
                </AiSmartEntrySection>

                <!-- Basic Information -->
                <section class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 sm:p-6">
                  <h3 class="text-xs font-bold text-slate-900 mb-5 flex items-center gap-2 uppercase tracking-wider">
                    <span class="w-1 h-3 bg-primary rounded-full"></span>
                    基础信息
                  </h3>
                  <div class="mb-5 space-y-1.5">
                    <label class="text-xs font-bold uppercase tracking-wider text-slate-500 ml-1">公司 LOGO</label>
                    <CustomerLogoUploader
                      :logo-url="customerLogoUrl"
                      :alt="formData.companyName || '公司 Logo'"
                      :disabled="submitting"
                      :size="72"
                      @uploaded="handleFormLogoUploaded"
                      @removed="handleFormLogoRemoved"
                    />
                  </div>
                  <DynamicFieldForm
                    ref="dynamicFieldFormRef"
                    entity-type="customer"
                    mode="form"
                    v-model="customerFieldValues"
                    :entity-id="isEdit ? (props.customer as any)?.customerId : null"
                    :full-span-field-names="['companyName', 'address', 'remark']"
                    class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4"
                  />
                  <div v-if="false" class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
                    <div class="md:col-span-2 space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">公司名称 <span class="text-red-400">*</span></label>
                      <el-input
                        v-model="formData.companyName"
                        placeholder="请输入公司全称"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">所属行业</label>
                      <el-input
                        v-model="formData.industry"
                        placeholder="例如：互联网 / 科技"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">客户级别</label>
                      <el-select v-model="formData.level" class="w-full wk-crm-el-field-select" size="large">
                        <el-option label="A级客户" value="A" />
                        <el-option label="B级客户" value="B" />
                        <el-option label="C级客户" value="C" />
                      </el-select>
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">商机阶段</label>
                      <el-select v-model="formData.stage" class="w-full wk-crm-el-field-select" size="large">
                        <el-option label="线索" value="lead" />
                        <el-option label="资格审查" value="qualified" />
                        <el-option label="方案报价" value="proposal" />
                        <el-option label="谈判中" value="negotiation" />
                        <el-option label="已成交" value="closed" />
                        <el-option label="已流失" value="lost" />
                      </el-select>
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">来源</label>
                      <el-input
                        v-model="formData.source"
                        placeholder="例如：官网咨询 / 展会 / 朋友介绍"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">网站</label>
                      <el-input
                        v-model="formData.website"
                        placeholder="例如：https://example.com"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">预计成交金额</label>
                      <el-input
                        v-model.number="formData.quotation"
                        type="number"
                        placeholder="请输入预计成交金额"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                    <div class="md:col-span-2 space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">地址</label>
                      <el-input
                        v-model="formData.address"
                        type="textarea"
                        :autosize="{ minRows: 2, maxRows: 4 }"
                        placeholder="请输入客户地址"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>

                    <DynamicFieldForm
                      ref="dynamicFieldFormRef"
                      entity-type="customer"
                      v-model="customerFieldValues"
                    />
                  </div>
                </section>

                <!-- Contact Information -->
                <section class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 sm:p-6">
                  <h3 class="text-xs font-bold text-slate-900 mb-5 flex items-center gap-2 uppercase tracking-wider">
                    <span class="w-1 h-3 bg-indigo-500 rounded-full"></span>
                    联系人信息
                  </h3>
                  <div class="grid grid-cols-1 md:grid-cols-2 gap-x-6 gap-y-4">
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">主要联系人</label>
                      <el-input
                        v-model="formData.contactName"
                        placeholder="请输入联系人姓名"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">手机号码</label>
                      <el-input
                        v-model="formData.contactPhone"
                        placeholder="请输入联系电话"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                    <div class="space-y-1.5 md:col-span-2">
                      <label class="text-xs font-bold text-slate-500 uppercase ml-1">电子邮箱</label>
                      <el-input
                        v-model="formData.contactEmail"
                        placeholder="请输入邮箱地址"
                        size="large"
                        class="w-full wk-crm-el-field-input"
                      />
                    </div>
                  </div>
                </section>
              </div>

              <div v-if="!isEdit" class="lg:col-span-5 space-y-5" :class="{ 'hidden': isMobile }">
                <AiParseInsightSidebar
                  :result="aiParseResult"
                  empty-description="录入客户信息或使用智能提取后，AI 将在此为您提供深度洞察与潜力评估。"
                >
                  <template #tip>
                    <p class="text-xs text-slate-600 leading-relaxed">
                      您可以直接粘贴该客户的简介、公司官网文字，或者<strong>直接在输入框 Ctrl+V 粘贴名片图片</strong>，AI 会自动识别并补全信息。
                    </p>
                  </template>
                </AiParseInsightSidebar>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useResponsive } from '@/composables/useResponsive'
import { useCustomerStore } from '@/stores/customer'
import { aiParseCustomer } from '@/api/customer'
import type { CustomerAiParseVO } from '@/api/customer'
import { getPresignedUploadUrl, uploadToMinIO } from '@/api/file'
import { isRequestErrorHandled } from '@/utils/requestError'
import DynamicFieldForm from '@/components/DynamicFieldForm.vue'
import AiSmartEntrySection from '@/components/crm/AiSmartEntrySection.vue'
import AiParseInsightSidebar from '@/components/crm/AiParseInsightSidebar.vue'
import CustomerLogoUploader from './CustomerLogoUploader.vue'
import type { CustomerAddBO, CustomerDetailVO, CustomerLevel, CustomerListVO, CustomerStage } from '@/types/customer'
import type { CustomField } from '@/types/customField'

type Mode = 'create' | 'edit'
type CustomerLike = CustomerListVO | CustomerDetailVO | null

const ALLOWED_STAGES: CustomerStage[] = ['lead', 'qualified', 'proposal', 'negotiation', 'closed', 'lost']

const props = defineProps<{
  modelValue: boolean
  mode: Mode
  customer?: CustomerLike
  /** 新建时预选的商机阶段（如阶段看板「+」带入） */
  initialStage?: CustomerStage | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'success', payload: { mode: Mode; customerId?: string; detail?: CustomerDetailVO | null }): void
}>()

const { isMobile } = useResponsive()
const customerStore = useCustomerStore()

const isEdit = computed(() => props.mode === 'edit')

const submitting = ref(false)
const dynamicFieldFormRef = ref<InstanceType<typeof DynamicFieldForm>>()
const customerFieldValues = ref<Record<string, any>>({})
const customerLogoUrl = ref('')
const customerLogoTouched = ref(false)

const formData = reactive<CustomerAddBO>({
  companyName: '',
  industry: '',
  level: undefined,
  stage: 'lead',
  source: '',
  website: '',
  logo: '',
  address: '',
  quotation: undefined,
  nextFollowTime: undefined,
  remark: '',
  description: '',
  contactName: '',
  contactPhone: '',
  contactEmail: ''
})

// AI Smart Input state (create only)
const aiInputText = ref('')
const aiParsing = ref(false)
const aiParseResult = ref<CustomerAiParseVO | null>(null)
const aiImageFile = ref<File | null>(null)
const aiImagePreview = ref<string | null>(null)

const CUSTOMER_SYSTEM_FIELD_NAMES = new Set([
  'companyName',
  'industry',
  'stage',
  'level',
  'source',
  'website',
  'quotation',
  'address',
  'nextFollowTime',
  'remark'
])

function padDateTimePart(value: number): string {
  return String(value).padStart(2, '0')
}

function normalizeDateTimeValue(value: unknown): string | undefined {
  if (value === null || value === undefined || value === '') {
    return undefined
  }

  const rawValue = String(value).trim()
  if (!rawValue) {
    return undefined
  }

  const normalizedValue = rawValue.replace('T', ' ').replace(/\.\d+Z?$/, '')
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(normalizedValue)) {
    return normalizedValue
  }
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(normalizedValue)) {
    return `${normalizedValue}:00`
  }

  const parsedDate = new Date(rawValue)
  if (Number.isNaN(parsedDate.getTime())) {
    return rawValue
  }

  return [
    parsedDate.getFullYear(),
    padDateTimePart(parsedDate.getMonth() + 1),
    padDateTimePart(parsedDate.getDate())
  ].join('-') + ` ${[
    padDateTimePart(parsedDate.getHours()),
    padDateTimePart(parsedDate.getMinutes()),
    padDateTimePart(parsedDate.getSeconds())
  ].join(':')}`
}

function buildCustomerFieldValues(c: CustomerLike): Record<string, any> {
  if (!c) {
    return {}
  }

  const anyCustomer = c as any
  return {
    companyName: c.companyName || '',
    industry: c.industry || '',
    level: (c.level || null) as CustomerLevel | null,
    stage: (c.stage || 'lead') as CustomerStage,
    source: anyCustomer.source || '',
    website: anyCustomer.website || '',
    quotation: anyCustomer.quotation ?? undefined,
    address: anyCustomer.address || '',
    nextFollowTime: normalizeDateTimeValue(anyCustomer.nextFollowTime),
    remark: anyCustomer.remark || '',
    ...(c.customFields ? { ...c.customFields } : {})
  }
}

function getDynamicFormFields(): CustomField[] {
  const exposedFields = dynamicFieldFormRef.value?.fields as CustomField[] | { value?: CustomField[] } | undefined
  if (Array.isArray(exposedFields)) {
    return exposedFields
  }
  if (exposedFields && Array.isArray(exposedFields.value)) {
    return exposedFields.value
  }
  return []
}

function getDynamicFormValues(): Record<string, any> {
  const exposedValues = dynamicFieldFormRef.value?.localValues as Record<string, any> | { value?: Record<string, any> } | undefined
  if (exposedValues && typeof exposedValues === 'object' && !Array.isArray(exposedValues)) {
    if ('value' in exposedValues && exposedValues.value && typeof exposedValues.value === 'object') {
      return { ...exposedValues.value }
    }
    return { ...exposedValues }
  }
  return { ...customerFieldValues.value }
}

function splitCustomerFieldValues() {
  const fieldMap = new Map(getDynamicFormFields().map(field => [field.fieldName, field]))
  const currentValues = getDynamicFormValues()
  const systemValues: Record<string, any> = {}
  const customValues: Record<string, any> = {}

  for (const [fieldName, fieldValue] of Object.entries(currentValues)) {
    const field = fieldMap.get(fieldName)
    const isSystemField = field ? field.fieldSource === 'system' : CUSTOMER_SYSTEM_FIELD_NAMES.has(fieldName)

    if (isSystemField) {
      systemValues[fieldName] = fieldValue
    } else {
      customValues[fieldName] = fieldValue
    }
  }

  return { systemValues, customValues }
}

function getPrimaryContactFromCustomer(c: CustomerLike): { name?: string; phone?: string; email?: string } {
  if (!c) return {}
  const anyC = c as any
  if (anyC.primaryContactName || anyC.primaryContactPhone) {
    return { name: anyC.primaryContactName, phone: anyC.primaryContactPhone, email: anyC.primaryContactEmail }
  }
  const contacts = Array.isArray(anyC.contacts) ? anyC.contacts : []
  const primary = contacts.find((x: any) => x?.isPrimary) || contacts[0]
  return primary ? { name: primary.name, phone: primary.phone, email: primary.email } : {}
}

function hydrateFromCustomer() {
  dynamicFieldFormRef.value?.clearUniqueFieldErrors()
  const c = props.customer ?? null
  Object.assign(formData, {
    companyName: c?.companyName || '',
    industry: c?.industry || '',
    level: (c?.level || undefined) as CustomerLevel | undefined,
    stage: (c?.stage || 'lead') as CustomerStage,
    source: (c?.source || '') as any,
    website: (c?.website || '') as any,
    logo: ((c as any)?.logo || '') as any,
    address: (c?.address || '') as any,
    quotation: (c?.quotation ?? undefined) as any,
    nextFollowTime: normalizeDateTimeValue((c as any)?.nextFollowTime) as any,
    remark: ((c as any)?.remark || '') as any,
    description: (c?.description || '') as any
  })
  const pc = getPrimaryContactFromCustomer(c)
  formData.contactName = pc.name || ''
  formData.contactPhone = pc.phone || ''
  formData.contactEmail = pc.email || ''
  customerLogoUrl.value = ((c as any)?.logoUrl || '') as string
  customerLogoTouched.value = false
  customerFieldValues.value = buildCustomerFieldValues(c)
}

function applyCreateInitialStage() {
  const s = props.initialStage
  if (!s || !ALLOWED_STAGES.includes(s)) return
  formData.stage = s
  customerFieldValues.value = { ...customerFieldValues.value, stage: s }
}

function resetAll() {
  dynamicFieldFormRef.value?.clearUniqueFieldErrors()
  Object.assign(formData, {
    companyName: '',
    industry: '',
    level: undefined,
    stage: 'lead',
    source: '',
    website: '',
    logo: '',
    address: '',
    quotation: undefined,
    nextFollowTime: undefined,
    remark: '',
    description: '',
    contactName: '',
    contactPhone: '',
    contactEmail: ''
  })
  customerLogoUrl.value = ''
  customerLogoTouched.value = false
  customerFieldValues.value = { level: null }
  aiInputText.value = ''
  aiParsing.value = false
  aiParseResult.value = null
  removeAiImage()
}

function handleClose() {
  emit('update:modelValue', false)
  resetAll()
}

// Keep local form in sync when opening / switching mode
watch(
  () => [props.modelValue, props.mode, props.customer, props.initialStage] as const,
  ([open]) => {
    if (!open) return
    if (props.mode === 'edit') hydrateFromCustomer()
    else {
      resetAll()
      applyCreateInitialStage()
    }
  }
)

function handleAiPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  for (let i = 0; i < items.length; i++) {
    if (items[i].kind === 'file' && items[i].type.startsWith('image/')) {
      const file = items[i].getAsFile()
      if (file) {
        aiImageFile.value = file
        aiImagePreview.value = URL.createObjectURL(file)
      }
    }
  }
}

function removeAiImage() {
  if (aiImagePreview.value) URL.revokeObjectURL(aiImagePreview.value)
  aiImageFile.value = null
  aiImagePreview.value = null
}

function handleFormLogoUploaded(payload: { logo: string; logoUrl: string }) {
  formData.logo = payload.logo
  customerLogoUrl.value = payload.logoUrl
  customerLogoTouched.value = true
}

function handleFormLogoRemoved() {
  formData.logo = ''
  customerLogoUrl.value = ''
  customerLogoTouched.value = true
}

async function handleAiExtract() {
  if (aiParsing.value) return
  if (!aiInputText.value.trim() && !aiImageFile.value) {
    ElMessage.warning('请输入文本或粘贴图片')
    return
  }

  aiParsing.value = true
  try {
    let imageObjectKey: string | undefined
    let imageMimeType: string | undefined

    if (aiImageFile.value) {
      const presigned = await getPresignedUploadUrl(aiImageFile.value.name, aiImageFile.value.type)
      await uploadToMinIO(aiImageFile.value, presigned.uploadUrl)
      imageObjectKey = presigned.objectKey
      imageMimeType = aiImageFile.value.type
    }

    const result = await aiParseCustomer({
      content: aiInputText.value.trim() || '请从图片中提取客户信息',
      imageObjectKey,
      imageMimeType
    })
    aiParseResult.value = result

    if (!hasCustomerFieldFilled(result)) {
      ElMessage.warning('本次智能解析没有提取到可填充的信息，请确认图片清晰或补充文字后重试')
      return
    }

    if (result.companyName) {
      formData.companyName = result.companyName
      customerFieldValues.value.companyName = result.companyName
    }
    if (result.industry) {
      formData.industry = result.industry
      customerFieldValues.value.industry = result.industry
    }
    if (result.level && ['A', 'B', 'C'].includes(result.level)) {
      formData.level = result.level as CustomerLevel
      customerFieldValues.value.level = result.level as CustomerLevel
    }
    if (result.stage && ['lead', 'qualified', 'proposal', 'negotiation', 'closed', 'lost'].includes(result.stage)) {
      formData.stage = result.stage as CustomerStage
      customerFieldValues.value.stage = result.stage as CustomerStage
    }
    if (result.source) {
      formData.source = result.source
      customerFieldValues.value.source = result.source
    }
    if (result.website) {
      formData.website = result.website
      customerFieldValues.value.website = result.website
    }
    if (result.remark) {
      formData.remark = result.remark
      customerFieldValues.value.remark = result.remark
    }
    if (result.contactName) formData.contactName = result.contactName
    if (result.contactPhone) formData.contactPhone = result.contactPhone
    if (result.contactEmail) formData.contactEmail = result.contactEmail

    ElMessage.success('AI 提取完成，信息已自动填充')
  } catch (error: unknown) {
    console.error('AI parse customer failed:', error)
    if (!isRequestErrorHandled(error)) {
      const message = error instanceof Error ? error.message : '未知错误'
      ElMessage.error('AI 解析失败: ' + message)
    }
  } finally {
    aiParsing.value = false
  }
}

function hasCustomerFieldFilled(result: CustomerAiParseVO | null): boolean {
  if (!result) return false
  return Boolean(
    result.companyName
    || result.industry
    || result.level
    || result.stage
    || result.source
    || result.website
    || result.remark
    || result.contactName
    || result.contactPhone
    || result.contactEmail
  )
}

async function handleSubmit() {
  const currentFieldValues = getDynamicFormValues()
  const companyName = String(currentFieldValues.companyName ?? formData.companyName ?? '').trim()

  if (!companyName) {
    ElMessage.warning('请输入公司名称')
    return
  }

  if (dynamicFieldFormRef.value) {
    const missingFields = dynamicFieldFormRef.value.getRequiredFieldLabels()
    if (missingFields.length > 0) {
      ElMessage.warning(`请填写必填字段: ${missingFields.join(', ')}`)
      return
    }
    const uniqueValid = await dynamicFieldFormRef.value.validateUniqueFields()
    if (!uniqueValid) {
      return
    }
  }

  submitting.value = true
  try {
    const { systemValues, customValues } = splitCustomerFieldValues()
    const submitData = {
      ...formData,
      ...systemValues,
      companyName,
      logo: isEdit.value && !customerLogoTouched.value ? undefined : formData.logo,
      aiParseSnapshot: !isEdit.value && aiParseResult.value ? JSON.stringify(aiParseResult.value) : undefined,
      customFields: customValues
    }

    if (props.mode === 'edit') {
      const customerId = (props.customer as any)?.customerId
      if (!customerId) {
        ElMessage.error('缺少 customerId，无法保存')
        return
      }
      const detail = await customerStore.editCustomer({ ...submitData, customerId })
      ElMessage.success('更新成功')
      emit('success', { mode: 'edit', customerId, detail })
    } else {
      const customerId = await customerStore.createCustomer(submitData)
      ElMessage.success('创建成功')
      emit('success', { mode: 'create', customerId: String(customerId) })
    }

    emit('update:modelValue', false)
    resetAll()
  } finally {
    submitting.value = false
  }
}
</script>
