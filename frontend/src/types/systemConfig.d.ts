/**
 * AI 服务提供商类型
 */
export type AiProvider = 'openai' | 'dashscope' | 'custom'

/**
 * AI 配置信息（从后端获取，API Key 脱敏）
 */
export interface AiConfig {
  provider: AiProvider
  apiUrl: string
  apiKey: string
  model: string
  temperature: number
  maxTokens: number
  updateTime?: string
}

/**
 * AI 配置更新参数
 */
export interface AiConfigUpdateBO {
  provider?: AiProvider
  apiUrl: string
  apiKey: string
  model: string
  temperature?: number
  maxTokens?: number
}

/**
 * AI 连接测试结果
 */
export interface AiConnectionTestResult {
  success: boolean
  responseTime: number
  message: string
  model?: string
}

/**
 * 预设的 AI 服务提供商配置
 */
export interface AiProviderPreset {
  label: string
  value: AiProvider
  baseUrl: string
  models: string[]
}

/**
 * MinIO 控制台配置
 */
export interface MinioConsoleConfig {
  enabled: boolean
  consoleUrl: string
}

/**
 * WeKnora 配置信息（从后端获取，API Key 脱敏）
 */
export interface WeKnoraConfig {
  enabled: boolean
  baseUrl: string
  apiKey: string
  knowledgeBaseId: string
  matchCount: number
  vectorThreshold: number
  autoRagEnabled: boolean
  updateTime?: string
}

/**
 * WeKnora 配置更新参数
 */
export interface WeKnoraConfigUpdateBO {
  enabled?: boolean
  baseUrl?: string
  apiKey?: string
  knowledgeBaseId?: string
  matchCount?: number
  vectorThreshold?: number
  autoRagEnabled?: boolean
}

/**
 * WeKnora 连接测试结果
 */
export interface WeKnoraConnectionTestResult {
  success: boolean
  responseTime: number
  message: string
  knowledgeCount?: number
}
