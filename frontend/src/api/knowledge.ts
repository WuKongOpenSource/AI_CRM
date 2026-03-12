import { post, get, upload, download, getToken, getApiBaseUrl } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { Knowledge, KnowledgeQueryBO, KnowledgeAiAnalyzeVO } from '@/types/common'

/**
 * Upload file to knowledge base
 */
export function uploadKnowledge(
  file: File,
  type?: string,
  customerId?: string,
  summary?: string
): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  if (type) formData.append('type', type)
  if (customerId) formData.append('customerId', customerId)
  if (summary) formData.append('summary', summary)
  return upload('/knowledge/upload', formData)
}

/**
 * Delete knowledge file
 */
export function deleteKnowledge(id: string): Promise<void> {
  return post(`/knowledge/delete/${id}`)
}

/**
 * Query knowledge base
 */
export function queryKnowledgeList(query: KnowledgeQueryBO): Promise<PageResult<Knowledge>> {
  return post('/knowledge/queryPageList', query)
}

/**
 * Get knowledge detail
 */
export function getKnowledgeDetail(id: string): Promise<Knowledge> {
  return get(`/knowledge/detail/${id}`)
}

/**
 * Download knowledge file
 */
export function downloadKnowledge(id: string, filename: string): Promise<void> {
  return download(`/knowledge/download/${id}`, filename)
}

/**
 * Reparse knowledge file
 */
export function reparseKnowledge(id: string): Promise<void> {
  return post(`/knowledge/reparse/${id}`)
}

/**
 * Add knowledge tag
 */
export function addKnowledgeTag(knowledgeId: string, tagName: string): Promise<void> {
  return post('/knowledge/addTag', null, { params: { knowledgeId, tagName } })
}

/**
 * Get knowledge file presigned URL
 */
export function getKnowledgeFileUrl(id: string): Promise<string> {
  return get(`/knowledge/url/${id}`)
}

/**
 * AI analyze knowledge document
 */
export function aiAnalyzeKnowledge(id: string): Promise<KnowledgeAiAnalyzeVO> {
  return post(`/knowledge/${id}/ai-analyze`)
}

/**
 * Ask AI about knowledge document (SSE streaming)
 */
export async function askKnowledgeQuestion(
  knowledgeId: string,
  question: string,
  history: Array<{ role: string; content: string }>,
  onChunk: (text: string) => void,
  onComplete?: () => void,
  onError?: (error: Error) => void
): Promise<void> {
  const token = getToken()
  const abortController = new AbortController()
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null
  let idleTimeoutId: ReturnType<typeof setTimeout> | null = null
  let isCompleted = false
  const IDLE_TIMEOUT_MS = 3000

  const cleanup = () => {
    if (idleTimeoutId) {
      clearTimeout(idleTimeoutId)
      idleTimeoutId = null
    }
    if (reader) {
      try { reader.releaseLock() } catch { /* ignore */ }
      reader = null
    }
  }

  const resetIdleTimeout = () => {
    if (idleTimeoutId) clearTimeout(idleTimeoutId)
    idleTimeoutId = setTimeout(() => {
      if (!isCompleted) {
        isCompleted = true
        abortController.abort()
      }
    }, IDLE_TIMEOUT_MS)
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}/knowledge/${knowledgeId}/ask`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Manager-Token': token } : {})
      },
      body: JSON.stringify({ question, history }),
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

    resetIdleTimeout()

    while (true) {
      let readResult: ReadableStreamReadResult<Uint8Array>
      try {
        readResult = await reader.read()
      } catch (e) {
        if (abortController.signal.aborted) break
        throw e
      }

      const { done, value } = readResult
      if (done) {
        if (buffer.trim()) {
          const parsedContent = parseSSEEvent(buffer)
          if (parsedContent) onChunk(parsedContent)
        }
        break
      }

      resetIdleTimeout()
      buffer += decoder.decode(value, { stream: true })

      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const event of events) {
        const parsedContent = parseSSEEvent(event)
        if (parsedContent) onChunk(parsedContent)
      }
    }

    isCompleted = true
    onComplete?.()
  } catch (error) {
    if (abortController.signal.aborted && isCompleted) {
      onComplete?.()
      return
    }
    onError?.(error as Error)
  } finally {
    cleanup()
  }
}

function parseSSEEvent(event: string): string | null {
  const lines = event.split('\n')
  const dataLines: string[] = []
  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('data:')) {
      const content = trimmed.slice(5)
      dataLines.push(content.startsWith(' ') ? content.slice(1) : content)
    }
  }
  if (dataLines.length === 0) return null
  return dataLines.join('\n')
}
