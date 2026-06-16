export type MailOptimizeCommand = 'professional' | 'polite' | 'concise' | 'sales'

type MailOptimizeOption = {
  label: string
  instruction: string
}

const MAIL_OPTIMIZE_OPTIONS: Record<MailOptimizeCommand, MailOptimizeOption> = {
  professional: {
    label: '专业化润色',
    instruction: '更专业的商务语气',
  },
  polite: {
    label: '更礼貌',
    instruction: '更礼貌、克制、得体的表达',
  },
  concise: {
    label: '更简洁',
    instruction: '更简洁',
  },
  sales: {
    label: '更强销售转化',
    instruction: '更强调客户价值、明确下一步行动，并提升销售转化',
  },
}

export function getMailOptimizeLabel(command: string): string {
  return resolveMailOptimizeOption(command).label
}

export function buildMailOptimizePrompt(command: string, bodyHtml: string, subject?: string): string {
  const option = resolveMailOptimizeOption(command)
  const subjectLine = subject?.trim() ? `邮件主题：${subject.trim()}` : ''
  return [
    '请优化下面这封邮件的正文。',
    `优化目标：${option.instruction}`,
    '要求：',
    '1. 只返回优化后的邮件正文 HTML，不要返回解释、标题、Markdown 代码块或操作提示语。',
    '2. 保留原始含义、事实、称呼、签名、变量占位符和必要的 HTML 结构。',
    '3. 如果原文是纯文本，也请使用简单的 <p>、<br>、<ul><li>...</li></ul> 表示。',
    subjectLine,
    '原始邮件正文 HTML：',
    bodyHtml.trim(),
  ].filter(Boolean).join('\n')
}

export function normalizeAiOptimizedMailBody(response: string): string {
  let value = stripMarkdownFence(response.trim())
  value = extractBodyContent(value)
  value = stripOptimizationIntro(value).trim()
  if (!value) return ''
  if (looksLikeHtml(value)) return value
  return plainTextToHtml(value)
}

export function resolveMailOptimizeFailureMessage(response: string): string | null {
  const value = stripMarkdownFence(response.trim())
  const plainText = stripHtmlTags(value).trim()
  if (!plainText) return null
  const knownFailurePrefixes = [
    '请先在系统设置',
    '当前 AI 模型不可用',
    'AI 服务认证失败',
    'AI 服务暂时无法处理请求',
    'AI 请求参数有误',
    'AI 服务商暂时不可用',
    'AI 服务返回错误',
    'AI 服务暂时不可用',
    '抱歉，处理您的请求时发生错误',
  ]
  return knownFailurePrefixes.some(prefix => plainText.startsWith(prefix)) ? plainText : null
}

function resolveMailOptimizeOption(command: string): MailOptimizeOption {
  return MAIL_OPTIMIZE_OPTIONS[command as MailOptimizeCommand] || MAIL_OPTIMIZE_OPTIONS.professional
}

function stripMarkdownFence(value: string): string {
  const match = value.match(/^```(?:html)?\s*([\s\S]*?)\s*```$/i)
  return match ? match[1].trim() : value
}

function extractBodyContent(value: string): string {
  const match = value.match(/<body[^>]*>([\s\S]*?)<\/body>/i)
  return match ? match[1].trim() : value
}

function stripOptimizationIntro(value: string): string {
  const introPatterns = [
    /^以下内容(?:已)?按[^：:\n]{0,40}[：:]\s*/i,
    /^以下是(?:优化后的)?邮件正文(?:：|:)?\s*/i,
    /^优化后的邮件(?:正文|内容)(?:如下)?[：:]?\s*/i,
    /^Here is (?:the )?optimized email body:?[\s\n]*/i,
  ]
  let next = value
  let changed = true
  while (changed) {
    changed = false
    for (const pattern of introPatterns) {
      if (pattern.test(next)) {
        next = next.replace(pattern, '').trimStart()
        changed = true
      }
    }
  }
  return next
}

function looksLikeHtml(value: string): boolean {
  return /<\/?[a-z][\s\S]*>/i.test(value)
}

function stripHtmlTags(value: string): string {
  return value.replace(/<[^>]+>/g, ' ').replace(/&nbsp;/g, ' ')
}

function plainTextToHtml(value: string): string {
  return value
    .split(/\n{2,}/)
    .map(paragraph => paragraph.trim())
    .filter(Boolean)
    .map(paragraph => `<p>${escapeHtml(paragraph).replace(/\n/g, '<br>')}</p>`)
    .join('')
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
