<template>
  <div v-if="variant === 'sidebar'" class="border-t border-slate-100 p-4">
    <div class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/60">
      <div class="mb-3 flex items-center justify-between gap-3">
        <p class="text-xs font-bold uppercase tracking-widest text-slate-400">AI 额度</p>
        <span class="inline-flex rounded-full px-2 py-1 text-[11px] font-bold" :class="aiStatusBadgeClass">
          {{ aiStatusBadgeText }}
        </span>
      </div>

      <template v-if="currentAiMode === 'gift'">
        <div class="mb-1 flex flex-wrap items-baseline gap-1">
          <span class="text-xs font-semibold tabular-nums text-slate-900">{{ giftTokenRemainingWan }}</span>
          <span class="text-xs font-medium text-slate-400">/ {{ giftTokenTotalWan }} 万 token</span>
        </div>
        <p v-if="giftTokenRemaining <= 0" class="mb-3 text-xs text-slate-500">
          Token 已用完，可购买套餐或配置 AI 服务后继续使用。
        </p>
        <div class="mb-4 h-2 overflow-hidden rounded-full bg-slate-100">
          <div
            class="h-full rounded-full transition-all"
            :class="giftTokenProgressClass"
            :style="{ width: `${giftTokenProgressPercent}%` }"
          />
        </div>
      </template>

      <div class="flex gap-2">
        <button
          type="button"
          class="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 transition-colors hover:bg-slate-50"
          @click="goToAiSettings"
        >
          AI 设置
        </button>
        <button
          type="button"
          class="flex-1 rounded-xl bg-primary px-3 py-2 text-xs font-bold text-white transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
          :disabled="currentAiMode !== 'gift' && !canManageAiConfig"
          @click="currentAiMode === 'gift' ? openTokenPurchaseDialog() : openApiKeySetup()"
        >
          {{ currentAiMode === 'gift' ? '购买 Token' : '配置 AI 服务' }}
        </button>
      </div>

      <template v-if="showModelStatus">
        <p class="mb-1 mt-4 text-xs font-bold uppercase tracking-wider text-primary">AI 模型状态</p>
        <div class="flex items-center gap-2">
          <div
            class="size-1.5 rounded-full"
            :class="hasAiApiKeyConfigured ? 'animate-pulse bg-emerald-500' : 'bg-amber-400'"
          />
          <span class="text-xs font-medium text-slate-600">
            {{ hasAiApiKeyConfigured ? 'AI 模型已就绪' : '请先配置 AI 服务' }}
          </span>
        </div>
      </template>
    </div>
  </div>

  <div v-else class="box-border w-full min-w-0 max-w-full overflow-hidden px-1 py-1">
    <div class="mb-3 flex min-w-0 items-start justify-between gap-2">
      <p class="min-w-0 flex-1 text-sm font-bold leading-snug text-slate-900">AI Token 额度</p>
      <span
        class="inline-flex max-w-[45%] shrink-0 items-center justify-center truncate rounded-full px-2 py-0.5 text-[11px] font-bold"
        :class="popoverBadgeClass"
      >
        {{ aiStatusBadgeText }}
      </span>
    </div>

    <template v-if="currentAiMode === 'gift'">
      <div class="mb-2 min-w-0 space-y-1 text-xs">
        <p class="break-words font-medium leading-relaxed text-slate-600">
          已使用
          <span class="font-semibold tabular-nums text-slate-900">{{ tokenUsedWan }}</span>
          / {{ tokenTotalWan }}万
        </p>
        <p class="font-semibold text-primary">剩余 {{ tokenProgressPercent }}%</p>
      </div>

      <div class="mb-4 h-1.5 w-full min-w-0 max-w-full overflow-hidden rounded-full bg-slate-100">
        <div
          class="h-full max-w-full rounded-full transition-all"
          :class="giftTokenProgressClass"
          :style="{ width: `${giftTokenProgressPercent}%` }"
        />
      </div>
    </template>

    <div class="flex min-w-0 w-full gap-2">
      <button
        type="button"
        class="inline-flex min-h-9 min-w-0 flex-1 items-center justify-center gap-1 rounded-xl border border-slate-200 bg-slate-50/80 px-2 py-2 text-xs font-bold text-slate-600 transition-colors hover:bg-slate-100"
        @click="goToAiSettings"
      >
        <span class="material-symbols-outlined shrink-0 text-[16px] leading-none">settings</span>
        <span class="min-w-0 truncate">AI 设置</span>
      </button>
      <button
        type="button"
        class="inline-flex min-h-9 min-w-0 flex-1 items-center justify-center gap-1 rounded-xl bg-primary px-2 py-2 text-xs font-bold text-white transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="currentAiMode !== 'gift' && !canManageAiConfig"
        @click="currentAiMode === 'gift' ? openTokenPurchaseDialog() : openApiKeySetup()"
      >
        <span class="material-symbols-outlined shrink-0 text-[16px] leading-none">bolt</span>
        <span class="min-w-0 truncate">{{ currentAiMode === 'gift' ? '购买 Token' : '配置 AI 服务' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useAiQuota } from '@/composables/useAiQuota'

withDefaults(
  defineProps<{
    variant?: 'sidebar' | 'popover'
    /** 侧栏原先通过全局样式隐藏模型状态，默认 false 保持原样 */
    showModelStatus?: boolean
  }>(),
  { variant: 'sidebar', showModelStatus: false }
)

const {
  loadAiConfig,
  currentAiMode,
  aiStatusBadgeText,
  aiStatusBadgeClass,
  giftTokenRemaining,
  giftTokenRemainingWan,
  giftTokenTotalWan,
  giftTokenProgressPercent,
  giftTokenProgressClass,
  tokenUsedWan,
  tokenTotalWan,
  tokenProgressPercent,
  hasAiApiKeyConfigured,
  canManageAiConfig,
  goToAiSettings,
  openTokenPurchaseDialog,
  openApiKeySetup,
} = useAiQuota()

onMounted(() => {
  void loadAiConfig()
})

const popoverBadgeClass = computed(() => {
  if (currentAiMode.value === 'gift' && giftTokenRemaining.value > 0) {
    return 'border border-amber-200/80 bg-amber-50 text-amber-800'
  }
  return aiStatusBadgeClass.value
})
</script>
