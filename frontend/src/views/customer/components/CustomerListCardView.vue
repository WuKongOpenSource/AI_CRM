<template>
  <div class="min-h-0 min-w-0 flex-1" :class="isMobile ? 'overflow-visible' : 'overflow-hidden'">
    <div
      class="wk-card-view-scroll min-h-0 overflow-x-hidden"
      :class="isMobile ? 'overflow-y-visible' : 'overflow-y-auto'"
      :style="isMobile ? undefined : { maxHeight: `${scrollMaxHeight}px` }"
    >
      <div
        v-if="customers.length === 0"
        class="rounded-3xl border border-dashed border-slate-200 bg-slate-50/30 py-24 text-center"
      >
        <span class="material-symbols-outlined mb-4 block text-5xl font-light text-slate-200">search_off</span>
        <p class="text-sm text-slate-400">暂无客户数据</p>
      </div>
      <div
        v-else
        class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5"
      >
        <div
          v-for="row in customers"
          :key="row.customerId"
          role="button"
          tabindex="0"
          class="group relative cursor-pointer rounded-xl border border-[#DFE1E6] bg-white p-3 shadow-sm transition-all duration-150 hover:border-primary"
          :class="cardHotClass(row)"
          @click="emit('rowClick', row)"
          @keydown.enter.prevent="emit('rowClick', row)"
          @keydown.space.prevent="emit('rowClick', row)"
        >
          <div class="mb-1 flex items-start gap-2">
            <div
              class="mt-0.5 flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-md border border-slate-100 bg-white"
            >
              <img
                v-if="row.logoUrl"
                :src="row.logoUrl"
                alt=""
                class="size-full object-contain p-0.5"
              />
              <span v-else class="text-xs font-bold text-slate-400">{{ row.companyName?.charAt(0) || '?' }}</span>
            </div>
            <div class="min-w-0 flex-1">
              <h3 class="truncate text-sm font-semibold leading-tight text-[#051a3e] transition-colors group-hover:text-primary">
                {{ row.companyName || '-' }}
              </h3>
              <p class="mt-0.5 text-[10px] font-medium text-slate-400">
                {{ industryLabel(row) || '通用行业' }}
                <template v-if="stageLabel(row)"> · {{ stageLabel(row) }}</template>
              </p>
            </div>
          </div>
          <div class="mb-3 flex items-center justify-between gap-2">
            <span class="text-sm font-bold text-primary">{{ formatCardQuotation(row.quotation) }}</span>
            <span
              v-if="getAiStatusMeta(row.aiStatusDetection)"
              class="inline-flex max-w-[104px] shrink-0 truncate rounded-lg border px-1.5 py-0.5 text-[10px] font-bold"
              :class="aiBadgeTemplateClass(row.aiStatusDetection)"
            >
              {{ getAiStatusMeta(row.aiStatusDetection)?.label }}
            </span>
          </div>
          <div class="mb-4 space-y-1.5">
            <div class="flex items-center gap-2 text-[11px] text-slate-500">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-slate-400">schedule</span>
              <span :class="lastFollowUpHighlightClass(row.lastContactTime)">
                最近跟进: {{ formatLastContactDate(row.lastContactTime) }}
              </span>
            </div>
            <div class="flex min-w-0 items-center gap-2 text-[11px] text-slate-500">
              <span class="material-symbols-outlined shrink-0 text-[14px] text-slate-400">person</span>
              <span class="min-w-0 truncate font-medium text-slate-600">
                联系人: {{ row.primaryContactName || '-' }}
                <template v-if="row.primaryContactPhone">· {{ row.primaryContactPhone }}</template>
              </span>
            </div>
          </div>
          <div class="flex items-center justify-between border-t border-slate-100 pt-3" data-row-action="true" @click.stop>
            <div class="flex min-w-0 items-center gap-2">
              <div
                class="flex size-5 shrink-0 items-center justify-center overflow-hidden rounded-full border border-slate-200 bg-slate-50 text-[10px] font-bold text-slate-500"
              >
                {{ row.ownerName?.charAt(0) || '?' }}
              </div>
              <span class="truncate text-[11px] font-medium text-slate-600">{{ row.ownerName || '-' }}</span>
            </div>
            <button
              type="button"
              class="flex items-center gap-1 rounded-lg border border-primary/20 px-2 py-1 text-primary transition-all hover:bg-primary/5"
              @click="emit('aiFollowUp', row)"
            >
              <span class="material-symbols-outlined text-[14px]">auto_awesome</span>
              <span class="text-[10px] font-bold uppercase tracking-wider">AI 跟进</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { CustomerListVO } from '@/types/customer'
import { useResponsive } from '@/composables/useResponsive'
import { getCustomerAiStatusMeta } from '@/utils/customerAi'
import { formatCardQuotation, formatLastContactDate, lastFollowUpHighlightClass } from '@/utils/customerListViewUi'

const { isMobile } = useResponsive()

defineProps<{
  customers: CustomerListVO[]
  scrollMaxHeight: number
  industryLabel: (row: CustomerListVO) => string
  stageLabel: (row: CustomerListVO) => string
}>()

const emit = defineEmits<{
  rowClick: [row: CustomerListVO]
  aiFollowUp: [row: CustomerListVO]
}>()

function getAiStatusMeta(value: string | undefined | null) {
  return getCustomerAiStatusMeta(value)
}

function aiBadgeTemplateClass(ai: string | undefined | null) {
  const meta = getCustomerAiStatusMeta(ai)
  if (!meta) return 'border-slate-200 bg-slate-100 text-slate-500'
  const label = meta.label
  if (label === '活跃状态' || label === '高意向') {
    return 'border-primary/10 bg-blue-50 text-primary'
  }
  return `${meta.badgeClass} border-slate-200/80`
}

function cardHotClass(row: CustomerListVO) {
  const meta = getCustomerAiStatusMeta(row.aiStatusDetection)
  if (meta?.label === '高意向' || meta?.label === '活跃状态') {
    return 'border-l-4 border-l-primary'
  }
  return ''
}
</script>

<style scoped>
.wk-card-view-scroll {
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
}

.wk-card-view-scroll::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.wk-card-view-scroll::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgb(148 163 184 / 0.55);
}
</style>
