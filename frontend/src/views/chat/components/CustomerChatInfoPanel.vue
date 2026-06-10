<template>
  <aside
    class="relative flex h-full flex-col border-l border-[#ececec] bg-white transition-[width] duration-150"
    :style="{ width: collapsed ? '44px' : `${panelWidth}px` }"
  >
    <button
      v-if="collapsed"
      type="button"
      class="flex h-full w-full items-start justify-center pt-4 text-[#5f6368] transition-colors hover:bg-[#f9f9f9]"
      title="展开客户信息"
      @click="collapsed = false"
    >
      <span class="material-symbols-outlined text-[20px]">chevron_left</span>
    </button>

    <template v-else>
      <div
        class="absolute left-0 top-0 z-10 h-full w-1 cursor-col-resize hover:bg-primary/20"
        @mousedown.prevent="startResize"
      />

      <header class="flex shrink-0 items-center justify-between gap-3 border-b border-[#ececec] px-4 py-3">
        <div class="min-w-0">
          <p class="truncate text-[15px] font-semibold text-[#0d0d0d]">{{ customer?.companyName || '客户信息' }}</p>
          <p class="mt-0.5 truncate text-xs text-[#8f8f8f]">{{ fullDetailMode ? '完整详情' : '关键信息' }}</p>
        </div>
        <div class="flex shrink-0 items-center gap-1">
          <button
            type="button"
            class="rounded-lg px-2.5 py-1.5 text-xs font-medium text-primary transition-colors hover:bg-primary/10"
            @click="basicDrawerOpen = true"
          >
            基本信息
          </button>
          <button
            type="button"
            class="flex size-8 items-center justify-center rounded-lg text-[#8f8f8f] transition-colors hover:bg-[#f3f3f3]"
            title="收起"
            @click="collapsed = true"
          >
            <span class="material-symbols-outlined text-[18px]">chevron_right</span>
          </button>
        </div>
      </header>

      <div v-if="loading" class="flex flex-1 items-center justify-center text-[#8f8f8f]">
        <span class="material-symbols-outlined animate-spin text-[22px]">progress_activity</span>
      </div>

      <div v-else-if="!customer" class="flex flex-1 items-center justify-center px-6 text-center text-sm text-[#8f8f8f]">
        暂无客户信息
      </div>

      <div v-else class="min-h-0 flex-1 overflow-y-auto px-4 py-4">
        <div v-if="fullDetailMode" class="space-y-5">
          <section>
            <h3 class="mb-2 text-sm font-semibold text-[#0d0d0d]">概览</h3>
            <div class="grid grid-cols-2 gap-2 text-sm">
              <InfoItem label="阶段" :value="customer.stageName || customer.stage" />
              <InfoItem label="等级" :value="customer.level" />
              <InfoItem label="行业" :value="customer.industry" />
              <InfoItem label="来源" :value="customer.source" />
              <InfoItem label="负责人" :value="customer.ownerName" />
              <InfoItem label="预计金额" :value="formatMoney(customer.quotation)" />
            </div>
          </section>

          <section>
            <h3 class="mb-2 text-sm font-semibold text-[#0d0d0d]">AI分析</h3>
            <div class="space-y-2 rounded-lg bg-[#f7f7f7] p-3 text-sm leading-6 text-[#333]">
              <p v-if="customer.aiStatusDetection"><span class="font-medium">状态：</span>{{ customer.aiStatusDetection }}</p>
              <p class="whitespace-pre-wrap">{{ customer.aiInsight || '暂无 AI 分析' }}</p>
            </div>
          </section>

          <section>
            <h3 class="mb-2 text-sm font-semibold text-[#0d0d0d]">最近活动</h3>
            <div class="space-y-2">
              <div
                v-for="item in recentActivities"
                :key="item.key"
                class="rounded-lg border border-[#ececec] px-3 py-2 text-sm"
              >
                <p class="line-clamp-2 text-[#0d0d0d]">{{ item.content }}</p>
                <p class="mt-1 text-xs text-[#8f8f8f]">{{ item.time }}</p>
              </div>
              <p v-if="recentActivities.length === 0" class="text-sm text-[#8f8f8f]">暂无活动</p>
            </div>
          </section>

          <section>
            <h3 class="mb-2 text-sm font-semibold text-[#0d0d0d]">关联业务</h3>
            <div class="grid grid-cols-3 gap-2 text-center">
              <MetricItem label="联系人" :value="customer.contacts?.length || 0" />
              <MetricItem label="任务" :value="customer.tasks?.length || 0" />
              <MetricItem label="文档" :value="customer.documents?.length || 0" />
            </div>
          </section>

          <section>
            <h3 class="mb-2 text-sm font-semibold text-[#0d0d0d]">联系人</h3>
            <div class="space-y-2">
              <div
                v-for="contact in customer.contacts?.slice(0, 8)"
                :key="contact.contactId"
                class="rounded-lg border border-[#ececec] px-3 py-2 text-sm"
              >
                <p class="font-medium text-[#0d0d0d]">{{ contact.name }}</p>
                <p class="mt-1 truncate text-xs text-[#8f8f8f]">{{ contact.position || '-' }} {{ contact.phone || '' }}</p>
              </div>
              <p v-if="!customer.contacts?.length" class="text-sm text-[#8f8f8f]">暂无联系人</p>
            </div>
          </section>

          <section>
            <h3 class="mb-2 text-sm font-semibold text-[#0d0d0d]">任务</h3>
            <div class="space-y-2">
              <div
                v-for="task in customer.tasks?.slice(0, 8)"
                :key="task.taskId"
                class="rounded-lg border border-[#ececec] px-3 py-2 text-sm"
              >
                <p class="line-clamp-2 font-medium text-[#0d0d0d]">{{ task.title }}</p>
                <p class="mt-1 text-xs text-[#8f8f8f]">{{ task.status }} · {{ task.dueDate || '无截止时间' }}</p>
              </div>
              <p v-if="!customer.tasks?.length" class="text-sm text-[#8f8f8f]">暂无任务</p>
            </div>
          </section>

          <section>
            <h3 class="mb-2 text-sm font-semibold text-[#0d0d0d]">文档中心</h3>
            <div class="space-y-2">
              <div
                v-for="document in customer.documents?.slice(0, 8)"
                :key="document.knowledgeId"
                class="rounded-lg border border-[#ececec] px-3 py-2 text-sm"
              >
                <p class="line-clamp-2 font-medium text-[#0d0d0d]">{{ document.name }}</p>
                <p class="mt-1 text-xs text-[#8f8f8f]">{{ document.type || 'document' }}</p>
              </div>
              <p v-if="!customer.documents?.length" class="text-sm text-[#8f8f8f]">暂无文档</p>
            </div>
          </section>
        </div>

        <el-collapse v-else v-model="openSections" class="customer-chat-panel-collapse">
          <el-collapse-item title="AI分析" name="ai">
            <div class="space-y-2 text-sm leading-6 text-[#333]">
              <p v-if="customer.aiStatusDetection"><span class="font-medium">状态：</span>{{ customer.aiStatusDetection }}</p>
              <p class="whitespace-pre-wrap">{{ customer.aiInsight || '暂无 AI 分析' }}</p>
            </div>
          </el-collapse-item>
          <el-collapse-item title="最近活动" name="activities">
            <div class="space-y-2">
              <div v-for="item in recentActivities" :key="item.key" class="text-sm">
                <p class="line-clamp-2 text-[#0d0d0d]">{{ item.content }}</p>
                <p class="text-xs text-[#8f8f8f]">{{ item.time }}</p>
              </div>
              <p v-if="recentActivities.length === 0" class="text-sm text-[#8f8f8f]">暂无活动</p>
            </div>
          </el-collapse-item>
          <el-collapse-item title="关联业务模块" name="modules">
            <div class="grid grid-cols-3 gap-2 text-center">
              <MetricItem label="联系人" :value="customer.contacts?.length || 0" />
              <MetricItem label="任务" :value="customer.tasks?.length || 0" />
              <MetricItem label="文档" :value="customer.documents?.length || 0" />
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <CustomerBasicInfoDrawer
        v-model="basicDrawerOpen"
        :customer="customer"
        :contacts="customer?.contacts || []"
        @contacts-updated="handleContactsUpdated"
      />
    </template>
  </aside>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getCustomerDetail } from '@/api/customer'
import CustomerBasicInfoDrawer from '@/views/customer/components/CustomerBasicInfoDrawer.vue'
import type { Contact, CustomerDetailVO } from '@/types/customer'
import { appEvents, APP_EVENT } from '@/utils/events'

const props = defineProps<{
  customerId: string
}>()

type CustomerDetailRefreshModule = 'contacts' | 'followUps' | 'tasks' | 'schedules'

type CustomerDetailRefreshPayload = {
  customerId?: string | number
  modules?: CustomerDetailRefreshModule[]
}

const MIN_WIDTH = 320
const DEFAULT_WIDTH = 360
const FULL_DETAIL_WIDTH = 720
const AI_ANALYSIS_POLL_INTERVAL_MS = 2500
const AI_ANALYSIS_POLL_MAX_ATTEMPTS = 24

const customer = ref<CustomerDetailVO | null>(null)
const loading = ref(false)
const collapsed = ref(false)
const panelWidth = ref(DEFAULT_WIDTH)
const viewportWidth = ref(typeof window === 'undefined' ? 1440 : window.innerWidth)
const basicDrawerOpen = ref(false)
const openSections = ref(['ai', 'activities', 'modules'])
let offCustomerDetailRefresh: (() => void) | null = null
let aiAnalysisPollTimer: ReturnType<typeof setTimeout> | null = null
let aiAnalysisPollAttempts = 0

const maxWidth = computed(() => Math.min(960, Math.floor(viewportWidth.value * 0.6)))
const fullDetailMode = computed(() => panelWidth.value >= FULL_DETAIL_WIDTH)

const recentActivities = computed(() => {
  const followUps = customer.value?.recentFollowUps || []
  return followUps.slice(0, fullDetailMode.value ? 6 : 3).map((item, index) => ({
    key: `${item.followUpId || index}`,
    content: item.content || item.typeName || '跟进记录',
    time: formatDate(item.followTime),
  }))
})

function isAiAnalysisPending() {
  return customer.value?.aiAnalysisStatus === 'pending' || customer.value?.aiAnalysisStatus === 'running'
}

function clearAiAnalysisPolling(resetAttempts = true) {
  if (aiAnalysisPollTimer) {
    clearTimeout(aiAnalysisPollTimer)
    aiAnalysisPollTimer = null
  }
  if (resetAttempts) {
    aiAnalysisPollAttempts = 0
  }
}

function scheduleAiAnalysisPolling(resetAttempts = false) {
  if (!props.customerId) return
  if (resetAttempts) {
    clearAiAnalysisPolling()
  }
  if (!isAiAnalysisPending()) {
    clearAiAnalysisPolling(resetAttempts)
    return
  }
  if (aiAnalysisPollTimer || aiAnalysisPollAttempts >= AI_ANALYSIS_POLL_MAX_ATTEMPTS) return

  const customerId = String(props.customerId)
  aiAnalysisPollTimer = setTimeout(async () => {
    aiAnalysisPollTimer = null
    if (String(props.customerId) !== customerId) {
      clearAiAnalysisPolling()
      return
    }
    aiAnalysisPollAttempts += 1
    try {
      await loadCustomer({ schedulePolling: false })
      appEvents.emit(APP_EVENT.CUSTOMER_LIST_REFRESH)
    } catch (error) {
      console.error('Failed to poll customer ai analysis status:', error)
    }
    if (isAiAnalysisPending() && aiAnalysisPollAttempts < AI_ANALYSIS_POLL_MAX_ATTEMPTS) {
      scheduleAiAnalysisPolling()
      return
    }
    clearAiAnalysisPolling()
  }, AI_ANALYSIS_POLL_INTERVAL_MS)
}

async function loadCustomer(options: { schedulePolling?: boolean } = {}) {
  if (!props.customerId) {
    customer.value = null
    clearAiAnalysisPolling()
    return
  }
  loading.value = true
  try {
    customer.value = await getCustomerDetail(props.customerId)
    if (options.schedulePolling !== false && isAiAnalysisPending()) {
      scheduleAiAnalysisPolling(true)
    }
  } finally {
    loading.value = false
  }
}

function handleCustomerDetailRefresh(payload?: CustomerDetailRefreshPayload) {
  if (
    payload?.modules?.length
    && !payload.modules.includes('contacts')
    && !payload.modules.includes('followUps')
  ) return
  const targetCustomerId = payload?.customerId ? String(payload.customerId) : ''
  if (!props.customerId || (targetCustomerId && targetCustomerId !== String(props.customerId))) return
  void loadCustomer()
}

function handleContactsUpdated(contacts: Contact[]) {
  if (customer.value) {
    customer.value = { ...customer.value, contacts }
  }
}

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth
  panelWidth.value = clampWidth(panelWidth.value)
}

function clampWidth(width: number) {
  return Math.max(MIN_WIDTH, Math.min(width, maxWidth.value))
}

function startResize(event: MouseEvent) {
  const startX = event.clientX
  const startWidth = panelWidth.value

  const onMove = (moveEvent: MouseEvent) => {
    panelWidth.value = clampWidth(startWidth - (moveEvent.clientX - startX))
  }
  const onUp = () => {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }

  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function formatDate(value?: string) {
  if (!value) return '暂无时间'
  return value.replace('T', ' ').slice(0, 16)
}

function formatMoney(value?: number) {
  if (value === undefined || value === null) return ''
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY', maximumFractionDigits: 0 }).format(value)
}

const InfoItem = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: [String, Number], default: '' },
  },
  setup(componentProps) {
    return () => h('div', { class: 'rounded-lg bg-[#f7f7f7] px-3 py-2 text-left' }, [
      h('p', { class: 'text-xs text-[#8f8f8f]' }, componentProps.label),
      h('p', { class: 'mt-1 truncate text-sm font-medium text-[#0d0d0d]' }, componentProps.value || '-'),
    ])
  },
})

const MetricItem = defineComponent({
  props: {
    label: { type: String, required: true },
    value: { type: Number, required: true },
  },
  setup(componentProps) {
    return () => h('div', { class: 'rounded-lg bg-[#f7f7f7] px-2 py-3' }, [
      h('p', { class: 'text-lg font-semibold text-[#0d0d0d]' }, String(componentProps.value)),
      h('p', { class: 'mt-1 text-xs text-[#8f8f8f]' }, componentProps.label),
    ])
  },
})

watch(() => props.customerId, () => {
  clearAiAnalysisPolling()
  void loadCustomer()
}, { immediate: true })

onMounted(() => {
  window.addEventListener('resize', updateViewportWidth)
  offCustomerDetailRefresh = appEvents.on<CustomerDetailRefreshPayload>(
    APP_EVENT.CUSTOMER_DETAIL_REFRESH,
    handleCustomerDetailRefresh
  )
})

onBeforeUnmount(() => {
  offCustomerDetailRefresh?.()
  offCustomerDetailRefresh = null
  clearAiAnalysisPolling()
  window.removeEventListener('resize', updateViewportWidth)
})
</script>

<style scoped>
.customer-chat-panel-collapse :deep(.el-collapse-item__header) {
  font-weight: 600;
  color: #0d0d0d;
}

.customer-chat-panel-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 16px;
}
</style>
