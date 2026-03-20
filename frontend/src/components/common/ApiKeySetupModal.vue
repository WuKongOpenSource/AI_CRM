<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[180] bg-slate-900/50 backdrop-blur-sm"
        @click="handleClose"
      />
    </Transition>

    <Transition name="scale-fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-[181] flex items-center justify-center p-4"
      >
        <div
          class="w-full max-w-md overflow-hidden rounded-[2rem] border border-slate-100 bg-white shadow-2xl shadow-slate-900/15"
          @click.stop
        >
          <div class="p-8">
            <div class="mx-auto mb-6 flex size-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-500 shadow-sm">
              <span class="material-symbols-outlined text-4xl">key</span>
            </div>

            <h3 class="mb-4 text-center text-xl font-bold text-slate-900">
              配置 API 密钥，开启 AI CRM 能力
            </h3>

            <div class="mb-8 space-y-3 px-2 text-center text-sm leading-relaxed text-slate-500">
              <p class="font-medium text-slate-800">配置 API Key 后，即可使用以下 AI 功能：</p>
              <div class="flex justify-center">
                <ul class="w-fit space-y-2 text-left text-slate-600">
                  <li class="flex items-start gap-2">
                    <span class="material-symbols-outlined mt-0.5 text-base text-primary">smart_toy</span>
                    <span>AI 助手：自动生成跟进记录、任务与总结</span>
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="material-symbols-outlined mt-0.5 text-base text-primary">description</span>
                    <span>文件解析：上传文件后自动结构化整理</span>
                  </li>
                  <li class="flex items-start gap-2">
                    <span class="material-symbols-outlined mt-0.5 text-base text-primary">insights</span>
                    <span>智能洞察：客户分析与预警</span>
                  </li>
                </ul>
              </div>
              <div class="border-t border-slate-50 pt-2">
                <p class="text-xs text-slate-400">当前仅支持阿里云 DashScope（通义千问）Key。</p>
                <p class="mt-1 text-xs font-bold text-amber-600">未配置将无法使用 AI 功能。</p>
              </div>
            </div>

            <div class="space-y-4">
              <div class="relative group">
                <div class="pointer-events-none absolute inset-y-0 left-4 flex items-center text-slate-400 transition-colors group-focus-within:text-primary">
                  <span class="material-symbols-outlined text-xl">password</span>
                </div>
                <input
                  ref="apiKeyInputRef"
                  v-model="localApiKey"
                  :type="showApiKey ? 'text' : 'password'"
                  placeholder="请输入您的 DashScope API Key"
                  class="w-full rounded-2xl border border-slate-200 bg-slate-50 py-4 pl-12 pr-12 text-sm text-slate-900 transition-all focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                  @keydown.enter.prevent="handleSave"
                />
                <button
                  type="button"
                  class="absolute inset-y-0 right-3 flex items-center text-slate-400 transition-colors hover:text-primary"
                  :disabled="loading"
                  @click="showApiKey = !showApiKey"
                >
                  <span class="material-symbols-outlined text-xl">
                    {{ showApiKey ? 'visibility_off' : 'visibility' }}
                  </span>
                </button>
                <p class="mt-2 px-2 text-[11px] text-slate-400">
                  保存后将默认使用通义千问 `qwen-max`
                </p>
              </div>

              <div class="flex gap-3 pt-4">
                <button
                  type="button"
                  class="flex-1 rounded-2xl bg-slate-100 px-6 py-4 text-sm font-bold text-slate-600 transition-all hover:bg-slate-200 disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="loading"
                  @click="handleClose"
                >
                  暂不配置
                </button>
                <button
                  type="button"
                  class="flex flex-1 items-center justify-center gap-2 rounded-2xl bg-primary px-6 py-4 text-sm font-bold text-white shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="loading || !localApiKey.trim()"
                  @click="handleSave"
                >
                  <span
                    v-if="loading"
                    class="material-symbols-outlined animate-spin text-xl"
                  >progress_activity</span>
                  <span>{{ loading ? '保存中...' : '立即保存' }}</span>
                </button>
              </div>
            </div>
          </div>

          <div class="border-t border-slate-100 bg-slate-50 p-4">
            <p class="text-center text-[10px] font-bold uppercase tracking-[0.32em] text-slate-400">
              悟空AI 安全加密传输
            </p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: boolean
  loading?: boolean
  initialApiKey?: string
}>(), {
  loading: false,
  initialApiKey: ''
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: string): void
}>()

const apiKeyInputRef = ref<HTMLInputElement | null>(null)
const localApiKey = ref('')
const showApiKey = ref(false)

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return

    localApiKey.value = props.initialApiKey
    showApiKey.value = false
    await nextTick()
    apiKeyInputRef.value?.focus()
  }
)

function handleClose() {
  if (props.loading) return
  emit('update:modelValue', false)
}

function handleSave() {
  const apiKey = localApiKey.value.trim()
  if (!apiKey || props.loading) return
  emit('save', apiKey)
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active,
.scale-fade-enter-active,
.scale-fade-leave-active {
  transition: all 0.24s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.scale-fade-enter-from,
.scale-fade-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.96);
}
</style>
