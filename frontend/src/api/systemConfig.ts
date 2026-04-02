import { get, post } from '@/utils/request'
import type {
  AiConfig,
  AiProviderActivateBO,
  AiConfigUpdateBO,
  AiConnectionTestResult,
  EnterpriseConfig,
  EnterpriseConfigUpdateBO,
  MinioConsoleConfig
} from '@/types/systemConfig'

export function getAiConfig(): Promise<AiConfig> {
  return get('/systemConfig/ai')
}

export function getAiConfigDetail(): Promise<AiConfig> {
  return get('/systemConfig/ai/detail')
}

export function updateAiConfig(data: AiConfigUpdateBO): Promise<void> {
  return post('/systemConfig/ai/update', data)
}

export function activateAiProvider(data: AiProviderActivateBO): Promise<void> {
  return post('/systemConfig/ai/activate', data)
}

export function useGiftAiConfig(): Promise<void> {
  return post('/systemConfig/ai/useGift')
}

export function useCustomAiConfig(): Promise<void> {
  return post('/systemConfig/ai/useCustom')
}

export function testAiConnection(data: AiConfigUpdateBO): Promise<AiConnectionTestResult> {
  return post('/systemConfig/ai/test', data)
}

export function getConfigsByType(type: string): Promise<Record<string, string>> {
  return get(`/systemConfig/byType/${type}`)
}

export function clearConfigCache(): Promise<void> {
  return post('/systemConfig/clearCache')
}

export function getMinioConsoleUrl(): Promise<MinioConsoleConfig> {
  return get('/systemConfig/minio/consoleUrl')
}

export function getMinioSsoUrl(): Promise<{ enabled: boolean; ssoUrl: string | null }> {
  return get('/systemConfig/minio/ssoUrl')
}

export function getEnterpriseConfig(): Promise<EnterpriseConfig> {
  return get('/systemConfig/enterprise')
}

export function updateEnterpriseConfig(data: EnterpriseConfigUpdateBO): Promise<void> {
  return post('/systemConfig/enterprise/update', data)
}
