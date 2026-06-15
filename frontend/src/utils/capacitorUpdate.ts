import { ElMessageBox } from 'element-plus'
import { h } from 'vue'
import {
  getNativeCapacitor,
  getNativePlatform,
  isNativePlatform,
  isNativePluginAvailable,
  registerNativePlugin
} from '@/utils/nativeMobileRuntime'
import {
  HOT_UPDATE_STATUS,
  type AppUpdateInfo,
  type SupportedPlatform,
  getPlatformUpdateInfo,
  shouldApplyUpdate
} from '@/utils/capacitorUpdateCore'

export {
  DOWNLOAD_UPDATE_STATUS,
  HOT_UPDATE_STATUS,
  compareVersion,
  getPlatformUpdateInfo,
  normalizeUpdateInfo,
  shouldApplyUpdate
} from '@/utils/capacitorUpdateCore'
export type { AppUpdateInfo, SupportedPlatform } from '@/utils/capacitorUpdateCore'

export const DEFAULT_UPDATE_CHECK_DELAY_MS = 2000
export const DEFAULT_UPDATE_CHECK_RETRY_DELAY_MS = 5000
export const DEFAULT_UPDATE_CHECK_MAX_ATTEMPTS = 2
export const DEFAULT_LIVE_UPDATE_HEALTH_DELAY_MS = 3000
export const DEFAULT_CURRENT_VERSION = '1.0.0'
export const DEFAULT_UPDATE_VERSION_URL = 'https://www.72crm.ai/version.json'

type Fetcher = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>

type BrowserPlugin = {
  open(options: { url: string }): Promise<void>
}

type DownloadBundleOptions = {
  bundleId: string
  url: string
  artifactType?: string
  checksum?: string
  signature?: string
}

type LiveUpdateModule = {
  LiveUpdate: {
    ready(): Promise<void>
    downloadBundle(options: DownloadBundleOptions): Promise<void>
    setNextBundle(options: { bundleId: string }): Promise<void>
    reload(): Promise<void>
  }
}

export type CheckForUpdatesOptions = {
  manifestUrl?: string
  fetcher?: Fetcher
}

export type CheckForUpdatesResult = 'updated' | 'none' | 'unsupported' | 'failed'

export type ScheduleCapacitorUpdateCheckOptions = {
  delayMs?: number
  retryDelayMs?: number
  maxAttempts?: number
}

let browserPlugin: BrowserPlugin | null = null
let isDownloadUpdateDialogVisible = false
let liveUpdateReadyPromise: Promise<void> | null = null

function getBrowserPlugin(): BrowserPlugin {
  if (!browserPlugin) {
    browserPlugin = registerNativePlugin<BrowserPlugin>('Browser')
    if (!browserPlugin) {
      throw new Error('Capacitor Browser plugin is unavailable')
    }
  }
  return browserPlugin
}

function isSupportedPlatform(platform: string): platform is SupportedPlatform {
  return platform === 'android' || platform === 'ios'
}

function getUpdateManifestUrl(): string {
  const raw = import.meta.env.VITE_UPDATE_VERSION_URL
  const configuredUrl = typeof raw === 'string' ? raw.trim() : ''
  return configuredUrl || DEFAULT_UPDATE_VERSION_URL
}

function getConfiguredAppVersion(): string {
  const raw = import.meta.env.VITE_APP_VERSION
  const configuredVersion = typeof raw === 'string' ? raw.trim() : ''
  return configuredVersion || DEFAULT_CURRENT_VERSION
}

function loadLiveUpdateModule(): Promise<LiveUpdateModule> {
  const modulePath = './capacitorLiveUpdateNative'
  return import(/* @vite-ignore */ modulePath) as Promise<LiveUpdateModule>
}

async function sendLiveUpdateReadySignal(): Promise<void> {
  if (!liveUpdateReadyPromise) {
    liveUpdateReadyPromise = loadLiveUpdateModule()
      .then(async ({ LiveUpdate }) => {
        await LiveUpdate.ready()
      })
      .catch((error) => {
        liveUpdateReadyPromise = null
        throw error
      })
  }

  await liveUpdateReadyPromise
}

export async function getCurrentVersion(): Promise<string> {
  return getConfiguredAppVersion()
}

export async function markLiveUpdateReady(): Promise<void> {
  if (!isNativePlatform()) return

  try {
    await sendLiveUpdateReadySignal()
  } catch (error) {
    console.warn('Live update ready signal failed:', error)
  }
}

export async function reportLiveUpdateHealth(): Promise<void> {
  if (!isNativePlatform()) return

  try {
    await sendLiveUpdateReadySignal()
    console.log('[HotUpdate] Health signal sent, version confirmed')
  } catch (error) {
    console.error('[HotUpdate] Failed to report health:', error)
  }
}

export function scheduleLiveUpdateHealthReport(delayMs = DEFAULT_LIVE_UPDATE_HEALTH_DELAY_MS): void {
  if (!isNativePlatform() || typeof window === 'undefined') return

  window.setTimeout(() => {
    void reportLiveUpdateHealth()
  }, delayMs)
}

export async function performHotUpdate(updateInfo: AppUpdateInfo): Promise<void> {
  const { LiveUpdate } = await loadLiveUpdateModule()
  const bundleId = updateInfo.bundleId || updateInfo.version
  const downloadOptions: DownloadBundleOptions = {
    bundleId,
    url: updateInfo.url
  }

  if (updateInfo.artifactType) {
    downloadOptions.artifactType = updateInfo.artifactType
  }
  if (updateInfo.checksum) {
    downloadOptions.checksum = updateInfo.checksum
  }
  if (updateInfo.signature) {
    downloadOptions.signature = updateInfo.signature
  }

  await LiveUpdate.downloadBundle(downloadOptions)
  await LiveUpdate.setNextBundle({ bundleId })
  await LiveUpdate.reload()
}

export async function performDownloadUpdate(updateUrl: string): Promise<void> {
  if (isNativePluginAvailable('Browser')) {
    await getBrowserPlugin().open({ url: updateUrl })
    return
  }

  if (typeof window !== 'undefined') {
    window.location.assign(updateUrl)
  }
}

export async function showDownloadUpdateDialog(
  updateInfo: AppUpdateInfo,
  currentVersion: string
): Promise<void> {
  if (isDownloadUpdateDialogVisible) return

  isDownloadUpdateDialogVisible = true

  const note = updateInfo.note || 'No update notes'
  const noteLines = note.split(/\r?\n/).filter((line) => line.trim())

  try {
    await ElMessageBox.confirm(
      h('div', { class: 'space-y-3 text-left' }, [
        h('p', { class: 'text-sm text-slate-600' }, `New version ${updateInfo.version} is available. Current version: ${currentVersion}`),
        h('div', { class: 'rounded bg-slate-50 p-3 text-sm leading-6 text-slate-700' }, [
          h('div', { class: 'mb-1 font-medium text-slate-900 w-full' }, 'Update notes'),
          noteLines.length
            ? h(
                'div',
                { class: 'space-y-1 whitespace-pre-wrap break-words' },
                noteLines.map((line) => h('p', line))
              )
            : h('p', 'No update notes')
        ])
      ]),
      'New Version',
      {
        confirmButtonText: 'Update',
        cancelButtonText: 'Later',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        distinguishCancelAndClose: true,
        type: 'info'
      }
    )
    await performDownloadUpdate(updateInfo.url)
  } catch {
    // User chose to update later.
  } finally {
    isDownloadUpdateDialogVisible = false
  }
}

export async function checkForUpdates(options: CheckForUpdatesOptions = {}): Promise<CheckForUpdatesResult> {
  if (!getNativeCapacitor()) return 'unsupported'

  const manifestUrl = options.manifestUrl || getUpdateManifestUrl()
  if (!manifestUrl) return 'unsupported'

  try {
    const currentVersion = await getCurrentVersion()
    const fetcher = options.fetcher || fetch
    const response = await fetcher(manifestUrl, { cache: 'no-store' })

    if (!response.ok) {
      throw new Error(`Version check request failed: ${response.status}`)
    }

    const platform = getNativePlatform()
    if (!isSupportedPlatform(platform)) return 'unsupported'

    const payload = await response.json()
    const updateInfo = getPlatformUpdateInfo(payload, platform)

    if (!shouldApplyUpdate(updateInfo, currentVersion) || !updateInfo) return 'none'

    if (updateInfo.status === HOT_UPDATE_STATUS) {
      await performHotUpdate(updateInfo)
      return 'updated'
    }

    await showDownloadUpdateDialog(updateInfo, currentVersion)
    return 'updated'
  } catch (error) {
    console.error('Update check failed:', error)
    return 'failed'
  }
}

export function scheduleCapacitorUpdateCheck(
  options: number | ScheduleCapacitorUpdateCheckOptions = DEFAULT_UPDATE_CHECK_DELAY_MS
): void {
  if (!isNativePlatform() || typeof window === 'undefined') return

  const delayMs = typeof options === 'number'
    ? options
    : options.delayMs ?? DEFAULT_UPDATE_CHECK_DELAY_MS
  const retryDelayMs = typeof options === 'number'
    ? DEFAULT_UPDATE_CHECK_RETRY_DELAY_MS
    : options.retryDelayMs ?? DEFAULT_UPDATE_CHECK_RETRY_DELAY_MS
  const configuredMaxAttempts = typeof options === 'number'
    ? DEFAULT_UPDATE_CHECK_MAX_ATTEMPTS
    : options.maxAttempts ?? DEFAULT_UPDATE_CHECK_MAX_ATTEMPTS
  const maxAttempts = Math.max(
    1,
    Math.floor(configuredMaxAttempts)
  )

  const runCheck = async (attempt: number): Promise<void> => {
    await markLiveUpdateReady()
    const result = await checkForUpdates()

    if (result === 'failed' && attempt < maxAttempts) {
      window.setTimeout(() => {
        void runCheck(attempt + 1)
      }, retryDelayMs)
    }
  }

  window.setTimeout(() => {
    void runCheck(1)
  }, delayMs)
}
