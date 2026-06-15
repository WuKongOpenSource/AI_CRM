export type NativeCapacitorRuntime = {
  isNativePlatform?: () => boolean
  isPluginAvailable?: (pluginName: string) => boolean
  getPlatform?: () => string
  registerPlugin?: <T>(pluginName: string) => T
  platform?: string
  isNative?: boolean
}

declare global {
  interface Window {
    Capacitor?: NativeCapacitorRuntime
  }
}

export function getNativeCapacitor(): NativeCapacitorRuntime | null {
  if (typeof window === 'undefined') return null

  const capacitor = window.Capacitor
  if (!capacitor) return null

  if (typeof capacitor.isNativePlatform === 'function') {
    return capacitor.isNativePlatform() ? capacitor : null
  }

  if (typeof capacitor.getPlatform === 'function') {
    return capacitor.getPlatform() !== 'web' ? capacitor : null
  }

  if (typeof capacitor.platform === 'string') {
    return capacitor.platform !== 'web' ? capacitor : null
  }

  return capacitor.isNative === true ? capacitor : null
}

export function isMobileViewport(): boolean {
  if (typeof window === 'undefined') return false

  return (
    window.innerWidth < 768 ||
    (typeof window.matchMedia === 'function' && window.matchMedia('(pointer: coarse)').matches)
  )
}

export function isNativePlatform(): boolean {
  return Boolean(getNativeCapacitor())
}

export function isNativeMobileRuntime(): boolean {
  return isMobileViewport() && isNativePlatform()
}

export function isNativePluginAvailable(pluginName: string): boolean {
  const capacitor = getNativeCapacitor()
  if (!capacitor || typeof capacitor.isPluginAvailable !== 'function') return false

  return capacitor.isPluginAvailable(pluginName)
}

export function getNativePlatform(): string {
  const capacitor = getNativeCapacitor()
  if (!capacitor) return 'web'

  if (typeof capacitor.getPlatform === 'function') {
    return capacitor.getPlatform()
  }

  return capacitor.platform || 'web'
}

export function registerNativePlugin<T>(pluginName: string): T | null {
  const capacitor = getNativeCapacitor()
  if (!capacitor || typeof capacitor.registerPlugin !== 'function') return null

  return capacitor.registerPlugin<T>(pluginName)
}
