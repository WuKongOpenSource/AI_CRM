export type MobileKeyboardViewportMetrics = {
  layoutHeight: number
  visualHeight: number
  offsetTop?: number
}

type MobileKeyboardInsetOptions = {
  gap?: number
  threshold?: number
}

export type ScrollRestoreMetrics = {
  scrollTop: number
  scrollHeight: number
  clientHeight: number
}

const DEFAULT_GAP_PX = 8
const DEFAULT_THRESHOLD_PX = 24

function toFiniteNumber(value: number): number {
  return Number.isFinite(value) ? value : 0
}

export function resolveMobileKeyboardInset(
  metrics: MobileKeyboardViewportMetrics,
  options: MobileKeyboardInsetOptions = {}
): number {
  const layoutHeight = toFiniteNumber(metrics.layoutHeight)
  const visualHeight = toFiniteNumber(metrics.visualHeight)
  const gap = options.gap ?? DEFAULT_GAP_PX
  const threshold = options.threshold ?? DEFAULT_THRESHOLD_PX
  const keyboardHeight = Math.max(0, layoutHeight - visualHeight)

  return keyboardHeight > threshold ? Math.round(keyboardHeight + gap) : 0
}

export function resolveMobileViewportTopOffset(metrics: MobileKeyboardViewportMetrics): number {
  return Math.max(0, Math.round(toFiniteNumber(metrics.offsetTop ?? 0)))
}

export function resolveRestoredScrollTop(metrics: ScrollRestoreMetrics): number {
  const maxScrollTop = Math.max(0, toFiniteNumber(metrics.scrollHeight) - toFiniteNumber(metrics.clientHeight))
  return Math.min(maxScrollTop, Math.max(0, Math.round(toFiniteNumber(metrics.scrollTop))))
}
