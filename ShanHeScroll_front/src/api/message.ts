import http, { extractData } from './index'

export interface MessageItem {
  id: number
  userId: number
  type: string
  title: string
  content: string
  relatedId: number | null
  isRead: boolean
  createdAt: string
}

export function getMessages() {
  return http.get<MessageItem[]>('/messages').then(extractData)
}

export function getUnreadCount() {
  return http.get<{ count: number }>('/messages/unread-count').then(extractData)
}

export function markAllAsRead() {
  return http.put('/messages/read-all').then(extractData)
}

export function markAsRead(id: number) {
  return http.put(`/messages/${id}/read`).then(extractData)
}
