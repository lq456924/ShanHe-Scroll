import axios from 'axios'
import type { AxiosResponse } from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 统一响应结构
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

// 请求拦截器：自动注入 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一错误处理
http.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const res = response.data
    if (res.code === 200) {
      return response
    }
    // 业务错误
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      // 触发全局登录失效事件（由 App.vue 监听弹出对话框）
      window.dispatchEvent(new CustomEvent('auth:expired'))
      return Promise.reject(new Error('登录已失效'))
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  },
)

// 从响应中提取 data
export function extractData<T>(response: AxiosResponse<ApiResult<T>>): T {
  return response.data.data
}

export default http
