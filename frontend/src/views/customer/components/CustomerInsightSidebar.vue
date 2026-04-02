<template>
  <div
    ref="sidebarRef"
    :class="[
      'customer-insight-sidebar w-full xl:flex-none overflow-hidden transition-[width] duration-300 ease-out',
      isExpanded ? 'xl:w-80' : 'xl:w-12'
    ]"
  >
    <div
      v-if="isExpanded"
      class="customer-insight-sidebar__expanded-panel rounded-2xl xl:rounded-none p-1 xl:p-0 border border-transparent xl:border-none bg-slate-50/50 xl:bg-transparent"
      :style="expandedPanelStyle"
    >
      <div class="flex items-center justify-between px-1 mb-2">
        <div class="flex items-center gap-2 min-w-0">
          <h3 class="text-xs font-bold text-slate-400 uppercase tracking-widest truncate">
            AI 智能洞察预警
          </h3>
          <span class="size-2 rounded-full bg-primary animate-pulse"></span>
        </div>

        <button
          type="button"
          class="size-8 flex items-center justify-center rounded-lg hover:bg-slate-200/50 text-slate-400 hover:text-slate-600 transition-colors"
          title="收起面板"
          @click="isExpanded = false"
        >
          <span class="material-symbols-outlined text-xl">last_page</span>
        </button>
      </div>

      <Transition
        enter-active-class="transition-all duration-200 ease-out"
        enter-from-class="opacity-0 -translate-y-1"
        enter-to-class="opacity-100 translate-y-0"
        leave-active-class="transition-all duration-150 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-1"
      >
        <div class="customer-insight-sidebar__body min-h-0 flex-1 overflow-y-auto pr-1">
          <div class="space-y-4">
            <div class="bg-white border border-slate-200 rounded-xl p-4 relative overflow-hidden group cursor-pointer hover:border-primary/50 hover:shadow-md transition-all">
              <div class="flex items-center gap-2 mb-2">
                <WkIcon name="ai" class="text-primary text-xl" />
                <h3 class="text-sm font-bold text-slate-900">高潜力客户预警</h3>
              </div>
              <p class="text-xs text-slate-500 leading-relaxed">发现 {{ negotiationCount }} 位客户近期成交概率显著提升，建议优先跟进。</p>
              <div class="mt-3 flex items-center justify-between">
                <span class="text-xs font-bold text-primary bg-primary/5 px-2 py-0.5 rounded">{{ negotiationCount }} 位待处理</span>
                <span class="material-symbols-outlined text-slate-300 group-hover:text-primary transition-colors text-sm">arrow_forward</span>
              </div>
            </div>

            <div class="bg-white border border-slate-200 rounded-xl p-4 relative overflow-hidden group cursor-pointer hover:border-indigo-400 hover:shadow-md transition-all">
              <div class="flex items-center gap-2 mb-2">
                <span class="material-symbols-outlined text-indigo-600 text-xl">mark_email_unread</span>
                <h3 class="text-sm font-bold text-slate-900">自动化跟进生成</h3>
              </div>
              <p class="text-xs text-slate-500 leading-relaxed">有 {{ overdueCount }} 个客户超过7天未跟进，建议尽快安排跟进计划。</p>
              <div class="mt-3 flex items-center justify-between">
                <span class="text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">{{ overdueCount }} 个待跟进</span>
                <span class="material-symbols-outlined text-slate-300 group-hover:text-indigo-600 transition-colors text-sm">arrow_forward</span>
              </div>
            </div>

            <div class="bg-white border border-slate-200 rounded-xl p-4 relative overflow-hidden group cursor-pointer hover:border-emerald-400 hover:shadow-md transition-all">
              <div class="flex items-center gap-2 mb-2">
                <span class="material-symbols-outlined text-emerald-600 text-xl">insights</span>
                <h3 class="text-sm font-bold text-slate-900">成交预测更新</h3>
              </div>
              <p class="text-xs text-slate-500 leading-relaxed">当前成交转化率 {{ conversionRate }}%，共 {{ closedCount }} 个客户已成交。</p>
              <div class="mt-3 flex items-center justify-between">
                <span class="text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">{{ conversionRate }}% 转化率</span>
                <span class="material-symbols-outlined text-slate-300 group-hover:text-emerald-600 transition-colors text-sm">arrow_forward</span>
              </div>
            </div>

            <div class="p-4 bg-slate-50 rounded-xl border border-slate-200 border-dashed">
              <p class="text-xs text-slate-400 leading-relaxed text-center italic">
                AI 助手正在实时分析您的客户数据，预警信息将在此处即时更新。
              </p>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <div
      v-else
      class="customer-insight-sidebar__collapsed-panel rounded-2xl xl:rounded-none p-1 xl:p-0 border border-transparent xl:border-none bg-transparent"
    >
      <div class="flex xl:justify-center justify-end px-1 mb-2">
        <button
          type="button"
          class="size-8 flex items-center justify-center rounded-lg hover:bg-slate-200/50 text-slate-400 hover:text-slate-600 transition-colors"
          title="展开面板"
          @click="isExpanded = true"
        >
          <span class="material-symbols-outlined text-xl">first_page</span>
        </button>
      </div>

      <div class="hidden xl:flex flex-col items-center gap-6 pt-4">
        <WkIcon name="ai" class="text-primary/40 text-xl animate-pulse" />
        <div class="h-20 w-px bg-slate-200"></div>
        <span class="[writing-mode:vertical-rl] text-xs font-bold text-slate-400 tracking-widest">
          <span class="[text-combine-upright:all]">AI</span> 智能洞察
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const AI_SIDEBAR_STORAGE_KEY = 'wk_ai_crm:customer_ai_sidebar_expanded:v1'

defineProps<{
  negotiationCount: number
  overdueCount: number
  closedCount: number
  conversionRate: string
}>()

function getInitialExpanded() {
  try {
    const raw = localStorage.getItem(AI_SIDEBAR_STORAGE_KEY)
    if (raw === null) return true
    return raw === '1'
  } catch {
    return true
  }
}

const isExpanded = ref(getInitialExpanded())
const sidebarRef = ref<HTMLElement | null>(null)
const expandedPanelMaxHeight = ref<number | null>(null)
let layoutObserver: ResizeObserver | null = null
let measureRaf = 0
let scrollContainer: HTMLElement | null = null

const expandedPanelStyle = computed(() => {
  if (!expandedPanelMaxHeight.value) return undefined
  return {
    maxHeight: `${expandedPanelMaxHeight.value}px`
  }
})

function updateExpandedPanelMaxHeight() {
  const sidebarEl = sidebarRef.value
  if (!sidebarEl) return

  const mainEl = sidebarEl.closest('main')
  const pageRootEl = sidebarEl.closest('[data-customer-page-root]')
  const viewportBottom = mainEl instanceof HTMLElement
    ? mainEl.getBoundingClientRect().bottom
    : window.innerHeight
  const sidebarTop = sidebarEl.getBoundingClientRect().top
  const pagePaddingBottom = pageRootEl instanceof HTMLElement
    ? parseFloat(window.getComputedStyle(pageRootEl).paddingBottom || '0')
    : 0
  const nextHeight = Math.floor(viewportBottom - sidebarTop - pagePaddingBottom - 1)

  expandedPanelMaxHeight.value = nextHeight > 0 ? nextHeight : null
}

function queueExpandedPanelMeasure() {
  if (measureRaf) cancelAnimationFrame(measureRaf)
  measureRaf = window.requestAnimationFrame(() => {
    measureRaf = 0
    updateExpandedPanelMaxHeight()
  })
}

watch(isExpanded, (value) => {
  try {
    localStorage.setItem(AI_SIDEBAR_STORAGE_KEY, value ? '1' : '0')
  } catch {
    // Ignore storage failures
  }

  if (value) {
    nextTick(() => {
      queueExpandedPanelMeasure()
    })
  }
})

onMounted(() => {
  queueExpandedPanelMeasure()
  window.addEventListener('resize', queueExpandedPanelMeasure, { passive: true })

  const sidebarEl = sidebarRef.value
  const mainEl = sidebarEl?.closest('main')
  const pageRootEl = sidebarEl?.closest('[data-customer-page-root]')

  if (mainEl instanceof HTMLElement) {
    scrollContainer = mainEl
    scrollContainer.addEventListener('scroll', queueExpandedPanelMeasure, { passive: true })
  }

  layoutObserver = new ResizeObserver(() => {
    queueExpandedPanelMeasure()
  })

  if (sidebarEl) layoutObserver.observe(sidebarEl)
  if (mainEl instanceof HTMLElement) layoutObserver.observe(mainEl)
  if (pageRootEl instanceof HTMLElement) layoutObserver.observe(pageRootEl)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', queueExpandedPanelMeasure)
  scrollContainer?.removeEventListener('scroll', queueExpandedPanelMeasure)
  if (layoutObserver) layoutObserver.disconnect()
  if (measureRaf) cancelAnimationFrame(measureRaf)
})
</script>

<style scoped>
.customer-insight-sidebar {
  will-change: width;
}

.customer-insight-sidebar__expanded-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.customer-insight-sidebar__body {
  overscroll-behavior: contain;
}

@media (min-width: 1280px) {
  .customer-insight-sidebar__expanded-panel {
    width: 20rem;
    min-width: 20rem;
  }

  .customer-insight-sidebar__collapsed-panel {
    width: 3rem;
    min-width: 3rem;
  }
}
</style>
