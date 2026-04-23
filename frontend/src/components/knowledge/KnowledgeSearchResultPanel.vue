<template>
  <div class="mx-auto max-w-6xl">
    <div class="mb-8 flex flex-wrap items-start justify-between gap-4">
      <div class="flex items-start gap-4">
        <button
          type="button"
          class="mt-1 flex size-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-500 transition-all hover:border-primary/30 hover:text-primary"
          @click="$emit('back')"
        >
          <span class="material-symbols-outlined text-lg">arrow_back</span>
        </button>
        <div>
          <h3 class="text-3xl font-black tracking-tight text-slate-950">AI 检索结果</h3>
          <p class="mt-2 text-sm text-slate-500">“{{ keyword }}”</p>
        </div>
      </div>

      <div v-if="result && !loading" class="flex items-center gap-6 text-xs text-slate-400">
        <span>检索耗时: {{ formatElapsed(result.tookMs) }}</span>
        <span class="font-bold text-primary">匹配度 {{ result.matchPercent }}%</span>
      </div>
    </div>

    <div class="space-y-10">
      <section class="overflow-hidden rounded-[2rem] border border-[#cfe1ff] bg-[linear-gradient(180deg,#eff6ff_0%,#eef5ff_100%)] p-6 shadow-[0_18px_45px_rgba(59,130,246,0.08)] md:p-8">
        <div class="mb-6 flex items-center gap-3 text-primary">
          <div class="flex size-9 items-center justify-center rounded-2xl bg-white/80 shadow-sm">
            <span class="material-symbols-outlined text-lg">neurology</span>
          </div>
          <div>
            <p class="text-sm font-black tracking-wide">AI 智能解答</p>
            <p class="text-xs text-primary/70">基于知识库内容生成的结论与依据</p>
          </div>
        </div>

        <div v-if="loading" class="space-y-4">
          <div class="h-5 w-48 animate-pulse rounded-full bg-white/70" />
          <div class="space-y-3 rounded-[1.75rem] bg-white/70 p-6">
            <div class="h-4 animate-pulse rounded-full bg-slate-200/70" />
            <div class="h-4 w-11/12 animate-pulse rounded-full bg-slate-200/70" />
            <div class="h-4 w-10/12 animate-pulse rounded-full bg-slate-200/70" />
            <div class="h-4 w-8/12 animate-pulse rounded-full bg-slate-200/70" />
          </div>
        </div>

        <div
          v-else-if="result?.answer"
          class="wk-markdown text-[15px] leading-8 text-slate-800"
          v-html="renderedAnswer"
        />

        <div v-else class="rounded-[1.75rem] bg-white/70 p-6 text-sm text-slate-500">
          当前没有生成可展示的解答内容。
        </div>
      </section>

      <section>
        <div class="mb-4 flex items-center justify-between gap-4">
          <p class="text-xs font-black uppercase tracking-[0.28em] text-slate-400">关联参考文档</p>
          <p v-if="result && !loading" class="text-xs text-slate-400">{{ result.totalHits }} 份命中文档</p>
        </div>

        <div v-if="loading" class="space-y-4">
          <div
            v-for="index in 3"
            :key="index"
            class="rounded-[1.75rem] border border-slate-200 bg-white p-6 shadow-sm"
          >
            <div class="mb-4 h-5 w-56 animate-pulse rounded-full bg-slate-200" />
            <div class="space-y-3">
              <div class="h-4 animate-pulse rounded-full bg-slate-100" />
              <div class="h-4 w-11/12 animate-pulse rounded-full bg-slate-100" />
            </div>
          </div>
        </div>

        <div v-else-if="!result?.references?.length" class="rounded-[1.75rem] border border-dashed border-slate-200 bg-white px-6 py-12 text-center text-sm text-slate-400">
          暂未找到相关参考文档。
        </div>

        <div v-else class="rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-sm md:p-6">
          <ul class="space-y-3">
            <li
              v-for="item in result.references"
              :key="item.knowledgeId"
              class="flex flex-wrap items-center gap-x-3 gap-y-2 text-sm leading-6"
            >
              <button
                type="button"
                class="max-w-full truncate font-bold text-primary transition-colors hover:text-primary/80 hover:underline"
                @click="$emit('open', item.knowledgeId)"
              >
                《{{ formatReferenceName(item.name) }}》
              </button>
              <span class="text-xs font-bold text-primary/80">相关度 {{ item.matchPercent }}%</span>
            </li>
          </ul>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { KnowledgeAiSearchVO } from '@/types/common'
import { normalizeAssistantMessageContent } from '@/utils/chatMessage'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps<{
  loading: boolean
  keyword: string
  result: KnowledgeAiSearchVO | null
}>()

defineEmits<{
  back: []
  open: [knowledgeId: string | number]
}>()

const normalizedAnswer = computed(() => normalizeAssistantMessageContent(props.result?.answer || ''))

const renderedAnswer = computed(() => renderMarkdown(normalizedAnswer.value))

function formatElapsed(ms?: number): string {
  if (!ms || ms <= 0) return '0.1s'
  return `${(ms / 1000).toFixed(1)}s`
}

function formatReferenceName(name?: string): string {
  return String(name || '').replace(/^\d+[_-]+/, '').trim() || '未命名文档'
}
</script>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
