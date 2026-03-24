import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Result } from '@/types/api'
import router from '@/router'

const TOKEN_KEY = 'Manager-Token'

/** 未配置、仅空白或显式空字符串时返回 ''，axios 将按相对当前页面的路径发请求 */
export function getApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL
  if (typeof raw !== 'string') return ''
  const trimmed = raw.trim()
  if (!trimmed) return ''
  return trimmed.replace(/\/$/, '')
}

let isRedirecting = false
let isPermissionAlertVisible = false

const service: AxiosInstance = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

function handleNotLogin(message?: string): Promise<never> {
  if (!isRedirecting) {
    isRedirecting = true
    ElMessage.error(message || '登录已过期，请重新登录')
    localStorage.removeItem(TOKEN_KEY)

    const currentPath = router.currentRoute.value.fullPath
    router.push({
      path: '/login',
      query: currentPath !== '/' && currentPath !== '/login' ? { redirect: currentPath } : {}
    }).finally(() => {
      isRedirecting = false
    })
  }
  return Promise.reject(new Error(message || '认证失败'))
}

function handleNoPermission(message?: string): Promise<never> {
  if (!isPermissionAlertVisible) {
    isPermissionAlertVisible = true
    ElMessageBox.alert(message || '无权操作', '提示', {
      confirmButtonText: '知道了',
      type: 'warning'
    }).finally(() => {
      isPermissionAlertVisible = false
    })
  }
  return Promise.reject(new Error(message || '无权操作'))
}

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

service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data
    if (res.code === 0) {
      return res.data
    }

    if (res.code === 302) {
      return handleNotLogin(res.msg)
    }

    if (res.code === 401) {
      return handleNoPermission(res.msg)
    }

    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    console.error('Response error:', error)

    const responseData = error.response?.data
    const httpStatus = error.response?.status
    const businessCode = responseData?.code

    if (businessCode === 302) {
      return handleNotLogin(responseData?.msg)
    }

    if (businessCode === 401) {
      return handleNoPermission(responseData?.msg)
    }

    if (httpStatus === 401) {
      return handleNotLogin(responseData?.msg)
    }

    const message = responseData?.msg || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

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

export function upload<T = any>(url: string, formData: FormData, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, formData, {
    ...config,
    headers: {
      'Content-Type': 'multipart/form-data',
      ...config?.headers
    }
  })
}

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
