import type { AiProviderPreset } from '@/types/systemConfig'
import type { FieldType } from '@/types/customField'

export const AI_PROVIDER_PRESETS: AiProviderPreset[] = [
  {
    label: 'OpenAI',
    value: 'openai',
    description: 'OpenAI 官方接口，适合标准 OpenAI 生态接入。',
    baseUrl: 'https://api.openai.com',
    models: ['gpt-5.4', 'gpt-5.2', 'gpt-5-mini', 'gpt-5-nano'],
    modelHint: '填写 OpenAI 官方模型名称，例如 gpt-5.4。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: true,
    supportsAudioTranscription: true
  },
  {
    label: '阿里云百炼 / 通义千问',
    value: 'dashscope',
    description: '通过 OpenAI 兼容接口接入通义千问系列模型。',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
    models: ['qwen3.5-plus', 'qwen3-max-2026-01-23', 'qwen3-coder-next', 'qwen3-coder-plus', 'qwen-plus-latest'],
    modelHint: '填写通义千问模型名称，例如 qwen3.5-plus。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: false,
    supportsAudioTranscription: true
  },
  {
    label: 'Moonshot AI / Kimi',
    value: 'moonshot',
    description: 'Kimi 兼容 OpenAI SDK，适合长文本与知识问答。',
    baseUrl: 'https://api.moonshot.cn',
    models: ['kimi-k2-thinking-turbo', 'kimi-k2-0905-preview', 'kimi-k2-turbo-preview', 'kimi-latest'],
    modelHint: '填写 Kimi 模型名称，例如 kimi-k2-0905-preview。旧版 kimi-thinking-preview 不建议作为 CRM 主模型。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: false,
    supportsAudioTranscription: false
  },
  {
    label: 'DeepSeek',
    value: 'deepseek',
    description: 'DeepSeek 提供与 OpenAI 兼容的接口，推荐使用 deepseek-chat。',
    baseUrl: 'https://api.deepseek.com',
    models: ['deepseek-chat', 'deepseek-reasoner'],
    modelHint: '填写 DeepSeek 模型名称，例如 deepseek-chat。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: false,
    supportsAudioTranscription: false
  },
  {
    label: '火山方舟 / 豆包',
    value: 'ark',
    description: '通常填写模型 ID 或 Endpoint ID。',
    baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
    models: [
      'doubao-seed-2-0-pro-260215',
      'doubao-seed-2-0-lite-260428',
      'doubao-seed-2-0-mini-260428',
      'doubao-seed-1-8-251228',
      'doubao-seed-1-8-32k-251228',
      'doubao-seed-code',
      'doubao-seed-1-6-251015'
    ],
    modelHint: '填写火山方舟模型 ID 或 Endpoint ID，例如 doubao-seed-2-0-pro-260215。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: false,
    supportsAudioTranscription: false
  },
  {
    label: '腾讯混元',
    value: 'hunyuan',
    description: '兼容 OpenAI 协议，函数调用与视觉能力需按模型选择。',
    baseUrl: 'https://api.hunyuan.cloud.tencent.com',
    models: ['hunyuan-t1-latest', 'hunyuan-turbos-latest', 'hunyuan-functioncall', 'hunyuan-vision'],
    modelHint: '填写混元模型名称，例如 hunyuan-t1-latest；如需工具调用，请使用 hunyuan-functioncall。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: false,
    supportsVision: false,
    supportsAudioTranscription: false
  },
  {
    label: 'MiniMax',
    value: 'minimax',
    description: 'MiniMax 提供 OpenAI 兼容接口，当前主推 MiniMax-M2.7 系列。',
    baseUrl: 'https://api.minimaxi.com',
    models: ['MiniMax-M2.7', 'MiniMax-M2.7-highspeed', 'MiniMax-M2.5', 'MiniMax-M2.5-highspeed', 'MiniMax-M2.1', 'MiniMax-M2.1-highspeed', 'MiniMax-M2'],
    modelHint: '填写 MiniMax 模型名称，例如 MiniMax-M2.7。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: false,
    supportsAudioTranscription: false
  },
  {
    label: '智谱 AI',
    value: 'zhipu',
    description: '支持 GLM 文本与视觉模型的 OpenAI 兼容接入。',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    models: ['glm-5', 'glm-4.6v', 'glm-4.5-air', 'glm-4.5v'],
    modelHint: '填写智谱模型名称，例如 glm-5。视觉理解可使用 glm-4.6v 或 glm-4.5v。',
    extraHeadersHint: '',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: false,
    supportsAudioTranscription: false
  },
  {
    label: '自定义 OpenAI 兼容服务',
    value: 'custom',
    description: '适用于自建或第三方兼容 OpenAI Chat Completions 的服务。',
    baseUrl: '',
    models: [],
    modelHint: '填写服务端要求的模型名称。',
    extraHeadersHint: '{"X-Your-Header":"value"}',
    supportsStream: true,
    supportsToolCall: true,
    supportsVision: false,
    supportsAudioTranscription: false
  }
]

export const SYSTEM_FIELD_TYPE_LABELS: Record<FieldType, string> = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  date: '日期',
  datetime: '日期时间',
  select: '单选下拉',
  multiselect: '多选下拉',
  checkbox: '开关'
}
