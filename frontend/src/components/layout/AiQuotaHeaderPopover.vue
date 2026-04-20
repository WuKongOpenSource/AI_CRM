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
        class="inline-flex max-w-[min(20rem,calc(100vw-8rem))] items-center gap-2 rounded-full border border-slate-200/80 bg-slate-100/80 px-3 py-1.5 text-left text-xs font-medium text-slate-700 shadow-sm transition-colors hover:border-slate-300 hover:bg-slate-100"
      >
        <span class="material-symbols-outlined shrink-0 text-[18px] leading-none text-primary">auto_awesome</span>
        <span v-if="currentAiMode === 'gift'" class="min-w-0 truncate tabular-nums">
          已使用 {{ tokenUsedWan }} / {{ tokenTotalWan }}万，剩余 {{ tokenProgressPercent }}%
        </span>
        <span v-else class="min-w-0 truncate text-slate-600">AI 额度 · 点击查看</span>
      </button>
    </template>
    <AiQuotaPanel variant="popover" />
  </el-popover>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AiQuotaPanel from '@/components/layout/AiQuotaPanel.vue'
import { useAiQuota } from '@/composables/useAiQuota'

const { loadAiConfig, currentAiMode, tokenUsedWan, tokenTotalWan, tokenProgressPercent } = useAiQuota()

onMounted(() => {
  void loadAiConfig()
})
</script>

<style>
/* Popper 挂在 body 上，需非 scoped；限制宽度避免内部 flex 随视口撑开 */
.ai-quota-popover {
  box-sizing: border-box;
  max-width: min(20rem, calc(100vw - 1.5rem));
}
</style>
