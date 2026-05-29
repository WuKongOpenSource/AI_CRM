import { post, get, del } from '@/utils/request'
import type {
  ExternalAuthAuthorizeResult,
  ExternalAuthBinding,
  ExternalAuthProvider,
  ExternalAuthRegisterParams,
  ExternalAuthTicketLoginParams,
  LoginParams,
  LoginResult,
  UserInfo
} from '@/types/api'

/**
 * Login
 */
export function login(params: LoginParams): Promise<LoginResult> {
  return post<LoginResult>('/auth/login', params)
}

/**
 * Logout
 */
export function logout(): Promise<void> {
  return post('/auth/logout')
}

/**
 * Get current user info
 */
export function getUserInfo(): Promise<UserInfo> {
  return get<UserInfo>('/auth/userInfo')
}

/**
 * Get current login user detail info
 */
export function getLoginUserDetail(): Promise<UserInfo> {
  return post('/managerUser/queryLoginUser')
}

/**
 * Update user profile
 */
export function updateProfile(data: { userId: string | number; realname?: string; mobile?: string; email?: string; post?: string; img?: string }): Promise<void> {
  return post('/managerUser/updateUser', data)
}

/**
 * Change password
 */
export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return post('/managerUser/updatePassword', null, {
    params: { oldPassword, newPassword }
  })
}

/**
 * Query user list
 */
export function queryUserList(params?: { search?: string; deptId?: string | number; roleId?: string | number; page?: number; limit?: number }): Promise<any> {
  return post('/managerUser/queryPageList', { page: 1, limit: 100, ...params })
}

/**
 * Add user
 */
export function addUser(data: {
  username: string
  password: string
  realname: string
  mobile?: string
  email?: string
  deptId?: number | string
  post?: string
  parentId?: number | string
  status?: number
  roleIds?: (number | string)[]
}): Promise<void> {
  return post('/managerUser/addUser', data)
}

/**
 * Update user info
 */
export function updateUserInfo(data: {
  userId: number | string
  realname?: string
  mobile?: string
  email?: string
  deptId?: number | string
  post?: string
  sex?: number
  status?: number
  password?: string
  parentId?: number | string
  roleIds?: (number | string)[]
}): Promise<void> {
  return post('/managerUser/updateUser', data)
}

/**
 * Reset username
 */
export function resetUsername(data: {
  userId: number | string
  username: string
  currentPassword?: string
}): Promise<void> {
  return post('/managerUser/resetUsername', data)
}

/**
 * Delete users
 */
export function deleteUsers(userIds: (number | string)[]): Promise<void> {
  return post('/managerUser/deleteByIds', userIds)
}

/**
 * Register
 */
export interface RegisterParams {
  email: string
  password: string
  verificationCode: string
  companyName: string
  realname?: string
}

export function register(params: RegisterParams): Promise<string> {
  return post<string>('/auth/register', params)
}

export interface ResetPasswordParams {
  email: string
  password: string
  verificationCode: string
}

export function resetPassword(params: ResetPasswordParams): Promise<string> {
  return post<string>('/auth/reset-password', params)
}

export interface CaptchaData {
  originalImageBase64: string
  jigsawImageBase64: string
  token: string
  secretKey?: string
  captchaType?: string
}

export interface CheckCaptchaParams {
  token: string
  pointX: number
  pointY?: number
  secretKey?: string
  captchaType?: string
}

export interface CheckCaptchaResult {
  captchaVerification: string
}

export interface SendEmailCodeParams {
  email: string
  type: number
  captchaVerification: string
}

export function getCaptcha(): Promise<CaptchaData> {
  return post<CaptchaData>('/cloud/getCaptcha', {
    captchaType: 'blockPuzzle'
  })
}

export function checkCaptcha(params: CheckCaptchaParams): Promise<CheckCaptchaResult> {
  return post<CheckCaptchaResult>('/cloud/checkCaptcha', params)
}

export function sendEmailCode(params: SendEmailCodeParams): Promise<void> {
  return post('/cloud/sendEmail', params)
}

/**
 * 获取当前用户权限树
 */
export function getUserAuth(): Promise<Record<string, any>> {
  return post<Record<string, any>>('/managerRole/auth')
}

/**
 * Get OIDC session token for MinIO SSO
 */
export function getOidcSessionToken(): Promise<{ sessionToken: string }> {
  return get('/auth/oidc-session')
}

export function getExternalAuthProviders(): Promise<ExternalAuthProvider[]> {
  return get('/auth/external/providers')
}

export function getExternalAuthAuthorizeUrl(provider: string, redirect: string): Promise<ExternalAuthAuthorizeResult> {
  return get(`/auth/external/${provider}/authorize`, {
    params: { redirect }
  })
}

export function exchangeExternalLoginTicket(params: ExternalAuthTicketLoginParams): Promise<LoginResult> {
  return post('/auth/external/login-ticket', params)
}

export function completeExternalRegister(params: ExternalAuthRegisterParams): Promise<LoginResult> {
  return post('/auth/external/register', params)
}

export function getExternalAuthBindings(): Promise<ExternalAuthBinding[]> {
  return get('/auth/external/bindings')
}

export function getExternalBindAuthorizeUrl(provider: string, redirect: string): Promise<ExternalAuthAuthorizeResult> {
  return get(`/auth/external/${provider}/bind/authorize`, {
    params: { redirect }
  })
}

export function unbindExternalAuth(provider: string): Promise<void> {
  return del(`/auth/external/${provider}/binding`)
}
