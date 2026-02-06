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
 * Get OIDC session token for MinIO SSO
 */
export function getOidcSessionToken(): Promise<{ sessionToken: string }> {
  return get('/auth/oidc-session')
}
