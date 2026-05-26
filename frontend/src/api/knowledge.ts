import { post, get, upload, download, getToken, getApiBaseUrl } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  Knowledge,
  KnowledgeQueryBO,
  KnowledgeAiAnalyzeVO,
  KnowledgeAiSearchBO,
  KnowledgeAiSearchVO,
  KnowledgePreviewTokenVO,
  KnowledgeTargetedScriptBO
} from '@/types/common'

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
 * Update knowledge related customer
 */
export function updateKnowledgeCustomer(knowledgeId: string, customerId?: string): Promise<void> {
  return post('/knowledge/updateCustomer', null, { params: { knowledgeId, customerId } })
}

/**
 * Get knowledge file content for frontend preview
 */
export function getKnowledgeFileBlob(id: string): Promise<Blob> {
  return get(`/knowledge/download/${id}`, { responseType: 'blob' })
}

/**
 * Create a short-lived Range preview URL for audio/video files
 */
export function getKnowledgePreviewToken(id: string): Promise<KnowledgePreviewTokenVO> {
  return post<KnowledgePreviewTokenVO>(`/knowledge/${id}/preview-token`).then(result => ({
    ...result,
    url: result.url.startsWith('/') ? `${getApiBaseUrl()}${result.url}` : result.url
  }))
}

/**
 * Get .doc file converted to HTML for preview
 */
export function getKnowledgePreviewHtml(id: string): Promise<string> {
  return get(`/knowledge/preview-html/${id}`)
}

/**
 * AI analyze knowledge document
 */
export function aiAnalyzeKnowledge(
  id: string,
  forceRefresh = false
): Promise<KnowledgeAiAnalyzeVO> {
  return post(`/knowledge/${id}/ai-analyze`, null, {
    params: { forceRefresh }
  })
}

/**
 * AI search across knowledge base and return summarized answer with references
 */
export function aiSearchKnowledge(query: KnowledgeAiSearchBO): Promise<KnowledgeAiSearchVO> {
  return post('/knowledge/ai-search', query)
}

/**
 * Stream targeted sales script content as it is generated
 */
export async function streamKnowledgeTargetedScript(
  data: KnowledgeTargetedScriptBO,
  onChunk: (text: string) => void,
  onComplete?: () => void,
  onError?: (error: Error) => void
): Promise<void> {
  const token = getToken()
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null

  const cleanup = () => {
    if (reader) {
      try { reader.releaseLock() } catch { /* ignore */ }
      reader = null
    }
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}/knowledge/targeted-script/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        ...(token ? { 'Manager-Token': token } : {})
      },
      body: JSON.stringify(data)
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
      const { done, value } = await reader.read()
      if (done) {
        if (buffer.trim()) {
          const parsedContent = parseSSEEvent(buffer)
          if (parsedContent) onChunk(parsedContent)
        }
        break
      }

      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const event of events) {
        const parsedContent = parseSSEEvent(event)
        if (parsedContent) onChunk(parsedContent)
      }
    }

    onComplete?.()
  } catch (error) {
    onError?.(error as Error)
    throw error
  } finally {
    cleanup()
  }
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
  let reader: ReadableStreamDefaultReader<Uint8Array> | null = null

  const cleanup = () => {
    if (reader) {
      try { reader.releaseLock() } catch { /* ignore */ }
      reader = null
    }
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}/knowledge/${knowledgeId}/ask`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        ...(token ? { 'Manager-Token': token } : {})
      },
      body: JSON.stringify({ question, history })
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
        if (buffer.trim()) {
          const parsedContent = parseSSEEvent(buffer)
          if (parsedContent) onChunk(parsedContent)
        }
        break
      }

      buffer += decoder.decode(value, { stream: true })

      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const event of events) {
        const parsedContent = parseSSEEvent(event)
        if (parsedContent) onChunk(parsedContent)
      }
    }

    onComplete?.()
  } catch (error) {
    onError?.(error as Error)
    throw error
  } finally {
    cleanup()
  }
}

function parseSSEEvent(event: string): string | null {
  const lines = event.split(/\r?\n/)
  const dataLines: string[] = []
  for (const line of lines) {
    if (line.startsWith('data:')) {
      const content = line.slice(5)
      dataLines.push(content.startsWith(' ') ? content.slice(1) : content)
    }
  }
  if (dataLines.length === 0) return null
  return dataLines.join('\n')
}
