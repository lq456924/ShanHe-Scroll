import http, { extractData } from './index'

export interface AlbumPhoto {
  id: number
  albumId: number
  url: string
  description: string | null
  sortOrder: number
  createdAt: string
}

export interface Album {
  id: number
  userId: number
  regionId: number | null
  title: string
  description: string | null
  coverImage: string | null
  visibility: number
  createdAt: string
  updatedAt: string
  photos: AlbumPhoto[]
  photoCount: number | null
}

/** 我的相册列表 */
export function getMyAlbums() {
  return http.get<Album[]>('/album').then(extractData)
}

/** 创建相册 */
export function createAlbum(data: {
  regionId: number | null
  title: string
  description?: string
  coverImage?: string
}) {
  return http.post<Album>('/album', data).then(extractData)
}

/** 相册详情（含全部照片） */
export function getAlbumDetail(id: number) {
  return http.get<Album>(`/album/${id}`).then(extractData)
}

/** 添加照片 */
export function addPhoto(albumId: number, url: string, description?: string) {
  return http.post(`/album/${albumId}/photo`, { url, description }).then(extractData)
}

/** 更新照片描述 */
export function updatePhotoDesc(albumId: number, photoId: number, description: string) {
  return http.put(`/album/${albumId}/photo/${photoId}`, { description }).then(extractData)
}

/** 删除照片 */
export function deletePhoto(albumId: number, photoId: number) {
  return http.delete<string>(`/album/${albumId}/photo/${photoId}`).then(extractData)
}

/** 删除相册 */
export function deleteAlbum(id: number) {
  return http.delete<string>(`/album/${id}`).then(extractData)
}
