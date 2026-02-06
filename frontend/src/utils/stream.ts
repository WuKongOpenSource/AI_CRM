import { getToken } from './request'

/**
 * Fetch with Server-Sent Events (SSE) for streaming AI responses
 */
export async function fetchSSE(
  url: string,
  body: any,
  onChunk: (text: string) => void,
  onError?: (error: Error) => void,
  onComplete?: () => void
): Promise<void> {
  const token = getToken()

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Manager-Token': token } : {})
      },
      body: JSON.stringify(body)
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('Response body is not readable')
    }

    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()

      if (done) {
        onComplete?.()
        break
      }

      const text = decoder.decode(value, { stream: true })
      onChunk(text)
    }
  } catch (error) {
    const err = error instanceof Error ? error : new Error(String(error))
    onError?.(err)
    throw err
  }
}

/**
 * Parse SSE data format
 * SSE format: data: {json}\n\n
 */
export function parseSSEData(rawData: string): string[] {
  const lines = rawData.split('\n')
  const results: string[] = []

  for (const line of lines) {
    if (line.startsWith('data: ')) {
      const data = line.slice(6).trim()
      if (data && data !== '[DONE]') {
        results.push(data)
      }
    }
  }

  return results
}

/**
 * Create an EventSource-like interface for SSE
 */
export class SSEClient {
  private abortController: AbortController | null = null

  async connect(
    url: string,
    body: any,
    handlers: {
      onMessage?: (data: string) => void
      onError?: (error: Error) => void
      onClose?: () => void
    }
  ): Promise<void> {
    this.abortController = new AbortController()
    const token = getToken()

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
          ...(token ? { 'Manager-Token': token } : {})
        },
        body: JSON.stringify(body),
        signal: this.abortController.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('Response body is not readable')
      }

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()

        if (done) {
          handlers.onClose?.()
          break
        }

        buffer += decoder.decode(value, { stream: true })

        // Process complete lines
        const lines = buffer.split('\n')
        buffer = lines.pop() || '' // Keep incomplete line in buffer

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            if (data && data !== '[DONE]') {
              handlers.onMessage?.(data)
            }
          }
        }
      }
    } catch (error) {
      if ((error as Error).name !== 'AbortError') {
        handlers.onError?.(error as Error)
      }
    }
  }

  close(): void {
    this.abortController?.abort()
    this.abortController = null
  }
}
