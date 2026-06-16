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
export type LoginType = 'PC' | 'MOBILE'

export interface LoginParams {
  username: string
  password: string
  loginType?: LoginType
  captchaVerification?: string
}

export interface LoginResult {
  token?: string
  userInfo?: UserInfo
}

export interface UserPreferences {
  sidebarModuleOrder?: string[]
}

export interface UserPreferenceUpdateParams {
  sidebarModuleOrder?: string[]
}

export type ExternalAuthProviderCode = 'google' | 'outlook' | 'wechat'

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
  loginType?: LoginType
}

export interface ExternalAuthRegisterParams {
  ticket: string
  companyName?: string
  password?: string
  loginType?: LoginType
}

export interface ExternalAuthBinding {
  provider: ExternalAuthProviderCode
  providerName: string
  bound: boolean
  subject?: string
  displayName?: string
  email?: string
  bindTime?: string
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
