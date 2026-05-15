/**
 * 将字节数格式化为可读文案。入参为原始字节数（B），可为接口返回的 `number` 或数字字符串。
 */
export function formatFileSize(bytes: number | string | undefined | null): string {
  const b = resolveKnowledgeFileSizeBytes(bytes)
  if (!Number.isFinite(b) || b < 0) return '0 B'
  const n = Math.floor(b)
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * 知识库 query 等接口返回的 `fileSize` 单位为字节（B）；部分场景可能为数字字符串。
 */
export function resolveKnowledgeFileSizeBytes(raw: unknown): number {
  if (raw === null || raw === undefined) return 0
  if (typeof raw === 'number' && Number.isFinite(raw)) return Math.max(0, Math.floor(raw))
  if (typeof raw === 'string' && raw.trim() !== '') {
    const n = Number(raw)
    if (Number.isFinite(n)) return Math.max(0, Math.floor(n))
  }
  return 0
}
