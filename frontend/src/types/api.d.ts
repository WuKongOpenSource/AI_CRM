// API Response types
export interface Result<T = any> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  list: T[]
  totalRow: number
  pageSize: number
  pageNumber: number
  extraData?: any
}

export interface PageQuery {
  pageNumber?: number
  pageSize?: number
}

// Auth types
export interface LoginParams {
  username: string
  password: string
  captchaVerification?: string
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

export type ExternalAuthProviderCode = 'google' | 'wechat' | 'outlook'

export interface ExternalAuthProvider {
  provider: ExternalAuthProviderCode
  name: string
  enabled: boolean
}

export interface ExternalAuthAuthorizeResult {
  provider: ExternalAuthProviderCode
  authorizeUrl: string
}

export interface ExternalAuthTicketLoginParams {
  ticket: string
}

export interface ExternalAuthBinding {
  provider: ExternalAuthProviderCode
  name: string
  subject: string
  email?: string
  displayName?: string
  avatarUrl?: string
  bindTime?: string
  lastLoginTime?: string
}

export interface UserInfo {
  userId: string
  username: string
  realname: string
  avatar?: string
  img?: string
  imgUrl?: string
  roles: string[]
  mobile?: string
  email?: string
  post?: string
  deptId?: number
  deptName?: string
  sex?: number
  preferences?: UserPreferences
}

export interface UserPreferences {
  sidebarModuleOrder?: string[]
}

export interface UserPreferenceUpdateParams {
  sidebarModuleOrder?: string[]
}
