<template>
  <section class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
    <div class="px-5 py-3 border-b border-slate-100 bg-slate-50/50 flex items-center justify-between">
      <div class="flex items-center gap-2">
        <WkIcon name="ai" class="text-primary text-lg" />
        <h3 class="text-xs font-bold text-slate-900 uppercase tracking-wider">AI 智能录入</h3>
      </div>
      <span class="text-xs font-bold text-slate-400 uppercase tracking-widest">{{ headerHint }}</span>
    </div>
    <div class="p-4 space-y-3">
      <div class="relative">
        <el-input
          :model-value="modelValue"
          type="textarea"
          :rows="4"
          resize="none"
          class="wk-crm-el-field-input wk-crm-el-field-ai w-full"
          :placeholder="placeholder"
          @update:model-value="emit('update:modelValue', $event)"
          @paste="emit('paste', $event)"
        />
        <div v-if="aiImagePreview" class="absolute right-3 bottom-3">
          <div class="relative group">
            <img :src="aiImagePreview" alt="名片图片" class="h-16 w-24 object-cover rounded-lg border-2 border-primary shadow-lg" />
            <button
              type="button"
              class="absolute -top-2 -right-2 size-5 bg-red-500 text-white rounded-full flex items-center justify-center shadow-md hover:bg-red-600 transition-colors"
              aria-label="移除图片"
              title="移除图片"
              @click="emit('remove-image')"
            >
              <span class="material-symbols-outlined text-[12px] font-bold">close</span>
            </button>
          </div>
        </div>
      </div>

      <div class="flex justify-end items-center gap-3">
        <span v-if="showImageHint" class="text-xs text-primary font-bold flex items-center gap-1 animate-pulse">
          <span class="material-symbols-outlined text-sm">image</span>
          检测到名片图片，点击智能提取
        </span>
        <button
          type="button"
          :disabled="aiParsing || !canExtract"
          :class="[
            'flex items-center gap-2 px-5 py-2 rounded-xl text-xs font-bold transition-all',
            aiParsing || !canExtract
              ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
              : 'bg-slate-900 text-white hover:bg-slate-800 shadow-lg shadow-slate-900/10'
          ]"
          @click="emit('extract')"
        >
          <template v-if="aiParsing">
            <span class="size-3 border-2 border-slate-300 border-t-slate-600 rounded-full animate-spin"></span>
            解析中...
          </template>
          <template v-else>
            <WkIcon name="ai" class="text-sm" />
            智能提取
          </template>
        </button>
      </div>

      <slot name="after-actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
/**
 * AiSmartEntrySection — CRM「AI 智能录入」通用区块
 *
 * 用途：在新建客户/联系人等弹窗中，提供统一的粘贴文本或名片图、触发「智能提取」的 UI。
 * 业务解析与表单回填由父组件在 @extract 中调用接口完成；本组件仅负责展示与交互。
 *
 * Props:
 * - modelValue: 文本域内容（支持 v-model）
 * - placeholder: 输入框占位提示
 * - headerHint: 标题栏右侧辅助说明，默认「粘贴名片、邮件或简介」
 * - aiImagePreview: 粘贴图片后的预览 URL（如 object URL），无图传 null
 * - aiParsing: 是否正在解析（控制按钮 loading 与禁用）
 * - canExtract: 是否允许点击「智能提取」（如：有文本或已选图片）
 * - showImageHint: 是否显示「检测到名片图片」提示
 *
 * Emits:
 * - update:modelValue: 文本变更
 * - paste: 原生粘贴，父组件可从中读取剪贴板图片
 * - extract: 用户点击「智能提取」
 * - remove-image: 用户移除已粘贴的图片预览
 *
 * Slots:
 * - after-actions: 放在操作行之后，例如移动端展示 AI 解析摘要
 */
import WkIcon from '@/components/common/WkIcon.vue'

withDefaults(
  defineProps<{
    modelValue: string
    placeholder: string
    headerHint?: string
    aiImagePreview: string | null
    aiParsing: boolean
    canExtract: boolean
    showImageHint?: boolean
  }>(),
  {
    headerHint: '粘贴名片、邮件或简介',
    showImageHint: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [v: string]
  paste: [e: ClipboardEvent]
  extract: []
  'remove-image': []
}>()
</script>
