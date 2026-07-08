import http, { extractData } from './index'

// ---- 用户管理 ----

export function getUsers() {
  return http.get('/admin/users').then(extractData)
}

export function createUser(data: { username: string; password: string; nickname?: string; email?: string; role?: number }) {
  return http.post('/admin/users', data).then(extractData)
}

export function resetPassword(id: number, password: string) {
  return http.put(`/admin/users/${id}/reset-password`, { password }).then(extractData)
}

export function banUser(id: number) {
  return http.put(`/admin/users/${id}/ban`).then(extractData)
}

export function unbanUser(id: number) {
  return http.put(`/admin/users/${id}/unban`).then(extractData)
}

export function updateUserRole(id: number, role: number) {
  return http.put(`/admin/users/${id}/role`, { role }).then(extractData)
}

export function deleteUser(id: number) {
  return http.delete(`/admin/users/${id}`).then(extractData)
}

// ---- 漂流瓶审核 ----

export function getPendingBottles() {
  return http.get('/admin/bottles/pending').then(extractData)
}

export function approveBottle(id: number) {
  return http.put(`/admin/bottles/${id}/approve`).then(extractData)
}

export function rejectBottle(id: number) {
  return http.put(`/admin/bottles/${id}/reject`).then(extractData)
}

// ---- 景点审核 ----

export function getPendingAttractions() {
  return http.get('/admin/attractions/pending').then(extractData)
}

export function approveAttraction(id: number) {
  return http.put(`/admin/attractions/${id}/approve`).then(extractData)
}

export function rejectAttraction(id: number) {
  return http.put(`/admin/attractions/${id}/reject`).then(extractData)
}
