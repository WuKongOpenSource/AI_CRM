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
}

export interface PageQuery {
  pageNumber?: number
  pageSize?: number
}

// Auth types
export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
}

export interface UserInfo {
  userId: string
  username: string
  realname: string
  avatar?: string
  roles: string[]
  mobile?: string
  email?: string
  post?: string
  deptId?: number
  sex?: number
}
