export function parseDownloadFileName(contentDisposition = '', fallbackName = 'download.bin') {
  const match = /filename="([^"]+)"/i.exec(String(contentDisposition || ''))
  return match?.[1] || fallbackName
}

export function downloadBlobResponse(response, fallbackName = 'download.bin') {
  const blob = response?.data
  const fileName = parseDownloadFileName(response?.headers?.['content-disposition'], fallbackName)
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = fileName
  document.body.append(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(objectUrl)
  return fileName
}
