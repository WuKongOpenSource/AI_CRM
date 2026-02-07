import { post, get, getToken } from '@/utils/request'
import type { ChatSession, ChatMessage } from '@/types/common'

/**
 * Create chat session
 */
export function createSession(data: {
  title?: string
  agentId?: string
  customerId?: string
}): Promise<string> {
  return post('/chat/session/create', data)
}

/**
 * Get session list
 */
export function getSessionList(): Promise<ChatSession[]> {
  return get('/chat/session/list')
}

/**
 * Delete session
 */
export function deleteSession(id: string): Promise<void> {
  return post(`/chat/session/delete/${id}`)
}

/**
 * Get message list
 */
export function getMessageList(sessionId: string): Promise<ChatMessage[]> {
  return get(`/chat/message/list/${sessionId}`)
}

/**
 * Send message (streaming)
 * Handles SSE (Server-Sent Events) format parsing
 */
export async function sendMessageStream(
  sessionId: string,
  content: string,
  onChunk: (text: string) => void,
  onComplete?: () => void,
  onError?: (error: Error) => void
): Promise<void> {
  const token = getToken()
  const abortController = new AbortController()
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
  let idleTimeoutId: ReturnType<typeof setTimeout> | null = null
  let isCompleted = false
  const IDLE_TIMEOUT_MS = 3000 // 3 seconds idle = stream ended

  // Cleanup function
  const cleanup = () => {
    if (idleTimeoutId) {
      clearTimeout(idleTimeoutId)
      idleTimeoutId = null
    }
    if (reader) {
      try {
        reader.releaseLock()
      } catch {
        // Ignore release errors
      }
      reader = null
    }
  }

  // Reset idle timeout when data is received
  const resetIdleTimeout = () => {
    if (idleTimeoutId) {
      clearTimeout(idleTimeoutId)
    }
    idleTimeoutId = setTimeout(() => {
      // No data for IDLE_TIMEOUT_MS, consider stream complete
      if (!isCompleted) {
        isCompleted = true
        abortController.abort()
      }
    }, IDLE_TIMEOUT_MS)
  }

  try {
    const response = await fetch('/crmapi/chat/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Manager-Token': token } : {})
      },
      body: JSON.stringify({ sessionId, content }),
      signal: abortController.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    reader = response.body?.getReader() ?? null
    if (!reader) {
      throw new Error('Response body is not readable')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    // Start idle timeout
    resetIdleTimeout()

    while (true) {
      let readResult: ReadableStreamReadResult<Uint8Array>

      try {
        readResult = await reader.read()
      } catch (e) {
        // If aborted due to idle timeout, treat as complete
        if (abortController.signal.aborted) {
          break
        }
        throw e
      }

      const { done, value } = readResult

      if (done) {
        // Process any remaining buffer content
        if (buffer.trim()) {
          const parsedContent = parseSSEEvent(buffer)
          if (parsedContent) {
            onChunk(parsedContent)
          }
        }
        break
      }

      // Reset idle timeout since we received data
      resetIdleTimeout()

      // Decode the chunk and add to buffer
      buffer += decoder.decode(value, { stream: true })

      // Process complete SSE events from buffer (events are separated by \n\n)
      const events = buffer.split('\n\n')
      // Keep the last potentially incomplete event in buffer
      buffer = events.pop() || ''

      for (const event of events) {
        const parsedContent = parseSSEEvent(event)
        if (parsedContent) {
          onChunk(parsedContent)
        }
      }
    }

    // Stream completed successfully
    isCompleted = true
    onComplete?.()
  } catch (error) {
    // Don't report error if it was caused by our abort (idle timeout)
    if (abortController.signal.aborted && isCompleted) {
      onComplete?.()
      return
    }
    onError?.(error as Error)
    throw error
  } finally {
    cleanup()
  }
}

/**
 * Parse a complete SSE event
 * An event may contain multiple data: lines (for multiline content)
 * These should be joined with newlines according to SSE spec
 */
function parseSSEEvent(event: string): string | null {
  const lines = event.split('\n')
  const dataLines: string[] = []

  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('data:')) {
      const content = trimmed.slice(5)
      // Handle "data: " (with space) vs "data:" (without space)
      dataLines.push(content.startsWith(' ') ? content.slice(1) : content)
    }
  }

  if (dataLines.length === 0) return null
  // Join multiple data lines with newline (SSE spec for multiline data)
  return dataLines.join('\n')
}

/**
 * Send message (sync)
 */
export function sendMessageSync(sessionId: string, content: string): Promise<string> {
  return post('/chat/sendSync', { sessionId, content })
}
