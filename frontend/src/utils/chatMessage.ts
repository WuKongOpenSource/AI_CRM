const KNOWLEDGE_RESPONSE_PATTERN =
  /(RAG知识库回答|检索结果|参考文件|参考文档|检索信息|Summary|References?)/i

const TRAILING_ELLIPSIS_PATTERN = /(?:\.{3,}|…{1,})\s*$/
const LINE_END_ELLIPSIS_PATTERN = /([^\n])(?:\.{3,}|…{1,})\s*(?=\n|$)/g

export function normalizeAssistantMessageContent(
  content: string | undefined | null,
  isStreaming = false
): string {
  const raw = String(content || '').replace(/\r\n?/g, '\n')
  if (!raw || isStreaming) {
    return raw
  }

  const normalized = raw.trimEnd()
  if (KNOWLEDGE_RESPONSE_PATTERN.test(normalized)) {
    return normalized.replace(LINE_END_ELLIPSIS_PATTERN, '$1（内容节选）')
  }

  return normalized.replace(TRAILING_ELLIPSIS_PATTERN, '').trimEnd()
}

export function getAssistantMessageStatusLabel(isStreaming = false): string {
  return isStreaming ? '正在生成' : '已完成'
}

export function getAssistantMessagePlaceholder(isStreaming = false): string {
  return isStreaming ? '思考中' : ''
}
