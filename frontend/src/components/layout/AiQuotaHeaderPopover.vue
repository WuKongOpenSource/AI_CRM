<template>
  <el-popover
    trigger="click"
    placement="bottom-start"
    :show-arrow="false"
    :offset="8"
    :width="320"
    popper-class="ai-quota-popover"
  >
    <template #reference>
      <button
        type="button"
        class="inline-flex items-center gap-2 text-left text-xs font-medium text-slate-700 transition-colors hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-700/80"
        :class="compact
          ? 'max-w-[4.25rem] shrink-0 justify-center rounded-full bg-slate-100 px-1.5 py-0.5 text-[10px] font-bold text-slate-600 shadow-none hover:bg-slate-200 dark:bg-slate-800 dark:text-slate-200'
          : 'w-full max-w-[min(20rem,calc(100vw-8rem))] rounded-full border border-transparent bg-slate-100/80 px-3 py-1.5 shadow-sm hover:border-slate-300 dark:bg-slate-800/80 dark:shadow-black/20 dark:hover:border-slate-600'"
        @click.stop
      >
        <WkIcon v-if="!compact" name="ai" :size="18" class="shrink-0 leading-none text-primary" />
        <span v-if="currentAiMode === 'gift'" class="min-w-0 truncate tabular-nums" :class="compact ? 'inline text-[10px]' : 'hidden md:inline'">
          <template v-if="compact">剩余{{ creditProgressPercent }}%</template>
          <template v-else><span class="hidden md:inline">已使用 </span>{{ creditUsedWan }} / {{ creditTotalWan }}万<span class="hidden md:inline">，剩余 </span>{{ creditProgressPercent }}%</template>
        </span>
        <span v-else class="min-w-0 truncate text-slate-600 dark:text-slate-300">{{ compact ? '自定义模型' : 'AI 积分 · 点击查看' }}</span>
      </button>
    </template>
    <AiQuotaPanel variant="popover" />
  </el-popover>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import WkIcon from '@/components/common/WkIcon.vue'
import AiQuotaPanel from '@/components/layout/AiQuotaPanel.vue'
import { useAiQuota } from '@/composables/useAiQuota'

withDefaults(defineProps<{ compact?: boolean }>(), { compact: false })

const { loadAiConfig, currentAiMode, creditUsedWan, creditTotalWan, creditProgressPercent } = useAiQuota()

onMounted(() => {
  void loadAiConfig()
})
</script>

<style>
/* Popper 挂在 body 上，需非 scoped；限制宽度避免内部 flex 随视口撑开 */
.ai-quota-popover {
  box-sizing: border-box;
  max-width: min(20rem, calc(100vw - 1.5rem));
  border: 1px solid var(--wk-border-subtle) !important;
  background: var(--wk-bg-surface) !important;
  color: var(--wk-text-primary) !important;
  box-shadow: 0 16px 42px rgb(var(--wk-shadow-color) / 0.24) !important;
}
</style>
