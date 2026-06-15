<template>
  <div class="space-y-6">
    <el-card shadow="never" class="!border-slate-200">
      <template #header>
        <div class="flex items-center justify-between gap-4">
          <span class="font-medium">AI 大模型配置</span>
          <el-tag v-if="aiConfigForm.updateTime" size="small" type="info">
            最后更新 {{ formatTime(aiConfigForm.updateTime) }}
          </el-tag>
        </div>
      </template>

      <div class="mb-6 rounded-2xl border border-slate-200 bg-white px-4 py-4">
        <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <p class="text-xs font-bold uppercase tracking-widest text-slate-400">已保存服务商</p>
            <p class="mt-2 text-sm text-slate-600">
              已配置 {{ configuredProviders.length }} / {{ providerOptions.length }} 个服务商，切换编辑时不会互相覆盖。
            </p>
          </div>
          <el-tag type="primary" effect="plain">
            多服务商配置池
          </el-tag>
        </div>

        <div v-if="configuredProviders.length" class="mt-4 flex flex-wrap gap-2">
          <button
            v-for="provider in configuredProviders"
            :key="provider.value"
            type="button"
            class="inline-flex items-center gap-2 rounded-full border px-3 py-2 text-sm font-medium transition-all"
            :class="provider.value === aiConfigForm.provider
              ? 'border-primary bg-primary/5 text-primary'
              : 'border-slate-200 bg-slate-50 text-slate-600 hover:border-primary/40 hover:text-primary'"
            @click="handleProviderChange(provider.value)"
          >
            <span>{{ provider.label }}</span>
            <span
              class="rounded-full px-2 py-0.5 text-[11px]"
              :class="provider.active ? 'bg-primary text-white' : 'bg-slate-200 text-slate-600'"
            >
              {{ provider.active ? '当前生效' : '已保存' }}
            </span>
          </button>
        </div>
        <p v-else class="mt-4 text-sm text-slate-400">
          还没有已保存的服务商配置，保存后会自动加入配置池。
        </p>
      </div>

      <el-form :model="aiConfigForm" label-position="top" class="max-w-3xl">
        <el-form-item label="AI 服务商">
          <el-select v-model="aiConfigForm.provider" class="w-full" @change="handleProviderChange">
            <el-option
              v-for="preset in providerOptions"
              :key="preset.value"
              :label="preset.label"
              :value="preset.value"
            />
          </el-select>
          <div class="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-400">
            <span>{{ currentProviderPreset?.description || '支持 OpenAI 兼容接口的通用模型配置。' }}</span>
            <el-tag v-if="currentProviderPreset?.configured" size="small" type="success" effect="plain">
              已保存该服务商配置
            </el-tag>
            <el-tag v-if="currentProviderPreset?.active" size="small" type="primary" effect="plain">
              当前默认生效
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="API 基础地址">
          <el-input v-model="aiConfigForm.apiUrl" placeholder="https://api.openai.com">
            <template #prepend>URL</template>
          </el-input>
          <div class="mt-1 text-xs text-slate-400">
            这里填写兼容 OpenAI Chat Completions 的基础地址，不需要额外填写结尾的 `/v1`。
          </div>
        </el-form-item>

        <el-form-item label="API 密钥">
          <div class="flex w-full gap-2">
            <el-input
              v-model="aiConfigForm.apiKey"
              :type="showApiKey ? 'text' : 'password'"
              placeholder="sk-xxxxxx"
              class="flex-1"
            >
              <template #prepend>Key</template>
              <template #suffix>
                <el-icon class="cursor-pointer" @click="showApiKey = !showApiKey">
                  <View v-if="showApiKey" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
            <el-button :loading="testingConnection" @click="handleTestConnection">
              <el-icon class="mr-1"><Connection /></el-icon>
              测试连接
            </el-button>
          </div>
          <div class="mt-1 text-xs text-slate-400">
            <template v-if="currentProviderPreset?.apiKeyConfigured">
              当前服务商已保存 API Key，不重新输入时保存会沿用已有 Key；如需覆盖或测试请重新输入。
            </template>
            <template v-else>
              出于安全原因，系统不会回显已保存的 API Key。保存或测试前需要重新输入。
            </template>
          </div>
          <div v-if="connectionTestResult" class="mt-2">
            <el-alert :type="connectionTestResult.success ? 'success' : 'error'" :closable="false" show-icon>
              <template #title>
                {{ connectionTestResult.success ? '连接成功' : '连接失败' }}
                <span class="ml-2 text-slate-500">({{ connectionTestResult.responseTime }}ms)</span>
              </template>
              <template #default>
                {{ connectionTestResult.message }}
              </template>
            </el-alert>
          </div>
        </el-form-item>

        <el-form-item label="模型">
          <el-select
            v-model="aiConfigForm.model"
            class="w-full"
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入模型名称"
          >
            <el-option
              v-for="model in currentProviderModels"
              :key="model"
              :label="model"
              :value="model"
            />
          </el-select>
          <div class="mt-1 text-xs text-slate-400">
            {{ currentModelHint }}
          </div>
        </el-form-item>

        <el-form-item label="模型能力">
          <div class="flex flex-wrap gap-2">
            <el-tag :type="currentCapabilities.supportsStream ? 'success' : 'info'">
              {{ currentCapabilities.supportsStream ? '支持流式输出' : '不支持流式输出' }}
            </el-tag>
            <el-tag :type="currentCapabilities.supportsToolCall ? 'success' : 'warning'">
              {{ currentCapabilities.supportsToolCall ? '支持工具调用' : '不支持工具调用' }}
            </el-tag>
            <el-tag :type="currentCapabilities.supportsVision ? 'success' : 'info'">
              {{ currentCapabilities.supportsVision ? '支持视觉输入' : '文本模型' }}
            </el-tag>
            <el-tag :type="currentCapabilities.supportsAudioTranscription ? 'success' : 'info'">
              {{ currentCapabilities.supportsAudioTranscription ? '支持语音转写' : '不支持语音转写' }}
            </el-tag>
          </div>
          <div class="mt-1 text-xs text-slate-400">
            能力展示优先使用当前已保存模型的后端判定；修改模型后，最终以“测试连接”和“保存配置”的校验结果为准。
          </div>
        </el-form-item>

        <el-form-item label="额外请求头 (JSON)">
          <el-input
            v-model="aiConfigForm.extraHeadersJson"
            type="textarea"
            :rows="4"
            placeholder="{&quot;appid&quot;:&quot;your-app-id&quot;}"
          />
          <div class="mt-1 text-xs text-slate-400">
            用于兼容某些服务商的附加请求头。{{ currentExtraHeadersHint || '如无需额外请求头可留空。' }}
          </div>
          <div v-if="currentProviderPreset?.savedExtraHeadersConfigured" class="mt-1 text-xs text-emerald-600">
            当前服务商已保存额外请求头，页面中展示的是管理端可编辑的原始配置。
          </div>
        </el-form-item>

        <el-form-item label="Temperature">
          <div class="flex w-full items-center gap-4">
            <el-slider
              v-model="aiConfigForm.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              :format-tooltip="(value: number) => value.toFixed(1)"
              class="flex-1"
            />
            <el-input-number
              v-model="aiConfigForm.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              :precision="1"
              class="w-24"
            />
          </div>
        </el-form-item>

        <el-form-item label="最大 Token 数">
          <el-input-number
            v-model="aiConfigForm.maxTokens"
            :min="100"
            :max="128000"
            :step="100"
            class="w-full"
          />
        </el-form-item>

        <div class="flex flex-wrap gap-3 border-t border-slate-200 pt-4">
          <el-button type="primary" :loading="savingAiConfig" @click="handleSaveAiConfig">
            <el-icon class="mr-1"><Document /></el-icon>
            保存当前服务商配置
          </el-button>
          <el-button
            plain
            :disabled="!currentProviderPreset?.configured || currentProviderPreset?.active"
            :loading="activatingProvider"
            @click="handleActivateProvider"
          >
            启用当前服务商
          </el-button>
          <el-button @click="loadAiConfig">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="!border-slate-200">
      <template #header>
        <span class="font-medium">配置说明</span>
      </template>

      <div class="space-y-3 text-sm text-slate-600">
        <p>当前 CRM 默认要求主对话模型支持工具调用，否则无法正常驱动客户、任务、跟进等内置工具。</p>
        <p>现在支持为不同服务商分别保存配置，切换编辑时会自动回填对应服务商的历史参数。</p>
        <p>如果你要接入百度千帆等需要额外头信息的服务，可以在“额外请求头 (JSON)”中补充。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Document, Hide, View } from '@element-plus/icons-vue'
import {
  activateAiProvider,
  getAiConfigDetail,
  testAiConnection,
  updateAiConfig
} from '@/api/systemConfig'
import type {
  AiConfig,
  AiConfigUpdateBO,
  AiConnectionTestResult,
  AiModelCapabilities,
  AiProvider,
  AiProviderPreset
} from '@/types/systemConfig'
import { AI_PROVIDER_PRESETS } from '../constants'

type AiConfigFormState = AiConfigUpdateBO & {
  updateTime?: string
  configured?: boolean
  apiKeyConfigured?: boolean
}

type ProviderDraftMap = Partial<Record<AiProvider, AiConfigFormState>>

const DEFAULT_PROVIDER: AiProvider = 'dashscope'

const showApiKey = ref(false)
const savingAiConfig = ref(false)
const testingConnection = ref(false)
const activatingProvider = ref(false)
const connectionTestResult = ref<AiConnectionTestResult | null>(null)
const providerOptions = ref<AiProviderPreset[]>(AI_PROVIDER_PRESETS)
const providerDrafts = ref<ProviderDraftMap>({})
const loadedConfig = ref<AiConfig | null>(null)

const aiConfigForm = reactive<AiConfigFormState>({
  provider: DEFAULT_PROVIDER,
  apiUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
  apiKey: '',
  model: 'qwen3.5-plus',
  temperature: 0.7,
  maxTokens: 2048,
  extraHeadersJson: '',
  updateTime: undefined,
  configured: false,
  apiKeyConfigured: false
})

const currentProviderPreset = computed(() => {
  return providerOptions.value.find((item) => item.value === aiConfigForm.provider) || null
})

const configuredProviders = computed(() => providerOptions.value.filter((item) => item.configured))
const currentProviderModels = computed(() => currentProviderPreset.value?.models || [])

const currentCapabilities = computed<AiModelCapabilities>(() => {
  if (
    loadedConfig.value &&
    loadedConfig.value.provider === aiConfigForm.provider &&
    loadedConfig.value.model === aiConfigForm.model &&
    loadedConfig.value.capabilities
  ) {
    return loadedConfig.value.capabilities
  }

  return inferCapabilities(aiConfigForm.provider, aiConfigForm.model)
})

const currentModelHint = computed(() => {
  if (
    loadedConfig.value &&
    loadedConfig.value.provider === aiConfigForm.provider &&
    loadedConfig.value.model === aiConfigForm.model &&
    loadedConfig.value.modelHint
  ) {
    return loadedConfig.value.modelHint
  }
  return currentProviderPreset.value?.modelHint || '请选择或输入可用模型名称。'
})

const currentExtraHeadersHint = computed(() => {
  if (
    loadedConfig.value &&
    loadedConfig.value.provider === aiConfigForm.provider &&
    loadedConfig.value.extraHeadersHint
  ) {
    return loadedConfig.value.extraHeadersHint
  }
  return currentProviderPreset.value?.extraHeadersHint || ''
})

onMounted(async () => {
  await loadAiConfig()
})

function createProviderDraft(preset?: AiProviderPreset | null): AiConfigFormState {
  const provider = (preset?.value || DEFAULT_PROVIDER) as AiProvider
  return {
    provider,
    apiUrl: preset?.savedApiUrl || preset?.baseUrl || '',
    apiKey: '',
    model: preset?.savedModel || preset?.models?.[0] || '',
    temperature: preset?.savedTemperature ?? 0.7,
    maxTokens: preset?.savedMaxTokens ?? 2048,
    extraHeadersJson: preset?.savedExtraHeadersJson ?? '',
    updateTime: undefined,
    configured: Boolean(preset?.configured),
    apiKeyConfigured: Boolean(preset?.apiKeyConfigured)
  }
}

function getCurrentProvider(): AiProvider {
  return aiConfigForm.provider ?? DEFAULT_PROVIDER
}

function syncCurrentFormToDraft() {
  const provider = getCurrentProvider()
  providerDrafts.value[provider] = {
    provider,
    apiUrl: aiConfigForm.apiUrl,
    apiKey: aiConfigForm.apiKey,
    model: aiConfigForm.model,
    temperature: aiConfigForm.temperature,
    maxTokens: aiConfigForm.maxTokens,
    extraHeadersJson: aiConfigForm.extraHeadersJson || '',
    updateTime: aiConfigForm.updateTime,
    configured: aiConfigForm.configured,
    apiKeyConfigured: aiConfigForm.apiKeyConfigured
  }
}

function applyProviderDraft(provider: AiProvider) {
  const preset = providerOptions.value.find((item) => item.value === provider) || null
  const draft = providerDrafts.value[provider] || createProviderDraft(preset)
  providerDrafts.value[provider] = draft

  Object.assign(aiConfigForm, {
    provider,
    apiUrl: draft.apiUrl,
    apiKey: draft.apiKey,
    model: draft.model,
    temperature: draft.temperature ?? 0.7,
    maxTokens: draft.maxTokens ?? 2048,
    extraHeadersJson: draft.extraHeadersJson || '',
    updateTime: draft.updateTime,
    configured: draft.configured ?? false,
    apiKeyConfigured: draft.apiKeyConfigured ?? false
  })

  showApiKey.value = false
  connectionTestResult.value = null
}

function handleProviderChange(provider: AiProvider) {
  syncCurrentFormToDraft()
  applyProviderDraft(provider)
}

async function loadAiConfig() {
  try {
    const config = await getAiConfigDetail()
    loadedConfig.value = config
    providerOptions.value = config.availableProviders?.length ? config.availableProviders : AI_PROVIDER_PRESETS

    const nextDrafts: ProviderDraftMap = {}
    providerOptions.value.forEach((preset) => {
      nextDrafts[preset.value] = createProviderDraft(preset)
    })

    const currentProvider = (config.provider
      || providerOptions.value.find((item) => item.active)?.value
      || providerOptions.value[0]?.value
      || DEFAULT_PROVIDER) as AiProvider

    const currentDraft = nextDrafts[currentProvider]
    if (currentDraft) {
      currentDraft.updateTime = config.updateTime
    }

    providerDrafts.value = nextDrafts
    applyProviderDraft(currentProvider)
  } catch {
    // Error handled by interceptor
  }
}

async function handleActivateProvider() {
  const provider = getCurrentProvider()

  activatingProvider.value = true
  try {
    await activateAiProvider({ provider })
    ElMessage.success(`已切换到 ${currentProviderPreset.value?.label || provider} 配置`)
    await loadAiConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    activatingProvider.value = false
  }
}

async function handleTestConnection() {
  if (!aiConfigForm.apiUrl?.trim() || !aiConfigForm.apiKey?.trim()) {
    ElMessage.warning('请先填写 API 地址和 API 密钥')
    return
  }
  if (!aiConfigForm.model?.trim()) {
    ElMessage.warning('请先填写模型名称')
    return
  }

  testingConnection.value = true
  connectionTestResult.value = null

  try {
    const result = await testAiConnection({
      provider: getCurrentProvider(),
      apiUrl: aiConfigForm.apiUrl.trim(),
      apiKey: aiConfigForm.apiKey.trim(),
      model: aiConfigForm.model.trim(),
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens,
      extraHeadersJson: aiConfigForm.extraHeadersJson?.trim() || ''
    })
    connectionTestResult.value = result
  } catch (error: any) {
    connectionTestResult.value = {
      success: false,
      responseTime: 0,
      message: error.message || '连接测试失败'
    }
  } finally {
    testingConnection.value = false
  }
}

async function handleSaveAiConfig() {
  const trimmedApiKey = aiConfigForm.apiKey?.trim() || ''
  if (!aiConfigForm.apiUrl?.trim()) {
    ElMessage.warning('请填写 API 地址')
    return
  }
  if (!trimmedApiKey && !currentProviderPreset.value?.apiKeyConfigured) {
    ElMessage.warning('请填写 API 密钥，或先保存当前服务商的 API Key')
    return
  }
  if (!aiConfigForm.model?.trim()) {
    ElMessage.warning('请选择或输入模型名称')
    return
  }

  savingAiConfig.value = true
  try {
    await updateAiConfig({
      provider: getCurrentProvider(),
      apiUrl: aiConfigForm.apiUrl.trim(),
      apiKey: trimmedApiKey,
      model: aiConfigForm.model.trim(),
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens,
      extraHeadersJson: aiConfigForm.extraHeadersJson?.trim() || ''
    })
    ElMessage.success('当前服务商配置已保存，并已设为默认生效')
    await loadAiConfig()
  } catch {
    // Error handled by interceptor
  } finally {
    savingAiConfig.value = false
  }
}

function formatTime(time: string | undefined): string {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function inferCapabilities(provider: AiProvider | undefined, model: string | undefined): AiModelCapabilities {
  const normalizedProvider = (provider || '').toLowerCase()
  const normalizedModel = (model || '').trim().toLowerCase()

  let supportsStream = currentProviderPreset.value?.supportsStream ?? true
  let supportsToolCall = currentProviderPreset.value?.supportsToolCall ?? true
  let supportsVision = currentProviderPreset.value?.supportsVision ?? false
  const supportsAudioTranscription = currentProviderPreset.value?.supportsAudioTranscription ?? false

  if (normalizedProvider === 'hunyuan') {
    supportsToolCall = normalizedModel.includes('functioncall')
    supportsVision = normalizedModel.includes('vision')
  } else if (normalizedProvider === 'moonshot') {
    supportsToolCall = normalizedModel !== '' && !normalizedModel.includes('kimi-thinking-preview')
    supportsVision = ['kimi-k2.5', 'kimi-k2.6', 'kimi-k2-5', 'kimi-k2-6', 'vision', 'vl']
      .some((keyword) => normalizedModel.includes(keyword))
  } else {
    if (['gpt-5', '4o', '4.1', 'vision', 'omni', '3.5-plus', 'doubao-seed-2-0', 'doubao-seed-2.0', '1-8', '4.6v', '4.5v', '4v', 'vl'].some((keyword) => normalizedModel.includes(keyword))) {
      supportsVision = true
    }
    if (['openai', 'dashscope', 'deepseek', 'ark', 'minimax', 'zhipu', 'custom'].includes(normalizedProvider)) {
      supportsToolCall = true
    }
  }

  return {
    supportsStream,
    supportsToolCall,
    supportsVision,
    supportsAudioTranscription
  }
}
</script>
