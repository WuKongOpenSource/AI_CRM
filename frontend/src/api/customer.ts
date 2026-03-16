import { post, get, upload } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  CustomerListVO,
  CustomerDetailVO,
  CustomerAddBO,
  CustomerUpdateBO,
  CustomerQueryBO,
  CustomerImportPreview,
  CustomerImportRow,
  CustomerImportResult
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
 * Transfer customer to new owner
 */
export function transferCustomer(customerIds: string[], newOwnerId: string): Promise<void> {
  return post('/customer/transfer', { customerIds, newOwnerId })
}

/**
 * Get customer statistics
 */
export function getCustomerStatistics(): Promise<any> {
  return get('/customer/statistics')
}

/**
 * Export customers to Excel
 */
export function exportCustomers(data: { customerIds?: string[], keyword?: string, stage?: string, level?: string }): Promise<Blob> {
  return post('/customer/export', data, { responseType: 'blob' })
}

/**
 * Download import template
 */
export function downloadImportTemplate(): Promise<Blob> {
  return get('/customer/import/template', { responseType: 'blob' })
}

/**
 * Import preview (upload Excel, parse + validate + detect duplicates)
 */
export function importCustomerPreview(file: File): Promise<CustomerImportPreview> {
  const formData = new FormData()
  formData.append('file', file)
  return upload('/customer/import/preview', formData)
}

/**
 * Confirm import
 */
export function confirmCustomerImport(rows: CustomerImportRow[]): Promise<CustomerImportResult> {
  return post('/customer/import/confirm', rows)
}

/**
 * AI smart input - parse customer info
 */
export interface CustomerAiParseBO {
  content: string
  imageObjectKey?: string
  imageMimeType?: string
}

export interface CustomerAiParseVO {
  companyName?: string
  industry?: string
  level?: string
  stage?: string
  source?: string
  remark?: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  contactPosition?: string
  score?: number
  tags?: string[]
  summary?: string
  nextStep?: string
  keyPoints?: string[]
}

export function aiParseCustomer(data: CustomerAiParseBO): Promise<CustomerAiParseVO> {
  return post('/customer/ai-parse', data)
}
