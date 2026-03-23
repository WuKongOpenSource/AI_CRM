<template>
  <div class="space-y-5">
    <template v-if="result">
      <div v-if="result.score != null" class="bg-slate-900 rounded-2xl p-6 text-white relative overflow-hidden">
        <div class="relative z-10">
          <div class="flex items-center justify-between mb-4">
            <span class="text-xs font-bold text-primary uppercase tracking-widest">AI 潜力评分</span>
            <span class="material-symbols-outlined text-primary text-lg">verified</span>
          </div>
          <div class="flex items-baseline gap-2 mb-1">
            <span class="text-5xl font-black">{{ result.score }}</span>
            <span class="text-lg text-slate-400">/ 100</span>
          </div>
          <p class="text-xs text-slate-400 mb-4">{{ scoreCaption }}</p>
          <div v-if="result.tags?.length" class="flex flex-wrap gap-1.5">
            <span
              v-for="tag in result.tags"
              :key="tag"
              class="px-2 py-0.5 bg-white/10 rounded-md text-xs font-bold uppercase tracking-wider"
            >{{ tag }}</span>
          </div>
        </div>
        <div class="absolute -right-8 -bottom-8 size-32 bg-primary/20 rounded-full blur-3xl"></div>
      </div>

      <div v-if="result.summary || result.nextStep" class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-4">
        <div v-if="result.summary">
          <h4 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2 flex items-center gap-2">
            <span class="material-symbols-outlined text-sm text-primary">analytics</span>
            AI 深度分析
          </h4>
          <p class="text-xs text-slate-700 leading-relaxed">{{ result.summary }}</p>
        </div>
        <div v-if="result.nextStep" :class="{ 'pt-4 border-t border-slate-100': result.summary }">
          <h4 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-2 flex items-center gap-2">
            <span class="material-symbols-outlined text-sm text-primary">rocket_launch</span>
            建议下一步行动
          </h4>
          <p class="text-xs text-slate-700 leading-relaxed">{{ result.nextStep }}</p>
        </div>
      </div>

      <div v-if="result.keyPoints?.length" class="bg-white rounded-2xl border border-slate-200 shadow-sm p-5">
        <h4 class="text-xs font-bold text-slate-400 uppercase tracking-widest mb-3 flex items-center gap-2">
          <span class="material-symbols-outlined text-sm text-primary">checklist</span>
          关键要点
        </h4>
        <ul class="space-y-2">
          <li
            v-for="(point, i) in result.keyPoints"
            :key="i"
            class="text-xs text-slate-700 flex items-start gap-2 leading-relaxed"
          >
            <span class="text-primary mt-0.5 shrink-0">•</span> {{ point }}
          </li>
        </ul>
      </div>
    </template>

    <div
      v-else
      class="bg-white rounded-2xl border border-slate-200 border-dashed p-8 text-center space-y-3 min-h-[300px] flex flex-col items-center justify-center"
    >
      <div class="size-12 bg-slate-50 rounded-xl flex items-center justify-center mx-auto">
        <WkIcon name="ai" class="text-slate-300 text-2xl" />
      </div>
      <div>
        <h4 class="text-xs font-bold text-slate-900">{{ emptyTitle }}</h4>
        <p class="text-xs text-slate-400 mt-1 leading-relaxed whitespace-pre-line">{{ emptyDescription }}</p>
      </div>
    </div>

    <div class="p-5 bg-primary/5 rounded-2xl border border-primary/10">
      <h4 class="text-xs font-bold text-primary uppercase tracking-widest mb-2 flex items-center gap-2">
        <span class="material-symbols-outlined text-sm">lightbulb</span>
        小提示
      </h4>
      <div class="text-xs text-slate-600 leading-relaxed">
        <slot name="tip">您可以直接粘贴简介或邮件正文，或者<strong>在输入框 Ctrl+V 粘贴名片图片</strong>，AI 会自动识别并补全信息。</slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * AiParseInsightSidebar — AI 解析结果右侧洞察栏
 *
 * 用途：与客户/联系人新建弹窗右侧栏配套，展示客户 AI 解析接口返回的结构化洞察
 * （潜力评分、标签、摘要、建议下一步、关键要点）。无数据时显示占位说明 + 底部「小提示」。
 *
 * 说明：当前类型为 CustomerAiParseVO；联系人场景若复用同一解析接口，可直接传入同一结构。
 *
 * Props:
 * - result: 解析结果；为 null 时显示虚线空状态卡片（不展示评分/摘要等块）
 * - emptyTitle: 无 result 时的空状态标题，默认「等待 AI 分析」
 * - emptyDescription: 无 result 时的说明文案，支持换行（whitespace-pre-line）
 * - scoreCaption: 评分卡片下方说明，默认与客户场景一致的评估描述
 *
 * Slots:
 * - tip: 底部「小提示」正文，用于各业务自定义引导文案；有默认内容
 */
import WkIcon from '@/components/common/WkIcon.vue'
import type { CustomerAiParseVO } from '@/api/customer'

withDefaults(
  defineProps<{
    result: CustomerAiParseVO | null
    emptyTitle?: string
    emptyDescription?: string
    scoreCaption?: string
  }>(),
  {
    emptyTitle: '等待 AI 分析',
    emptyDescription: '',
    scoreCaption: '基于行业匹配度、需求迫切度及规模评估'
  }
)
</script>
