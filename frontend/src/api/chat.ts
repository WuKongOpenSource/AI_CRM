import { post, get, getToken, getApiBaseUrl } from '@/utils/request'
import type { ChatSession, ChatMessage, ChatAttachmentDTO, ChatModelOption, ChatAppOption } from '@/types/common'
import {
  createQuotaExhaustedError,
  parseChatSSEEvent,
  parseHttpQuotaErrorPayload,
  parseMaybeJson,
  parseQuotaPayloadFromSSE
} from '@/utils/chatStream'

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
  candidateId?: string
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

/**
 * Delete session
 */
export function deleteSession(id: string): Promise<void> {
  return post(`/chat/session/delete/${id}`)
}

/**
 * Pin or unpin session
 */
export function setSessionPinned(id: string, pinned: boolean): Promise<void> {
  return post(`/chat/session/pin/${id}`, { pinned })
}

/**
 * Get message list
 */
export function getMessageList(sessionId: string): Promise<ChatMessage[]> {
  return get(`/chat/message/list/${sessionId}`)
}

export function getChatModelOptions(): Promise<ChatModelOption[]> {
  return get('/chat/model/options')
}

export function getChatAppOptions(): Promise<ChatAppOption[]> {
  return get('/chat/app/options')
}

/**
 * Send message (streaming)
 * Handles SSE (Server-Sent Events) format parsing
 */
export async function sendMessageStream(
  sessionId: string,
  content: string,
  onChunk: (text: string) => void,
  onComplete?: () => void | Promise<void>,
  onError?: (error: Error) => void,
  attachments?: ChatAttachmentDTO[],
  appCode?: string,
  ragEnabled?: boolean,
  modelProvider?: string,
  modelName?: string,
  modelSource?: string,
  knowledgeIds?: string[],
  projectId?: string,
  projectTaskId?: string,
  productId?: string,
  candidateId?: string,
  signal?: AbortSignal
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
      signal,
      body: JSON.stringify({
        sessionId,
        content,
        attachments: attachments || undefined,
        appCode: appCode || undefined,
        ragEnabled,
        modelProvider: modelProvider || undefined,
        modelName: modelName || undefined,
        modelSource: modelSource || undefined,
        productId: productId || undefined,
        candidateId: candidateId || undefined,
        projectId: projectId || undefined,
        projectTaskId: projectTaskId || undefined,
        knowledgeIds:
          knowledgeIds?.length && knowledgeIds.length > 0
            ? knowledgeIds
            : undefined
      })
    })

    if (!response.ok) {
      throw await resolveStreamHttpError(response)
    }

    reader = response.body?.getReader() ?? null
    if (!reader) {
      throw new Error('响应内容不可读取')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const readResult = await reader.read()
      const { done, value } = readResult

      if (done) {
        // Process any remaining buffer content
        if (buffer.trim()) {
          handleParsedSSEEvent(buffer, onChunk)
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
        handleParsedSSEEvent(event, onChunk)
      }
    }

    // Stream completed successfully
    await onComplete?.()
  } catch (error) {
    const err = error instanceof Error ? error : new Error(String(error))
    if (err.name === 'AbortError') {
      return
    }
    onError?.(err)
    throw err
  } finally {
    cleanup()
  }
}

function handleParsedSSEEvent(event: string, onChunk: (text: string) => void) {
  const parsedEvent = parseChatSSEEvent(event)
  if (!parsedEvent) return

  const quotaPayload = parseQuotaPayloadFromSSE(parsedEvent)
  if (quotaPayload) {
    throw createQuotaExhaustedError(quotaPayload)
  }

  onChunk(parsedEvent.data)
}

async function resolveStreamHttpError(response: Response): Promise<Error> {
  const responseText = await response.text().catch(() => '')
  const parsedBody = parseMaybeJson(responseText)
  const quotaPayload = parseHttpQuotaErrorPayload(parsedBody ?? responseText)
  if (quotaPayload) {
    return createQuotaExhaustedError(quotaPayload)
  }

  return new Error(resolveHttpErrorMessage(response.status, parsedBody, responseText))
}

function resolveHttpErrorMessage(status: number, parsedBody: unknown, responseText: string): string {
  if (parsedBody && typeof parsedBody === 'object') {
    const record = parsedBody as Record<string, unknown>
    const error = record.error
    if (error && typeof error === 'object' && !Array.isArray(error)) {
      const errorRecord = error as Record<string, unknown>
      if (typeof errorRecord.message === 'string' && errorRecord.message) return errorRecord.message
    }
    if (typeof record.message === 'string' && record.message) return record.message
    if (typeof record.msg === 'string' && record.msg) return record.msg
  }
  if (responseText.trim()) return responseText.trim()
  return `请求失败，HTTP 状态码：${status}`
}

/**
 * Send message (sync)
 */
export function sendMessageSync(
  sessionId: string,
  content: string,
  attachments?: ChatAttachmentDTO[],
  appCode?: string,
  ragEnabled?: boolean,
  modelProvider?: string,
  modelName?: string,
  modelSource?: string,
  knowledgeIds?: string[],
  projectId?: string,
  projectTaskId?: string,
  productId?: string,
  candidateId?: string
): Promise<string> {
  return post('/chat/sendSync', {
    sessionId,
    content,
    attachments: attachments || undefined,
    appCode: appCode || undefined,
    ragEnabled,
    modelProvider: modelProvider || undefined,
    modelName: modelName || undefined,
    modelSource: modelSource || undefined,
    productId: productId || undefined,
    candidateId: candidateId || undefined,
    projectId: projectId || undefined,
    projectTaskId: projectTaskId || undefined,
    knowledgeIds:
      knowledgeIds?.length && knowledgeIds.length > 0
        ? knowledgeIds
        : undefined
  })
}
