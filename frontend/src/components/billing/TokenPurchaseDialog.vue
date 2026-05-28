<template>
  <el-dialog
    :model-value="modelValue"
    width="min(1028px, calc(100vw - 32px))"
    :show-close="false"
    append-to-body
    class="token-purchase-dialog wk-dialog--flush"
    @close="handleClose"
  >
    <div class="token-purchase-shell">
      <header class="token-purchase-header">
        <div class="flex min-w-0 items-center gap-4">
          <span class="flex size-12 shrink-0 items-center justify-center rounded-2xl bg-blue-50 text-[#0b5edb]">
            <span class="material-symbols-outlined text-[30px] leading-none">bolt</span>
          </span>
          <div class="min-w-0">
            <h3 class="text-[22px] font-black leading-tight text-[#171717]">购买套餐</h3>
            <p class="mt-1 text-[15px] leading-snug text-[#646a73]">购买套餐，即刻开启高效 AI 体验</p>
          </div>
        </div>

        <div class="flex shrink-0 items-center gap-2">
          <button
            type="button"
            class="token-icon-button text-slate-300 hover:text-slate-600"
            aria-label="查看账单"
            title="查看账单"
            @click="openOrdersDrawer"
          >
            <span class="material-symbols-outlined text-[22px] leading-none">receipt_long</span>
          </button>
          <button
            type="button"
            class="token-icon-button text-slate-700 hover:bg-slate-100 hover:text-slate-900"
            aria-label="关闭"
            @click="handleClose"
          >
            <span class="material-symbols-outlined text-[28px] leading-none">close</span>
          </button>
        </div>
      </header>

      <div class="token-purchase-body">
        <section class="token-plan-pane">
          <div class="token-plan-card">
            <p class="text-[14px] font-black text-[#0557d8]">当前套餐</p>
            <div class="mt-5 flex flex-wrap items-end gap-x-2 gap-y-1">
              <span class="text-[42px] font-black leading-none tracking-tight text-[#075cdf]">
                ¥{{ formatPrice(currentPlan?.priceFen ?? 9900) }}
              </span>
              <span class="pb-1 text-[18px] font-black text-[#4f5665]">
                / {{ formatCredit(currentPlan?.creditAmount ?? 5000) }} 积分
              </span>
            </div>
          </div>

          <div class="mt-5">
            <div class="mb-3 flex items-center gap-1.5 text-[14px] font-black text-[#2c313a]">
              <span>算力预估</span>
              <span class="material-symbols-outlined text-[15px] leading-none text-[#a2a8b3]">info</span>
            </div>

            <div class="space-y-3">
              <div
                v-for="item in computeEstimates"
                :key="item.model"
                class="flex items-center justify-between gap-4 text-[14px] leading-none"
              >
                <div class="flex min-w-0 items-center gap-1.5">
                  <span class="truncate text-[#8d939f]">{{ item.model }}</span>
                  <span class="material-symbols-outlined shrink-0 text-[14px] leading-none text-[#c2c7d0]">info</span>
                </div>
                <span class="shrink-0 tabular-nums text-[#8d939f]">{{ item.messages }}</span>
              </div>
            </div>

            <p class="mt-8 text-center text-[13px] italic leading-relaxed text-[#4b5563]">
              算力积分长期有效，按需消耗，永不过期
            </p>
          </div>
        </section>

        <section class="token-payment-pane">
          <div class="token-channel-tabs" role="tablist" aria-label="支付方式">
            <button
              v-for="channel in channels"
              :key="channel.code"
              type="button"
              class="token-channel-tab"
              :class="[
                selectedChannel === channel.code && channel.enabled ? 'is-active' : '',
                !channel.enabled || creatingOrder ? 'is-disabled' : ''
              ]"
              :disabled="!channel.enabled || creatingOrder"
              role="tab"
              :aria-selected="selectedChannel === channel.code"
              @click="handleChannelSelect(channel)"
            >
              <img
                :src="paymentChannelLogos[channel.code]"
                :alt="`${channel.label} logo`"
                class="token-channel-logo"
              />
              <span>{{ channel.label }}</span>
            </button>
          </div>

          <p v-if="selectedChannelUnavailableReason" class="mt-3 text-center text-xs text-rose-500">
            {{ selectedChannelUnavailableReason }}
          </p>

          <div class="token-payment-stage">
            <div class="token-qr-frame">
              <template v-if="creatingOrder || loadingOptions">
                <span class="material-symbols-outlined animate-spin text-[38px] leading-none text-[#0b5edb]">progress_activity</span>
                <span class="mt-3 text-xs font-semibold text-slate-400">支付码生成中</span>
              </template>

              <template v-else-if="currentOrder?.paymentMode === 'page'">
                <iframe
                  v-if="currentOrder.paymentFormHtml"
                  :key="currentOrder.orderNo"
                  :srcdoc="currentOrder.paymentFormHtml"
                  title="alipay payment"
                  class="token-payment-code token-payment-iframe"
                  referrerpolicy="no-referrer"
                  scrolling="auto"
                />
                <div v-else class="token-qr-placeholder">
                  <span class="material-symbols-outlined text-[30px] leading-none text-sky-500">open_in_new</span>
                  <span>支付宝收银台加载中</span>
                </div>
              </template>

              <template v-else-if="currentOrder?.qrCodeImage">
                <img
                  :src="currentOrder.qrCodeImage"
                  alt="payment qr code"
                  class="token-payment-code object-contain"
                />
              </template>

              <template v-else>
                <div class="token-qr-placeholder">
                  <span class="material-symbols-outlined text-[34px] leading-none text-slate-300">qr_code_2</span>
                  <span>{{ orderError || '等待支付码' }}</span>
                </div>
              </template>
            </div>

            <div class="mt-6 text-center">
              <h4 class="text-[20px] font-black leading-tight text-[#171717]">
                {{ paymentTitle }}
              </h4>
              <div
                class="mx-auto mt-3 inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-[13px] font-bold"
                :class="statusPillClass"
              >
                <span class="size-2 rounded-full" :class="statusDotClass"></span>
                <span>{{ statusTextForStage }}</span>
              </div>
              <p v-if="currentOrder?.expireTime && isPaying" class="mt-3 text-xs text-amber-600">
                过期时间：{{ formatTime(currentOrder.expireTime) }}
              </p>
              <p v-if="orderError && !isPaying" class="mt-3 text-xs text-rose-500">
                {{ orderError }}
              </p>
            </div>
          </div>

          <button
            type="button"
            class="token-paid-button"
            :disabled="loadingOptions || creatingOrder || refreshingOrder || !canCreateOrder"
            @click="handlePrimaryPaymentAction"
          >
            <span v-if="refreshingOrder || creatingOrder" class="material-symbols-outlined animate-spin text-[20px] leading-none">progress_activity</span>
            <span>{{ primaryButtonText }}</span>
          </button>
        </section>
      </div>
    </div>
  </el-dialog>

  <el-drawer v-model="ordersDrawerVisible" title="最近账单" size="420px" append-to-body>
    <div class="space-y-3">
      <div v-if="recentOrders.length === 0" class="rounded-2xl border border-dashed border-slate-200 px-6 py-10 text-center text-sm text-slate-400">
        暂无账单记录
      </div>
      <div
        v-for="order in recentOrders"
        :key="order.orderNo"
        class="rounded-2xl border border-slate-200 bg-white px-4 py-4 shadow-sm"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0">
            <div class="truncate text-base font-bold text-slate-900">{{ order.planName }}</div>
            <div class="mt-1 text-sm text-slate-500">{{ formatCredit(order.creditAmount) }} 积分</div>
            <div class="mt-2 break-all text-xs text-slate-400">{{ order.orderNo }}</div>
          </div>
          <el-tag :type="statusTagType(order.status)">{{ statusText(order.status) }}</el-tag>
        </div>
        <div class="mt-3 flex items-center justify-between text-sm">
          <span class="font-semibold text-slate-500">{{ order.paymentChannelLabel }}</span>
          <span class="font-black text-slate-900">{{ formatPriceLabel(order.amountFen) }}</span>
        </div>
        <div class="mt-2 text-xs text-slate-400">{{ formatTime(order.createTime) }}</div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createTokenPurchaseOrder,
  getTokenPurchaseOptions,
  getTokenPurchaseOrder,
  listRecentTokenPurchaseOrders
} from '@/api/tokenPurchase'
import alipayLogoUrl from '@/assets/payment/alipay.png'
import wechatLogoUrl from '@/assets/payment/wechat.png'
import type {
  PaymentChannel,
  TokenPurchaseChannel,
  TokenPurchaseOptions,
  TokenPurchaseOrder,
  TokenPurchasePlan
} from '@/types/tokenPurchase'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'paid', order: TokenPurchaseOrder): void
}>()

const paymentChannelLogos: Record<PaymentChannel, string> = {
  wechat: wechatLogoUrl,
  alipay: alipayLogoUrl
}

const computeEstimates = [
  { model: 'Qwen3.6-plus', messages: '约 1200 条消息' },
  { model: 'DeepSeek V4 Pro', messages: '约 1100 条消息' },
  { model: 'Kimi K2.6', messages: '约 260 条消息' },
  { model: 'GPT-5.4', messages: '约 70 条消息' }
]

const loadingOptions = ref(false)
const creatingOrder = ref(false)
const refreshingOrder = ref(false)
const ordersDrawerVisible = ref(false)
const options = ref<TokenPurchaseOptions | null>(null)
const selectedPlanId = ref('')
const selectedChannel = ref<PaymentChannel>('wechat')
const currentOrder = ref<TokenPurchaseOrder | null>(null)
const recentOrders = ref<TokenPurchaseOrder[]>([])
const orderError = ref('')
let pollingTimer: ReturnType<typeof setInterval> | null = null
let orderRequestSeq = 0

const channels = computed<TokenPurchaseChannel[]>(() => options.value?.channels || [])
const currentPlan = computed<TokenPurchasePlan | null>(() => {
  return options.value?.plans.find(plan => plan.id === selectedPlanId.value) || options.value?.plans[0] || null
})
const selectedChannelOption = computed(() => {
  return channels.value.find(channel => channel.code === selectedChannel.value) || null
})
const selectedChannelUnavailableReason = computed(() => {
  const channel = selectedChannelOption.value
  if (!channel || channel.enabled) return ''
  return channel.unavailableReason || '当前支付方式不可用'
})
const isPaying = computed(() => currentOrder.value?.status === 'PENDING')
const canCreateOrder = computed(() => {
  if (!options.value?.enabled || creatingOrder.value || loadingOptions.value) return false
  if (!currentPlan.value) return false
  return channels.value.some(channel => channel.code === selectedChannel.value && channel.enabled)
})
const paymentTitle = computed(() => {
  const label = selectedChannelOption.value?.label || (selectedChannel.value === 'wechat' ? '微信' : '支付宝')
  if (currentOrder.value?.status && currentOrder.value.status !== 'PENDING') {
    return '支付未完成'
  }
  return `使用${label}扫码支付`
})
const statusTextForStage = computed(() => {
  if (loadingOptions.value || creatingOrder.value) return '生成支付码中'
  if (!currentOrder.value) return options.value?.enabled === false ? '购买暂不可用' : '等待生成'
  if (currentOrder.value.status === 'PENDING') return '等待支付中'
  return statusText(currentOrder.value.status)
})
const statusPillClass = computed(() => {
  const status = currentOrder.value?.status
  if (status === 'PENDING') return 'bg-emerald-50 text-emerald-600'
  if (status) return 'bg-amber-50 text-amber-600'
  return 'bg-slate-100 text-slate-500'
})
const statusDotClass = computed(() => {
  const status = currentOrder.value?.status
  if (status === 'PENDING') return 'bg-emerald-400'
  if (status) return 'bg-amber-400'
  return 'bg-slate-300'
})
const primaryButtonText = computed(() => {
  if (creatingOrder.value) return '正在生成支付码'
  if (refreshingOrder.value) return '正在确认支付'
  if (!currentOrder.value || currentOrder.value.status !== 'PENDING') return '重新生成支付码'
  return '我已完成支付'
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      loadInitialData()
    } else {
      stopPolling()
    }
  }
)

onBeforeUnmount(stopPolling)

async function loadInitialData() {
  stopPolling()
  currentOrder.value = null
  orderError.value = ''
  await loadOptions()
  loadRecentOrders().catch(() => undefined)
  if (canCreateOrder.value) {
    await createOrderForSelectedChannel()
  }
}

async function loadOptions() {
  loadingOptions.value = true
  try {
    const result = await getTokenPurchaseOptions()
    options.value = result
    selectedPlanId.value = result.plans[0]?.id || ''
    const wechatChannel = result.channels.find(channel => channel.code === 'wechat' && channel.enabled)
    const firstEnabledChannel = result.channels.find(channel => channel.enabled)
    selectedChannel.value = (wechatChannel || firstEnabledChannel)?.code || 'wechat'
  } finally {
    loadingOptions.value = false
  }
}

async function loadRecentOrders() {
  recentOrders.value = await listRecentTokenPurchaseOrders(10)
}

async function handleChannelSelect(channel: TokenPurchaseChannel) {
  if (!channel.enabled || creatingOrder.value) {
    orderError.value = channel.unavailableReason || '当前支付方式不可用'
    return
  }
  if (selectedChannel.value === channel.code && currentOrder.value?.status === 'PENDING') {
    return
  }
  selectedChannel.value = channel.code
  await createOrderForSelectedChannel()
}

async function createOrderForSelectedChannel() {
  if (!canCreateOrder.value || !selectedPlanId.value) return

  const requestSeq = ++orderRequestSeq
  stopPolling()
  creatingOrder.value = true
  orderError.value = ''
  currentOrder.value = null

  try {
    const order = await createTokenPurchaseOrder({
      planId: selectedPlanId.value,
      paymentChannel: selectedChannel.value
    })
    if (requestSeq !== orderRequestSeq) return

    currentOrder.value = order
    if (order.status === 'PENDING') {
      startPolling()
    } else if (order.status === 'PAID') {
      await handlePaid(order)
    } else {
      orderError.value = '支付码未生成，请重新尝试'
      await loadRecentOrders()
    }
  } catch (error: any) {
    if (requestSeq === orderRequestSeq) {
      orderError.value = error?.message || '支付订单生成失败'
    }
  } finally {
    if (requestSeq === orderRequestSeq) {
      creatingOrder.value = false
    }
  }
}

async function refreshCurrentOrder(showPendingMessage = false) {
  const orderNo = currentOrder.value?.orderNo
  if (!orderNo) return

  refreshingOrder.value = true
  try {
    const order = await getTokenPurchaseOrder(orderNo)
    if (currentOrder.value?.orderNo !== orderNo) return

    currentOrder.value = order
    if (order.status === 'PAID') {
      await handlePaid(order)
    } else if (order.status !== 'PENDING') {
      stopPolling()
      orderError.value = '支付未完成，请重新生成支付码'
      await loadRecentOrders()
    } else if (showPendingMessage) {
      ElMessage.info('暂未查询到支付成功，请稍后再试')
    }
  } finally {
    refreshingOrder.value = false
  }
}

function startPolling() {
  stopPolling()
  pollingTimer = setInterval(() => {
    refreshCurrentOrder().catch(() => undefined)
  }, 3000)
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

async function handlePaid(order: TokenPurchaseOrder) {
  stopPolling()
  ElMessage.success('支付成功，积分已到账')
  await Promise.all([loadOptions(), loadRecentOrders()])
  emit('paid', order)
  emit('update:modelValue', false)
  currentOrder.value = null
}

async function handlePrimaryPaymentAction() {
  if (currentOrder.value?.status === 'PENDING') {
    await refreshCurrentOrder(true)
    return
  }
  await createOrderForSelectedChannel()
}

function openOrdersDrawer() {
  ordersDrawerVisible.value = true
}

function handleClose() {
  stopPolling()
  currentOrder.value = null
  emit('update:modelValue', false)
}

function formatCredit(value: number): string {
  return new Intl.NumberFormat('zh-CN').format(value || 0)
}

function formatPrice(priceFen: number): string {
  return ((priceFen || 0) / 100).toFixed(0)
}

function formatPriceLabel(priceFen: number): string {
  return `¥${formatPrice(priceFen)}`
}

function formatTime(value?: string) {
  if (!value) return '时间待定'
  return new Date(value).toLocaleString('zh-CN')
}

function statusText(status: TokenPurchaseOrder['status']) {
  switch (status) {
    case 'PAID':
      return '已支付'
    case 'PENDING':
      return '待支付'
    case 'EXPIRED':
      return '已过期'
    case 'CLOSED':
      return '已关闭'
    default:
      return '支付失败'
  }
}

function statusTagType(status: TokenPurchaseOrder['status']) {
  switch (status) {
    case 'PAID':
      return 'success'
    case 'PENDING':
      return 'warning'
    case 'EXPIRED':
    case 'CLOSED':
      return 'info'
    default:
      return 'danger'
  }
}
</script>

<style>
.token-purchase-dialog .el-dialog {
  padding: 0;
  border-radius: 18px;
  overflow: hidden;
}

.token-purchase-dialog .el-dialog__header {
  display: none;
}

.token-purchase-dialog .el-dialog__body {
  padding: 0 !important;
}

.token-purchase-shell {
  overflow: hidden;
  border-radius: 18px;
  background: #fff;
}

.token-purchase-header {
  display: flex;
  min-height: 110px;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  border-bottom: 1px solid #eef1f5;
  padding: 28px 36px;
}

.token-icon-button {
  display: inline-flex;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.token-purchase-body {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1.05fr);
  min-height: 590px;
}

.token-plan-pane {
  padding: 38px 36px 34px;
}

.token-payment-pane {
  display: flex;
  flex-direction: column;
  border-left: 1px solid #f1f3f7;
  background: #fbfbfc;
  padding: 38px 36px 36px;
}

.token-plan-card {
  min-height: 140px;
  border: 1px solid #bfd7ff;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #f3f7fc 100%);
  padding: 28px 28px 24px;
}

.token-channel-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  overflow: hidden;
  border: 1px solid #e8ebf1;
  border-radius: 14px;
  background: #fff;
  padding: 4px;
}

.token-channel-tab {
  display: inline-flex;
  min-height: 48px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
  color: #3f4652;
  font-size: 16px;
  font-weight: 800;
  transition: background-color 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.token-channel-tab.is-active {
  background: #0b5edb;
  color: #fff;
  box-shadow: 0 4px 10px rgba(11, 94, 219, 0.22);
}

.token-channel-tab.is-disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.token-channel-logo {
  display: block;
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 4px;
  object-fit: contain;
}

.token-channel-tab.is-active .token-channel-logo {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.92);
}

.token-payment-stage {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 362px;
  padding: 28px 0;
}

.token-qr-frame {
  display: flex;
  width: 260px;
  height: 260px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 10px solid #fff;
  border-radius: 18px;
  background:
    radial-gradient(circle, rgba(148, 163, 184, 0.18) 1px, transparent 1px) 0 0 / 10px 10px,
    #f3f4f6;
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.14);
}

.token-payment-code {
  display: block;
  width: 205px;
  height: 205px;
}

.token-payment-iframe {
  border: 0;
  border-radius: 10px;
  background: #fff;
}

.token-qr-placeholder {
  display: flex;
  width: 178px;
  height: 178px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  text-align: center;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 700;
}

.token-paid-button {
  display: inline-flex;
  min-height: 52px;
  width: 100%;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 13px;
  background: #0b5edb;
  color: #fff;
  font-size: 18px;
  font-weight: 900;
  box-shadow: 0 8px 16px rgba(11, 94, 219, 0.28);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.token-paid-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(11, 94, 219, 0.32);
}

.token-paid-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

@media (max-width: 820px) {
  .token-purchase-header {
    min-height: auto;
    padding: 22px 20px;
  }

  .token-purchase-body {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .token-plan-pane,
  .token-payment-pane {
    padding: 24px 20px;
  }

  .token-payment-pane {
    border-left: 0;
    border-top: 1px solid #f1f3f7;
  }

  .token-payment-stage {
    min-height: 320px;
  }
}
</style>
