// 将模型返回的 Markdown/HTML 转为可展示内容，并清理脚本和危险链接。
export const escapeHtml = (value) => String(value)
  .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;').replace(/'/g, '&#39;')

export const sanitizeHtml = (html) => {
  const template = document.createElement('template')
  template.innerHTML = html
  template.content.querySelectorAll('script,style,iframe,object,embed,form,link,meta,svg,math').forEach(node => node.remove())
  template.content.querySelectorAll('*').forEach(node => {
    Array.from(node.attributes).forEach(attribute => {
      if (attribute.name.toLowerCase().startsWith('on')) node.removeAttribute(attribute.name)
      if ((attribute.name === 'href' || attribute.name === 'src') && /^(javascript:|data:)/i.test(attribute.value.trim())) node.removeAttribute(attribute.name)
    })
  })
  return template.innerHTML
}

export const renderMarkdown = (source) => {
  if (!source) return ''
  let text = String(source).replace(/\r\n/g, '\n')
  const codeBlocks = []
  text = text.replace(/```([\w-]*)\n?([\s\S]*?)```/g, (_, language, code) => {
    const index = codeBlocks.push(`<pre class="rich-code"><code${language ? ` data-language="${escapeHtml(language)}"` : ''}>${escapeHtml(code.trimEnd())}</code></pre>`) - 1
    return `\n@@CODE_BLOCK_${index}@@\n`
  })
  text = text.replace(/^### (.+)$/gm, '<h5>$1</h5>').replace(/^## (.+)$/gm, '<h4>$1</h4>').replace(/^# (.+)$/gm, '<h3>$1</h3>')
  text = text.replace(/(?:^\|[^\n]+\|\n?)+/gm, block => {
    const rows = block.trim().split('\n').map(row => row.trim()).filter(Boolean)
    if (rows.length < 2 || !/^\|?\s*:?-{2,}/.test(rows[1])) return block
    const cells = row => row.replace(/^\|/, '').replace(/\|$/, '').split('|').map(cell => cell.trim())
    const header = cells(rows[0])
    const body = rows.slice(2).map(row => cells(row))
    return `<div class="rich-table-scroll"><table><thead><tr>${header.map(cell => `<th>${cell}</th>`).join('')}</tr></thead><tbody>${body.map(row => `<tr>${row.map(cell => `<td>${cell}</td>`).join('')}</tr>`).join('')}</tbody></table></div>\n`
  })
  text = text.replace(/^[-*] (.+)$/gm, '<li>$1</li>').replace(/^(\d+)\. (.+)$/gm, '<li value="$1">$2</li>')
  text = text.replace(/((?:<li(?: value="\d+")?>.*?<\/li>\n?)+)/g, list => {
    const ordered = /<li value="\d+">/.test(list)
    return `<${ordered ? 'ol' : 'ul'}>${list.replace(/\n/g, '')}</${ordered ? 'ol' : 'ul'}>`
  })
  text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>').replace(/__([^_]+?)__/g, '<strong>$1</strong>')
    .replace(/`([^`\n]+)`/g, '<code class="rich-inline-code">$1</code>')
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
  text = text.split(/\n{2,}/).map(block => {
    if (/^<(h[3-5]|pre|ul|ol|li)/.test(block.trim()) || block.includes('@@CODE_BLOCK_')) return block
    return block.trim() ? `<p>${block.replace(/\n/g, '<br>')}</p>` : ''
  }).join('')
  text = text.replace(/@@CODE_BLOCK_(\d+)@@/g, (_, index) => codeBlocks[Number(index)] || '')
  return sanitizeHtml(text)
}
