import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAiConfig, getAiConfigDetail, updateAiConfig } from '@/api/systemConfig'
import type { AiConfig, AiConfigUpdateBO, AiMode, AiProvider, AiProviderPreset } from '@/types/systemConfig'
import { useUserStore } from '@/stores/user'

const DEFAULT_CHAT_AI_CONFIG: AiConfigUpdateBO = {
  provider: 'dashscope',
  apiUrl: 'https://dashscope.aliyuncs.com/compatible-mode',
  apiKey: '',
  model: 'qwen3.6-plus',
  temperature: 0.7,
  maxTokens: 4096,
}

const aiConfig = ref<AiConfig | null>(null)
const aiConfigLoaded = ref(false)
const isApiKeyModalOpen = ref(false)
const apiKeySetupInitialConfig = ref<Partial<AiConfigUpdateBO> | null>(null)
const apiKeySetupProviderOptions = ref<AiProviderPreset[]>([])
const savingApiKey = ref(false)
const resumeSendAfterApiKeySave = ref(false)
const isTokenPurchaseDialogOpen = ref(false)
const resumeSendAfterTokenPurchase = ref(false)

let resumeSendHandler: (() => Promise<void>) | null = null

export function registerAiQuotaResumeSendHandler(handler: () => Promise<void>) {
  resumeSendHandler = handler
}

export function unregisterAiQuotaResumeSendHandler() {
  resumeSendHandler = null
}

function normalizeAiConfig(config?: Partial<AiConfig> | Partial<AiConfigUpdateBO> | null): AiConfig {
  return {
    provider: config?.provider || DEFAULT_CHAT_AI_CONFIG.provider || 'dashscope',
    apiUrl: config?.apiUrl || DEFAULT_CHAT_AI_CONFIG.apiUrl,
    apiKey: config?.apiKey || '',
    model: config?.model || DEFAULT_CHAT_AI_CONFIG.model,
    temperature: config?.temperature ?? DEFAULT_CHAT_AI_CONFIG.temperature ?? 0.7,
    maxTokens: config?.maxTokens ?? DEFAULT_CHAT_AI_CONFIG.maxTokens ?? 4096,
    extraHeadersConfigured: (config as Partial<AiConfig> | null)?.extraHeadersConfigured ?? false,
    extraHeadersJson: (config as Partial<AiConfig> | null)?.extraHeadersJson ?? '',
    capabilities: (config as Partial<AiConfig> | null)?.capabilities,
    modelHint: (config as Partial<AiConfig> | null)?.modelHint,
    extraHeadersHint: (config as Partial<AiConfig> | null)?.extraHeadersHint,
    availableProviders: (config as Partial<AiConfig> | null)?.availableProviders,
    mode: (config as Partial<AiConfig> | null)?.mode || 'gift',
    customConfigSaved: (config as Partial<AiConfig> | null)?.customConfigSaved ?? false,
    ready: (config as Partial<AiConfig> | null)?.ready ?? Boolean(config?.apiKey?.trim()),
    giftCreditTotal: (config as Partial<AiConfig> | null)?.giftCreditTotal ?? 0,
    giftCreditUsed: (config as Partial<AiConfig> | null)?.giftCreditUsed ?? 0,
    giftCreditRemaining: (config as Partial<AiConfig> | null)?.giftCreditRemaining ?? 0,
    giftCreditAvailable: (config as Partial<AiConfig> | null)?.giftCreditAvailable
      ?? (((config as Partial<AiConfig> | null)?.giftCreditRemaining ?? 0) > 0),
    purchasedCreditTotal: (config as Partial<AiConfig> | null)?.purchasedCreditTotal ?? 0,
    purchasedCreditUsed: (config as Partial<AiConfig> | null)?.purchasedCreditUsed ?? 0,
    purchasedCreditRemaining: (config as Partial<AiConfig> | null)?.purchasedCreditRemaining ?? 0,
    creditTotal: (config as Partial<AiConfig> | null)?.creditTotal
      ?? ((config as Partial<AiConfig> | null)?.giftCreditTotal ?? 0),
    creditUsed: (config as Partial<AiConfig> | null)?.creditUsed
      ?? ((config as Partial<AiConfig> | null)?.giftCreditUsed ?? 0),
    creditRemaining: (config as Partial<AiConfig> | null)?.creditRemaining
      ?? ((config as Partial<AiConfig> | null)?.giftCreditRemaining ?? 0),
    creditAvailable: (config as Partial<AiConfig> | null)?.creditAvailable
      ?? (((config as Partial<AiConfig> | null)?.creditRemaining
        ?? (config as Partial<AiConfig> | null)?.giftCreditRemaining
        ?? 0) > 0),
    updateTime: config && 'updateTime' in config ? config.updateTime : undefined,
  }
}

export function formatCreditAmount(value: number): string {
  const normalizedValue = Number.isFinite(value) ? Math.max(0, value) : 0
  if (normalizedValue >= 10000) {
    const wanValue = normalizedValue / 10000
    const formattedWan = Number.isInteger(wanValue) ? wanValue.toFixed(0) : wanValue.toFixed(1)
    return `${formattedWan}万`
  }
  return new Intl.NumberFormat('zh-CN').format(normalizedValue)
}

export function formatWanCredit(value: number): string {
  return formatCreditAmount(value)
}

export function useAiQuota() {
  const router = useRouter()
  const userStore = useUserStore()

  const currentAiMode = computed<AiMode>(() => aiConfig.value?.mode || 'gift')
  const aiReady = computed(() => Boolean(aiConfig.value?.ready))
  const hasAiApiKeyConfigured = computed(() => aiReady.value)
  const canManageAiConfig = computed(() => userStore.hasPermission('config:ai'))
  const creditTotal = computed(() => aiConfig.value?.creditTotal ?? aiConfig.value?.giftCreditTotal ?? 0)
  const creditRemaining = computed(() => aiConfig.value?.creditRemaining ?? aiConfig.value?.giftCreditRemaining ?? 0)
  const creditUsed = computed(() => {
    const explicit = aiConfig.value?.creditUsed ?? aiConfig.value?.giftCreditUsed
    if (explicit != null && explicit >= 0) return explicit
    return Math.max(0, creditTotal.value - creditRemaining.value)
  })
  const creditProgressPercent = computed(() => {
    if (creditTotal.value <= 0) return 0
    return Math.max(0, Math.min(100, Math.round((creditRemaining.value / creditTotal.value) * 100)))
  })
  const creditRemainingWan = computed(() => formatWanCredit(creditRemaining.value))
  const creditTotalWan = computed(() => formatWanCredit(creditTotal.value))
  const creditUsedWan = computed(() => formatWanCredit(creditUsed.value))

  const giftCreditRemaining = creditRemaining
  const giftCreditProgressPercent = creditProgressPercent
  const giftCreditRemainingWan = creditRemainingWan
  const giftCreditTotalWan = creditTotalWan

  const aiStatusBadgeText = computed(() => {
    if (creditRemaining.value <= 0) {
      return '积分已用完'
    }
    if (currentAiMode.value === 'gift') {
      return giftCreditRemaining.value > 0 ? '赠送积分' : '已用完'
    }
    return aiReady.value ? '自定义模型已就绪' : '待配置'
  })

  const aiStatusBadgeClass = computed(() => {
    if (creditRemaining.value <= 0) {
      return 'bg-amber-50 text-amber-600 dark:bg-amber-400/10 dark:text-amber-200'
    }
    if (currentAiMode.value === 'gift') {
      return giftCreditRemaining.value > 0
        ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-400/10 dark:text-emerald-200'
        : 'bg-amber-50 text-amber-600 dark:bg-amber-400/10 dark:text-amber-200'
    }
    return aiReady.value
      ? 'bg-blue-50 text-blue-600 dark:bg-blue-400/10 dark:text-blue-200'
      : 'bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-300'
  })

  const giftCreditProgressClass = computed(() => {
    if (giftCreditRemaining.value <= 0) return 'bg-amber-400'
    return currentAiMode.value === 'gift' ? 'bg-primary' : 'bg-blue-500'
  })

  async function loadAiConfig(force = false): Promise<AiConfig | null> {
    if (aiConfigLoaded.value && !force) {
      return aiConfig.value
    }

    try {
      const config = await getAiConfig()
      aiConfig.value = normalizeAiConfig(config)
    } catch {
      if (!aiConfig.value) {
        aiConfig.value = normalizeAiConfig()
      }
    } finally {
      aiConfigLoaded.value = true
    }

    return aiConfig.value
  }

  function goToAiSettings() {
    void router.push('/settings/system/api')
  }

  function handleApiKeyModalVisibleChange(visible: boolean) {
    isApiKeyModalOpen.value = visible

    if (!visible && !savingApiKey.value) {
      apiKeySetupInitialConfig.value = null
      resumeSendAfterApiKeySave.value = false
    }
  }

  async function prepareApiKeySetupModal() {
    if (!canManageAiConfig.value) return

    try {
      const detailConfig = await getAiConfigDetail()
      apiKeySetupProviderOptions.value = detailConfig.availableProviders?.length
        ? detailConfig.availableProviders
        : []
      apiKeySetupInitialConfig.value = {
        provider: (detailConfig.provider || DEFAULT_CHAT_AI_CONFIG.provider) as AiProvider,
        apiUrl: detailConfig.apiUrl || DEFAULT_CHAT_AI_CONFIG.apiUrl,
        apiKey: '',
        model: detailConfig.model || DEFAULT_CHAT_AI_CONFIG.model,
        temperature: detailConfig.temperature ?? DEFAULT_CHAT_AI_CONFIG.temperature,
        maxTokens: detailConfig.maxTokens ?? DEFAULT_CHAT_AI_CONFIG.maxTokens,
        extraHeadersJson: detailConfig.extraHeadersJson ?? '',
      }
    } catch {
      apiKeySetupProviderOptions.value = []
      apiKeySetupInitialConfig.value = { ...DEFAULT_CHAT_AI_CONFIG }
    }
  }

  function resolveProviderLabel(provider?: AiProvider): string {
    return apiKeySetupProviderOptions.value.find((item) => item.value === provider)?.label || 'AI 服务商'
  }

  async function handleSaveApiKey(payload: AiConfigUpdateBO) {
    const resolvedProvider = (payload.provider || DEFAULT_CHAT_AI_CONFIG.provider) as AiProvider
    const trimmedApiKey = payload.apiKey.trim()
    const trimmedApiUrl = payload.apiUrl.trim()
    const trimmedModel = payload.model.trim()
    const canReuseSavedApiKey = Boolean(
      trimmedApiKey
      || apiKeySetupProviderOptions.value.find((item) => item.value === resolvedProvider)?.apiKeyConfigured
    )

    if (!canReuseSavedApiKey) {
      ElMessage.warning('请输入 API Key，或先保存当前服务商的 API Key')
      return
    }
    if (!trimmedApiUrl) {
      ElMessage.warning('请输入 API 地址')
      return
    }
    if (!trimmedModel) {
      ElMessage.warning('请输入模型名称')
      return
    }

    savingApiKey.value = true

    try {
      const nextPayload: AiConfigUpdateBO = {
        ...DEFAULT_CHAT_AI_CONFIG,
        ...payload,
        provider: resolvedProvider,
        apiUrl: trimmedApiUrl,
        apiKey: trimmedApiKey,
        model: trimmedModel,
        extraHeadersJson: payload.extraHeadersJson?.trim() || '',
      }

      await updateAiConfig(nextPayload)
      await loadAiConfig(true)
      isApiKeyModalOpen.value = false
      apiKeySetupInitialConfig.value = null
      ElMessage.success(`${resolveProviderLabel(nextPayload.provider)} 配置保存成功`)

      const shouldResumeSend = resumeSendAfterApiKeySave.value
      resumeSendAfterApiKeySave.value = false

      if (shouldResumeSend && resumeSendHandler) {
        await nextTick()
        await resumeSendHandler()
      }
    } catch {
      // Error handled by interceptor
    } finally {
      savingApiKey.value = false
    }
  }

  function openApiKeySetup() {
    if (!canManageAiConfig.value) {
      ElMessage.warning('当前账号没有 AI 配置权限，请联系管理员。')
      return
    }
    resumeSendAfterApiKeySave.value = false
    void prepareApiKeySetupModal().then(() => {
      isApiKeyModalOpen.value = true
    })
  }

  function openTokenPurchaseDialog() {
    resumeSendAfterTokenPurchase.value = false
    isTokenPurchaseDialogOpen.value = true
  }

  function handleTokenPurchaseDialogVisibleChange(visible: boolean) {
    isTokenPurchaseDialogOpen.value = visible
    if (!visible) {
      resumeSendAfterTokenPurchase.value = false
    }
  }

  async function handleTokenPurchasePaid() {
    await loadAiConfig(true)
    const shouldResumeSend = resumeSendAfterTokenPurchase.value
    resumeSendAfterTokenPurchase.value = false
    if (shouldResumeSend && resumeSendHandler) {
      await nextTick()
      await resumeSendHandler()
    }
  }

  async function ensureAiAvailableForSend(): Promise<boolean> {
    if (!aiConfigLoaded.value || !aiConfig.value?.ready) {
      await loadAiConfig(true)
    }

    if (aiConfig.value?.ready) {
      return true
    }

    if (creditRemaining.value <= 0) {
      resumeSendAfterTokenPurchase.value = true
      isTokenPurchaseDialogOpen.value = true
      return false
    }

    if (!canManageAiConfig.value) {
      if (creditRemaining.value <= 0) {
        ElMessage.warning('AI 积分已用完，请联系管理员购买套餐。')
      } else {
        ElMessage.warning('当前 AI 服务未就绪，请联系管理员处理。')
      }
      return false
    }

    resumeSendAfterApiKeySave.value = true
    await prepareApiKeySetupModal()
    isApiKeyModalOpen.value = true
    return false
  }

  return {
    aiConfig,
    aiConfigLoaded,
    isApiKeyModalOpen,
    apiKeySetupInitialConfig,
    apiKeySetupProviderOptions,
    savingApiKey,
    resumeSendAfterApiKeySave,
    isTokenPurchaseDialogOpen,
    resumeSendAfterTokenPurchase,
    currentAiMode,
    hasAiApiKeyConfigured,
    canManageAiConfig,
    creditTotal,
    creditRemaining,
    creditUsed,
    creditProgressPercent,
    creditRemainingWan,
    creditTotalWan,
    creditUsedWan,
    giftCreditRemaining,
    giftCreditProgressPercent,
    giftCreditRemainingWan,
    giftCreditTotalWan,
    aiStatusBadgeText,
    aiStatusBadgeClass,
    giftCreditProgressClass,
    loadAiConfig,
    goToAiSettings,
    handleApiKeyModalVisibleChange,
    handleSaveApiKey,
    openApiKeySetup,
    openTokenPurchaseDialog,
    handleTokenPurchaseDialogVisibleChange,
    handleTokenPurchasePaid,
    prepareApiKeySetupModal,
    ensureAiAvailableForSend,
  }
}
