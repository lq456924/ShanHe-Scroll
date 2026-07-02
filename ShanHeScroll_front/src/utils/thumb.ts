/** 根据原图 URL 计算缩略图 URL */
export function thumbUrl(url: string | null | undefined): string | undefined {
  if (!url) return undefined
  const i = url.lastIndexOf('/')
  if (i < 0) return url
  return url.substring(0, i) + '/thumb_' + url.substring(i + 1)
}
