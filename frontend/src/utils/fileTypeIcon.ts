export type FileTypeIconKind =
  | 'word'
  | 'excel'
  | 'powerpoint'
  | 'pdf'
  | 'image'
  | 'audio'
  | 'markdown'
  | 'text'
  | 'generic'

export interface FileTypeIconInput {
  fileName?: string | null
  mimeType?: string | null
  knowledgeType?: string | null
}

export interface FileTypeIconMeta {
  kind: FileTypeIconKind
  label: string
  shortLabel: string
}

function getFileExtension(fileName?: string | null): string {
  const normalized = String(fileName || '').trim().toLowerCase()
  const match = normalized.match(/\.([a-z0-9]+)(?:[?#].*)?$/)
  return match?.[1] || ''
}

export function resolveFileTypeIconMeta(input: FileTypeIconInput): FileTypeIconMeta {
  const extension = getFileExtension(input.fileName)
  const mime = String(input.mimeType || '').toLowerCase()
  const knowledgeType = String(input.knowledgeType || '').toLowerCase()

  if (mime.includes('pdf') || extension === 'pdf') {
    return { kind: 'pdf', label: 'PDF', shortLabel: 'PDF' }
  }
  if (mime.includes('word') || mime.includes('msword') || ['doc', 'docx'].includes(extension)) {
    return { kind: 'word', label: 'Word', shortLabel: 'W' }
  }
  if (
    mime.includes('excel')
    || mime.includes('spreadsheet')
    || mime.includes('csv')
    || ['xls', 'xlsx', 'csv'].includes(extension)
  ) {
    return { kind: 'excel', label: extension === 'csv' ? 'CSV' : 'Excel', shortLabel: extension === 'csv' ? 'CSV' : 'X' }
  }
  if (mime.includes('powerpoint') || mime.includes('presentation') || ['ppt', 'pptx'].includes(extension)) {
    return { kind: 'powerpoint', label: 'PowerPoint', shortLabel: 'P' }
  }
  if (mime.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp'].includes(extension)) {
    return { kind: 'image', label: '图片', shortLabel: 'IMG' }
  }
  if (mime.startsWith('audio/') || ['mp3', 'wav', 'm4a', 'aac', 'webm', 'ogg'].includes(extension) || knowledgeType === 'recording') {
    return { kind: 'audio', label: '音频', shortLabel: 'AUD' }
  }
  if (['md', 'markdown'].includes(extension)) {
    return { kind: 'markdown', label: 'Markdown', shortLabel: 'MD' }
  }
  if (mime.startsWith('text/') || ['txt', 'json', 'xml'].includes(extension)) {
    return { kind: 'text', label: extension ? extension.toUpperCase() : '文本', shortLabel: extension ? extension.toUpperCase() : 'TXT' }
  }

  return { kind: 'generic', label: '文档', shortLabel: 'DOC' }
}
