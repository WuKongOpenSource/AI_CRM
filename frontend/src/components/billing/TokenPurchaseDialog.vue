<template>
  <el-dialog
    :model-value="modelValue"
    width="640px"
    :show-close="false"
    append-to-body
    class="token-purchase-dialog"
    @close="handleClose"
  >
    <div class="overflow-hidden rounded-[28px] border border-slate-200 bg-white shadow-xl shadow-slate-200/70">
      <div class="flex items-start justify-between border-b border-slate-200 bg-slate-50 px-8 py-6">
        <div class="flex items-start gap-4">
          <div class="flex size-14 items-center justify-center rounded-2xl bg-primary text-white shadow-lg shadow-primary/20">
            <span class="material-symbols-outlined text-2xl">bolt</span>
          </div>
          <div>
            <h3 class="text-[30px] font-black tracking-tight text-slate-900">Token 余额不足</h3>
            <p class="mt-1 text-sm text-slate-500">请购买更多 Token 以继续使用 AI 功能。</p>
          </div>
        </div>
        <button
          type="button"
          class="rounded-full p-2 text-slate-400 transition-colors hover:bg-slate-200/70 hover:text-slate-700"
          @click="handleClose"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <div class="space-y-6 px-8 py-7">
        <div class="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-4">
          <div class="flex items-center justify-between gap-4">
            <span class="text-sm font-semibold text-slate-500">当前剩余 Token</span>
            <span class="text-3xl font-black tracking-tight text-rose-500">{{ formatToken(options?.tokenRemaining ?? 0) }}</span>
          </div>
          <p class="mt-2 text-xs text-slate-400">
            赠送额度 {{ formatToken(options?.giftTokenRemaining ?? 0) }}，已购额度 {{ formatToken(options?.purchasedTokenRemaining ?? 0) }}
          </p>
        </div>

        <template v-if="isPaying && currentOrder">
          <div class="rounded-3xl border border-primary/20 bg-primary/[0.03] px-6 py-6">
            <div class="flex flex-col gap-6 md:flex-row md:items-center">
              <div class="mx-auto flex size-[220px] items-center justify-center rounded-3xl border border-slate-200 bg-white p-4 shadow-sm">
                <img v-if="currentOrder.qrCodeImage" :src="currentOrder.qrCodeImage" alt="payment qr code" class="size-full object-contain" />
                <div v-else class="text-sm text-slate-400">二维码生成中</div>
              </div>
              <div class="flex-1">
                <p class="text-xs font-bold uppercase tracking-[0.22em] text-primary/70">{{ currentOrder.paymentChannelLabel }}</p>
                <h4 class="mt-2 text-2xl font-black text-slate-900">{{ currentOrder.planName }}</h4>
                <p class="mt-2 text-sm text-slate-500">{{ formatToken(currentOrder.tokenAmount) }} Token，支付 {{ currentOrder.amountDisplay }}</p>
                <div class="mt-4 rounded-2xl bg-white px-4 py-3 text-sm text-slate-600 shadow-sm">
                  <p>订单号：{{ currentOrder.orderNo }}</p>
                  <p class="mt-1">请使用{{ currentOrder.paymentChannelLabel }}扫码完成支付。</p>
                  <p v-if="currentOrder.expireTime" class="mt-1 text-amber-600">过期时间：{{ formatTime(currentOrder.expireTime) }}</p>
                </div>
                <div class="mt-5 flex flex-wrap gap-3">
                  <el-button type="primary" :loading="refreshingOrder" @click="refreshCurrentOrder">刷新状态</el-button>
                  <el-button @click="backToSelection">更换支付方式</el-button>
                  <el-button text @click="openOrdersDrawer">管理账单</el-button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="space-y-3">
            <p class="text-center text-sm font-bold tracking-[0.2em] text-slate-400">选择套餐</p>
            <button
              v-for="plan in options?.plans || []"
              :key="plan.id"
              type="button"
              class="w-full rounded-3xl border px-6 py-5 text-left transition-all"
              :class="selectedPlanId === plan.id
                ? 'border-primary bg-primary/[0.04] shadow-lg shadow-primary/10'
                : 'border-slate-200 bg-white hover:border-primary/30 hover:bg-slate-50'"
              @click="selectedPlanId = plan.id"
            >
              <div class="flex items-center justify-between gap-4">
                <div class="flex items-center gap-4">
                  <div class="flex size-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                    <span class="material-symbols-outlined">auto_awesome</span>
                  </div>
                  <div>
                    <div class="text-3xl font-black tracking-tight text-slate-900">{{ formatToken(plan.tokenAmount) }} Token</div>
                    <div class="mt-1 text-sm font-medium text-slate-500">{{ plan.description || '标准充值方案' }}</div>
                  </div>
                </div>
                <div class="text-right">
                  <div class="text-[42px] font-black tracking-tight text-primary">¥{{ formatPrice(plan.priceFen) }}</div>
                  <div class="text-sm text-slate-400">一次性付费</div>
                </div>
              </div>
            </button>
          </div>

          <div class="space-y-3">
            <p class="text-center text-sm font-bold tracking-[0.2em] text-slate-400">选择支付方式</p>
            <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
              <button
                v-for="channel in options?.channels || []"
                :key="channel.code"
                type="button"
                class="rounded-2xl border px-5 py-4 text-left transition-all"
                :class="selectedChannel === channel.code && channel.enabled
                  ? 'border-primary bg-primary/[0.04] shadow-lg shadow-primary/10'
                  : 'border-slate-200 bg-white hover:border-primary/30'"
                :disabled="!channel.enabled"
                @click="channel.enabled && (selectedChannel = channel.code)"
              >
                <div class="flex items-center gap-3">
                  <div
                    class="flex size-11 items-center justify-center rounded-2xl text-lg font-black"
                    :class="channel.code === 'wechat' ? 'bg-emerald-50 text-emerald-600' : 'bg-sky-50 text-sky-600'"
                  >
                    {{ channel.code === 'wechat' ? '微' : '支' }}
                  </div>
                  <div>
                    <div class="text-lg font-bold text-slate-900">{{ channel.label }}</div>
                    <div class="mt-1 text-xs text-slate-400">
                      {{ channel.enabled ? '扫码完成支付' : (channel.unavailableReason || '当前不可用') }}
                    </div>
                  </div>
                </div>
              </button>
            </div>
          </div>
        </template>
      </div>

      <div class="flex items-center justify-between border-t border-slate-200 px-8 py-6">
        <el-button text @click="openOrdersDrawer">管理账单</el-button>
        <el-button
          v-if="!isPaying"
          type="primary"
          size="large"
          :loading="creatingOrder"
          :disabled="!canCreateOrder"
          @click="submitOrder"
        >
          确认购买
        </el-button>
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
          <div>
            <div class="text-base font-bold text-slate-900">{{ order.planName }}</div>
            <div class="mt-1 text-sm text-slate-500">{{ formatToken(order.tokenAmount) }} Token</div>
            <div class="mt-2 text-xs text-slate-400">{{ order.orderNo }}</div>
          </div>
          <el-tag :type="statusTagType(order.status)">{{ statusText(order.status) }}</el-tag>
        </div>
        <div class="mt-3 flex items-center justify-between text-sm">
          <span class="font-semibold text-slate-500">{{ order.paymentChannelLabel }}</span>
          <span class="font-black text-slate-900">{{ order.amountDisplay }}</span>
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
import type {
  PaymentChannel,
  TokenPurchaseOptions,
  TokenPurchaseOrder
} from '@/types/tokenPurchase'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'paid', order: TokenPurchaseOrder): void
}>()

const loadingOptions = ref(false)
const creatingOrder = ref(false)
const refreshingOrder = ref(false)
const ordersDrawerVisible = ref(false)
const options = ref<TokenPurchaseOptions | null>(null)
const selectedPlanId = ref('')
const selectedChannel = ref<PaymentChannel>('wechat')
const currentOrder = ref<TokenPurchaseOrder | null>(null)
const recentOrders = ref<TokenPurchaseOrder[]>([])
let pollingTimer: ReturnType<typeof setInterval> | null = null

const isPaying = computed(() => currentOrder.value?.status === 'PENDING')
const canCreateOrder = computed(() => {
  if (!options.value?.enabled || creatingOrder.value) return false
  if (!selectedPlanId.value) return false
  return options.value.channels.some(channel => channel.code === selectedChannel.value && channel.enabled)
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
  await Promise.all([loadOptions(), loadRecentOrders()])
}

async function loadOptions() {
  loadingOptions.value = true
  try {
    const result = await getTokenPurchaseOptions()
    options.value = result
    if (!selectedPlanId.value) {
      selectedPlanId.value = result.plans[0]?.id || ''
    }
    const firstEnabledChannel = result.channels.find(channel => channel.enabled)?.code
    if (firstEnabledChannel) {
      selectedChannel.value = firstEnabledChannel
    }
  } finally {
    loadingOptions.value = false
  }
}

async function loadRecentOrders() {
  recentOrders.value = await listRecentTokenPurchaseOrders(10)
}

async function submitOrder() {
  if (!canCreateOrder.value || !selectedPlanId.value) return
  creatingOrder.value = true
  try {
    const order = await createTokenPurchaseOrder({
      planId: selectedPlanId.value,
      paymentChannel: selectedChannel.value
    })
    currentOrder.value = order
    startPolling()
  } finally {
    creatingOrder.value = false
  }
}

async function refreshCurrentOrder() {
  if (!currentOrder.value?.orderNo) return
  refreshingOrder.value = true
  try {
    const order = await getTokenPurchaseOrder(currentOrder.value.orderNo)
    currentOrder.value = order
    if (order.status === 'PAID') {
      await handlePaid(order)
    } else if (order.status !== 'PENDING') {
      stopPolling()
      await loadRecentOrders()
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
  ElMessage.success('支付成功，Token 已到账')
  await Promise.all([loadOptions(), loadRecentOrders()])
  emit('paid', order)
  emit('update:modelValue', false)
  currentOrder.value = null
}

function backToSelection() {
  stopPolling()
  currentOrder.value = null
}

function openOrdersDrawer() {
  ordersDrawerVisible.value = true
}

function handleClose() {
  stopPolling()
  currentOrder.value = null
  emit('update:modelValue', false)
}

function formatToken(value: number): string {
  return new Intl.NumberFormat('zh-CN').format(value || 0)
}

function formatPrice(priceFen: number): string {
  return ((priceFen || 0) / 100).toFixed(0)
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

<style scoped>
:deep(.token-purchase-dialog .el-dialog) {
  padding: 0;
  border-radius: 28px;
  overflow: hidden;
}

:deep(.token-purchase-dialog .el-dialog__header) {
  display: none;
}

:deep(.token-purchase-dialog .el-dialog__body) {
  padding: 0;
}
</style>
