import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/api'
import router from '@/router'

const TOKEN_KEY = 'Manager-Token'

// 防止多个请求同时触发重复跳转
let isRedirecting = false

// Create axios instance
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || `${import.meta.env.VITE_BASE_PATH || '/'}crmapi`,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers[TOKEN_KEY] = token
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor
service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    // Handle blob responses (file downloads) - return data directly
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data

    // Success code is 0
    if (res.code === 0) {
      return res.data
    }

    // Authentication error
    if (res.code === 302 || res.code === 401) {
      // 防止多个请求同时触发跳转
      if (!isRedirecting) {
        isRedirecting = true
        ElMessage.error(res.msg || '登录已过期，请重新登录')
        localStorage.removeItem(TOKEN_KEY)

        // 保存当前路径，登录后返回
        const currentPath = router.currentRoute.value.fullPath
        router.push({
          path: '/login',
          query: currentPath !== '/' && currentPath !== '/login' ? { redirect: currentPath } : {}
        }).finally(() => {
          isRedirecting = false
        })
      }
      return Promise.reject(new Error(res.msg || '认证失败'))
    }

    // Other errors
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    console.error('Response error:', error)

    // 处理 HTTP 401 或响应体中 code 为 302/401 的情况（未登录）
    const responseData = error.response?.data
    const httpStatus = error.response?.status
    const businessCode = responseData?.code

    if (httpStatus === 401 || businessCode === 302 || businessCode === 401) {
      // 防止多个请求同时触发跳转
      if (!isRedirecting) {
        isRedirecting = true
        ElMessage.error(responseData?.msg || '登录已过期，请重新登录')
        localStorage.removeItem(TOKEN_KEY)

        // 保存当前路径，登录后返回
        const currentPath = router.currentRoute.value.fullPath
        router.push({
          path: '/login',
          query: currentPath !== '/' && currentPath !== '/login' ? { redirect: currentPath } : {}
        }).finally(() => {
          isRedirecting = false
        })
      }
      return Promise.reject(new Error(responseData?.msg || '认证失败'))
    }

    const message = responseData?.msg || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// Export request methods
export function get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config)
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config)
}

// For file uploads
export function upload<T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, formData, {
    ...config,
    headers: {
      'Content-Type': 'multipart/form-data',
      ...config?.headers
    }
  })
}

// For file downloads
export function download(url: string, filename?: string): Promise<void> {
  return service.get(url, { responseType: 'blob' }).then((response: any) => {
    const blob = new Blob([response])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  })
}

// Token management
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export default service
