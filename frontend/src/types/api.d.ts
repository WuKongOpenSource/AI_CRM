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
  tenantId?: string
  loginType?: LoginType
}

export interface LoginTenantOption {
  tenantId: string
  tenantName: string
  realname?: string
}

export interface LoginResult {
  token?: string
  userInfo?: UserInfo
  requiresTenantSelection?: boolean
  tenantOptions?: LoginTenantOption[]
}

export type ExternalAuthProviderCode = 'google' | 'wechat' | 'wecom'

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
  realname?: string
  email?: string
  verificationCode?: string
  loginType?: LoginType
}

export interface ExternalAuthBinding {
  provider: ExternalAuthProviderCode
  providerName: string
  bound: boolean
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
  tenantId?: string | number
  tenantCreator?: boolean
}
