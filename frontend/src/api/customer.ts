import { post, get } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  CustomerListVO,
  CustomerDetailVO,
  CustomerAddBO,
  CustomerUpdateBO,
  CustomerQueryBO
} from '@/types/customer'

/**
 * Create customer
 */
export function addCustomer(data: CustomerAddBO): Promise<string> {
  return post('/customer/add', data)
}

/**
 * Update customer
 */
export function updateCustomer(data: CustomerUpdateBO): Promise<void> {
  return post('/customer/update', data)
}

/**
 * Delete customer
 */
export function deleteCustomer(id: string): Promise<void> {
  return post(`/customer/delete/${id}`)
}

/**
 * Query customers with pagination
 */
export function queryCustomerList(query: CustomerQueryBO): Promise<PageResult<CustomerListVO>> {
  return post('/customer/queryPageList', query)
}

/**
 * Get customer detail
 */
export function getCustomerDetail(id: string): Promise<CustomerDetailVO> {
  return get(`/customer/detail/${id}`)
}

/**
 * Update customer stage
 */
export function updateCustomerStage(customerId: string, stage: string): Promise<void> {
  return post('/customer/updateStage', null, { params: { customerId, stage } })
}

/**
 * Add customer tag
 */
export function addCustomerTag(customerId: string, tagName: string, color?: string): Promise<void> {
  return post('/customer/addTag', null, { params: { customerId, tagName, color } })
}

/**
 * Remove customer tag
 */
export function removeCustomerTag(customerId: string, tagId: string): Promise<void> {
  return post('/customer/removeTag', null, { params: { customerId, tagId } })
}

/**
 * Get customer statistics
 */
export function getCustomerStatistics(): Promise<any> {
  return get('/customer/statistics')
}
