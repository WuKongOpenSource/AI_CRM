export const RELATION_CUSTOMER_VIEW_PERMISSION = 'customer:view'

export function canQueryRelationCustomers(hasPermission: (permission: string) => boolean): boolean {
  return hasPermission(RELATION_CUSTOMER_VIEW_PERMISSION)
}

export function shouldLoadRelationCustomerOptions(state: {
  canQueryCustomers: boolean
  loading: boolean
}): boolean {
  return state.canQueryCustomers && !state.loading
}

export function getRelationCustomerSelectPlaceholder(canQueryCustomers: boolean): string {
  return canQueryCustomers ? '搜索并选择客户' : '暂无客户权限'
}
