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
        class="fixed inset-0 z-[181] flex items-center justify-center p-5 sm:p-8"
      >
        <div
          class="flex max-h-[calc(100vh-2.5rem)] w-full max-w-lg flex-col overflow-hidden rounded-[2rem] border border-slate-100 bg-white shadow-2xl shadow-slate-900/15 sm:max-h-[calc(100vh-4rem)]"
          @click.stop
        >
          <div class="min-h-0 flex-1 overflow-y-auto p-6 sm:p-8">
            <div class="mx-auto mb-6 flex size-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-500 shadow-sm">
              <span class="material-symbols-outlined text-4xl">key</span>
            </div>

            <h3 class="mb-3 text-center text-xl font-bold text-slate-900">
              配置 AI 服务，开启智能 CRM
            </h3>

            <div class="space-y-4">
              <div>
                <p class="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-slate-400">AI 服务商</p>
                <div class="grid max-h-64 grid-cols-2 gap-2 overflow-y-auto pr-1">
                  <button
                    v-for="preset in providerOptions"
                    :key="preset.value"
                    type="button"
                    class="rounded-2xl border px-3 py-3 text-sm font-semibold transition-all"
                    :class="preset.value === localForm.provider
                      ? 'border-primary bg-primary/5 text-primary shadow-sm'
                      : 'border-slate-200 bg-slate-50 text-slate-600 hover:border-primary/40 hover:text-primary'"
                    :disabled="loading"
                    @click="handleProviderChange(preset.value)"
                  >
                    <div>{{ preset.label }}</div>
                    <div class="mt-1 text-[11px] font-medium">
                      {{ preset.active ? '当前默认' : preset.configured ? '已保存' : '未配置' }}
                    </div>
                  </button>
                </div>
              </div>

              <div v-if="showApiUrlField" class="space-y-2">
                <p class="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">API 地址</p>
                <input
                  v-model="localForm.apiUrl"
                  type="text"
                  placeholder="请输入兼容 OpenAI 的 API 地址"
                  class="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-900 transition-all focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <div class="space-y-2">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">模型</p>
                  <span class="text-[11px] text-slate-400">{{ currentProviderPreset?.modelHint }}</span>
                </div>
                <el-select
                  v-model="localForm.model"
                  class="w-full api-key-model-select"
                  filterable
                  allow-create
                  default-first-option
                  :reserve-keyword="false"
                  popper-class="api-key-model-select-popper"
                  :placeholder="currentModelPlaceholder"
                >
                  <el-option
                    v-for="model in currentModelOptions"
                    :key="model"
                    :label="model"
                    :value="model"
                  />
                </el-select>
              </div>

              <div class="space-y-2">
                <p class="text-xs font-bold uppercase tracking-[0.2em] text-slate-400">API Key</p>
                <div class="relative group">
                  <div class="pointer-events-none absolute inset-y-0 left-4 flex items-center text-slate-400 transition-colors group-focus-within:text-primary">
                    <span class="material-symbols-outlined text-xl">password</span>
                  </div>
                  <input
                    ref="apiKeyInputRef"
                    v-model="localForm.apiKey"
                    :type="showApiKey ? 'text' : 'password'"
                    :placeholder="currentApiKeyPlaceholder"
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
                </div>
                <p class="px-1 text-[11px] text-slate-400">
                  <template v-if="currentProviderPreset?.apiKeyConfigured">
                    当前服务商已保存过 API Key，不重新输入时会沿用已有 Key；如需覆盖请重新输入。
                  </template>
                  <template v-else>
                    当前服务商尚未配置 API Key，保存后会自动设为默认生效。
                  </template>
                </p>
              </div>

              <p class="rounded-2xl bg-slate-50 px-4 py-3 text-xs leading-6 text-slate-500">
                高级能力如额外请求头建议在“系统设置 / API / AI”中配置，这里会优先满足常见服务商的快速接入。
              </p>

              <div class="flex gap-3 pt-2">
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
                  :disabled="!canSave || loading"
                  @click="handleSave"
                >
                  <span
                    v-if="loading"
                    class="material-symbols-outlined animate-spin text-xl"
                  >progress_activity</span>
                  <span>{{ loading ? '保存中...' : '保存并启用' }}</span>
                </button>
              </div>
            </div>
          </div>

          <div class="shrink-0 border-t border-slate-100 bg-slate-50 p-4">
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
import { computed, nextTick, reactive, ref, watch } from 'vue'
import type { AiConfigUpdateBO, AiProvider, AiProviderPreset } from '@/types/systemConfig'

type ProviderDraftMap = Partial<Record<AiProvider, AiConfigUpdateBO>>

const DEFAULT_PROVIDER: AiProvider = 'dashscope'
const DEFAULT_API_URL = 'https://dashscope.aliyuncs.com/compatible-mode'
const DEFAULT_MODEL = 'qwen3.5-plus'

const props = withDefaults(defineProps<{
  modelValue: boolean
  loading?: boolean
  providerOptions?: AiProviderPreset[]
  initialConfig?: Partial<AiConfigUpdateBO> | null
}>(), {
  loading: false,
  providerOptions: () => [],
  initialConfig: null
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', value: AiConfigUpdateBO): void
}>()

const apiKeyInputRef = ref<HTMLInputElement | null>(null)
const showApiKey = ref(false)
const drafts = ref<ProviderDraftMap>({})

const localForm = reactive<AiConfigUpdateBO>({
  provider: DEFAULT_PROVIDER,
  apiUrl: DEFAULT_API_URL,
  apiKey: '',
  model: DEFAULT_MODEL,
  temperature: 0.7,
  maxTokens: 4096,
  extraHeadersJson: ''
})

const providerOptions = computed(() => props.providerOptions || [])
const currentProviderPreset = computed(() => {
  return providerOptions.value.find((item) => item.value === localForm.provider) || null
})
const currentModelOptions = computed(() => {
  const options = new Set<string>()
  const savedModel = currentProviderPreset.value?.savedModel?.trim()
  if (savedModel) {
    options.add(savedModel)
  }
  currentProviderPreset.value?.models?.forEach((model) => {
    const value = model.trim()
    if (value) {
      options.add(value)
    }
  })
  return Array.from(options)
})
const showApiUrlField = computed(() => !currentProviderPreset.value?.baseUrl)
const currentModelPlaceholder = computed(() => {
  return currentProviderPreset.value?.savedModel
    || currentProviderPreset.value?.models?.[0]
    || '请输入模型名称'
})
const currentApiKeyPlaceholder = computed(() => {
  if (currentProviderPreset.value?.apiKeyConfigured) {
    return `已保存 ${currentProviderPreset.value?.label || 'AI'} API Key，可留空沿用`
  }
  return `请输入您的 ${currentProviderPreset.value?.label || 'AI'} API Key`
})
const hasReusableApiKey = computed(() => {
  return Boolean(localForm.apiKey?.trim() || currentProviderPreset.value?.apiKeyConfigured)
})
const canSave = computed(() => {
  return Boolean(localForm.provider && localForm.apiUrl?.trim() && hasReusableApiKey.value && localForm.model?.trim())
})

watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return

    initializeForm()
    showApiKey.value = false
    await nextTick()
    apiKeyInputRef.value?.focus()
  }
)

function createDraft(provider: AiProvider): AiConfigUpdateBO {
  const preset = providerOptions.value.find((item) => item.value === provider) || null
  return {
    provider,
    apiUrl: preset?.savedApiUrl || preset?.baseUrl || DEFAULT_API_URL,
    apiKey: '',
    model: preset?.savedModel || preset?.models?.[0] || DEFAULT_MODEL,
    temperature: preset?.savedTemperature ?? props.initialConfig?.temperature ?? 0.7,
    maxTokens: preset?.savedMaxTokens ?? props.initialConfig?.maxTokens ?? 4096,
    extraHeadersJson: preset?.savedExtraHeadersJson || props.initialConfig?.extraHeadersJson || ''
  }
}

function syncCurrentDraft() {
  const provider = localForm.provider || DEFAULT_PROVIDER
  drafts.value[provider] = {
    provider,
    apiUrl: localForm.apiUrl,
    apiKey: localForm.apiKey,
    model: localForm.model,
    temperature: localForm.temperature,
    maxTokens: localForm.maxTokens,
    extraHeadersJson: localForm.extraHeadersJson || ''
  }
}

function applyDraft(provider: AiProvider) {
  const draft = drafts.value[provider] || createDraft(provider)
  drafts.value[provider] = draft

  Object.assign(localForm, {
    provider,
    apiUrl: draft.apiUrl,
    apiKey: draft.apiKey,
    model: draft.model,
    temperature: draft.temperature ?? 0.7,
    maxTokens: draft.maxTokens ?? 4096,
    extraHeadersJson: draft.extraHeadersJson || ''
  })
}

function initializeForm() {
  const nextDrafts: ProviderDraftMap = {}
  providerOptions.value.forEach((item) => {
    nextDrafts[item.value] = createDraft(item.value)
  })

  const initialProvider = (props.initialConfig?.provider
    || providerOptions.value.find((item) => item.active)?.value
    || providerOptions.value[0]?.value
    || DEFAULT_PROVIDER) as AiProvider

  const initialDraft = nextDrafts[initialProvider] || createDraft(initialProvider)
  nextDrafts[initialProvider] = {
    ...initialDraft,
    provider: initialProvider,
    apiUrl: props.initialConfig?.apiUrl || initialDraft.apiUrl,
    model: props.initialConfig?.model || initialDraft.model,
    temperature: props.initialConfig?.temperature ?? initialDraft.temperature,
    maxTokens: props.initialConfig?.maxTokens ?? initialDraft.maxTokens,
    extraHeadersJson: props.initialConfig?.extraHeadersJson || initialDraft.extraHeadersJson || '',
    apiKey: props.initialConfig?.apiKey || ''
  }

  drafts.value = nextDrafts
  applyDraft(initialProvider)
}

function handleProviderChange(provider: AiProvider) {
  syncCurrentDraft()
  applyDraft(provider)
}

function handleClose() {
  if (props.loading) return
  emit('update:modelValue', false)
}

function handleSave() {
  if (!canSave.value || props.loading) return

  syncCurrentDraft()
  emit('save', {
    provider: localForm.provider,
    apiUrl: localForm.apiUrl.trim(),
    apiKey: localForm.apiKey.trim(),
    model: localForm.model.trim(),
    temperature: localForm.temperature,
    maxTokens: localForm.maxTokens,
    extraHeadersJson: localForm.extraHeadersJson?.trim() || ''
  })
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

:deep(.api-key-model-select .el-select__wrapper) {
  min-height: 56px;
  border-radius: 1rem;
  background-color: rgb(248 250 252);
  box-shadow: none;
}

:deep(.api-key-model-select .el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px rgb(59 130 246 / 0.2);
}

:global(.api-key-model-select-popper .el-select-dropdown__wrap) {
  max-height: 156px;
}

:global(.api-key-model-select-popper .el-scrollbar__wrap) {
  overflow-y: auto;
  overscroll-behavior: contain;
}
</style>
