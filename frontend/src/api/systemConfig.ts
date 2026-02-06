import { get, post } from '@/utils/request'
import type {
  AiConfig,
  AiConfigUpdateBO,
  AiConnectionTestResult,
  MinioConsoleConfig,
  WeKnoraConfig,
  WeKnoraConfigUpdateBO,
  WeKnoraConnectionTestResult
} from '@/types/systemConfig'

/**
 * 获取 AI 配置
 */
export function getAiConfig(): Promise<AiConfig> {
  return get('/systemConfig/ai')
}

/**
 * 更新 AI 配置
 */
export function updateAiConfig(data: AiConfigUpdateBO): Promise<void> {
  return post('/systemConfig/ai/update', data)
}

/**
 * 测试 AI 连接
 */
export function testAiConnection(data: AiConfigUpdateBO): Promise<AiConnectionTestResult> {
  return post('/systemConfig/ai/test', data)
}

/**
 * 按类型获取配置
 */
export function getConfigsByType(type: string): Promise<Record<string, string>> {
  return get(`/systemConfig/byType/${type}`)
}

/**
 * 清除配置缓存
 */
export function clearConfigCache(): Promise<void> {
  return post('/systemConfig/clearCache')
}

/**
 * 获取 MinIO 控制台地址
 */
export function getMinioConsoleUrl(): Promise<MinioConsoleConfig> {
  return get('/systemConfig/minio/consoleUrl')
}

/**
 * 获取 MinIO SSO 登录 URL（包含 session_token）
 */
export function getMinioSsoUrl(): Promise<{ enabled: boolean; ssoUrl: string | null }> {
  return get('/systemConfig/minio/ssoUrl')
}

// ==================== WeKnora 配置接口 ====================

/**
 * 获取 WeKnora 配置
 */
export function getWeKnoraConfig(): Promise<WeKnoraConfig> {
  return get('/systemConfig/weknora')
}

/**
 * 更新 WeKnora 配置
 */
export function updateWeKnoraConfig(data: WeKnoraConfigUpdateBO): Promise<void> {
  return post('/systemConfig/weknora/update', data)
}

/**
 * 测试 WeKnora 连接
 */
export function testWeKnoraConnection(data: WeKnoraConfigUpdateBO): Promise<WeKnoraConnectionTestResult> {
  return post('/systemConfig/weknora/test', data)
}
