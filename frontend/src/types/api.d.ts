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
  tenantId?: string
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
}
