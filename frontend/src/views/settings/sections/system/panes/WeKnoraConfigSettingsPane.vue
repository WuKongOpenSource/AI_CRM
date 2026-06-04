<template>
  <div class="space-y-6">
    <el-card shadow="never" class="!border-slate-200">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">WeKnora 知识库服务配置</span>
          <el-tag v-if="form.updateTime" size="small" type="info">
            最后更新: {{ formatTime(form.updateTime) }}
          </el-tag>
        </div>
      </template>

      <el-form :model="form" label-position="top" class="max-w-3xl">
        <el-form-item label="启用 WeKnora">
          <div class="flex items-center gap-3">
            <el-switch v-model="form.enabled" />
            <span class="text-sm text-slate-500">
              {{ form.enabled ? '已启用 - 文档将同步到知识库' : '未启用 - 仅使用本地存储' }}
            </span>
          </div>
        </el-form-item>

        <el-form-item label="API 基础地址">
          <el-input v-model="form.baseUrl" placeholder="http://app:8080/api/v1">
            <template #prepend>URL</template>
          </el-input>
        </el-form-item>

        <el-form-item label="WeKnora 租户 API Key">
          <div class="flex flex-wrap gap-2 w-full">
            <el-input
              v-model="form.apiKey"
              :type="showWeKnoraKey ? 'text' : 'password'"
              placeholder="同步成功后自动填充，手动填写 WeKnora 生成的 sk-- 密钥"
              class="flex-1"
            >
              <template #prepend>Key</template>
              <template #suffix>
                <el-icon class="cursor-pointer" @click="showWeKnoraKey = !showWeKnoraKey">
                  <View v-if="showWeKnoraKey" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
            <el-button :loading="testing" @click="handleTestConnection">
              <el-icon class="mr-1"><Connection /></el-icon>
              测试连接
            </el-button>
            <p class="basis-full text-xs text-slate-400 mt-1">
              这里不是模型厂商 Key；模型 Key 请填写在下方 LLM / Embedding 配置中。
            </p>
          </div>
        </el-form-item>

        <el-form-item label="默认知识库名称">
          <el-input v-model="form.knowledgeBaseName" placeholder="CRM Default Knowledge Base" />
        </el-form-item>

        <el-form-item label="默认知识库 ID">
          <el-input v-model="form.knowledgeBaseId" placeholder="同步成功后自动填充" readonly />
        </el-form-item>

        <el-divider content-position="left">LLM 模型</el-divider>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
          <el-form-item label="服务商">
            <el-select v-model="form.llmProvider" class="w-full" filterable allow-create @change="handleLlmProviderChange">
              <el-option
                v-for="provider in MODEL_PROVIDERS"
                :key="provider.value"
                :label="provider.label"
                :value="provider.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="模型名称">
            <el-input v-model="form.llmModelName" placeholder="qwen-max" />
          </el-form-item>
        </div>

        <el-form-item label="Base URL">
          <el-input v-model="form.llmBaseUrl" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
        </el-form-item>

        <el-form-item label="API Key">
          <div class="flex gap-2 w-full">
            <el-input
              v-model="form.llmApiKey"
              :type="showLlmKey ? 'text' : 'password'"
              placeholder="LLM API Key"
              class="flex-1"
            >
              <template #suffix>
                <el-icon class="cursor-pointer" @click="showLlmKey = !showLlmKey">
                  <View v-if="showLlmKey" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
            <el-button :loading="testingLlm" @click="handleTestLlmModel">
              <el-icon class="mr-1"><Connection /></el-icon>
              测试连接
            </el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="form.llmModelId" label="LLM 模型 ID">
          <el-input v-model="form.llmModelId" readonly />
        </el-form-item>

        <el-divider content-position="left">Embedding 模型</el-divider>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
          <el-form-item label="服务商">
            <el-select v-model="form.embeddingProvider" class="w-full" filterable allow-create @change="handleEmbeddingProviderChange">
              <el-option
                v-for="provider in MODEL_PROVIDERS"
                :key="provider.value"
                :label="provider.label"
                :value="provider.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="模型名称">
            <el-input v-model="form.embeddingModelName" placeholder="text-embedding-v3" />
          </el-form-item>
        </div>

        <el-form-item label="Base URL">
          <el-input v-model="form.embeddingBaseUrl" placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
        </el-form-item>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-x-4">
          <el-form-item label="API Key">
            <div class="flex gap-2 w-full">
              <el-input
                v-model="form.embeddingApiKey"
                :type="showEmbeddingKey ? 'text' : 'password'"
                placeholder="Embedding API Key"
                class="flex-1"
              >
                <template #suffix>
                  <el-icon class="cursor-pointer" @click="showEmbeddingKey = !showEmbeddingKey">
                    <View v-if="showEmbeddingKey" />
                    <Hide v-else />
                  </el-icon>
                </template>
              </el-input>
              <el-button :loading="testingEmbedding" @click="handleTestEmbeddingModel">
                <el-icon class="mr-1"><Connection /></el-icon>
                测试连接
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="向量维度">
            <el-input-number v-model="form.embeddingDimension" :min="1" :step="1" class="w-full" />
          </el-form-item>
        </div>

        <el-form-item v-if="form.embeddingModelId" label="Embedding 模型 ID">
          <el-input v-model="form.embeddingModelId" readonly />
        </el-form-item>

        <div v-if="syncResult" class="mb-4">
          <el-alert :type="syncResult.success ? 'success' : 'error'" :closable="false" show-icon>
            <template #title>
              {{ syncResult.message }}
            </template>
          </el-alert>
        </div>

        <div class="flex gap-3 pt-4 border-t border-slate-200">
          <el-button type="primary" :loading="syncing" @click="handleSyncModels">
            <el-icon class="mr-1"><Connection /></el-icon>
            同步模型并创建知识库
          </el-button>
          <el-button :loading="saving" @click="handleSaveConfig">仅保存配置</el-button>
          <el-button @click="loadConfig">重置</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Hide, View } from '@element-plus/icons-vue'
import {
  getWeKnoraConfig,
  syncWeKnoraModels,
  testWeKnoraModel,
  testWeKnoraConnection,
  updateWeKnoraConfig
} from '@/api/systemConfig'
import type { WeKnoraConnectionTestResult, WeKnoraModelSyncBO, WeKnoraModelSyncResult } from '@/types/systemConfig'

const MODEL_PROVIDERS = [
  {
    label: '阿里云 DashScope',
    value: 'aliyun',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    llmModel: 'qwen-max',
    embeddingModel: 'text-embedding-v3',
    dimension: 1024
  },
  {
    label: 'OpenAI 兼容接口',
    value: 'custom',
    baseUrl: '',
    llmModel: '',
    embeddingModel: '',
    dimension: 1536
  }
] as const

const form = reactive<WeKnoraModelSyncBO & {
  knowledgeBaseId?: string
  llmModelId?: string
  embeddingModelId?: string
  matchCount?: number
  vectorThreshold?: number
  autoRagEnabled?: boolean
  updateTime?: string
}>({
  enabled: true,
  baseUrl: 'http://app:8080/api/v1',
  apiKey: '',
  knowledgeBaseName: 'CRM Default Knowledge Base',
  knowledgeBaseId: '',
  llmProvider: 'aliyun',
  llmModelName: 'qwen-max',
  llmBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  llmApiKey: '',
  llmModelId: '',
  embeddingProvider: 'aliyun',
  embeddingModelName: 'text-embedding-v3',
  embeddingBaseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
  embeddingApiKey: '',
  embeddingDimension: 1024,
  embeddingModelId: '',
  matchCount: 5,
  vectorThreshold: 0.5,
  autoRagEnabled: true,
  updateTime: undefined
})

const showWeKnoraKey = ref(false)
const showLlmKey = ref(false)
const showEmbeddingKey = ref(false)
const testing = ref(false)
const testingLlm = ref(false)
const testingEmbedding = ref(false)
const saving = ref(false)
const syncing = ref(false)
const syncResult = ref<WeKnoraModelSyncResult | null>(null)

onMounted(async () => {
  await loadConfig()
})

async function loadConfig() {
  try {
    const config = await getWeKnoraConfig()
    Object.assign(form, {
      enabled: config.enabled ?? true,
      baseUrl: config.baseUrl || 'http://app:8080/api/v1',
      apiKey: config.apiKey || '',
      knowledgeBaseId: config.knowledgeBaseId || '',
      knowledgeBaseName: config.knowledgeBaseName || 'CRM Default Knowledge Base',
      llmProvider: config.llmProvider || 'aliyun',
      llmModelName: config.llmModelName || 'qwen-max',
      llmBaseUrl: config.llmBaseUrl || 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      llmApiKey: config.llmApiKey || '',
      llmModelId: config.llmModelId || '',
      embeddingProvider: config.embeddingProvider || 'aliyun',
      embeddingModelName: config.embeddingModelName || 'text-embedding-v3',
      embeddingBaseUrl: config.embeddingBaseUrl || 'https://dashscope.aliyuncs.com/compatible-mode/v1',
      embeddingApiKey: config.embeddingApiKey || '',
      embeddingDimension: config.embeddingDimension || 1024,
      embeddingModelId: config.embeddingModelId || '',
      matchCount: config.matchCount ?? 5,
      vectorThreshold: config.vectorThreshold ?? 0.5,
      autoRagEnabled: config.autoRagEnabled ?? true,
      updateTime: config.updateTime
    })
    syncResult.value = null
  } catch {
    // 错误提示由请求拦截器统一处理。
  }
}

function handleLlmProviderChange(provider: string) {
  const preset = MODEL_PROVIDERS.find((item) => item.value === provider)
  if (!preset) return
  form.llmBaseUrl = preset.baseUrl
  form.llmModelName = preset.llmModel
}

function handleEmbeddingProviderChange(provider: string) {
  const preset = MODEL_PROVIDERS.find((item) => item.value === provider)
  if (!preset) return
  form.embeddingBaseUrl = preset.baseUrl
  form.embeddingModelName = preset.embeddingModel
  form.embeddingDimension = preset.dimension
}

async function handleTestConnection() {
  if (!form.baseUrl) {
    ElMessage.warning('请填写 WeKnora API 地址和密钥')
    return
  }

  testing.value = true
  try {
    const result: WeKnoraConnectionTestResult = await testWeKnoraConnection({
      baseUrl: form.baseUrl,
      apiKey: form.apiKey
    })
    if (result.success) {
      ElMessage.success(result.message)
      await loadConfig()
    } else {
      ElMessage.error(result.message)
    }
  } catch {
  } finally {
    testing.value = false
  }
}

async function handleTestLlmModel() {
  if (!form.llmModelName || !form.llmBaseUrl || !form.llmApiKey) {
    ElMessage.warning('请填写 LLM 模型名称、Base URL 和 API Key')
    return
  }

  testingLlm.value = true
  try {
    const result = await testWeKnoraModel({
      modelType: 'llm',
      provider: form.llmProvider,
      modelName: form.llmModelName,
      baseUrl: form.llmBaseUrl,
      apiKey: form.llmApiKey
    })
    if (result.success) {
      ElMessage.success(result.message)
    } else {
      ElMessage.error(result.message)
    }
  } catch {
  } finally {
    testingLlm.value = false
  }
}

async function handleTestEmbeddingModel() {
  if (!form.embeddingModelName || !form.embeddingBaseUrl || !form.embeddingApiKey || !form.embeddingDimension) {
    ElMessage.warning('请填写 Embedding 模型名称、Base URL、API Key 和向量维度')
    return
  }

  testingEmbedding.value = true
  try {
    const result = await testWeKnoraModel({
      modelType: 'embedding',
      provider: form.embeddingProvider,
      modelName: form.embeddingModelName,
      baseUrl: form.embeddingBaseUrl,
      apiKey: form.embeddingApiKey,
      embeddingDimension: form.embeddingDimension
    })
    if (result.success) {
      const dimensionText = result.embeddingDimension ? `，维度 ${result.embeddingDimension}` : ''
      ElMessage.success(`${result.message}${dimensionText}`)
    } else {
      ElMessage.error(result.message)
    }
  } catch {
  } finally {
    testingEmbedding.value = false
  }
}

async function handleSaveConfig() {
  saving.value = true
  try {
    await updateWeKnoraConfig({
      enabled: form.enabled,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      knowledgeBaseId: form.knowledgeBaseId,
      matchCount: form.matchCount,
      vectorThreshold: form.vectorThreshold,
      autoRagEnabled: form.autoRagEnabled
    })
    ElMessage.success('WeKnora 配置已保存')
    await loadConfig()
  } catch {
  } finally {
    saving.value = false
  }
}

async function handleSyncModels() {
  if (!form.baseUrl) {
    ElMessage.warning('请填写 WeKnora API 地址和密钥')
    return
  }
  if (!form.llmModelName || !form.llmBaseUrl || !form.llmApiKey) {
    ElMessage.warning('请填写 LLM 模型名称、Base URL 和 API Key')
    return
  }
  if (!form.embeddingModelName || !form.embeddingBaseUrl || !form.embeddingApiKey || !form.embeddingDimension) {
    ElMessage.warning('请填写 Embedding 模型名称、Base URL、API Key 和向量维度')
    return
  }

  syncing.value = true
  syncResult.value = null
  try {
    const result = await syncWeKnoraModels({ ...form })
    syncResult.value = result
    ElMessage.success('WeKnora 模型和默认知识库已同步')
    await loadConfig()
  } catch {
  } finally {
    syncing.value = false
  }
}

function formatTime(time: string | undefined): string {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}
</script>
