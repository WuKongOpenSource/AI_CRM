import { post, get } from '@/utils/request'
import type { LoginParams, LoginResult, UserInfo } from '@/types/api'

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
export function updateProfile(data: { realname?: string; mobile?: string }): Promise<void> {
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
  roleIds?: (number | string)[]
}): Promise<void> {
  return post('/managerUser/updateUser', data)
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

/**
 * Get OIDC session token for MinIO SSO
 */
export function getOidcSessionToken(): Promise<{ sessionToken: string }> {
  return get('/auth/oidc-session')
}
