import DOMPurify from 'dompurify'
import { marked } from 'marked'

const renderer = new marked.Renderer()

renderer.link = ({ href, title, tokens }) => {
  const text = tokens.map(token => token.raw).join('')
  const safeHref = href || '#'
  const titleAttr = title ? ` title="${title}"` : ''
  return `<a href="${safeHref}" target="_blank" rel="noopener noreferrer"${titleAttr}>${text}</a>`
}

marked.setOptions({
  gfm: true,
  breaks: true
})

export function renderMarkdown(content: string): string {
  if (!content) {
    return ''
  }

  const html = marked.parse(content, {
    async: false,
    renderer
  })

  return DOMPurify.sanitize(html, {
    USE_PROFILES: { html: true }
  })
}
