import http, { extractData } from './index'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string | null
  email: string | null
  phone: string | null
  gender: number | null
  bio: string | null
  location: string | null
  role: number | null
  status: number | null
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

/** 发送注册验证码 */
export function sendRegisterCode(email: string) {
  return http.post<string>('/user/register/code', { email }).then(extractData)
}

/** 注册 */
export function register(data: { email: string; password: string; nickname: string; code: string }) {
  return http.post<UserInfo>('/user/register', data).then(extractData)
}

/** 登录（支持邮箱或用户名） */
export function login(account: string, password: string) {
  return http.post<LoginResponse>('/user/login', { account, password }).then(extractData)
}

/** 退出登录 */
export function logout() {
  return http.post<string>('/user/logout').then(extractData)
}

/** 获取个人资料 */
export function getProfile() {
  return http.get<UserInfo>('/user/profile').then(extractData)
}

/** 修改个人资料 */
export function updateProfile(data: Partial<UserInfo>) {
  return http.put<UserInfo>('/user/profile', data).then(extractData)
}

/** 修改密码 */
export function changePassword(oldPassword: string, newPassword: string) {
  return http.put<string>('/user/password', { oldPassword, newPassword }).then(extractData)
}

/** 发送密码重置验证码（无需登录） */
export function sendResetPasswordCode(email: string) {
  return http.post<string>('/user/password/reset/code', { email }).then(extractData)
}

/** 通过验证码重置密码（无需登录） */
export function resetPassword(data: { email: string; code: string; newPassword: string }) {
  return http.post<string>('/user/password/reset', data).then(extractData)
}
