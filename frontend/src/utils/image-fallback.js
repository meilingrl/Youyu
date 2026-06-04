export function fallbackImage(label = 'Youyu') {
  const text = String(label || 'Youyu').trim().slice(0, 10) || 'Youyu'
  const svg = `
<svg xmlns="http://www.w3.org/2000/svg" width="640" height="640" viewBox="0 0 640 640">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#f6efe5"/>
      <stop offset="0.55" stop-color="#dbe9df"/>
      <stop offset="1" stop-color="#f2d5bf"/>
    </linearGradient>
  </defs>
  <rect width="640" height="640" fill="url(#bg)"/>
  <rect x="54" y="54" width="532" height="532" rx="38" fill="rgba(255,255,255,0.52)" stroke="rgba(50,91,63,0.18)" stroke-width="2"/>
  <circle cx="226" cy="244" r="54" fill="rgba(201,93,49,0.22)"/>
  <path d="M112 504l120-148 82 84 70-92 124 156H112z" fill="rgba(50,91,63,0.28)"/>
  <text x="320" y="560" text-anchor="middle" font-family="Arial, sans-serif" font-size="34" font-weight="700" fill="#325b3f">${escapeSvg(text)}</text>
</svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

export function handleImageFallback(event, label) {
  const image = event?.target
  if (!image || image.dataset.fallbackApplied === 'true') {
    return
  }
  image.dataset.fallbackApplied = 'true'
  image.src = fallbackImage(label || image.alt || 'Youyu')
}

function escapeSvg(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
