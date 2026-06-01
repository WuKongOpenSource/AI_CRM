type LegacyGetUserMedia = (
  constraints: MediaStreamConstraints,
  onSuccess: (stream: MediaStream) => void,
  onError: (error: DOMException) => void
) => void

type LegacyMediaNavigator = Navigator & {
  getUserMedia?: LegacyGetUserMedia
  webkitGetUserMedia?: LegacyGetUserMedia
  mozGetUserMedia?: LegacyGetUserMedia
  msGetUserMedia?: LegacyGetUserMedia
}

function getLegacyGetUserMedia(): LegacyGetUserMedia | undefined {
  if (typeof navigator === 'undefined') return undefined
  const nav = navigator as LegacyMediaNavigator
  return nav.getUserMedia
    || nav.webkitGetUserMedia
    || nav.mozGetUserMedia
    || nav.msGetUserMedia
}

export function hasMobileAudioInputSupport(): boolean {
  if (typeof navigator === 'undefined') return false
  return Boolean(navigator.mediaDevices?.getUserMedia || getLegacyGetUserMedia())
}

export async function requestMobileAudioStream(
  constraints: MediaStreamConstraints = { audio: true }
): Promise<MediaStream> {
  if (navigator.mediaDevices?.getUserMedia) {
    return navigator.mediaDevices.getUserMedia(constraints)
  }

  const legacyGetUserMedia = getLegacyGetUserMedia()
  if (!legacyGetUserMedia) {
    throw new DOMException('Audio input is not supported', 'NotSupportedError')
  }

  return new Promise((resolve, reject) => {
    legacyGetUserMedia.call(navigator, constraints, resolve, reject)
  })
}

export function canCaptureMobileAudioFile(): boolean {
  return typeof document !== 'undefined' && typeof File !== 'undefined'
}

export function captureMobileAudioFile(): Promise<File | null> {
  if (!canCaptureMobileAudioFile()) {
    return Promise.resolve(null)
  }

  return new Promise((resolve) => {
    const input = document.createElement('input')
    let settled = false
    let focusTimer: number | undefined

    const cleanup = () => {
      if (focusTimer) {
        window.clearTimeout(focusTimer)
      }
      input.removeEventListener('change', handleChange)
      window.removeEventListener('focus', handleWindowFocus)
      input.remove()
    }

    const settle = (file: File | null) => {
      if (settled) return
      settled = true
      cleanup()
      resolve(file)
    }

    const handleChange = () => {
      settle(input.files?.[0] ?? null)
    }

    const handleWindowFocus = () => {
      if (focusTimer) {
        window.clearTimeout(focusTimer)
      }
      focusTimer = window.setTimeout(() => {
        if (!input.files || input.files.length === 0) {
          settle(null)
        }
      }, 800)
    }

    input.type = 'file'
    input.accept = 'audio/*'
    input.setAttribute('capture', 'microphone')
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    input.style.top = '0'
    input.style.opacity = '0'
    input.addEventListener('change', handleChange)
    window.addEventListener('focus', handleWindowFocus)
    document.body.appendChild(input)
    input.click()
  })
}
