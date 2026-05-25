<template>
  <el-drawer
    :model-value="modelValue"
    direction="rtl"
    :size="isMobile ? '100%' : '420px'"
    :with-header="false"
    destroy-on-close
    class="customer-basic-info-drawer"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-if="customer" class="flex h-full flex-col bg-white shadow-2xl">
      <div class="flex shrink-0 items-center justify-between border-b border-slate-100 bg-slate-50/60 px-6 py-5">
        <div class="flex min-w-0 items-center gap-3">
          <div class="flex size-10 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary">
            <span class="material-symbols-outlined text-base leading-none">description</span>
          </div>
          <div class="min-w-0">
            <h3 class="truncate text-base font-bold text-slate-900">{{ drawerTitle }}</h3>
            <p class="truncate text-xs text-slate-400">{{ drawerSubtitle }}</p>
          </div>
        </div>
        <div class="flex shrink-0 items-center gap-1">
          <button
            v-if="canEditCustomer"
            type="button"
            class="flex size-8 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-200 hover:text-slate-700"
            title="编辑客户"
            aria-label="编辑客户"
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

      <div class="min-h-0 flex-1 overflow-y-auto px-6 py-6">
        <div class="space-y-5 text-left">
          <div>
            <p class="mb-2 text-xs font-bold uppercase tracking-wider text-slate-400">公司 LOGO</p>
            <CustomerLogoUploader
              :logo-url="customer.logoUrl"
              :alt="customer.companyName || '公司 Logo'"
              :disabled="!canEditCustomer || logoSaving"
              :size="64"
              @uploaded="handleLogoUploaded"
              @removed="handleLogoRemoved"
            />
          </div>

          <section
            v-if="currentAiReportVisible"
            class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4"
          >
            <div class="flex items-center gap-2">
              <span class="material-symbols-outlined text-base text-primary">auto_awesome</span>
              <p class="text-xs font-bold uppercase tracking-wider text-slate-500">{{ aiReportSummaryTitle }}</p>
            </div>
            <div v-if="currentAiStatusDetection" class="mt-3">
              <span
                v-if="getAiStatusMeta(currentAiStatusDetection)"
                class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-bold shadow-sm"
                :class="getAiStatusMeta(currentAiStatusDetection)?.badgeClass"
              >
                <span class="size-1.5 rounded-full mr-1.5" :class="getAiStatusMeta(currentAiStatusDetection)?.dotClass"></span>
                {{ getAiStatusMeta(currentAiStatusDetection)?.label }}
              </span>
              <p v-else class="text-sm leading-6 text-slate-700">{{ currentAiStatusDetection }}</p>
            </div>
            <p class="mt-3 text-sm leading-6 text-slate-700 whitespace-pre-wrap break-words">
              {{ currentAiInsight || '暂无 AI 报告摘要' }}
            </p>
          </section>

          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">公司全称</p>
            <InlineEditableField
              :model-value="customer.companyName"
              :field="customerInlineFields.companyName"
              :display-value="customer.companyName || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('companyName', value)"
            >
              <p class="text-sm text-slate-900 font-medium">{{ customer.companyName }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">所属行业</p>
            <InlineEditableField
              :model-value="customer.industry"
              :field="customerInlineFields.industry"
              :display-value="customer.industry || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('industry', value)"
            >
              <p class="text-sm text-slate-900 font-medium">{{ customer.industry || '-' }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户来源</p>
            <InlineEditableField
              :model-value="customer.source"
              :field="customerInlineFields.source"
              :display-value="customer.source || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('source', value)"
            >
              <p class="text-sm text-slate-900 font-medium">{{ customer.source || '-' }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">主要联系人</p>
            <InlineEditableField
              :model-value="primaryContact?.name"
              :field="primaryContactInlineFields.primaryContactName"
              :display-value="primaryContact?.name || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('primaryContactName', value, 'contact')"
            >
              <p class="text-sm text-slate-900 font-medium">{{ primaryContact?.name || '-' }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">联系电话</p>
            <InlineEditableField
              :model-value="primaryContact?.phone"
              :field="primaryContactInlineFields.primaryContactPhone"
              :display-value="primaryContact?.phone || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('primaryContactPhone', value, 'contact')"
            >
              <p class="text-sm text-slate-900 font-medium font-mono">{{ primaryContact?.phone || '-' }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">电子邮箱</p>
            <InlineEditableField
              :model-value="primaryContact?.email"
              :field="primaryContactInlineFields.primaryContactEmail"
              :display-value="primaryContact?.email || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('primaryContactEmail', value, 'contact')"
            >
              <p class="text-sm text-slate-900 font-medium break-all">{{ primaryContact?.email || '-' }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">客户地址</p>
            <InlineEditableField
              :model-value="customer.address"
              :field="customerInlineFields.address"
              :display-value="customer.address || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('address', value)"
            >
              <p class="text-sm text-slate-900 font-medium whitespace-pre-wrap break-words">{{ customer.address || '-' }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">公司网站</p>
            <InlineEditableField
              :model-value="customer.website"
              :field="customerInlineFields.website"
              :display-value="customer.website || '-'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('website', value)"
            >
              <a
                v-if="customer.website"
                :href="customer.website"
                target="_blank"
                rel="noopener noreferrer"
                class="text-sm font-medium text-primary hover:underline break-all"
                data-row-action="true"
              >
                {{ customer.website }}
              </a>
              <p v-else class="text-sm text-slate-900 font-medium">-</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">预计成交金额</p>
            <InlineEditableField
              :model-value="customer.quotation"
              :field="customerInlineFields.quotation"
              :display-value="formatAmount(customer.quotation)"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('quotation', value)"
            >
              <p class="text-sm text-slate-900 font-medium">{{ formatAmount(customer.quotation) }}</p>
            </InlineEditableField>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">负责人</p>
            <p class="text-sm text-slate-900 font-medium">{{ customer.ownerName || '-' }}</p>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">创建人</p>
            <p class="text-sm text-slate-900 font-medium">{{ customer.createUserName || customer.createUserId || '-' }}</p>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">创建时间</p>
            <p class="text-sm text-slate-900 font-medium">{{ formatDateTime(customer.createTime) }}</p>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">更新时间</p>
            <p class="text-sm text-slate-900 font-medium">{{ formatDateTime(customer.updateTime) }}</p>
          </div>
          <div>
            <p class="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">备注</p>
            <InlineEditableField
              :model-value="customer.remark"
              :field="customerInlineFields.remark"
              :display-value="customer.remark || '暂无备注'"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave('remark', value)"
            >
              <p class="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap break-words">{{ customer.remark || '暂无备注' }}</p>
            </InlineEditableField>
          </div>
          <div v-for="field in customFields" :key="`drawer-${field.fieldId}`">
            <p class="text-xs font-bold text-slate-400 tracking-wider mb-1">{{ field.fieldLabel }}</p>
            <InlineEditableField
              :model-value="customer.customFields?.[field.fieldName]"
              :field="field"
              :display-value="formatCustomFieldDisplayValue(field, customer.customFields?.[field.fieldName])"
              :editable="canEditCustomer"
              :save-handler="(value) => handleInlineDetailFieldSave(field.fieldName, value, 'custom')"
            >
              <p class="text-sm text-slate-900 font-medium whitespace-pre-wrap break-words">{{ formatCustomFieldDisplayValue(field, customer.customFields?.[field.fieldName]) }}</p>
            </InlineEditableField>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useCustomerStore } from '@/stores/customer'
import { useUserStore } from '@/stores/user'
import { useResponsive } from '@/composables/useResponsive'
import InlineEditableField from '@/components/common/InlineEditableField.vue'
import CustomerLogoUploader from './CustomerLogoUploader.vue'
import type { Contact, CustomerAiReportVO, CustomerDetailVO } from '@/types/customer'
import type { CustomField } from '@/types/customField'
import { compactCustomerAiInsight, getCustomerAiStatusMeta } from '@/utils/customerAi'
import { formatCustomFieldValue as formatCustomFieldDisplayValue } from '@/utils/customFieldDisplay'

const props = defineProps<{
  modelValue: boolean
  customer: CustomerDetailVO | null
  contacts: Contact[]
  customFields: CustomField[]
  latestAiReport?: CustomerAiReportVO | null
}>()

const emit = defineEmits<{
  'update:modelValue': [open: boolean]
  contactsUpdated: [contacts: Contact[]]
  edit: []
}>()

const { isMobile } = useResponsive()
const customerStore = useCustomerStore()
const userStore = useUserStore()

const drawerTitle =  '\u57fa\u672c\u4fe1\u606f'
const drawerSubtitle = '\u67e5\u770b\u5e76\u7ef4\u62a4\u5ba2\u6237\u8be6\u7ec6\u8d44\u6599'
const aiReportSummaryTitle = 'AI \u62a5\u544a\u6458\u8981'

const customerInlineFields: Record<string, Partial<CustomField>> = {
  companyName: { fieldName: 'companyName', fieldLabel: '公司全称', fieldType: 'text', isRequired: true },
  industry: { fieldName: 'industry', fieldLabel: '所属行业', fieldType: 'text' },
  source: { fieldName: 'source', fieldLabel: '客户来源', fieldType: 'text' },
  address: { fieldName: 'address', fieldLabel: '客户地址', fieldType: 'textarea' },
  website: { fieldName: 'website', fieldLabel: '公司网站', fieldType: 'text' },
  quotation: { fieldName: 'quotation', fieldLabel: '预计成交金额', fieldType: 'number' },
  remark: { fieldName: 'remark', fieldLabel: '备注', fieldType: 'textarea' }
}

const primaryContactInlineFields: Record<string, Partial<CustomField>> = {
  primaryContactName: { fieldName: 'primaryContactName', fieldLabel: '主要联系人', fieldType: 'text' },
  primaryContactPhone: { fieldName: 'primaryContactPhone', fieldLabel: '联系电话', fieldType: 'text' },
  primaryContactEmail: { fieldName: 'primaryContactEmail', fieldLabel: '电子邮箱', fieldType: 'text' }
}

const canEditCustomer = computed(() => userStore.hasPermission('customer:edit'))
const logoSaving = ref(false)

function isPrimaryContact(contact?: Pick<Contact, 'isPrimary'> | null): boolean {
  const value = contact?.isPrimary as boolean | number | string | undefined
  return value === true || value === 1 || value === '1'
}

const primaryContact = computed(() =>
  props.contacts.find(contact => isPrimaryContact(contact)) || props.contacts[0] || null
)

const currentAiStatusDetection = computed(() =>
  (props.latestAiReport?.aiStatusDetection || props.customer?.aiStatusDetection || '').trim()
)

const currentAiInsight = computed(() =>
  compactCustomerAiInsight(props.latestAiReport?.aiInsight || props.customer?.aiInsight)
)

const currentAiReportVisible = computed(() =>
  !!currentAiStatusDetection.value || !!currentAiInsight.value
)

function getAiStatusMeta(value: string | undefined | null) {
  return getCustomerAiStatusMeta(value)
}

function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function formatAmount(value?: number | null): string {
  if (value === null || value === undefined) return '-'
  const amount = Number(value)
  if (!Number.isFinite(amount)) return '-'
  return `¥ ${amount.toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  })}`
}

async function handleInlineDetailFieldSave(
  fieldName: string,
  value: unknown,
  fieldSource: 'system' | 'custom' | 'contact' = 'system'
) {
  if (!props.customer) return
  const customerId = props.customer.customerId
  const detail = await customerStore.editCustomerField(
    { customerId, fieldName, fieldSource, value },
    { refreshList: false }
  )

  if (fieldSource === 'contact') {
    emit('contactsUpdated', detail.contacts || [])
  }
  ElMessage.success('保存成功')
}

async function saveLogoValue(logo: string, successMessage: string) {
  if (!props.customer || logoSaving.value) return
  logoSaving.value = true
  try {
    await customerStore.editCustomerField(
      {
        customerId: props.customer.customerId,
        fieldName: 'logo',
        fieldSource: 'system',
        value: logo
      },
      { refreshList: true }
    )
    ElMessage.success(successMessage)
  } finally {
    logoSaving.value = false
  }
}

async function handleLogoUploaded(payload: { logo: string; logoUrl: string }) {
  await saveLogoValue(payload.logo, 'Logo 保存成功')
}

async function handleLogoRemoved() {
  await saveLogoValue('', 'Logo 已移除')
}
</script>

<style>
.customer-basic-info-drawer .el-drawer__body {
  padding: 0 !important;
}
</style>
