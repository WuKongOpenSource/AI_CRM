import { post, get, getToken, getApiBaseUrl } from '@/utils/request'
import type { ChatSession, ChatMessage, ChatAttachmentDTO, ChatAppOption } from '@/types/common'

/**
 * Create chat session
 */
export function createSession(data: {
  title?: string
  agentId?: string
  customerId?: string
  employeeId?: string
  relationId?: string
  productId?: string
  projectId?: string
  projectTaskId?: string
  appCode?: string
}): Promise<string> {
  return post('/chat/session/create', data)
}

/**
 * Get session list
 */
export function getSessionList(): Promise<ChatSession[]> {
  return get('/chat/session/list')
}

export function getChatApplications(): Promise<ChatAppOption[]> {
  return get('/chat/applications')
}

/**
 * Delete session
 */
export function deleteSession(id: string): Promise<void> {
  return post(`/chat/session/delete/${id}`)
}

export function updateSessionPin(id: string, pinned: boolean): Promise<void> {
  return post(`/chat/session/pin/${id}`, { pinned })
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
  onError?: (error: Error) => void,
  attachments?: ChatAttachmentDTO[],
  ragEnabled?: boolean,
  appCode?: string
): Promise<void> {
  const token = getToken()
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null

  // Cleanup function
  const cleanup = () => {
    if (reader) {
      try {
        reader.releaseLock()
      } catch {
        // Ignore release errors
      }
      reader = null
    }
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}/chat/send`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        ...(token ? { 'Manager-Token': token } : {})
      },
      body: JSON.stringify({ sessionId, content, attachments: attachments || undefined, ragEnabled, appCode })
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

    while (true) {
      const readResult = await reader.read()
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
    onComplete?.()
  } catch (error) {
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
  const lines = event.split(/\r?\n/)
  const dataLines: string[] = []

  for (const line of lines) {
    if (line.startsWith('data:')) {
      const content = line.slice(5)
      // Per SSE spec, strip at most one leading space after "data:"
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
export function sendMessageSync(
  sessionId: string,
  content: string,
  attachments?: ChatAttachmentDTO[],
  ragEnabled?: boolean,
  appCode?: string
): Promise<string> {
  return post('/chat/sendSync', { sessionId, content, attachments: attachments || undefined, ragEnabled, appCode })
}
