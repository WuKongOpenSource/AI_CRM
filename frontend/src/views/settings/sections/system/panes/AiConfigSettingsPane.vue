<template>
  <div class="space-y-6">
    <el-card shadow="never" class="!border-slate-200">
      <template #header>
        <div class="flex items-center justify-between">
          <span class="font-medium">AI 大模型配置</span>
          <el-tag v-if="aiConfigForm.updateTime" size="small" type="info">
            最后更新: {{ formatTime(aiConfigForm.updateTime) }}
          </el-tag>
        </div>
      </template>

      <el-form :model="aiConfigForm" label-position="top" class="max-w-2xl">
        <el-form-item label="AI 服务提供商">
          <el-select v-model="aiConfigForm.provider" class="w-full" @change="handleProviderChange">
            <el-option
              v-for="preset in AI_PROVIDER_PRESETS"
              :key="preset.value"
              :label="preset.label"
              :value="preset.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="API 基础地址">
          <el-input v-model="aiConfigForm.apiUrl" placeholder="https://api.openai.com/v1">
            <template #prepend>URL</template>
          </el-input>
          <div class="text-xs text-slate-400 mt-1">
            OpenAI 兼容接口的基础 URL，末尾不要加斜杠
          </div>
        </el-form-item>

        <el-form-item label="API 密钥">
          <div class="flex gap-2 w-full">
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
          <div v-if="connectionTestResult" class="mt-2">
            <el-alert :type="connectionTestResult.success ? 'success' : 'error'" :closable="false" show-icon>
              <template #title>
                {{ connectionTestResult.success ? '连接成功' : '连接失败' }}
                <span class="text-slate-500 ml-2">({{ connectionTestResult.responseTime }}ms)</span>
              </template>
              <template #default>
                {{ connectionTestResult.message }}
              </template>
            </el-alert>
          </div>
        </el-form-item>

        <el-form-item label="模型">
          <el-select v-model="aiConfigForm.model" class="w-full" filterable allow-create>
            <el-option
              v-for="model in currentProviderModels"
              :key="model"
              :label="model"
              :value="model"
            />
          </el-select>
          <div class="text-xs text-slate-400 mt-1">
            可以从列表选择或直接输入自定义模型名称
          </div>
        </el-form-item>

        <el-form-item label="Temperature (创造性)">
          <div class="flex items-center gap-4 w-full">
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
          <div class="text-xs text-slate-400 mt-1">
            值越低回复越确定，值越高回复越有创造性。推荐值：0.7
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
          <div class="text-xs text-slate-400 mt-1">
            单次对话允许的最大 Token 数量，包括输入和输出
          </div>
        </el-form-item>

        <div class="flex gap-3 pt-4 border-t border-slate-200">
          <el-button type="primary" :loading="savingAiConfig" @click="handleSaveAiConfig">
            <el-icon class="mr-1"><Document /></el-icon>
            保存 AI 配置
          </el-button>
          <el-button @click="loadAiConfig">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="!border-slate-200">
      <template #header>
        <span class="font-medium">配置说明</span>
      </template>
      <div class="text-sm text-slate-600 space-y-2">
        <p><strong>OpenAI:</strong> 使用 OpenAI 官方 API，需要有效的 API Key</p>
        <p><strong>阿里云 DashScope:</strong> 使用阿里云通义千问系列模型，API 地址为 https://dashscope.aliyuncs.com/compatible-mode/</p>
        <p><strong>自定义:</strong> 任何 OpenAI 兼容的 API 服务，如 LocalAI、Ollama 等</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Connection, Document, Hide, View } from '@element-plus/icons-vue'
import { getAiConfig, testAiConnection, updateAiConfig } from '@/api/systemConfig'
import type { AiConfigUpdateBO, AiConnectionTestResult, AiProvider } from '@/types/systemConfig'
import { AI_PROVIDER_PRESETS } from '../constants'

const showApiKey = ref(false)
const savingAiConfig = ref(false)
const testingConnection = ref(false)
const connectionTestResult = ref<AiConnectionTestResult | null>(null)

const aiConfigForm = reactive<AiConfigUpdateBO & { updateTime?: string }>({
  provider: 'dashscope',
  apiUrl: '',
  apiKey: '',
  model: '',
  temperature: 0.7,
  maxTokens: 2048,
  updateTime: undefined
})

const currentProviderModels = computed(() => {
  const preset = AI_PROVIDER_PRESETS.find((item) => item.value === aiConfigForm.provider)
  return preset?.models || []
})

onMounted(async () => {
  await loadAiConfig()
})

function handleProviderChange(provider: AiProvider) {
  const preset = AI_PROVIDER_PRESETS.find((item) => item.value === provider)
  if (preset) {
    aiConfigForm.apiUrl = preset.baseUrl
    if (preset.models.length > 0) {
      aiConfigForm.model = preset.models[0]
    }
  }
  connectionTestResult.value = null
}

async function loadAiConfig() {
  try {
    const config = await getAiConfig()
    Object.assign(aiConfigForm, {
      provider: (config.provider || 'dashscope') as AiProvider,
      apiUrl: config.apiUrl || '',
      apiKey: config.apiKey || '',
      model: config.model || '',
      temperature: config.temperature ?? 0.7,
      maxTokens: config.maxTokens ?? 2048,
      updateTime: config.updateTime
    })
    connectionTestResult.value = null
  } catch {
    // Error handled by interceptor
  }
}

async function handleTestConnection() {
  if (!aiConfigForm.apiUrl || !aiConfigForm.apiKey) {
    ElMessage.warning('请先填写 API 地址和密钥')
    return
  }

  testingConnection.value = true
  connectionTestResult.value = null

  try {
    const result = await testAiConnection({
      provider: aiConfigForm.provider,
      apiUrl: aiConfigForm.apiUrl,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens
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
  if (!aiConfigForm.apiUrl) {
    ElMessage.warning('请填写 API 地址')
    return
  }
  if (!aiConfigForm.apiKey) {
    ElMessage.warning('请填写 API 密钥')
    return
  }
  if (!aiConfigForm.model) {
    ElMessage.warning('请选择或输入模型名称')
    return
  }

  savingAiConfig.value = true
  try {
    // 已保存的密钥会以脱敏形式回显；如果用户没有重新输入，后端会沿用原密钥。
    await updateAiConfig({
      provider: aiConfigForm.provider,
      apiUrl: aiConfigForm.apiUrl,
      apiKey: aiConfigForm.apiKey,
      model: aiConfigForm.model,
      temperature: aiConfigForm.temperature,
      maxTokens: aiConfigForm.maxTokens
    })
    ElMessage.success('AI 配置保存成功，已立即生效')
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
</script>
