export const CHAT_ATTACHMENT_ACCEPT =
  'image/*,audio/*,video/*,.png,.jpg,.jpeg,.gif,.webp,.bmp,.svg,.avif,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.json,.xml,.mp3,.wav,.m4a,.aac,.ogg,.oga,.opus,.flac,.weba,.mp4,.webm,.mov,.m4v,.avi,.mkv,.ogv,.3gp'
export const MAX_CHAT_ATTACHMENT_SIZE = 50 * 1024 * 1024
export const MAX_CHAT_ATTACHMENT_COUNT = 5

const MIME_EXTENSION_MAP: Record<string, string> = {
  'application/json': 'json',
  'application/msword': 'doc',
  'application/pdf': 'pdf',
  'application/vnd.ms-excel': 'xls',
  'application/vnd.ms-powerpoint': 'ppt',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'pptx',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'xlsx',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'docx',
  'application/xml': 'xml',
  'audio/aac': 'aac',
  'audio/amr': 'amr',
  'audio/flac': 'flac',
  'audio/mp3': 'mp3',
  'audio/mp4': 'm4a',
  'audio/mpeg': 'mp3',
  'audio/ogg': 'ogg',
  'audio/opus': 'opus',
  'audio/wav': 'wav',
  'audio/webm': 'weba',
  'audio/x-wav': 'wav',
  'image/avif': 'avif',
  'image/bmp': 'bmp',
  'image/gif': 'gif',
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/svg+xml': 'svg',
  'image/webp': 'webp',
  'text/csv': 'csv',
  'text/markdown': 'md',
  'text/plain': 'txt',
  'text/xml': 'xml',
  'video/3gpp': '3gp',
  'video/mp4': 'mp4',
  'video/ogg': 'ogv',
  'video/quicktime': 'mov',
  'video/webm': 'webm',
  'video/x-m4v': 'm4v',
  'video/x-matroska': 'mkv',
  'video/x-msvideo': 'avi'
}

function resolveFileExtension(file: File): string {
  if (file.name.includes('.')) {
    return file.name.split('.').pop()?.toLowerCase() || 'bin'
  }

  if (file.type in MIME_EXTENSION_MAP) {
    return MIME_EXTENSION_MAP[file.type]
  }

  if (file.type.startsWith('image/')) {
    return file.type.slice('image/'.length) || 'png'
  }

  if (file.type.startsWith('audio/')) {
    return file.type.slice('audio/'.length) || 'mp3'
  }

  if (file.type.startsWith('video/')) {
    return file.type.slice('video/'.length) || 'mp4'
  }

  return 'bin'
}

function normalizeChatFile(file: File): File {
  if (file.name.trim()) {
    return file
  }

  const prefix = file.type.startsWith('image/')
    ? 'clipboard-image'
    : file.type.startsWith('audio/')
      ? 'clipboard-audio'
      : file.type.startsWith('video/')
        ? 'clipboard-video'
        : 'clipboard-file'
  const extension = resolveFileExtension(file)

  return new File([file], `${prefix}-${Date.now()}.${extension}`, {
    type: file.type || 'application/octet-stream',
    lastModified: file.lastModified || Date.now()
  })
}

export function extractClipboardFiles(event: ClipboardEvent): File[] {
  const clipboardFiles = Array.from(event.clipboardData?.files || [])
  if (clipboardFiles.length > 0) {
    return clipboardFiles.map(normalizeChatFile)
  }

  const items = event.clipboardData?.items
  if (!items) {
    return []
  }

  const files: File[] = []
  for (let index = 0; index < items.length; index += 1) {
    const item = items[index]
    if (item.kind !== 'file') {
      continue
    }

    const file = item.getAsFile()
    if (file) {
      files.push(normalizeChatFile(file))
    }
  }

  return files
}

export function mergeChatFiles(existingFiles: File[], incomingFiles: File[]): { files: File[]; error?: string } {
  if (incomingFiles.length === 0) {
    return { files: existingFiles }
  }

  const normalizedFiles = incomingFiles.map(normalizeChatFile)

  if (existingFiles.length + normalizedFiles.length > MAX_CHAT_ATTACHMENT_COUNT) {
    return {
      files: existingFiles,
      error: `最多只能上传${MAX_CHAT_ATTACHMENT_COUNT}个文件`
    }
  }

  const oversizedFile = normalizedFiles.find((file) => file.size > MAX_CHAT_ATTACHMENT_SIZE)
  if (oversizedFile) {
    return {
      files: existingFiles,
      error: `文件"${oversizedFile.name}"超过50MB限制`
    }
  }

  return {
    files: [...existingFiles, ...normalizedFiles]
  }
}
