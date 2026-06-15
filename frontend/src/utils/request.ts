import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import { h } from 'vue'
import type { Result } from '@/types/api'
import router from '@/router'
import { isRequestErrorHandled, markRequestErrorHandled } from '@/utils/requestError'

const TOKEN_KEY = 'Manager-Token'
const DEFAULT_NOT_LOGIN_MESSAGE = '登录已过期，请重新登录'
const KICKOUT_NOTICE_PATTERN = /当前用户于(.+?)在其他IP(?:\((.+?)\))?登录，当前登录已被退出/

type KickoutNotice = {
  time: string
  ip: string
}

type WkAxiosRequestConfig = AxiosRequestConfig & {
  silentError?: boolean
}

export function getApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL
  if (typeof raw !== 'string') return ''
  const trimmed = raw.trim()
  if (!trimmed) return ''
  return trimmed.replace(/\/$/, '')
}

let isRedirecting = false
let isPermissionAlertVisible = false
let isNotLoginAlertVisible = false
let errorMessageTimer: ReturnType<typeof setTimeout> | null = null
let pendingErrorMessage = ''

const ERROR_MESSAGE_DEBOUNCE_MS = 120

const service: AxiosInstance = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 120000,
  headers: {
    'Content-Type': 'application/json'
  }
})

function showErrorMessage(message?: string): void {
  pendingErrorMessage = typeof message === 'string' && message.trim() ? message.trim() : '请求失败'

  if (errorMessageTimer) {
    clearTimeout(errorMessageTimer)
  }

  errorMessageTimer = setTimeout(() => {
    ElMessage.error(pendingErrorMessage)
    errorMessageTimer = null
  }, ERROR_MESSAGE_DEBOUNCE_MS)
}

function shouldSilenceError(config?: AxiosRequestConfig): boolean {
  return Boolean((config as WkAxiosRequestConfig | undefined)?.silentError)
}

function parseKickoutNotice(message: string): KickoutNotice | null {
  const match = message.trim().match(KICKOUT_NOTICE_PATTERN)
  if (!match) {
    return null
  }

  return {
    time: match[1]?.trim() || '--',
    ip: match[2]?.trim() || '--'
  }
}

function shouldShowLoginNoticeDialog(message: string): boolean {
  return message.includes('当前登录已被退出')
}

function buildKickoutDialogMessage(message: string) {
  const notice = parseKickoutNotice(message)

  const buildInfoRow = (label: string, value: string) =>
    h('div', { class: 'account-kickout-dialog__info-row' }, [
      h('span', { class: 'account-kickout-dialog__info-label' }, label),
      h('span', { class: 'account-kickout-dialog__info-value' }, value || '--')
    ])

  return h('div', { class: 'account-kickout-dialog__content' }, [
    h('div', { class: 'account-kickout-dialog__icon-shell' }, [
      h('div', { class: 'account-kickout-dialog__icon-ring' }, [
        h(WarningFilled, { class: 'account-kickout-dialog__icon' })
      ])
    ]),
    h('h3', { class: 'account-kickout-dialog__title' }, '账号下线通知'),
    h(
      'p',
      { class: 'account-kickout-dialog__desc' },
      '您的账号已在其他设备登录。如非本人操作，请及时修改密码。'
    ),
    h('div', { class: 'account-kickout-dialog__info' }, [
      buildInfoRow('下线时间', notice?.time || '--'),
      buildInfoRow('异地 IP', notice?.ip || '--')
    ])
  ])
}

function handleNotLogin(message?: string): Promise<never> {
  const resolvedMessage =
    typeof message === 'string' && message.trim() ? message.trim() : DEFAULT_NOT_LOGIN_MESSAGE

  if (!isRedirecting) {
    isRedirecting = true
    localStorage.removeItem(TOKEN_KEY)

    const currentPath = router.currentRoute.value.fullPath
    const redirectToLogin = () =>
      router
        .push({
          path: '/login',
          query: currentPath !== '/' && currentPath !== '/login' ? { redirect: currentPath } : {}
        })
        .finally(() => {
          isRedirecting = false
        })

    if (shouldShowLoginNoticeDialog(resolvedMessage)) {
      if (!isNotLoginAlertVisible) {
        isNotLoginAlertVisible = true
        ElMessageBox({
          title: '',
          message: buildKickoutDialogMessage(resolvedMessage),
          customClass: 'account-kickout-dialog',
          modalClass: 'account-kickout-dialog__overlay',
          showClose: false,
          closeOnClickModal: false,
          closeOnPressEscape: false,
          closeOnHashChange: false,
          showCancelButton: false,
          confirmButtonText: '重新登录',
          confirmButtonClass: 'account-kickout-dialog__confirm',
          autofocus: false
        }).finally(() => {
          isNotLoginAlertVisible = false
          void redirectToLogin()
        })
      }
    } else {
      showErrorMessage(resolvedMessage)
      void redirectToLogin()
    }
  }

  return Promise.reject(markRequestErrorHandled(new Error(resolvedMessage || '认证失败')))
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
  return Promise.reject(markRequestErrorHandled(new Error(message || '无权操作')))
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

    if (!shouldSilenceError(response.config)) {
      showErrorMessage(res.msg || '请求失败')
    }
    return Promise.reject(markRequestErrorHandled(new Error(res.msg || '请求失败')))
  },
  (error) => {
    if (isRequestErrorHandled(error)) {
      return Promise.reject(error)
    }

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
    if (!shouldSilenceError(error.config)) {
      showErrorMessage(message)
    }
    markRequestErrorHandled(error)
    return Promise.reject(error)
  }
)

export function get<T = any>(url: string, config?: WkAxiosRequestConfig): Promise<T> {
  return service.get(url, config)
}

export function post<T = any>(url: string, data?: any, config?: WkAxiosRequestConfig): Promise<T> {
  return service.post(url, data, config)
}

export function put<T = any>(url: string, data?: any, config?: WkAxiosRequestConfig): Promise<T> {
  return service.put(url, data, config)
}

export function del<T = any>(url: string, config?: WkAxiosRequestConfig): Promise<T> {
  return service.delete(url, config)
}

export function upload<T = any>(url: string, formData: FormData, config?: WkAxiosRequestConfig): Promise<T> {
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
